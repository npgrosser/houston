package de.npgrosser.houston.utils

import java.security.MessageDigest
import java.util.*
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

private fun String.toSecretKey(): SecretKeySpec {
    val sha = MessageDigest.getInstance("SHA-1")
    val key = Arrays.copyOf(sha.digest(this.toByteArray()), 16)
    return SecretKeySpec(key, "AES")
}

fun encryptBytes(bytes: ByteArray, secret: String): ByteArray {
    val cipher = Cipher.getInstance("AES/ECB/PKCS5Padding")
    cipher.init(Cipher.ENCRYPT_MODE, secret.toSecretKey())
    return cipher.doFinal(bytes)
}

fun decryptBytes(bytes: ByteArray, secret: String): ByteArray {
    val cipher = Cipher.getInstance("AES/ECB/PKCS5Padding")
    cipher.init(Cipher.DECRYPT_MODE, secret.toSecretKey())
    return cipher.doFinal(bytes)
}

