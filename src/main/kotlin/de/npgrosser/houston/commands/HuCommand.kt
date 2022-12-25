package de.npgrosser.houston.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.context
import com.github.ajalt.clikt.output.CliktHelpFormatter
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.multiple
import com.github.ajalt.clikt.parameters.options.*
import com.github.ajalt.clikt.parameters.types.file
import com.github.ajalt.clikt.parameters.types.int
import de.npgrosser.houston.*
import de.npgrosser.houston.completion.OpenAiPromptCompleter
import de.npgrosser.houston.config.HoustonConfig
import de.npgrosser.houston.config.configFile
import de.npgrosser.houston.config.defaultConfigContent
import de.npgrosser.houston.config.loadHoustonConfig
import de.npgrosser.houston.context.HoustonContextManager
import de.npgrosser.houston.context.houstonUserDir
import de.npgrosser.houston.openai.OpenAi
import de.npgrosser.houston.utils.*
import java.io.File
import java.util.*

private val osSpecificDefaultShell =
    if (System.getProperty("os.name").lowercase(Locale.getDefault()).contains("win")) "powershell" else "bash"

@Suppress("MemberVisibilityCanBePrivate")
class HuCommand : CliktCommand() {
    init {
        context {
            // unfortunately, `showDefaultValues = true` does not work for lazy default values
            helpFormatter = CliktHelpFormatter(showDefaultValues = true)
        }
        prepare()
    }

    val description by argument().multiple()
    val force by option("-y", help = "Run the generated program without asking for confirmation").flag()
    val debug by option("--debug", help = "Print debug information").flag(default = false)
    val packages by option(
        "-p",
        help = "Provide info about installed packages as context information (support limited to brew atm)"
    ).flag()
    val files by option("-f", help = "Provide file name and content as context information").file(mustExist = true)
        .multiple()
    val tree by option("-t", help = "Provide current file tree as context information").flag()
    val treeDepth by option(
        "-td",
        help = "Maximum depth of the file tree to provide as context information (unlimited if not explicitly set)"
    ).int()
    val commands by option("-r", help = "Run the command and provide the output as context information").multiple()
    val output by option("-o", help = "Write the resulting script to an output file").file()
    val contexts by option(
        "-c",
        help = "Add additional Houston Context Files by specifying their name e.g. 'hardware-details' to load ~/.houston/hardware-details.ctxt"
    ).multiple()


    val shell by option("--shell", help = "Specify the shell that Houston should use to run commands").defaultLazy {
        userConfig.defaultShell ?: osSpecificDefaultShell
    }

    // region openai
    val model by option("--model", help = "Use a different model than the default one").defaultLazy {
        userConfig.openAi?.model ?: "text-davinci-003"
    }
    val maxTokens by option("--max-tokens", help = "Use a different token limit than the default one").int()
        .defaultLazy {
            userConfig.openAi?.maxTokens ?: 1024
        }
    private lateinit var apiKey: String
    private lateinit var userConfig: HoustonConfig
    // endregion openai


    /**
     * runs before argument parsing
     */
    private fun prepare() {
        // create configDir if it does not exist
        if (!houstonUserDir.toFile().exists()) {
            houstonUserDir.toFile().mkdir()
            // add empty trusted_dirs and default.ctxt files
            houstonUserDir.resolve("trusted_dirs").toFile().createNewFile()
            houstonUserDir.resolve("default.ctxt").toFile().createNewFile()
        }
        // create default config file if it does not exist
        if (!configFile.exists()) {
            configFile.createNewFile()
            configFile.writeText(defaultConfigContent)
            println("Default config file created at ${configFile.absolutePath}")
        }

        userConfig = loadHoustonConfig()

        apiKey = userConfig.openAi?.apiKey ?: System.getenv("OPENAI_API_KEY")
                ?: error("OPENAI_API_KEY environment variable not set")
    }


    private fun createDetailedDescription(): String {

        var detailedDescription =
            "The task is to create a $shell script to ${
                description.joinToString(" ").trim().ifEmpty { "echo Hello World" }
            }. Make sure it works on ${osInfo()}.\n"
        for (command in commands) {
            detailedDescription = detailedDescription.withCommandContextInfo(command)
        }

