package de.npgrosser.houston.completion

import de.npgrosser.houston.openai.CompletionsRequest
import de.npgrosser.houston.openai.OpenAi
import java.io.File


class OpenAiPromptCompleter(
    private val openAi: OpenAi,
    private val stop: List<String> = emptyList(),
    private val model: String = "text-davinci-003",
    private val topP: Int = 1,
    private val maxTokens: Int = 2048,
) :
    PromptCompleter {

    override fun complete(prompt: String, suffix: String?): String {
        return openAi.completions(
            CompletionsRequest(
                prompt = prompt,
                suffix = suffix,
                topP = topP,
                stop = stop,
                model = model,
                maxTokens = maxTokens
            )
        ).choices.first().text
    }
}