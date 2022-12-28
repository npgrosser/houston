package de.npgrosser.houston.utils

import java.util.*


data class ProcessResult(val exitCode: Int, val stdOutput: String, val errOutput: String?)
interface CmdRunner {
    fun run(cmd: String): ProcessResult

    companion object {
        fun defaultForSystem(): CmdRunner = when (System.getProperty("os.name").lowercase(Locale.ROOT)) {
            "windows" -> PowerShellCmdRunner()
            else -> SimpleCmdRunner()
        }
    }

}

class SimpleCmdRunner : CmdRunner {
    override fun run(cmd: String): ProcessResult {
        val process = ProcessBuilder(cmd.tokens()).start()
        val output = Scanner(process.inputStream).use {
            try {
                it.nextLine()
            } catch (e: NoSuchElementException) {
                ""
            }
        }

        val error = Scanner(process.errorStream).use {
            try {
                it.nextLine()
            } catch (e: NoSuchElementException) {
                null
            }
        }

        val exitCode = process.waitFor()


        return ProcessResult(exitCode, output, error)
    }
}

class PowerShellCmdRunner : CmdRunner {
    override fun run(cmd: String): ProcessResult {
        return SimpleCmdRunner().run("powershell -Command \"$cmd\"")
    }
}