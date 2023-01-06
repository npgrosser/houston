package de.npgrosser.houston.utils

import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*

class ScriptRunnerTest {

    @Test
    fun `simple hello world command`() {
        val runner = ScriptRunner.defaultForSystem()
        val script = "echo 'Hello World'"
        val result = runner.run(script)

        assertEquals("", result.errOutput)
        assertEquals(0, result.exitCode)
        assertEquals("Hello World", result.stdOutput.trim())
    }

    @Test
    fun `more complete script`() {
        val runner = ScriptRunner.defaultForSystem(printStdOut = false, printErrOut = false) as ShellScriptRunner

        val script = when (runner.shell) {
            "bash" -> {
                """
                for i in {1..10}
                do
                    echo ${'$'}i
                done
            """.trimIndent()
            }

            "powershell", "pwsh" -> {
                """
                for (${'$'}i = 1; ${'$'}i -le 10; ${'$'}i++) {
                    echo ${'$'}i
                }
                """.trimIndent()
            }

            else -> throw IllegalStateException("Unsupported shell ${runner.shell}")
        }


        val result = runner.run(script)
        assertEquals("1\n2\n3\n4\n5\n6\n7\n8\n9\n10", result.stdOutput.trim())
        assertEquals("", result.errOutput)
        assertEquals(0, result.exitCode)
    }
}