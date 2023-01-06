package de.npgrosser.houston.commands

import com.fasterxml.jackson.databind.DatabindException
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.context
import com.github.ajalt.clikt.output.CliktHelpFormatter
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.multiple
import com.github.ajalt.clikt.parameters.options.*
import com.github.ajalt.clikt.parameters.types.file
import com.github.ajalt.clikt.parameters.types.int
import de.npgrosser.houston.*
import de.npgrosser.houston.config.*
import de.npgrosser.houston.context.HoustonContextManager
import de.npgrosser.houston.context.houstonUserDir
import de.npgrosser.houston.generator.DEFAULT_OPEN_AI_MODEL
import de.npgrosser.houston.generator.OpenAiScriptGenerator
import de.npgrosser.houston.generator.ScriptSpecification
import de.npgrosser.houston.openai.OpenAi
import de.npgrosser.houston.utils.*
import java.io.File
import java.lang.IllegalArgumentException
import java.util.*
import kotlin.system.exitProcess
import de.npgrosser.houston.utils.printError as defaultPrintError
import de.npgrosser.houston.utils.printWarning as defaultPrintWarning

private val osSpecificDefaultShell =
    if (isWindows()) "powershell" else "bash"

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
    val force by option("-y", "--force", help = "Run the generated program without asking for confirmation").flag()
    val dry by option(
        "-n",
        "--dry",
        help = "Don't ask if the generated script should run. Just print it to stdout"
    ).flag()
    val debug by option("--debug", help = "Print debug information").flag(default = false)
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

    val minimal by option(
        "--minimal",
        help = "Only print the generated script without any additional information or syntax highlighting"
    ).flag(default = false)

    val shell by option("--shell", help = "Specify the shell that Houston should use to run commands").defaultLazy {
        userConfig.defaultShell ?: osSpecificDefaultShell
    }

    // region openai
    val model by option("--model", help = "Use a different model than the default one").defaultLazy {
        userConfig.openAi?.model ?: DEFAULT_OPEN_AI_MODEL
    }
    val maxTokens by option("--max-tokens", help = "Use a different token limit than the default one").int()
        .defaultLazy {
            userConfig.openAi?.maxTokens ?: 1024
        }
    private lateinit var apiKey: String
    private lateinit var userConfig: HoustonConfig
    // endregion openai

    private var isPrepared = false


    private fun printInfo(msg: Any?) {
        if (isPrepared && minimal) return else println(msg)
    }

    private fun printError(msg: String) {
        if (isPrepared && minimal) return else defaultPrintError(msg)
    }

    private fun printWarning(msg: String) {
        if (isPrepared && minimal) return else defaultPrintWarning(msg)
    }

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
            // if config.yaml exists in the current directory, moving it to config.yml
            val legacyConfigFile = File(configFile.parentFile, "config.yaml")
            if (legacyConfigFile.exists()) {
                printWarning("Found legacy config file (config.yaml). Moving it to ${configFile.absolutePath}")
                legacyConfigFile.renameTo(configFile)
            } else {
                configFile.createNewFile()
                configFile.writeText(defaultConfigContent)
                printInfo("Default config file created at ${configFile.absolutePath}")
            }
        }

        userConfig = try {
            loadUserConfig()
        } catch (e: DatabindException) {
            printError("Invalid config file")
            printInfo("${e.message}".red())
            exitProcess(1)
        } catch (e: Exception) {
            printError("Config file could not be loaded: ${e.message}")
            exitProcess(1)
        }

        apiKey = userConfig.openAi?.apiKey ?: System.getenv("OPENAI_API_KEY") ?: ""

        if (apiKey.isBlank()) {
            printError("OPENAI_API_KEY environment variable not set.")
            exitProcess(1)
        }

        isPrepared = true
    }

    private fun createScriptSpec(): ScriptSpecification {
        val scriptGoal = description.joinToString(" ").trim().ifEmpty { "print Hello World!" }

        val contextInfo = mutableListOf<String>()

        contextInfo.add("Must work on ${osInfo()}")

        if (commands.isNotEmpty()) {
            val scriptRunner = ScriptRunner.defaultForSystem()
            for (cmd in commands) {
                contextInfo.add("Output of run `$cmd` is: `${scriptRunner.run(cmd).stdOutput}`")
            }
        }

        if (tree) {
            val depthInfo = if (treeDepth != null) " (limited to depth $treeDepth)" else ""
            contextInfo.add(
                "an overview of all files in the current directory $depthInfo: ${
                    filesRec(treeDepth).map {
                        it.relativeTo(
                            File(".")
                        )
                    }.joinToString("\n")
                }"
            )
        }

        for (file in files) {
            contextInfo.add("Content of file ${file.name}:\n ```${file.readText()}```")
        }

        val contextManager = HoustonContextManager()

        val contextFiles = try {
            contextManager.getRelevantContextFiles(contexts)
        } catch (e: IllegalArgumentException) {
            printError(e.message ?: "Unknown error")
            exitProcess(1)
        }
        for (contextFile in contextFiles) {

            val content = contextManager.readAndEvaluateContextFileContentIfTrusted(contextFile)
            if (content == null) {
                printWarning("The directory ${contextFile.absoluteFile.parentFile} is not trusted - the context file ${contextFile.absoluteFile} will be ignored.")
            } else {
                printInfo("Adding context from ${contextFile.absoluteFile}")
                if (content.isNotBlank()) {
                    contextInfo.add(content)
                }
            }
        }

        return ScriptSpecification(
            shell,
            scriptGoal,
            contextInfo
        )
    }

    override fun run() {
        val runMode = if (dry) {
            RunMode.DRY
        } else if (force) {
            RunMode.FORCE
        } else {
            this.userConfig.defaultRunMode
        }

        val scriptSpecification = createScriptSpec()

        val scriptGenerator = OpenAiScriptGenerator(
            OpenAi(apiKey),
            stop = listOf("\n```"),
            model = model,
            maxTokens = maxTokens,
            cache = FileBasedCache(houstonUserDir.resolve("cache").toFile())
        )

        if (debug) {
            printInfo(scriptSpecification)
            printInfo("Model: $model")
            printInfo("Max Tokens: $maxTokens")
            val (prompt, suffix) = scriptGenerator.generatePromptAndSuffix(scriptSpecification)
            printInfo("Generated prompt:\n```\n$prompt\n```")
            printInfo("Generated suffix:\n```\n$suffix\n```")
        }

        printInfo("Generating $shell script...".bold())

        val script = scriptGenerator.generate(scriptSpecification)

        printInfo("\rHere is a $shell script that should do the trick:".bold())



        if (minimal) {
            print(script)
        } else {
            println("============================".lightGray())
            for (line in script.trim().lines()) {
                val (codeLine, comment) = line.splitFirst("#")
                println((codeLine.cyan() + comment.gray()).bold())
            }
            println("============================".lightGray())
        }


        // run the script
        fun confirm(text: String): Boolean = this.confirm(text) ?: false

        val shouldRunScript = when (runMode) {
            RunMode.DRY -> false
            RunMode.FORCE -> true
            RunMode.ASK -> {
                confirm("Do you want me to run it for you?")
            }
        }

        if (!shouldRunScript) {
            printInfo("Let me know if you need anything else!")
        } else {
            printInfo("Ok, let's go!".bold())
            val exitCode = runScript(shell, script)
            printInfo(("Script finished with exit code $exitCode".let { if (exitCode == 0) it.green() else it.red() }).bold())
        }

        // write to output file if specified
        if (output != null) {

            // ask for confirmation if user chose to not run the script (expect run mode is dry)
            val shouldSaveScript =
                runMode == RunMode.DRY || shouldRunScript || confirm("Do you still want to save the script to ${output?.absolutePath}?")
            if (shouldSaveScript) {
                output!!.writeText("#!/bin/$shell\n$script")
                printInfo("The script was written to ${output!!.absolutePath}".bold())
            }
        }
    }

    private fun runScript(shell: String, scriptContent: String): Int {
        val runner = ShellScriptRunner(shell)
        return runner.run(scriptContent).exitCode
    }
}

fun main(args: Array<String>) = HuCommand().main(args)