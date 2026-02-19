package org.bigblackowl.vccadmin.otaUpdates

import io.github.aakira.napier.Napier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.bigblackowl.vccadmin.BuildConfig
import org.bigblackowl.vccadmin.data.entity.DesktopOs
import org.bigblackowl.vccadmin.data.entity.UpdateInfo
import org.bigblackowl.vccadmin.data.repository.NetworkMonitorProvider
import org.bigblackowl.vccadmin.data.repository.OtaUpdateRepository
import org.bigblackowl.vccadmin.utils.PlatformFileProvider
import java.security.MessageDigest
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.pow
import kotlin.system.exitProcess
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource

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
                if (!networkMonitorProvider.isConnected.value) {
                    delay(5.seconds)
                    if (!networkMonitorProvider.isConnected.value) {
                        _state.value = UpdateState.Error("No internet")
                        return@launch
                    }
                }

                val manifest = repo.fetchLatestManifest()
                if (manifest == null) {
                    Napier.d(tag = TAG) { "No manifest found" }
                    _state.value = UpdateState.NoUpdate
                    return@launch
                }

                Napier.d(tag = TAG) {
                    "Latest: tag=${manifest.tag}, desktop=${manifest.desktopVersion}"
                }

                // ✅ desktopVersion тепер nullable
                val remoteDesktopVersion = manifest.desktopVersion
                if (!isNewer(remoteDesktopVersion)) {
                    // обробка сміття: якщо локально є файл під останній тег — прибрати
                    val candidateName = manifest.pickAsset(os)?.name
                    if (!candidateName.isNullOrBlank()) {
                        cleanupLocalIfExists(candidateName)
                    }

                    Napier.d(tag = TAG) { "No update: remote <= local" }
                    _state.value = UpdateState.NoUpdate
                    return@launch
                }

                val asset = manifest.pickAsset(os)
                if (asset == null) {
                    _state.value = UpdateState.Error("No asset for OS=$os")
                    return@launch
                }

                // ✅ якщо файл з таким ім’ям вже існує — прибрати перед завантаженням
                cleanupLocalIfExists(asset.name)

                _state.value = UpdateState.Available(UpdateInfo(manifest, asset))
                Napier.d(tag = TAG) { "Update available: ${asset.name}, size=${asset.size}" }
            } catch (t: Throwable) {
                Napier.e(tag = TAG, throwable = t) { "Check updates failed" }
                _state.value = UpdateState.Error("Check updates failed: ${t.message}", t)
            }
        }
    }

    private suspend fun cleanupLocalIfExists(fileName: String) {
        if (fileName.isBlank()) return

        // якщо в тебе немає цих методів — додай їх у PlatformFileProvider
        val exists = runCatching { PlatformFileProvider.isFileExist(fileName) }.getOrNull()
        if (exists == true) {
            Napier.d(tag = TAG) { "Local file exists, deleting: $fileName" }
            runCatching { PlatformFileProvider.deleteFile(fileName) }
                .onFailure { Napier.w(tag = TAG) { "Failed to delete $fileName: ${it.message}" } }
        }
    }

    private fun parseVersion(v: String): List<Int> =
        v.split(".").mapNotNull { it.toIntOrNull() }

    // ✅ приймає nullable
    private fun isNewer(remote: String?): Boolean {
        val rvStr = remote?.trim().orEmpty()
        if (rvStr.isBlank()) return false

        val r = parseVersion(rvStr)
        val l = parseVersion(currentDesktopVersion)

        val maxSize = max(r.size, l.size)
        for (i in 0 until maxSize) {
            val rv = r.getOrNull(i) ?: 0
            val lv = l.getOrNull(i) ?: 0
            if (rv != lv) return rv > lv
        }
        return false
    }

    fun downloadUpdate(info: UpdateInfo) {
        scope.launch {
            try {
                _state.value = UpdateState.Downloading(progress = null, downloaded = "0 B", total = "—")
                Napier.d(tag = TAG) { "Downloading: ${info.asset.url}" }

                val time = TimeSource.Monotonic
                var lastEmitAt = time.markNow()
                var lastPercent = -1
                var lastDownloadedBucket = -1L

                val result = downloader.downloadBytesWithProgress(
                    url = info.asset.url,
                    onProgress = { downloaded, total, progress ->
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

                        val bucket = downloaded / (512 * 1024)
                        val timeOk = lastEmitAt.elapsedNow() >= 200.milliseconds
                        val bucketOk = bucket != lastDownloadedBucket

                        if (timeOk || bucketOk) {
                            lastEmitAt = time.markNow()
                            lastDownloadedBucket = bucket
                            _state.value = UpdateState.Downloading(
                                progress = null,
                                downloaded = downloaded.formatBytes(1),
                                total = total.formatBytesOrDash(1),
                            )
                        }
                    }
                )

                val bytes = result.bytes

                val safeTag = info.manifest.tag?.ifBlank { null }
                val fileName = info.asset.name
                    .takeIf { it.isNotBlank() }
                    ?: "update-${safeTag ?: "latest"}"

                // ✅ на випадок, якщо воно вже є
                cleanupLocalIfExists(fileName)

                PlatformFileProvider.saveFile(fileName, bytes)

                _state.value = UpdateState.Verifying(info)
                val computed = sha256(bytes)

                if (info.asset.sha256.isNotBlank() && !computed.equals(info.asset.sha256, ignoreCase = true)) {
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

    private fun sha256(bytes: ByteArray): String {
        val md = MessageDigest.getInstance("SHA-256")
        md.update(bytes)
        return md.digest().joinToString("") { "%02x".format(it) }
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

                pendingInstall = null
                _state.value = UpdateState.Idle
                delay(1000)
                exitProcess(0)
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

    // ✅ було "— MB" — це дивно
    private fun Long?.formatBytesOrDash(decimals: Int = 1): String =
        this?.formatBytes(decimals) ?: "—"

    private fun detectDesktopOs(): DesktopOs {
        val name = System.getProperty("os.name").lowercase()
        return when {
            name.contains("win") -> DesktopOs.WINDOWS
            name.contains("mac") -> DesktopOs.MACOS
            else -> DesktopOs.LINUX
        }
    }
}

