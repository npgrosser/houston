package de.npgrosser.houston.utils

import java.math.BigInteger
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.util.*


/**
 * Split a string in two parts at the first occurrence of the given separator.
 * The separator will be the prefix of the second value.
 */
fun String.splitFirst(separator: String): Pair<String, String> {
    val index = this.indexOf(separator)
    return if (index == -1) {
        this to ""
    } else {
        this.substring(0, index) to this.substring(index)
    }
}

fun String.green() = "\u001B[32m$this\u001B[0m"
fun String.bold() = "\u001B[1m$this\u001B[0m"
fun String.red() = "\u001B[31m$this\u001B[0m"
fun String.gray() = "\u001B[90m$this\u001B[0m"
fun String.cyan() = "\u001B[36m$this\u001B[0m"
fun String.lightGray() = "\u001B[37m$this\u001B[0m"
fun String.blue() = "\u001B[34m$this\u001B[0m"
fun String.yellow() = "\u001B[33m$this\u001B[0m"

fun textHashKey(text: String, length: Int = 32): String {
    val md: MessageDigest
    try {
        md = MessageDigest.getInstance("MD5")
    } catch (e: NoSuchAlgorithmException) {
        throw RuntimeException(e)
    }
    md.update(text.toByteArray())
    val hash = md.digest()
    return String.format("%0${length}x", BigInteger(1, hash)).substring(0, length)
}

fun String.tokens(): List<String> {
    val st = StringTokenizer(this)
    val tokens = ArrayList<String>(st.countTokens())
    for (i in 0 until st.countTokens()) {
        tokens.add(st.nextToken())
    }
    return tokens
}

fun ByteArray.base64Encode(): String {
    return Base64.getEncoder().encodeToString(this)
}

