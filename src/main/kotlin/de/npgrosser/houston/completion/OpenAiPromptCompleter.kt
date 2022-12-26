package de.npgrosser.houston.completion

import de.npgrosser.houston.utils.Cache
import de.npgrosser.houston.openai.CompletionsRequest
import de.npgrosser.houston.openai.OpenAi


class OpenAiPromptCompleter(
    private val openAi: OpenAi,
    private val stop: List<String> = emptyList(),
    private val model: String = "text-davinci-003",
    private val topP: Float = 1.0f,
    private val maxTokens: Int = 2048,
    private val cache: Cache = Cache.none()
) : PromptCompleter {

    override fun complete(prompt: String, suffix: String?): String {
        val input = CompletionsRequest(
            prompt = prompt,
            suffix = suffix,
            topP = topP,
            stop = stop,
            model = model,
            maxTokens = maxTokens
        )
        return cache.loadOrCompute(input, openAi::completions).choices.first().text
    }
}