package de.npgrosser.houston.utils

import java.util.concurrent.atomic.AtomicLong

fun printError(message: String) {
    println("ERROR: $message".red())
    errorCounter.incrementAndGet()
}

fun printWarning(message: String) {
    println("WARNING: $message".yellow())
    warnCounter.incrementAndGet()
}


// region runtime info about errors and warnings (mainly for testing)
private val warnCounter = AtomicLong(0)
private val errorCounter = AtomicLong(0)

fun getErrorCount() = errorCounter.get()
fun getWarningCount() = warnCounter.get()

fun resetErrorCount() = errorCounter.set(0)
fun resetWarningCount() = warnCounter.set(0)
// endregion