package de.npgrosser.houston.context

import de.npgrosser.houston.utils.CmdRunner
import de.npgrosser.houston.utils.SimpleCmdRunner
import java.io.File
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.*

val directoryContextFileNames = listOf(".houston", "houston.ctxt")
val houstonUserDir: Path = Path.of(System.getProperty("user.home")).resolve("houston")

class HoustonContextManager(
    private val houstonDefaultCtxtFile: Path = houstonUserDir.resolve("default.ctxt"),
    private val trustedDirsFile: Path = houstonUserDir.resolve("trusted_dirs"),
    private val cmdRunner: CmdRunner = SimpleCmdRunner()
) {
    fun getRelevantContextFiles(customContextNames: List<String>): List<File> {
        // find all houston.ctxt files in the current directory and all parent directories
        val directoryContextFiles = mutableListOf<File>()
        var currentDir: File? = File(".").absoluteFile
        while (currentDir != null && currentDir.exists()) {
            for (fileName in directoryContextFileNames) {
                val houstonCtxtFile = currentDir.resolve(fileName)
                if (houstonCtxtFile.isFile) {
                    directoryContextFiles.add(houstonCtxtFile)
                }
            }
            currentDir = currentDir.parentFile
        }

        val all = directoryContextFiles.toMutableList()
        if (houstonUserDir.exists()) {
            if (houstonDefaultCtxtFile.exists()) {
                all.add(houstonDefaultCtxtFile.toFile())
            }
            val named = houstonUserDir.listDirectoryEntries()
                .asSequence()
                .filter { it.isRegularFile() }
                .filter { it.nameWithoutExtension in customContextNames }
                .filter { it.name.endsWith(".ctxt") }
                .filter { it.name != houstonDefaultCtxtFile.fileName.toString() }
                .sortedBy { it.name }
                .map { it.toFile() }
                .toList()

            for (customContextName in customContextNames) {
                if (customContextName !in named.map { it.nameWithoutExtension }) {
                    println("Houston: Context file '$customContextName.ctxt' not found in $houstonUserDir")
                }
            }

            all.addAll(named)
        }


        return all.map { it.absoluteFile.normalize() }.distinct()
    }

    /**
     * Returns content of context file if trusted, otherwise null
     */
    fun readAndEvaluateContextFileContentIfTrusted(contextFile: File): String? {
        return if (isDirectoryTrusted(contextFile.absoluteFile.parentFile)) {
            Files.readAllLines(contextFile.toPath()).joinToString("\n") { evaluateContextFileContent(it) }
        } else {
            null
        }
    }

    /**
     * checks if the given directory or one of its parent directories is trusted
     * as houston context directory
     */
    internal fun isDirectoryTrusted(dir: File): Boolean {
        if (!dir.isDirectory || !trustedDirsFile.exists()) {
            return false
        }

        if (isSamePath(dir.toPath(), houstonUserDir)) {
            return true
        }

        fun matchPattern(pattern: String, path: File): Boolean {
            val normalizedPattern = File(pattern).absoluteFile.normalize().toString().replace("\\", "/")
            val normalizedPath = path.normalize().absolutePath
            val pathMatcher = FileSystems.getDefault().getPathMatcher("glob:${normalizedPattern}")
            return pathMatcher.matches(Path.of(normalizedPath))
        }

        return trustedDirsFile.readLines().map { it.trim() }.filter { it.isNotEmpty() }
            .any { matchPattern(it, dir) }
    }

    internal fun evaluateContextFileContent(template: String): String {
        var result = template
        var startIndex = result.indexOf("\${")
        while (startIndex != -1) {
            val endIndex = result.indexOf("}", startIndex)
            val cmd = result.substring(startIndex + 2, endIndex)

            // Execute the cmd and use its output as the new value
            val commandResult = cmdRunner.run(cmd)
            if (commandResult.exitCode != 0) {
                throw HoustonContextException(
                    "Houston: Command '$cmd' failed with exit code ${commandResult.exitCode} (${
                        commandResult.errOutput?.lines()?.first()
                    })"
                )
            }

            result = result.replace("\${$cmd}", commandResult.stdOutput)

            startIndex = result.indexOf("\${")
        }
        return result
    }
}


internal fun isSamePath(a: Path, b: Path): Boolean {
    // better for unit testing than Files.isSameFile, because it does not require the files to exist
    return a.absolute().normalize() == b.absolute().normalize()
}
