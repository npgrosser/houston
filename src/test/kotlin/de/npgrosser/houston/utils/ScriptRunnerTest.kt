package de.npgrosser.houston.utils

import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*

class ScriptRunnerTest {

    @Test
    fun `simple hello world command`() {
        // if Windows powershell, else bash
        val runner = ScriptRunner.defaultForSystem()
        val script = "echo 'Hello World'"
        val result = runner.run(script)
        assertEquals("Hello World", result.stdOutput.trim())
        assertEquals("", result.errOutput)
        assertEquals(0, result.exitCode)
    }

    @Test
    fun `more complete script`() {
        val (script, runner) = if (System.getProperty("os.name").startsWith("Windows")) {
            """
            for (${'$'}i = 1; ${'$'}i -le 10; ${'$'}i++) {
                echo ${'$'}i
            }
            """.trimIndent() to PowerShellScriptRunner(false, false)
        } else {
            """
            for i in {1..10}
            do
                echo ${'$'}i
            done
        """.trimIndent() to BashScriptRunner(false, false)
        }


        val result = runner.run(script)
        assertEquals("1\n2\n3\n4\n5\n6\n7\n8\n9\n10", result.stdOutput.trim())
        assertEquals("", result.errOutput)
        assertEquals(0, result.exitCode)
    }
}