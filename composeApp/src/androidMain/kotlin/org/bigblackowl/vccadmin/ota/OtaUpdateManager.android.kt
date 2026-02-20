@file:Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")

package org.bigblackowl.vccadmin.ota

import io.github.aakira.napier.Napier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.bigblackowl.vccadmin.BuildConfig
import org.bigblackowl.vccadmin.data.repository.NetworkMonitorProvider
import org.bigblackowl.vccadmin.data.repository.OtaUpdateRepository
import org.bigblackowl.vccadmin.data.utils.OtaDownloader
import org.bigblackowl.vccadmin.utils.PlatformFileProvider
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.pow
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource

actual class OtaUpdateManager(
    private val repo: OtaUpdateRepository,
    private val downloader: OtaDownloader,
    private val network: NetworkMonitorProvider,
) {
    private val currentVersion: String = BuildConfig.APP_VERSION

    private companion object {
        const val TAG = "OTA"
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _state = MutableStateFlow<UpdateState>(UpdateState.Idle)
    actual val state: StateFlow<UpdateState> = _state.asStateFlow()

    private var pending: PendingOpen? = null

    private var checkJob: Job? = null
    private var downloadJob: Job? = null

    private data class PendingOpen(val fileName: String, val info: UpdateInfo)

    actual fun check() {
        if (checkJob?.isActive == true) return
        checkJob = scope.launch {
            delay(250.milliseconds) // легкий debounce
            _state.value = UpdateState.Checking
            Napier.d(tag = TAG) { "Checking updates... local=$currentVersion (android)" }

            try {
                ensureInternetOrFail()

                val manifest = repo.fetchLatestManifest()
                if (manifest == null) {
                    _state.value = UpdateState.NoUpdate
                    return@launch
                }

                // ✅ для Android порівнюємо з android_version (є в твоєму JSON)
                val remote = manifest.androidVersion ?: manifest.versionName ?: manifest.tag
                if (!isRemoteNewer(remote)) {
                    manifest.assets.android?.name
                        ?.takeIf { it.isNotBlank() }
                        ?.let { cleanupLocalIfExists(it) }

                    _state.value = UpdateState.NoUpdate
                    return@launch
                }

                // ✅ беремо android asset
                val asset = manifest.assets.android ?: run {
                    _state.value = UpdateState.Error("No android asset in manifest")
                    return@launch
                }

                // guard: очікуємо apk
                if (!asset.name.endsWith(".apk", ignoreCase = true)) {
                    Napier.w(tag = TAG) { "Android asset is not .apk: ${asset.name}" }
                }

                cleanupLocalIfExists(asset.name)

                _state.value = UpdateState.Available(UpdateInfo(manifest, asset))
                Napier.d(tag = TAG) { "Update available: ${asset.name}, size=${asset.size}" }
            } catch (t: Throwable) {
                Napier.e(tag = TAG, throwable = t) { "Check failed" }
                _state.value = UpdateState.Error("Check failed: ${t.message}", t)
            }
        }
    }

    actual fun download() {
        val info = (state.value as? UpdateState.Available)?.info ?: return
        downloadIfAvailable(info)
    }

    /**
     * Android: НЕ інсталюємо APK.
     * Просто відкриваємо теку/екран файлів де лежить завантажений .apk.
     */
    actual fun install() {
        val p = pending ?: run {
            _state.value = UpdateState.Error("Немає завантаженого файлу. Спочатку скачай .apk.")
            return
        }

        if (state.value is UpdateState.Installing) return

        scope.launch {
            try {
                _state.value = UpdateState.Installing(p.info)
                PlatformFileProvider.openDownloadFolder()
                pending = null
                // Можна лишити ReadyToInstall, щоб кнопка “Install” лишалась як “Open folder”
                delay(300)
                _state.value = UpdateState.ReadyToInstall(p.info)
            } catch (t: Throwable) {
                _state.value = UpdateState.Error("Open folder failed: ${t.message}", t)
            }
        }
    }

    private fun downloadIfAvailable(info: UpdateInfo) {
        if (downloadJob?.isActive == true) return

        downloadJob = scope.launch(start = CoroutineStart.LAZY, context = Dispatchers.IO) {
            try {
                _state.value = UpdateState.Downloading(progress = null, downloaded = "0 B", total = "—")

                val time = TimeSource.Monotonic
                var lastEmit = time.markNow()
                var firstEmitted = false

                var lastDownloaded = 0L
                var lastTotal: Long? = null
                var lastProgress: Float? = null

                fun emit(force: Boolean = false) {
                    val shouldFirst = !firstEmitted && lastDownloaded > 0
                    val timeOk = lastEmit.elapsedNow() >= 200.milliseconds
                    if (!force && !shouldFirst && !timeOk) return

                    firstEmitted = true
                    lastEmit = time.markNow()

                    val p = lastProgress?.takeIf { it.isFinite() }?.coerceIn(0f, 1f)

                    _state.value = UpdateState.Downloading(
                        progress = if ((lastTotal ?: 0L) > 0L) p else null,
                        downloaded = lastDownloaded.formatBytes(1),
                        total = lastTotal?.formatBytesOrDash(1) ?: "—"
                    )
                }

                val result = downloader.downloadBytesWithProgress(
                    url = info.asset.url,
                    onProgress = { downloaded, total, progress ->
                        lastDownloaded = downloaded
                        lastTotal = total
                        lastProgress = progress
                        emit(force = true)
                    }
                )

                val bytes = result.bytes

                // фінальний емiт
                lastDownloaded = lastDownloaded.coerceAtLeast(bytes.size.toLong())
                lastTotal = lastTotal ?: bytes.size.toLong()
                lastProgress = 1f
                emit(force = true)

                val fileName = info.asset.name

                cleanupLocalIfExists(fileName)
                PlatformFileProvider.downloadFile(fileName, bytes)

                _state.value = UpdateState.Verifying(info)

                // SHA перевіряємо по файлу
                val computed = PlatformFileProvider.sha256OfSavedFile(fileName)
                if (info.asset.sha256.isNotBlank() &&
                    !computed.equals(info.asset.sha256, ignoreCase = true)
                ) {
                    Napier.e(tag = TAG) { "SHA256 mismatch. expected=${info.asset.sha256} computed=$computed" }
                    _state.value = UpdateState.Error("SHA256 mismatch. File may be corrupted.")
                    return@launch
                }

                pending = PendingOpen(fileName, info)
                _state.value = UpdateState.ReadyToInstall(info)
            } catch (t: Throwable) {
                Napier.e(tag = TAG, throwable = t) { "Download failed" }
                _state.value = UpdateState.Error("Download failed: ${t.message}", t)
            }
        }

        downloadJob?.start()
    }

    private suspend fun ensureInternetOrFail() {
        if (network.isConnected.value) return
        delay(4.seconds)
        if (!network.isConnected.value) error("No internet")
    }

    private suspend fun cleanupLocalIfExists(fileName: String) {
        if (fileName.isBlank()) return
        val exists = runCatching { PlatformFileProvider.isFileExist(fileName) }.getOrNull()
        if (exists == true) runCatching { PlatformFileProvider.deleteFile(fileName) }
    }

    private fun isRemoteNewer(remote: String?): Boolean {
        val rv = remote?.trim().orEmpty()
        if (rv.isBlank()) return false

        val r = rv.removePrefix("v").split(".").mapNotNull { it.toIntOrNull() }
        val l = currentVersion.removePrefix("v").split(".").mapNotNull { it.toIntOrNull() }

        val n = max(r.size, l.size)
        for (i in 0 until n) {
            val a = r.getOrNull(i) ?: 0
            val b = l.getOrNull(i) ?: 0
            if (a != b) return a > b
        }
        return false
    }

    private fun Long.formatBytes(decimals: Int = 1): String {
        if (this <= 0L) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (ln(this.toDouble()) / ln(1024.0)).toInt().coerceIn(0, units.lastIndex)
        val value = this / 1024.0.pow(digitGroups.toDouble())
        return "%.${decimals}f %s".format(value, units[digitGroups])
    }

    private fun Long?.formatBytesOrDash(decimals: Int = 1): String = this?.formatBytes(decimals) ?: "—"
}
