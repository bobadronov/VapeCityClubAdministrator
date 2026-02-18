package org.bigblackowl.vccadmin.otaUpdates

import io.github.aakira.napier.Napier
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.contentLength
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.readAvailable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.bigblackowl.vccadmin.BuildConfig
import org.bigblackowl.vccadmin.data.repository.NetworkMonitorProvider
import org.bigblackowl.vccadmin.utils.PlatformFileProvider
import java.io.ByteArrayOutputStream
import java.security.MessageDigest
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.pow
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource


enum class DesktopOs { WINDOWS, MACOS, LINUX }

@Serializable
data class UpdateManifest(
    val tag: String,
    val publishedAt: String,          // ISO-8601
    val versionName: String,          // "1.0.8"
    @SerialName("desktop_version")
    val desktopVersion: String,       // "1.0.807"
    val assets: Assets
) {
    fun pickAsset(os: DesktopOs): AssetInfo? = when (os) {
        DesktopOs.WINDOWS -> assets.windows
        DesktopOs.MACOS -> assets.macos
        DesktopOs.LINUX -> assets.linux
    }
}

@Serializable
data class Assets(
    val windows: AssetInfo? = null,
    val macos: AssetInfo? = null,
    val linux: AssetInfo? = null
)

@Serializable
data class AssetInfo(
    val name: String,
    val url: String,
    val size: Long,
    val sha256: String = ""
)

data class UpdateInfo(
    val manifest: UpdateManifest,
    val asset: AssetInfo,
)

sealed class UpdateState {
    object Idle : UpdateState()
    object Checking : UpdateState()
    object NoUpdate : UpdateState()

    data class Available(val info: UpdateInfo) : UpdateState()

    data class Downloading(
        val progress: Float? = null,
        val total: String? = null,
        val downloaded: String? = null,
    ) : UpdateState() // 0..1 або null

    object Downloaded : UpdateState()

    data class Verifying(val info: UpdateInfo) : UpdateState()
    object ReadyToInstall : UpdateState()

    data class Installing(val info: UpdateInfo) : UpdateState()
    data class Error(val message: String, val cause: Throwable? = null) : UpdateState()
}

private fun parseVersion(v: String): List<Int> =
    v.split(".").mapNotNull { it.toIntOrNull() }

private fun isNewer(remote: String): Boolean {
    val currentDesktopVersion: String = BuildConfig.APP_VERSION
    val r = parseVersion(remote)
    val l = parseVersion(currentDesktopVersion)
    val max = max(r.size, l.size)
    for (i in 0 until max) {
        val rv = r.getOrNull(i) ?: 0
        val lv = l.getOrNull(i) ?: 0
        if (rv != lv) return rv > lv
    }
    return false
}

fun detectDesktopOs(): DesktopOs {
    val name = System.getProperty("os.name").lowercase()
    return when {
        name.contains("win") -> DesktopOs.WINDOWS
        name.contains("mac") -> DesktopOs.MACOS
        else -> DesktopOs.LINUX
    }
}

class OtaUpdateRepository(
    private val supabase: SupabaseClient,
) {
    private companion object {
        const val UPDATE_TABLE_NAME = "admin_app_updates"
    }

    suspend fun fetchLatestManifest(): UpdateManifest? {
        val rows: List<RowLatest> = supabase
            .from(UPDATE_TABLE_NAME)
            .select()
            .decodeList()

        return rows.firstOrNull()?.manifest
    }

    @Serializable
    private data class RowLatest(
        val tag: String,
        val published_at: String? = null,
        val version_name: String,
        val desktop_version: String,
        val manifest: UpdateManifest
    )
}

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
        val total = response.contentLength() // може бути null
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
            if (n <= 0) break

            out.write(buf, 0, n)
            downloaded += n

            val progress = total?.let { if (it > 0) downloaded.toFloat() / it.toFloat() else null }
            onProgress?.invoke(downloaded, total, progress)
        }

        // фінальний апдейт (щоб UI гарантовано отримав 1f коли total відомий)
        if (total != null && total > 0) {
            onProgress?.invoke(downloaded, total, 1f)
        } else {
            onProgress?.invoke(downloaded, total, null)
        }

        return DownloadResult(
            bytes = out.toByteArray(),
            contentLength = total,
        )
    }

    fun sha256(bytes: ByteArray): String {
        val md = MessageDigest.getInstance("SHA-256")
        md.update(bytes)
        return md.digest().joinToString("") { "%02x".format(it) }
    }
}

