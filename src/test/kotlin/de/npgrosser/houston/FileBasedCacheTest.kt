package de.npgrosser.houston

import de.npgrosser.houston.utils.*
import org.junit.jupiter.api.Test
import java.nio.file.Files

class FileBasedCacheTest {
    @Test
    fun testCache() {
        resetErrorCount()
        resetWarningCount()
        val tmpDir = Files.createTempDirectory("houston-cache-test").toFile()
        val cache = FileBasedCache(tmpDir, 1000)

        var countComputed = 0
        fun compute(input: String): String {
            countComputed++
            return "$input computed"
        }

        val result = cache.loadOrCompute("test", ::compute)

        assert(result == "test computed")
        assert(cache.loadOrCompute("test", ::compute) == result)
        assert(countComputed == 1)

        assert(cache.loadOrCompute("test2", ::compute) == "test2 computed")

        assert(tmpDir.listFiles()!!.size == 2)

        assert(getErrorCount() == 0L)
        assert(getWarningCount() == 0L)
    }
}