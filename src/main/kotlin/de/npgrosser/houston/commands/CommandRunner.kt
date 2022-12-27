package de.npgrosser.houston.commands

import de.npgrosser.houston.utils.tokens
import java.util.*


data class CommandResult(val exitCode: Int, val stdOutput: String, val errOutput: String?)
interface CommandRunner {
    fun run(command: String): CommandResult
}

class SimpleCommandRunner : CommandRunner {
    override fun run(command: String): CommandResult {
        val process = ProcessBuilder(command.tokens()).start()
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


        return CommandResult(exitCode, output, error)
    }
}

class PowerShellCommandRunner : CommandRunner {
    override fun run(command: String): CommandResult {
        return SimpleCommandRunner().run("powershell -Command \"$command\"")
    }
}

fun main() {
    // run command using ProcessBuilder and capture output as String, at the end print output as wells as exit code
    val command = "echo Hello World"
    val runner = PowerShellCommandRunner()
    println(runner.run(command))

}