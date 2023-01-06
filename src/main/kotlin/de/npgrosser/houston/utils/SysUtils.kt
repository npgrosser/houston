package de.npgrosser.houston.utils

import java.io.File
import java.io.IOException


fun osInfo(): String {
    val osName = System.getProperty("os.name")
    val osVersion = System.getProperty("os.version")
    val osArch = System.getProperty("os.arch")
    val distroName = if (osName == "Linux") {
        getLinuxDistroName()
    } else {
        ""
    }

    val distroInfo = if (distroName.isEmpty()) {
        ""
    } else {
        " ($distroName)"
    }

    return "$osName$distroInfo $osVersion ($osArch)"
}

fun isWindows(): Boolean = System.getProperty("os.name").startsWith("Windows")

fun getSystemSpecificDefaultShell() = if (isWindows()) "powershell" else "bash"

private fun getLinuxDistroName(): String {
    // Try reading the /etc/os-release file
    val osReleaseFile = File("/etc/os-release")
    if (osReleaseFile.exists()) {
        osReleaseFile.readLines().forEach { line ->
            if (line.startsWith("NAME=")) {
                // The NAME property should contain the distro name, with quotes around it
                val distroName = line.substring("NAME=".length).replace("\"", "")
                // Return the distro name without the "Linux" suffix
                return distroName.replace("Linux", "").trim()
            }
        }
    }

    // If /etc/os-release doesn't exist or doesn't contain a NAME property, try parsing the /etc/issue file
    val issueFile = File("/etc/issue")
    if (issueFile.exists()) {
        val issueText = issueFile.readText()
        // The /etc/issue file may contain the distro name at the beginning of the line, followed by the version number
        val distroName = issueText.split(" ")[0]
        // Return the distro name without the "Linux" suffix
        return distroName.replace("Linux", "").trim()
    }

    // If /etc/issue doesn't exist or doesn't contain the distro name, try executing the "lsb_release" command
    return try {
        val process = ProcessBuilder("lsb_release", "-ds").start()
        val output = process.inputStream.bufferedReader().readText()

        // The output of the "lsb_release -ds" command should contain the distro name
        // Return the distro name without the "Linux" suffix
        output.replace("Linux", "").trim()
    } catch (e: IOException) {
        // If the "lsb_release" command fails, return an empty string
        ""
    }
}

//private val packageManagers = listOf(
//    "apt-get",
//    "apk",
//    "brew",
//    "choco",
//    "conda",
//    "fink",
//    "npm",
//    "pacman",
//    "pip",
//    "pip3",
//    "pkgng",
//    "pkgsrc",
//    "port",
//    "rpm",
//    "scoop",
//    "scoop",
//    "winget",
//    "yarn",
//    "yum"
//)
//
//fun listInstalledPackageManagers(): List<String> {
//    val path = System.getenv("PATH")
//    val pathDirs = path.split(File.pathSeparator)
//    return packageManagers.filter { commandName ->
//        pathDirs.any { pathDir ->
//            File(pathDir, commandName).exists() || File(pathDir, "$commandName.exe").exists()
//                    || File(pathDir, "$commandName.bat").exists()
//                    || File(pathDir, "$commandName.cmd").exists()
//        }
//    }
//}
