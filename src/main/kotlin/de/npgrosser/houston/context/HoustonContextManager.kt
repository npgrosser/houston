package de.npgrosser.houston.context

import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.util.*
import kotlin.io.path.*

val directoryContextFileNames = listOf(".houston", "houston.ctxt")
val houstonUserDir: Path = Path.of(System.getProperty("user.home")).resolve("houston")

class HoustonContextManager(
    private val houstonDefaultCtxtFile: Path = houstonUserDir.resolve("default.ctxt"),
    private val trustedDirsFile: Path = houstonUserDir.resolve("trusted_dirs")
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

    fun readAndEvaluateContextFileContentIfTrusted(houstonFile: File): String {
        return if (isDirectoryTrusted(houstonFile.absoluteFile.parentFile)) {
            println("Adding context from ${houstonFile.absoluteFile}")
            Files.readAllLines(houstonFile.toPath()).joinToString("\n") { evaluateContextFileContent(it) }
        } else {
            println("WARNING: The directory ${houstonFile.absoluteFile.parentFile} is not trusted - the context file ${houstonFile.absoluteFile} will be ignored.")
            ""
        }
    }

    /**
     * checks if the given directory or one of its parent directories is trusted
     * as houston context directory
     */
    private fun isDirectoryTrusted(dir: File): Boolean {
        if (!dir.isDirectory || !trustedDirsFile.exists()) {
            return false
        }
        if (Files.isSameFile(dir.toPath(), houstonUserDir)) {
            return true
        }

        fun fnMatch(pattern: String, path: String): Boolean {
            val regex =
                pattern.replace(
                    Regex("^~"),
                    System.getProperty("user.home")
                ).replace(".", "\\.")
                    .replace("*", ".*")
            return path.matches(Regex(regex))
        }

        return trustedDirsFile.readLines().map { it.trim() }.filter { it.isNotEmpty() }
            .any { fnMatch(it, dir.normalize().absolutePath) || fnMatch(it, dir.normalize().absolutePath + "/") }
    }
}

internal fun evaluateContextFileContent(template: String): String {
    var result = template
    var startIndex = result.indexOf("\${")
    while (startIndex != -1) {
        val endIndex = result.indexOf("}", startIndex)
        val variable = result.substring(startIndex + 2, endIndex)

        // Execute the variable as a command and use its output as the new value
        val output = Scanner(ProcessBuilder(variable).start().inputStream).use {
            it.nextLine()
        }
        result = result.replace("\${$variable}", output)

        startIndex = result.indexOf("\${")
    }
    return result
}