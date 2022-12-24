package de.npgrosser.houston.completion

import de.npgrosser.houston.utils.decryptText
import de.npgrosser.houston.utils.encryptText
import de.npgrosser.houston.utils.textHashKey
import java.io.File

interface PromptCompleter {

    fun complete(prompt: String, suffix: String?): String

    fun withCache(cacheDir: File): PromptCompleter {
        return CachedPromptCompleter(this, cacheDir)
    }
}

internal class CachedPromptCompleter(
    private val original: PromptCompleter,
    private val cacheDirectory: File,
    private val cacheSizeLimitInBytes: Int = 100 * 1024 * 1024 // 100 MB
) : PromptCompleter {
    override fun complete(prompt: String, suffix: String?): String {
        val fileName = textHashKey(prompt + textHashKey(suffix ?: ""))
        val secret = textHashKey(prompt + suffix)
        val cacheFile = cacheDirectory.resolve(fileName)

        return if (cacheFile.exists()) {
            decryptText(cacheFile.readBytes(), secret)
        } else {
            val result = original.complete(prompt, suffix)
            cacheFile.parentFile.mkdirs()

            val bytes = encryptText(result, secret)
            if (bytes.size <= cacheSizeLimitInBytes) {
                cacheFile.writeBytes(bytes)
            }
            cacheFile.writeBytes(bytes)
            while (cacheDirectory.listFiles()!!.sumOf { it.length().toInt() } > cacheSizeLimitInBytes) {
                cacheDirectory.listFiles()!!.minBy { it.lastModified() }?.delete()
            }
            result
        }
    }
}
