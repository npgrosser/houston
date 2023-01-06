package de.npgrosser.houston.utils

import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream


data class ScriptResult(val exitCode: Int, val stdOutput: String, val errOutput: String)
interface ScriptRunner {
    fun run(scriptContent: String): ScriptResult

    companion object {
        fun defaultForSystem(printStdOut: Boolean = true, printErrOut: Boolean = true): ScriptRunner {
            return ShellScriptRunner(getSystemSpecificDefaultShell(), printStdOut, printErrOut)
        }
    }
}

open class ShellScriptRunner(
    val shell: String,
    private val printStdOut: Boolean = true,
    private val printErrOut: Boolean = true
) : ScriptRunner {


    open fun shellArgs(): List<String> {
        if (shell == "powershell") {
            return listOf("-File")
        }
        return emptyList()
    }

    open fun fileExtension(): String {
        return when (shell) {
            "powershell", "pwsh" -> return "ps1"
            "shell", "bash" -> "sh"
            else -> ""
        }
    }

    private fun fileSuffix(): String {
        val extension = fileExtension()
        return if (extension.isEmpty()) {
            ""
        } else {
            ".$extension"
        }
    }

    override fun run(scriptContent: String): ScriptResult {
        val tmpScriptFile = File.createTempFile("houston", fileSuffix()).apply {
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
            val cmd = listOf(shell) + shellArgs() + listOf(tmpScriptFile.absolutePath)

            val process = ProcessBuilder(cmd).start()

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
                sb.appendLine(line)
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