package de.npgrosser.houston

import de.npgrosser.houston.context.HoustonContextManager
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.DisabledOnOs
import org.junit.jupiter.api.condition.EnabledOnOs
import org.junit.jupiter.api.condition.OS
import java.io.File
import kotlin.test.assertEquals

class HoustonContextManagerTest {

    @Test
    fun testEvaluateTemplate() {
        val template = "Hello \${echo World}"
        val expected = "Hello World"

        val manager = HoustonContextManager()
        val actual = manager.evaluateContextFileLine(template)
        assertEquals(expected, actual)
    }

    @Test
    @EnabledOnOs(OS.WINDOWS)
    fun testEvaluateTemplateWithArgsWindows() {
        val template = "result: \${echo \$args[0]} \${echo \$args[1]} \${echo \$args}"
        val expected = "result: a b a\nb\nc"

        val manager = HoustonContextManager()
        val actual = manager.evaluateContextFileLine(template, "a", "b", "c").replace("\r", "")
        assertEquals(expected, actual)

    }

    @Test
    @DisabledOnOs(OS.WINDOWS)
    fun testEvaluateTemplateWithArgsUnix() {
        val template = "result: \${echo \$1} \${echo \$2} \${echo \$@}"
        val expected = "result: a b a b c"

        val manager = HoustonContextManager()
        val actual = manager.evaluateContextFileLine(template, "a", "b", "c").replace("\r", "")
        assertEquals(expected, actual)
    }


    @Test
    fun testIsDirectoryTrusted() {
        val pattern0 = "/tmp/houston/all"
        val pattern1 = "/tmp/houston/all/*"
        val pattern2 = "/tmp/houston/one"
        val pattern3 = "/tmp/houston/some/*_trusted/*"
        val pattern4 = "/tmp/houston/slash/"

        val trustedDirs = listOf(pattern0, pattern1, pattern2, pattern3, pattern4)

        val tmpFile = File.createTempFile("houston_test", "trusted_dirs")
        tmpFile.writeText(trustedDirs.joinToString("\n"))

        val contextManager = HoustonContextManager(trustedDirsFile = tmpFile.toPath())

        class FakeDir(path: String) : File(path) {
            override fun isDirectory(): Boolean {
                return true
            }
        }

        assertEquals(true, contextManager.isDirectoryTrusted(FakeDir("/tmp/houston/all/any")))
        assertEquals(true, contextManager.isDirectoryTrusted(FakeDir("/tmp/houston/all")))

        assertEquals(true, contextManager.isDirectoryTrusted(FakeDir("/tmp/houston/one")))
        assertEquals(true, contextManager.isDirectoryTrusted(FakeDir("/tmp/houston/one/")))
        assertEquals(false, contextManager.isDirectoryTrusted(FakeDir("/tmp/houston/one/sub")))

        assertEquals(false, contextManager.isDirectoryTrusted(FakeDir("/tmp/houston/other")))
        assertEquals(false, contextManager.isDirectoryTrusted(FakeDir("/tmp/houston/other/any")))
        assertEquals(false, contextManager.isDirectoryTrusted(FakeDir("/tmp/houston/other/any/any")))

        assertEquals(false, contextManager.isDirectoryTrusted(FakeDir("/tmp/houston/some/any")))
        assertEquals(false, contextManager.isDirectoryTrusted(FakeDir("/tmp/houston/some/any/any")))
        assertEquals(true, contextManager.isDirectoryTrusted(FakeDir("/tmp/houston/some/any_trusted/any")))

        assertEquals(true, contextManager.isDirectoryTrusted(FakeDir("/tmp/houston/slash")))
        assertEquals(true, contextManager.isDirectoryTrusted(FakeDir("/tmp/houston/slash/")))
        assertEquals(false, contextManager.isDirectoryTrusted(FakeDir("/tmp/houston/slash/sub")))
    }

    @Test
    @EnabledOnOs(OS.WINDOWS)
    fun testIsDirectoryTrustedWindowsPaths() {
        val pattern0 = "C:\\tmp\\houston\\all"
        val pattern1 = "C:\\tmp\\houston\\all\\*"
        val pattern2 = "C:\\tmp\\houston\\one"
        val pattern3 = "C:\\tmp\\houston\\some\\*_trusted\\*"
        val pattern4 = "C:\\tmp\\houston\\slash\\"

        val trustedDirs = listOf(pattern0, pattern1, pattern2, pattern3, pattern4)

        val tmpFile = File.createTempFile("houston_test", "trusted_dirs")
        tmpFile.writeText(trustedDirs.joinToString("\n"))

        val contextManager = HoustonContextManager(trustedDirsFile = tmpFile.toPath())

        class FakeDir(path: String) : File(path) {
            override fun isDirectory(): Boolean {
                return true
            }
        }

        assertEquals(true, contextManager.isDirectoryTrusted(FakeDir("C:\\tmp\\houston\\all\\any")))
        assertEquals(true, contextManager.isDirectoryTrusted(FakeDir("C:\\tmp\\houston\\all")))

        assertEquals(true, contextManager.isDirectoryTrusted(FakeDir("C:\\tmp\\houston\\one")))
        assertEquals(true, contextManager.isDirectoryTrusted(FakeDir("C:\\tmp\\houston\\one\\")))
        assertEquals(false, contextManager.isDirectoryTrusted(FakeDir("C:\\tmp\\houston\\one\\sub")))

        assertEquals(false, contextManager.isDirectoryTrusted(FakeDir("C:\\tmp\\houston\\other")))
        assertEquals(false, contextManager.isDirectoryTrusted(FakeDir("C:\\tmp\\houston\\other\\any")))
        assertEquals(false, contextManager.isDirectoryTrusted(FakeDir("C:\\tmp\\houston\\other\\any\\any")))

        assertEquals(false, contextManager.isDirectoryTrusted(FakeDir("C:\\tmp\\houston\\some\\any")))
        assertEquals(false, contextManager.isDirectoryTrusted(FakeDir("C:\\tmp\\houston\\some\\any\\any")))
        assertEquals(true, contextManager.isDirectoryTrusted(FakeDir("C:\\tmp\\houston\\some\\any_trusted\\any")))

        assertEquals(true, contextManager.isDirectoryTrusted(FakeDir("C:\\tmp\\houston\\slash")))
        assertEquals(true, contextManager.isDirectoryTrusted(FakeDir("C:\\tmp\\houston\\slash\\")))
        assertEquals(false, contextManager.isDirectoryTrusted(FakeDir("C:\\tmp\\houston\\slash\\sub")))
    }
}
