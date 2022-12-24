package de.npgrosser.houston.utils

import java.io.File


fun filesRec(
    depth: Int? = null,
    dir: File = File(".")
): List<File> {
    val dirsAndFiles = dir.listFiles() ?: return emptyList()
    val dirs = dirsAndFiles.filter { it.isDirectory }
    val files = dirsAndFiles.filter { it.isFile }
    val filesRec = if (depth == null || depth > 0) {
        dirs.flatMap { filesRec(depth?.minus(1), it) }
    } else {
        emptyList()
    }
    return files + filesRec
}