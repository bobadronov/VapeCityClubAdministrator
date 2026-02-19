package org.bigblackowl.vccadmin.otaUpdates

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.contentLength
import io.ktor.http.isSuccess
import io.ktor.utils.io.core.readText
import io.ktor.utils.io.jvm.javaio.toInputStream
import io.ktor.utils.io.readRemaining
import kotlinx.coroutines.delay
import java.io.ByteArrayOutputStream

class OtaDownloader(
    private val http: HttpClient,
) {

    data class DownloadResult(
        val bytes: ByteArray,
        val contentLength: Long? = null,
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as DownloadResult

            if (contentLength != other.contentLength) return false
            if (!bytes.contentEquals(other.bytes)) return false

            return true
        }

        override fun hashCode(): Int {
            var result = contentLength?.hashCode() ?: 0
            result = 31 * result + bytes.contentHashCode()
            return result
        }
    }

    /**
     * @param onProgress progress 0f..1f, або null якщо Content-Length невідомий
     */
    suspend fun downloadBytesWithProgress(
        url: String,
        onProgress: ((downloadedBytes: Long, totalBytes: Long?, progress: Float?) -> Unit)? = null,
    ): DownloadResult {
        val response: HttpResponse = http.get(url)

        // 1) Перевір статус (часто 2KB = HTML/JSON помилка)
        if (!response.status.isSuccess()) {
            val bodyPreview = runCatching { response.bodyAsChannel().readRemaining().readText() }.getOrNull()
            error("HTTP ${response.status}. Body: ${bodyPreview?.take(300)}")
        }

        val total = response.contentLength()
        val input = response.bodyAsChannel().toInputStream()

        val out = ByteArrayOutputStream(
            when {
                total != null && total in 1..Int.MAX_VALUE -> total.toInt()
                else -> 64 * 1024
            }
        )

        val buf = ByteArray(DEFAULT_BUFFER_SIZE)
        var downloaded = 0L

        while (true) {
            val n = input.read(buf)          // <- блокує, 0 не повертає як "ще нема"
            if (n < 0) break                 // EOF
            out.write(buf, 0, n)
            downloaded += n

            val progress = total?.let { if (it > 0) downloaded.toFloat() / it.toFloat() else null }
            onProgress?.invoke(downloaded, total, progress)
            delay(100)
        }

        // фінальний емiт
        onProgress?.invoke(downloaded, total, total?.let { 1f })

        delay(100)

        return DownloadResult(out.toByteArray(), total)
    }
}