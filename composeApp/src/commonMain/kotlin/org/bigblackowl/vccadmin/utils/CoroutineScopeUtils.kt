package org.bigblackowl.vccadmin.utils

import io.ktor.client.plugins.HttpRequestTimeoutException
import kotlinx.coroutines.CoroutineScope

import kotlinx.coroutines.delay

suspend fun <T> CoroutineScope.withRetry(
    maxAttempts: Int = 3,
    delayMillis: Long = 2000L,
    onTimeoutError: String = "Таймаут: повільний інтернет",
    block: suspend () -> T
): T? {
    var attempts = 0
    while (attempts < maxAttempts) {
        try {
            return block()
        } catch (_: HttpRequestTimeoutException) {  // Або специфічний для клієнта (Retrofit/Ktor)
            attempts++
            if (attempts < maxAttempts) {
                delay(delayMillis)
            } else {
                throw Exception(onTimeoutError)  // Або обробіть інакше
            }
        }
    }
    return null
}