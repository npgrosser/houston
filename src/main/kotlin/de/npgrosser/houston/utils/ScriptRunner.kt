package de.npgrosser.houston.utils

import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream


data class ScriptResult(val exitCode: Int, val stdOutput: String, val errOutput: String)

open class ScriptRunner(
    val shell: String,
    private val suppressOutput: Boolean = false,
) {
    // shell might be a path, so we only want the last part.
    private val shellName: String = File(shell).name

    companion object {
        fun defaultForSystem(suppressOutput: Boolean = false): ScriptRunner {
            return ScriptRunner(getSystemSpecificDefaultShell(), suppressOutput)
        }
    }

    private fun shellCommand(scriptFile: File, vararg args: String): List<String> {

        val scriptFilePathString = if (isMicrosoftWslBash()) {
            // wsl does not like windows paths
            toWslPath(scriptFile.absolutePath)
        } else {
            scriptFile.absolutePath
        }

        return if (shellName == "powershell") {
            listOf(shell, "-File", scriptFilePathString, *args)
        } else {
            listOf(shell, scriptFilePathString, *args)
        }
    }

    open fun fileExtension(): String {
        return when (shellName) {
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

    private fun isMicrosoftWslBash(): Boolean {
        return isWindows() && shellName == "bash" && which("bash")?.startsWith(System.getenv("windir") + "\\system32") ?: false
    }

    fun run(scriptContent: String, vararg args: String): ScriptResult {
        val tmpScriptFile = File.createTempFile("houston", fileSuffix()).apply {
            deleteOnExit()
            val content = if (scriptContent.startsWith("#!")) {
                scriptContent
            } else {
                "#!/usr/bin/env $shellName".trimIndent() + System.lineSeparator() + scriptContent
            }
            writeText(content)
            setExecutable(true)
        }

        try {
            val cmd = shellCommand(tmpScriptFile, *args)

            val process = ProcessBuilder(cmd).redirectInput(ProcessBuilder.Redirect.INHERIT).start()
            val (stdOutput, errOutput) = runBlocking { captureProcessOutput(process, !suppressOutput) }
            val exitCode = process.waitFor()
            return ScriptResult(exitCode, stdOutput, errOutput)
        } finally {
            if (!tmpScriptFile.delete()) {
                printWarning("Could not delete temporary script file ${tmpScriptFile.absolutePath}")
            }
        }
    }
}

private suspend fun captureProcessOutput(
    process: Process,
    print: Boolean
): Pair<String, String> {
    suspend fun readAndPrintInputStream(
        inputStream: InputStream
    ): String {
        val sb = StringBuilder()
        withContext(IO) {
            inputStream.bufferedReader().use { reader ->
                var c: Int
                while (true) {
                    c = reader.read()
                    if (c == -1) {
                        break
                    }
                    sb.append(c.toChar())
                    if (print) {
                        print(c.toChar())
                    }
                }

            }
        }
        return sb.toString()
    }

    return coroutineScope {
        val stdOutJob = async { readAndPrintInputStream(process.inputStream) }
        val errOutJob = async { readAndPrintInputStream(process.errorStream) }
        stdOutJob.await() to errOutJob.await()
    }
}