class OtaUpdateManager(
    private val repo: OtaUpdateRepository,
    private val downloader: OtaDownloader,
    private val networkMonitorProvider: NetworkMonitorProvider,
) {
    private val currentDesktopVersion: String = BuildConfig.APP_VERSION
    private val os: DesktopOs = detectDesktopOs()

    private companion object {
        const val TAG = "OTA"
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _state = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val state: StateFlow<UpdateState> = _state.asStateFlow()
    private data class PendingInstall(val fileName: String, val info: UpdateInfo)
    private var pendingInstall: PendingInstall? = null

    fun checkOnAppStart() {
        scope.launch {
            if (_state.value is UpdateState.Checking) return@launch
            _state.value = UpdateState.Checking
            Napier.d(tag = TAG) { "Checking updates... local=$currentDesktopVersion os=$os" }

            try {
                // якщо є метод у провайдера — використай, інакше прибери цю перевірку
                if (!networkMonitorProvider.isConnected.value) {
                    delay(3.seconds)
                    if (!networkMonitorProvider.isConnected.value) return@launch
                }

                val manifest = repo.fetchLatestManifest()
                if (manifest == null) {
                    Napier.d(tag = TAG) { "No manifest found" }
                    _state.value = UpdateState.NoUpdate
                    return@launch
                }

                Napier.d(tag = TAG) { "Latest: tag=${manifest.tag}, desktop=${manifest.desktopVersion}" }

                if (!isNewer(manifest.desktopVersion)) {
                    Napier.d(tag = TAG) { "No update: remote <= local" }
                    _state.value = UpdateState.NoUpdate
                    return@launch
                }

                val asset = manifest.pickAsset(os)
                if (asset == null) {
                    _state.value = UpdateState.Error("No asset for OS=$os")
                    return@launch
                }

                _state.value = UpdateState.Available(UpdateInfo(manifest, asset))
                Napier.d(tag = TAG) { "Update available: ${asset.name}, size=${asset.size}" }
            } catch (t: Throwable) {
                Napier.e(tag = TAG, throwable = t) { "Check updates failed" }
                _state.value = UpdateState.Error("Check updates failed: ${t.message}", t)
            }
        }
    }

    fun downloadUpdate(info: UpdateInfo) {
        scope.launch {
            try {
                _state.value = UpdateState.Downloading(progress = null, downloaded = "0 B", total = "—")
                Napier.d(tag = TAG) { "Downloading: ${info.asset.url}" }

                val time = TimeSource.Monotonic
                var lastEmitAt = time.markNow()
                var lastPercent = -1
                var lastDownloadedBucket = -1L // щоб без total теж рідше оновлювати

                val result = downloader.downloadBytesWithProgress(
                    url = info.asset.url,
                    onProgress = { downloaded, total, progress ->

                        // 1) якщо total відомий — оновлюємо коли змінився % або пройшло 120мс
                        if (total != null && total > 0 && progress != null) {
                            val percent = (progress * 100).toInt().coerceIn(0, 100)
                            val timeOk = lastEmitAt.elapsedNow() >= 120.milliseconds
                            val percentOk = percent != lastPercent

                            if (timeOk || percentOk) {
                                lastEmitAt = time.markNow()
                                lastPercent = percent

                                _state.value = UpdateState.Downloading(
                                    progress = progress,
                                    downloaded = downloaded.formatBytes(1),
                                    total = total.formatBytesOrDash(1),
                                )
                            }
                            return@downloadBytesWithProgress
                        }

                        // 2) якщо total НЕ відомий — оновлюємо по “бакетах” (наприклад, кожні 512KB) або раз на 200мс
                        val bucket = downloaded / (512 * 1024)
                        val timeOk = lastEmitAt.elapsedNow() >= 200.milliseconds
                        val bucketOk = bucket != lastDownloadedBucket

                        if (timeOk || bucketOk) {
                            lastEmitAt = time.markNow()
                            lastDownloadedBucket = bucket

                            _state.value = UpdateState.Downloading(
                                progress = null,
                                downloaded = downloaded.formatBytes(1),
                                total = total.formatBytesOrDash(1), // буде "—"
                            )
                        }
                    }
                )

                val bytes = result.bytes
                val fileName = info.asset.name.ifBlank { "update-${info.manifest.tag}" }

                PlatformFileProvider.saveFile(fileName, bytes)

                _state.value = UpdateState.Downloaded

                _state.value = UpdateState.Verifying(info)
                val computed = downloader.sha256(bytes)

                if (info.asset.sha256.isNotBlank() &&
                    !computed.equals(info.asset.sha256, ignoreCase = true)
                ) {
                    _state.value = UpdateState.Error("SHA256 mismatch. File may be corrupted.")
                    return@launch
                }
                pendingInstall = PendingInstall(fileName = fileName, info = info)
                _state.value = UpdateState.ReadyToInstall
            } catch (t: Throwable) {
                _state.value = UpdateState.Error("Download failed: ${t.message}", t)
            }
        }
    }

    fun startInstall() {
        scope.launch {
            val pending = pendingInstall
            if (pending == null) {
                _state.value = UpdateState.Error("Немає завантаженого оновлення. Спочатку скачай файл.")
                return@launch
            }

            val (fileName, info) = pending

            try {
                _state.value = UpdateState.Installing(info)
                Napier.d(tag = TAG) { "Opening installer: $fileName" }

                PlatformFileProvider.openFile(fileName)

                // Після старту інсталятора можна очистити pending, щоб не ставили вдруге
                pendingInstall = null
                _state.value = UpdateState.Idle
//                exitProcess(0)
            } catch (t: Throwable) {
                _state.value = UpdateState.Error("Install failed: ${t.message}", t)
            }
        }
    }


    private fun Long.formatBytes(decimals: Int = 1): String {
        if (this <= 0L) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (ln(this.toDouble()) / ln(1024.0)).toInt().coerceIn(0, units.lastIndex)
        val value = this / 1024.0.pow(digitGroups.toDouble())
        return "%.${decimals}f %s".format(value, units[digitGroups])
    }

    private fun Long?.formatBytesOrDash(decimals: Int = 1): String =
        this?.formatBytes(decimals) ?: "— MB"
}

