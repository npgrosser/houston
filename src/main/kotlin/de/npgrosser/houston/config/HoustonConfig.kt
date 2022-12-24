package de.npgrosser.houston.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.readValue
import de.npgrosser.houston.context.houstonUserDir
import org.intellij.lang.annotations.Language
import java.io.File

private const val CONFIG_FILE_NAME = "config.yaml"
private val yamlObjectMapper = ObjectMapper(YAMLFactory())
val configFile = File(houstonUserDir.toFile(), CONFIG_FILE_NAME)

@Language("yaml")
val defaultConfigContent = """
    ## Overwrites the default shell that Houston should use to run commands (default: bash on linux and mac, powershell on windows)
    # defaultShell: bash
    
    # Configuration options for the OpenAI API
    openAi:
      apiKey: null # if not null, will be used instead of the OPENAI_API_KEY environment variable
      model: "text-davinci-003"
      maxTokens: 1024
    """.trimIndent()

data class HoustonOpenAiConfig(
    val apiKey: String? = null,
    val model: String = "text-davinci-003",
    val maxTokens: Int = 1024
)

data class HoustonConfig(val defaultShell: String? = null, val openAi: HoustonOpenAiConfig? = null)

fun loadHoustonConfig(): HoustonConfig {
    return if (configFile.exists()) {
        return yamlObjectMapper.readValue(configFile)
    } else {
        HoustonConfig()
    }
}