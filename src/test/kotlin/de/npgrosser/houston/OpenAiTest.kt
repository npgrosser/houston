package de.npgrosser.houston

import de.npgrosser.houston.openai.Choice
import de.npgrosser.houston.openai.CompletionsRequest
import de.npgrosser.houston.openai.CompletionsResponse
import de.npgrosser.houston.openai.Usage
import de.npgrosser.houston.utils.deserialize
import de.npgrosser.houston.utils.serialize
import org.junit.jupiter.api.Test

class OpenAiTest {
    @Test
    fun testCompletionsRequestCanBeSerialized() {
        val original = CompletionsRequest(
            prompt = "Hello World",
            maxTokens = 10,
            temperature = 0.5f,
            topP = 2f,
            frequencyPenalty = 0.0f,
            presencePenalty = 0.0f,
            stop = listOf("stop1", "stop2")
        )

        val serialized = serialize(original)
        val deserialized = deserialize(serialized) as CompletionsRequest

        assert(original == deserialized)
    }

    @Test
    fun testCompletionsResponseCanBeSerialized() {
        val original = CompletionsResponse(
            id = "id",
            model = "model",
            created = 123,
            `object` = "object",
            usage = Usage(1, 2),
            choices = listOf(
                Choice(
                    index = 0,
                    text = "text",
                    finishReason = "finishReason"
                )
            )
        )

        val serialized = serialize(original)
        val deserialized = deserialize(serialized) as CompletionsResponse
        assert(original == deserialized)
    }
}