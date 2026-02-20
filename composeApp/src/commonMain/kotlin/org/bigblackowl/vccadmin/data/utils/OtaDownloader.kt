package org.bigblackowl.vccadmin.data.utils

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsChannel
import io.ktor.client.statement.bodyAsText
import io.ktor.http.contentLength
import io.ktor.http.isSuccess
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.readAvailable
import kotlinx.coroutines.yield
import kotlin.math.min

class OtaDownloader(
    private val http: HttpClient,
) {
    private companion object {
        private const val DEFAULT_BUFFER_SIZE: Int = 8 * 1024
    }

    data class DownloadResult(val bytes: ByteArray) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other == null || this::class != other::class) return false
            other as DownloadResult
            return bytes.contentEquals(other.bytes)
        }
        override fun hashCode(): Int = bytes.contentHashCode()
    }

    /**
     * @param onProgress progress 0f..1f, або null якщо Content-Length невідомий
     */
    suspend fun downloadBytesWithProgress(
        url: String,
        onProgress: (downloadedBytes: Long, totalBytes: Long?, progress: Float?) -> Unit,
    ): DownloadResult {
        val response: HttpResponse = http.get(url)

        if (!response.status.isSuccess()) {
            val preview = runCatching { response.bodyAsText() }.getOrNull()
            error("HTTP ${response.status}. Body: ${preview?.take(300)}")
        }

        val total = response.contentLength()
        val channel: ByteReadChannel = response.bodyAsChannel()

        val buf = ByteArray(DEFAULT_BUFFER_SIZE)
        val chunks = ArrayList<ByteArray>(64)

        var downloaded = 0L

        while (!channel.isClosedForRead) {
            val n = channel.readAvailable(buf, 0, buf.size)
            if (n == -1) break
            if (n == 0) continue

            // копіюємо рівно n байтів у новий масив
            chunks += buf.copyOfRange(0, n)
            downloaded += n

            val progress = total?.let { t ->
                if (t > 0L) (downloaded.toDouble() / t.toDouble()).toFloat().coerceIn(0f, 1f) else null
            }

            // KMP-friendly "yield" як твій Duration.ZERO
            yield()

            onProgress(downloaded, total, progress)
        }

        // фінальний емiт
        onProgress(downloaded, total, total?.let { 1f })

        val bytes = concatChunks(chunks, downloaded)
        return DownloadResult(bytes)
    }

    private fun concatChunks(chunks: List<ByteArray>, totalSizeLong: Long): ByteArray {
        val totalSize = totalSizeLong.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
        val out = ByteArray(totalSize)

        var offset = 0
        for (c in chunks) {
            val len = min(c.size, totalSize - offset)
            if (len <= 0) break
            c.copyInto(out, destinationOffset = offset, startIndex = 0, endIndex = len)
            offset += len
        }
        return out
    }
}