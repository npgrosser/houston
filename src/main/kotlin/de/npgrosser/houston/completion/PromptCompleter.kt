package de.npgrosser.houston.completion

interface PromptCompleter {

    fun complete(prompt: String, suffix: String?): String
}