        if (packages) {
            detailedDescription =
                detailedDescription.withCommandContextInfo(
                    "brew list",
                    "an overview of installed brew packages that you can use"
                )
        }

        if (tree) {
            val depthInfo = if (treeDepth != null) {
                " (limited to depth $treeDepth)"
            } else {
                ""
            }
            detailedDescription = detailedDescription.withContextInfo(
                "an overview of all files in the current directory$depthInfo",
                filesRec(treeDepth).map { it.relativeTo(File(".")) }.joinToString("\n")
            )
        }

        for (file in files) {
            detailedDescription += "\nThe content of the file ${file.name} is:\n```"
            detailedDescription += file.readText()
            detailedDescription += "```\n\n"
        }


        val extraInfosSb = StringBuilder()

        val contextManager = HoustonContextManager()

        for (contextFile in contextManager.getRelevantContextFiles(contexts)) {
            extraInfosSb.appendLine(contextManager.readAndEvaluateContextFileContentIfTrusted(contextFile))
        }

        val extraInfos = extraInfosSb.toString().trim()
        if (extraInfos.isNotEmpty()) {
            detailedDescription += "\nHere is a list of additional information and requirements:\n${extraInfos}"
        }

        return detailedDescription.trim()
    }

    override fun run() {
        val scriptDescription = createDetailedDescription()


        val completer = OpenAiPromptCompleter(
            OpenAi(apiKey),
            stop = listOf("\n```"),
            maxTokens = maxTokens
        ).withCache(
            houstonUserDir.resolve("cache").toFile()
        )

        val prefix = "$scriptDescription\n\nThe final $shell script: \n\n```$shell\n#!/bin/$shell\n"
        val suffix = null // "\n```"

        if (debug) {
            println("PREFIX")
            println(prefix.blue())
            println("SUFFIX")
            println(suffix)
        }

        print("Generating $shell script...".bold())

        val script =
            completer.complete(prefix, suffix)

        println("\rHere is a $shell script that should do the trick:".bold())


        println("============================".lightGray())
        for (line in script.trim().lines()) {
            val (codeLine, comment) = line.splitFirst("#")
            println((codeLine.cyan() + comment.gray()).bold())
        }
        println("============================".lightGray())


        // run the script
        val shouldRunScript = force || confirm("Do you want me to start it for you?") ?: false
        if (!shouldRunScript) {
            println("Ok, just let me know if you need anything else.")
        } else {
            println("Ok, let's go!".bold())
            val exitCode = runScript(shell, script)
            println(("Script finished with exit code $exitCode".let { if (exitCode == 0) it.green() else it.red() }).bold())
        }

        if (output != null) {
            // write to output file
            val shouldSaveScript =
                shouldRunScript || confirm("Do you want to save the script to ${output?.absolutePath} anyway?") ?: false
            if (shouldSaveScript) {
                output!!.writeText("#!/bin/$shell\n$script")
                println("The script was written to ${output!!.absolutePath}".bold())
            }
        }
    }
}

private fun runScript(command: String, scriptContent: String): Int {
    return if (command.lowercase() == "powershell") {
        val scriptFile = File.createTempFile("houston", ".ps1")
        scriptFile.writeText(scriptContent)
        val process = ProcessBuilder(command, "-File", scriptFile.absolutePath).inheritIO().start()
        process.waitFor()
        scriptFile.delete()
        process.exitValue()
    } else {
        val process = ProcessBuilder(command, "-c", scriptContent).inheritIO().start()
        process.waitFor()
        process.exitValue()
    }
}

private fun String.withContextInfo(description: String, details: String): String {
    var updatedString = this
    updatedString += "\nHere is $description:\n"
    updatedString += "```\n$details\n```\n"
    return updatedString
}

private fun String.withCommandContextInfo(
    command: String,
    description: String = "the output of the command `$command`"
): String {
    val output = ProcessBuilder(command.split(" ")).start().inputStream.bufferedReader().readText()
    return withContextInfo(description, output)
}

fun main(args: Array<String>) = HuCommand()
    .main(args)