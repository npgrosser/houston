package de.npgrosser.houston.utils

import de.npgrosser.houston.utils.*
import java.io.File

interface Cache {
    /**
     * Load the value for the given key input from the cache or compute it if it is not in the cache.
     * Input and output must be serializable (by jackson).
     */
    fun <T : Any, R> loadOrCompute(input: T, calc: (input: T) -> R): R

    companion object {
        fun none(): Cache {
            return object : Cache {
                override fun <T : Any, R> loadOrCompute(input: T, calc: (input: T) -> R): R {
                    return calc(input)
                }
            }
        }
    }
}

class FileBasedCache(
    private val cacheDirectory: File,
    private val cacheSizeLimitInBytes: Int = 100 * 1024 * 1024 // 100 MB
) : Cache {
    override fun <T : Any, R> loadOrCompute(input: T, calc: (input: T) -> R): R {

        val inputSerialized = serialize(input)

        val inputSerializedB64 = inputSerialized.base64Encode()
        val fileName = textHashKey(inputSerializedB64)
        val secret = textHashKey(inputSerializedB64 + textHashKey(input::class.java.name))

        val cacheFile = cacheDirectory.resolve(fileName)

        if (cacheFile.exists()) {
            try {
                val resultSerialized = decryptBytes(cacheFile.readBytes(), secret)
                @Suppress("UNCHECKED_CAST") // trusting the cache here to not contain invalid data
                return deserialize(resultSerialized) as R
            } catch (e: Exception) {
                printWarning("Failed to load cache file $cacheFile: ${e.message}")
                try {
                    printWarning("Deleting corrupted cache file $cacheFile")
                    cacheFile.delete()
                } catch (e: Exception) {
                    printWarning("Failed to delete $cacheFile: ${e.message}")
                }
            }
        }

        val result = calc(input)
        try {
            cacheFile.parentFile.mkdirs()

            val resultSerialized = serialize(result)
            val encryptedSerializedResult = encryptBytes(resultSerialized, secret)

            if (encryptedSerializedResult.size <= cacheSizeLimitInBytes) {
                cacheFile.writeBytes(encryptedSerializedResult)
            }
            cacheFile.writeBytes(encryptedSerializedResult)
        } catch (e: Exception) {
            printWarning("Failed to write cache file $cacheFile: $e")
        } finally {
            try {
                while (cacheDirectory.listFiles()!!.sumOf { it.length().toInt() } > cacheSizeLimitInBytes) {
                    cacheDirectory.listFiles()!!.minBy { it.lastModified() }?.delete()
                }
            } catch (e: Exception) {
                printWarning("Failed to clean up cache directory $cacheDirectory: $e")
            }
        }

        return result
    }
}
