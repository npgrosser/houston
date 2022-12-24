package de.npgrosser.houston

import de.npgrosser.houston.context.evaluateContextFileContent
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

/**
 * test for all public functions in ContextFiles.kt
 * using temporary files and directories and mocking
 */
class HoustonContextManagerTest {
    @Test
    fun testEvaluateTemplate() {
        val template = "Hello \${echo World}"
        val expected = "Hello World"
        val actual = evaluateContextFileContent(template)
        assertEquals(expected, actual)
    }

}
