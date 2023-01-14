package de.npgrosser.houston.generator

import de.npgrosser.houston.openai.CompletionsRequest
import de.npgrosser.houston.openai.OpenAi
import de.npgrosser.houston.utils.Cache

const val DEFAULT_OPEN_AI_MODEL = "text-davinci-003"

val modelAliases: Map<String, String> = mapOf(
    "text" to "text-davinci-003",
    "davinci" to "text-davinci-003",
    "code" to "code-davinci-002",
    "codex" to "code-davinci-002"
)

class OpenAiScriptGenerator(
    private val openAi: OpenAi,
    private val stop: List<String> = emptyList(),
    private val model: String = DEFAULT_OPEN_AI_MODEL,
    private val topP: Float = 1.0f,
    private val maxTokens: Int = 2048,
    private val cache: Cache = Cache.none()
) : ScriptGenerator {
    internal fun generatePromptAndSuffix(specification: ScriptSpecification): Pair<String, String> {
        val sb = StringBuilder()

        sb.appendLine("#!/bin/env ${specification.lang}")
        sb.appendLine("#")
        sb.appendLine("# ${specification.lang} script to ${specification.goal}")

        if (specification.requirements.isNotEmpty()) {
            sb.appendLine("#")
            sb.appendLine("# Requirements:")
            specification.requirements.forEach { requirement ->
                val withDash = if (requirement.startsWith("-")) requirement else "- $requirement"
                withDash.lines().forEach { line ->
                    sb.appendLine("# $line")
                }
            }
        }

        sb.appendLine("#")
        sb.appendLine("# Start of script:")
        val prompt = sb.toString()
        val suffix = "# End of script"
        return prompt to suffix
    }

    override fun generate(specification: ScriptSpecification): String {
        val (prompt, suffix) = generatePromptAndSuffix(specification)

        val input = CompletionsRequest(
            prompt = prompt,
            suffix = suffix,
            topP = topP,
            stop = stop,
            model = modelAliases[model] ?: model,
            maxTokens = maxTokens
        )

        return cache.loadOrCompute(input, openAi::completions).choices.first().text
    }
}
