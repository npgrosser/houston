package de.npgrosser.houston.config

import com.fasterxml.jackson.databind.MapperFeature
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper
import com.fasterxml.jackson.module.kotlin.readValue
import de.npgrosser.houston.context.houstonUserDir
import org.intellij.lang.annotations.Language
import java.io.File

private const val CONFIG_FILE_NAME = "config.yml"
private val yamlObjectMapper = YAMLMapper.builder().enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS).build()

val configFile = File(houstonUserDir.toFile(), CONFIG_FILE_NAME)


@Language("yaml")
val defaultConfigContent = """
    ## Overwrites the default shell that Houston should use to run commands (default: bash on linux and mac, powershell on windows)
    # defaultShell: bash
    
    ## Overwrite the defaultRunMode (default: ask). 
    ## Possible values:
    ##  ask: Ask for confirmation before running the generated script
    ##  dry: Print the generated script to stdout
    ##  force: Run the generated script without asking for confirmation
    ## This can still be overwritten by the command line options -y and -n (which are aliases for --force and -dry)
    # defaultRunMode: ask
    
    # Configuration options for the OpenAI API
    openAi:
      apiKey: null # if not null, will be used instead of the OPENAI_API_KEY environment variable
      model: "text-davinci-003"
      maxTokens: 1024
    """.trimIndent()

data class HoustonConfig(
    val defaultShell: String? = null,
    val defaultRunMode: RunMode = RunMode.ASK,
    val openAi: HoustonOpenAiConfig? = null
)

enum class RunMode {
    DRY,
    FORCE,
    ASK
}

data class HoustonOpenAiConfig(
    val apiKey: String? = null,
    val model: String = "text-davinci-003",
    val maxTokens: Int = 1024
)


fun loadUserConfig(): HoustonConfig {
    return if (configFile.exists()) {
        return yamlObjectMapper.readValue(configFile)
    } else {
        HoustonConfig()
    }
}