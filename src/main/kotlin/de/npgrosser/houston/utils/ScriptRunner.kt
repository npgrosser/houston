package de.npgrosser.houston.utils

import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream
import java.util.*


data class ScriptResult(val exitCode: Int, val stdOutput: String, val errOutput: String)
interface ScriptRunner {
    fun run(scriptContent: String): ScriptResult

    companion object {
        fun defaultForSystem(): ScriptRunner = when (System.getProperty("os.name").lowercase(Locale.ROOT)) {
            "windows" -> PowerShellScriptRunner()
            else -> BashScriptRunner()
        }
    }
}

open class ShellScriptRunner(
    private val shell: String,
    private val printStdOut: Boolean = true,
    private val printErrOut: Boolean = true
) : ScriptRunner {

    override fun run(scriptContent: String): ScriptResult {
        val tmpScriptFile = File.createTempFile("houston", ".${shell}").apply {
            deleteOnExit()
            val content = if (scriptContent.startsWith("#!")) {
                scriptContent
            } else {
                "#!/usr/bin/env $shell".trimIndent() + System.lineSeparator() + scriptContent
            }
            writeText(content)
            setExecutable(true)
        }

        try {
            val process = ProcessBuilder(shell, tmpScriptFile.absolutePath).start()

            val (stdOut, errOut) = runBlocking { collectProcessOutput(process, printStdOut, printErrOut) }

            process.waitFor()

            return ScriptResult(process.exitValue(), stdOut, errOut)
        } finally {
            if (!tmpScriptFile.delete()) {
                printWarning("Could not delete temporary script file ${tmpScriptFile.absolutePath}")
            }
        }
    }
}

private suspend fun collectProcessOutput(
    process: Process,
    printStdOut: Boolean,
    printErrOut: Boolean
): Pair<String, String> {
    suspend fun readInputStream(
        inputStream: InputStream,
        print: Boolean
    ): String {
        val sb = StringBuilder()
        withContext(IO) {
            inputStream.bufferedReader().forEachLine { line ->
                if (print) {
                    println(line)
                }
                sb.append(line).append(System.lineSeparator())
            }
        }
        return sb.toString()
    }

    return coroutineScope {
        val stdOutJob = async { readInputStream(process.inputStream, printStdOut) }
        val errOutJob = async { readInputStream(process.errorStream, printErrOut) }
        stdOutJob.await() to errOutJob.await()
    }
}

class BashScriptRunner(printStdOut: Boolean = true, printErrOut: Boolean = true) :
    ShellScriptRunner("bash", printStdOut, printErrOut)

class PowerShellScriptRunner(printStdOut: Boolean = true, printErrOut: Boolean = true) :
    ShellScriptRunner("pwsh", printStdOut, printErrOut)