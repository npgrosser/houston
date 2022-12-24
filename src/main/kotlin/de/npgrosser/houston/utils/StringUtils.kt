package de.npgrosser.houston.utils

import java.math.BigInteger
import java.nio.file.Files
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.util.*
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec
import kotlin.collections.ArrayList


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

private fun String.toSecretKey(): SecretKeySpec {
    val sha = MessageDigest.getInstance("SHA-1")
    val key = Arrays.copyOf(sha.digest(this.toByteArray()), 16)
    return SecretKeySpec(key, "AES")
}

fun encryptText(stringToEncrypt: String, secret: String): ByteArray {
    val cipher = Cipher.getInstance("AES/ECB/PKCS5Padding")
    cipher.init(Cipher.ENCRYPT_MODE, secret.toSecretKey())
    return cipher.doFinal(stringToEncrypt.toByteArray())
}

fun decryptText(encryptedString: ByteArray, secret: String): String {
    val cipher = Cipher.getInstance("AES/ECB/PKCS5Padding")
    cipher.init(Cipher.DECRYPT_MODE, secret.toSecretKey())
    return String(cipher.doFinal(encryptedString))
}


fun main() {
    // encrypt decrypt example
    val text = "hello worldasasssssssssaaaaaaaaaaaddddddd\n\n\nworldasasssssssssaaaaaaaaaaaddddddd"
    val secret = textHashKey("0a7281d392d6e89d3f5b69a600a2c8ef")
    val file = Files.createTempFile("encrypted", ".txt").toFile()
    file.writeBytes(encryptText(text, secret))
    val decrypted = decryptText(Files.readAllBytes(file.toPath()), secret)
    print(decrypted)
}
