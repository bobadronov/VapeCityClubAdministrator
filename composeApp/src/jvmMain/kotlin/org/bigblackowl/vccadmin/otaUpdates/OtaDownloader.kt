package org.bigblackowl.vccadmin.otaUpdates

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsChannel
import io.ktor.client.statement.bodyAsText
import io.ktor.http.contentLength
import io.ktor.http.isSuccess
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.readAvailable
import kotlinx.coroutines.delay
import java.io.ByteArrayOutputStream
import kotlin.time.Duration

class OtaDownloader(
    private val http: HttpClient,
) {
    data class DownloadResult(
        val bytes: ByteArray,
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as DownloadResult

            if (!bytes.contentEquals(other.bytes)) return false

            return true
        }

        override fun hashCode(): Int {
            return bytes.contentHashCode()
        }
    }

    /**
     * @param onProgress progress 0f..1f, або null якщо Content-Length невідомий
     */
    suspend fun downloadBytesWithProgress(
        url: String,
        onProgress: ((downloadedBytes: Long, totalBytes: Long?, progress: Float?) -> Unit),
    ): DownloadResult {
        val response: HttpResponse = http.get(url)

        if (!response.status.isSuccess()) {
            val preview = runCatching { response.bodyAsText() }.getOrNull()
            error("HTTP ${response.status}. Body: ${preview?.take(300)}")
        }

        val total = response.contentLength()
        val channel: ByteReadChannel = response.bodyAsChannel()

        val out = ByteArrayOutputStream(
            when {
                total != null && total in 1..Int.MAX_VALUE -> total.toInt()
                else -> 64 * 1024
            }
        )

        val buf = ByteArray(DEFAULT_BUFFER_SIZE)
        var downloaded = 0L

        while (!channel.isClosedForRead) {
            val n = channel.readAvailable(buf, 0, buf.size)

            if (n == -1) break
            if (n == 0) continue // <- важливо

            out.write(buf, 0, n)
            downloaded += n

            val progress = total?.let { t ->
                if (t > 0) (downloaded.toDouble() / t.toDouble()).toFloat().coerceIn(0f, 1f) else null
            }
            delay(Duration.ZERO)
            onProgress.invoke(downloaded, total, progress)
        }

        // фінальний емiт
        onProgress.invoke(downloaded, total, total?.let { 1f })

        return DownloadResult(out.toByteArray())
    }
}
