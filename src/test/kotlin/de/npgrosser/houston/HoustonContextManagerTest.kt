package de.npgrosser.houston

import de.npgrosser.houston.utils.PowerShellCmdRunner
import de.npgrosser.houston.context.HoustonContextManager
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.test.assertEquals

class HoustonContextManagerTest {

    @Test
    fun testEvaluateTemplate() {
        val template = "Hello \${echo World}"
        val expected = "Hello World"

        // if windows use powershell to prepare command
        val manager = if (System.getProperty("os.name").startsWith("Windows")) {
            HoustonContextManager(cmdRunner = PowerShellCmdRunner())
        } else {
            HoustonContextManager()
        }

        val actual = manager.evaluateContextFileContent(template)
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
    fun testIsDirectoryTrustedWindowsPaths() {
        if (!System.getProperty("os.name").startsWith("Windows")) {
            return
        }
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
