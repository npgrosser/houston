package de.npgrosser.houston.openai

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.PropertyNamingStrategies.SnakeCaseStrategy
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.Serializable
import java.util.concurrent.TimeUnit
import kotlin.system.exitProcess

private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

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
        val client = OkHttpClient.Builder()
            .readTimeout(300, TimeUnit.SECONDS)
            .build()
        val requestBody = objectMapper.writeValueAsString(completionsRequest)
        val request = Request.Builder()
            .url("https://api.openai.com/v1/completions")
            .addHeader("Authorization", "Bearer $apiKey")
            .post(requestBody.toRequestBody(jsonMediaType))
            .build()
        val response: Response = client.newCall(request).execute()
        if (response.code != 200) {
            println("Error: " + response.code)
            println(response.body?.string())
            exitProcess(1)
        }

        return objectMapper.readValue(response.body!!.string())
    }
}
