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
import org.bigblackowl.vccadmin.data.entity.AdminAppUpdate
import org.bigblackowl.vccadmin.data.entity.UpdateInfo
import org.bigblackowl.vccadmin.data.repository.NetworkMonitorProvider
import org.bigblackowl.vccadmin.data.repository.OtaUpdateRepository
import org.bigblackowl.vccadmin.data.utils.OtaDownloader
import org.bigblackowl.vccadmin.utils.AppStringProvider
import org.bigblackowl.vccadmin.utils.PlatformFileProvider
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource

actual class OtaUpdateManager(
    private val repo: OtaUpdateRepository,
    private val downloader: OtaDownloader,
    private val network: NetworkMonitorProvider,
) {
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
            delay(250.milliseconds) // debounce
            _state.value = UpdateState.Checking
            Napier.d(tag = TAG) { "Checking updates... android" }

            try {
                ensureInternetOrFail()

                // ✅ тепер це AdminAppUpdate?
                val manifest: AdminAppUpdate = repo.fetchLatestManifest() ?: run {
                    _state.value = UpdateState.NoUpdate
                    return@launch
                }

                // ✅ Android порівнюємо по manifest.version
                val remoteVersion = manifest.version?.trim().orEmpty()
                if (remoteVersion.isBlank()) {
                    _state.value = UpdateState.Error("Manifest has blank version")
                    return@launch
                }

                if (!AppStringProvider.isRemoteNewer(remoteVersion)) {
                    // якщо оновлення не треба — можна прибрати попередній apk, якщо раптом залишився
                    manifest.android?.name
                        ?.takeIf { it.isNotBlank() }
                        ?.let { cleanupLocalIfExists(it) }

                    _state.value = UpdateState.NoUpdate
                    return@launch
                }

                // ✅ беремо android asset напряму
                val asset = manifest.android ?: run {
                    _state.value = UpdateState.Error("No android asset in manifest")
                    return@launch
                }

                if (!asset.name.endsWith(".apk", ignoreCase = true)) {
                    Napier.w(tag = TAG) { "Android asset is not .apk: ${asset.name}" }
                }

                cleanupLocalIfExists(asset.name)

                // UpdateInfo має вміти тримати manifest + asset як раніше
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
                        downloaded = AppStringProvider.formatBytesToString(lastDownloaded),
                        total = lastTotal?.let { AppStringProvider.formatBytesToString(it) } ?: "—"
                    )
                }

                val result = downloader.downloadBytesWithProgress(
                    url = info.asset.url,
                    onProgress = { downloaded, total, progress ->
                        lastDownloaded = downloaded
                        lastTotal = total
                        lastProgress = progress
                        emit(force = false)
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

                // ✅ SHA перевіряємо лише якщо sha256 заданий в маніфесті
                val expected = info.asset.sha256?.trim().orEmpty()
                if (expected.isNotBlank()) {
                    val computed = PlatformFileProvider.sha256OfSavedFile(fileName)
                    if (!computed.equals(expected, ignoreCase = true)) {
                        Napier.e(tag = TAG) { "SHA256 mismatch. expected=$expected computed=$computed" }
                        _state.value = UpdateState.Error("SHA256 mismatch. File may be corrupted.")
                        return@launch
                    }
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
}