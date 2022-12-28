package de.npgrosser.houston.generator

data class ScriptSpecification(val lang: String, val goal: String, val requirements: List<String>)

interface ScriptGenerator {
    fun generate(specification: ScriptSpecification): String
}