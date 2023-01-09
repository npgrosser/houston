package de.npgrosser.houston.context

import de.npgrosser.houston.utils.ScriptRunner
import java.io.File
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.absolute
import kotlin.io.path.exists
import kotlin.io.path.readLines

val directoryContextFileNames = listOf(".houston", "houston.ctxt")
val houstonUserDir: Path = Path.of(System.getProperty("user.home")).resolve("houston")

class HoustonContextManager(
    private val houstonDefaultCtxtFile: Path = houstonUserDir.resolve("default.ctxt"),
    private val trustedDirsFile: Path = houstonUserDir.resolve("trusted_dirs"),
    private val cmdRunner: ScriptRunner = ScriptRunner.defaultForSystem(suppressOutput = true)
) {
    private fun getNamedContextFile(name: String): File {
        val file = houstonUserDir.resolve("$name.ctxt").toFile()
        val customCtxtFile = houstonUserDir.resolve("$name.ctxt")
        if (customCtxtFile.exists()) {
            return customCtxtFile.toFile()
        }

        error("Houston context file '$file' does not exist")
    }

    private fun getImplicitContextFiles(): List<File> {
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
        }

        return all.map { it.absoluteFile.normalize() }.distinct()
    }

    /**
     * Returns content of context file if trusted, otherwise null
     */
    private fun readAndEvaluateContextFileContentIfTrusted(contextFile: File, vararg args: String): String? {
        return if (isDirectoryTrusted(contextFile.absoluteFile.parentFile)) {
            Files.readAllLines(contextFile.toPath()).mapIndexed { index, line ->
                try {
                    evaluateContextFileLine(line, *args)
                } catch (e: HoustonContextException) {
                    throw HoustonContextException("Error in context file '${contextFile.absolutePath}' at line ${index + 1}: ${e.message}")
                }
            }.joinToString("\n")

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

    internal fun evaluateContextFileLine(template: String, vararg args: String): String {
        var result = template
        var startIndex = result.indexOf("\${")
        while (startIndex != -1) {
            val endIndex = result.indexOf("}", startIndex)
            val cmd = result.substring(startIndex + 2, endIndex)

            // Execute the cmd and use its output as the new value
            val commandResult = cmdRunner.run(cmd, *args)
            if (commandResult.exitCode != 0) {
                throw HoustonContextException(
                    "Command '$cmd' failed with exit code ${commandResult.exitCode} (${
                        commandResult.errOutput.lines().first()
                    })"
                )
            }

            result = result.replace("\${$cmd}", commandResult.stdOutput.trimEnd())

            startIndex = result.indexOf("\${")
        }
        return result
    }

    internal fun evaluateContextFiles(namedContexts: List<Pair<String, List<String>>>): EvaluateContextFilesResult {
        val implicitContext = this.getImplicitContextFiles().map { it to emptyList<String>() }
        val explicitContext = namedContexts.map {
            this.getNamedContextFile(it.first) to it.second
        }

        val contextInfo = mutableListOf<String>()
        val untrustedContextFiles = mutableListOf<File>()
        val trustedContextFiles = mutableListOf<File>()

        for ((contextFile, args) in implicitContext + explicitContext) {
            val contextText = this.readAndEvaluateContextFileContentIfTrusted(contextFile, *args.toTypedArray())
            if (contextText == null) {
                untrustedContextFiles.add(contextFile)
            } else {
                trustedContextFiles.add(contextFile)
                if (contextText.isNotBlank()) {
                    contextInfo.add(contextText)
                }
            }
        }

        return EvaluateContextFilesResult(contextInfo, trustedContextFiles, untrustedContextFiles)
    }
}

internal data class EvaluateContextFilesResult(
    val contextInfo: List<String>,
    val trustedContextFiles: List<File>,
    val untrustedContextFiles: List<File>
)


internal fun isSamePath(a: Path, b: Path): Boolean {
    // better for unit testing than Files.isSameFile, because it does not require the files to exist
    return a.absolute().normalize() == b.absolute().normalize()
}
