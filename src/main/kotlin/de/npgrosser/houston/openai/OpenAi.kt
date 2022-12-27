package de.npgrosser.houston.openai

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.PropertyNamingStrategies.SnakeCaseStrategy
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.util.*
import io.ktor.util.reflect.*
import kotlinx.coroutines.runBlocking
import java.io.Serializable

val objectMapper = ObjectMapper().setPropertyNamingStrategy(SnakeCaseStrategy()).registerKotlinModule()


data class CompletionsRequest(
    val prompt: String,
    @JsonInclude(JsonInclude.Include.NON_NULL)
    val suffix: String? = null,
    val temperature: Float = 0f,
    val maxTokens: Int = 2048,
    val topP: Float = 1f,
    val frequencyPenalty: Float = 0f,
    val presencePenalty: Float = 0f,
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    val stop: List<String> = emptyList(),
    val model: String = "text-davinci-003"
) : Serializable

@JsonIgnoreProperties(ignoreUnknown = true)
data class Choice(val text: String, val finishReason: String, val index: Int) : Serializable

@JsonIgnoreProperties(ignoreUnknown = true)
data class Usage(val promptTokens: Int, val totalTokens: Int) : Serializable

@JsonIgnoreProperties(ignoreUnknown = true)
data class CompletionsResponse(
    val id: String,
    val `object`: String,
    val choices: List<Choice>,
    val model: String,
    val usage: Usage,
    val created: Long
) : Serializable

class OpenAi(private val apiKey: String) {

    fun completions(completionsRequest: CompletionsRequest): CompletionsResponse {
        val client = HttpClient(CIO)

        val requestBody: String = objectMapper.writeValueAsString(completionsRequest)

        return runBlocking {
            val response = client.post("https://api.openai.com/v1/completions") {
                header("Authorization", "Bearer $apiKey")
                contentType(ContentType.Application.Json)
                setBody(requestBody)
            }
            if (response.status.value != 200) {
                error("Error: ${response.status.value} ${response.status.description}")
            }
            objectMapper.readValue(response.bodyAsText())
        }
    }
}
