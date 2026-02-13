//package org.bigblackowl.vccadmin.otaUpdates
//
//import VCCAdministrator.composeApp.BuildConfig
//import androidx.compose.material.icons.Icons
//import androidx.compose.material.icons.filled.CheckCircle
//import androidx.compose.material.icons.filled.CloudDownload
//import androidx.compose.material.icons.filled.Error
//import androidx.compose.material.icons.filled.Sync
//import androidx.compose.material.icons.filled.SystemUpdate
//import androidx.compose.ui.graphics.vector.ImageVector
//import androidx.lifecycle.ViewModel
//import androidx.lifecycle.viewModelScope
//import io.github.aakira.napier.Napier
//import io.github.jan.supabase.SupabaseClient
//import io.github.jan.supabase.postgrest.postgrest
//import io.github.jan.supabase.postgrest.query.Order
//import io.github.vinceglb.filekit.PlatformFile
//import io.github.vinceglb.filekit.readBytes
//import kotlinx.coroutines.delay
//import kotlinx.coroutines.flow.MutableStateFlow
//import kotlinx.coroutines.flow.StateFlow
//import kotlinx.coroutines.flow.asStateFlow
//import kotlinx.coroutines.launch
//import kotlinx.serialization.SerialName
//import kotlinx.serialization.Serializable
//import org.bigblackowl.vccadmin.resourses.StringProvider
//import org.kotlincrypto.hash.sha2.SHA256
//import kotlin.math.max
//import kotlin.time.Duration.Companion.seconds
//
//@Serializable
//data class UpdateInfo(
//    val id: String,
//    val version: String,
//    val changelog: String,
//    @SerialName("platform") val platform: Platform,
//    @SerialName("file_path") val filePath: String,
//    @SerialName("file_hash") val fileHash: String, // SHA256
//    @SerialName("upload_time") val uploadTime: Long,
//)
//
//sealed class UpdateState {
//    object Checking : UpdateState()
//    object NoUpdateAvailable : UpdateState()
//    data class Available(val updateInfo: UpdateInfo) : UpdateState()
//    object Downloading : UpdateState()
//    data class Downloaded(val filePath: String, val updateInfo: UpdateInfo) : UpdateState()
//    data class ReadyToInstall(val filePath: String, val updateInfo: UpdateInfo) : UpdateState()
//    data class Error(val message: String) : UpdateState()
//
//    companion object {
//        fun UpdateState.getStatusIcons(): ImageVector = when (this) {
//            is NoUpdateAvailable -> Icons.Default.CheckCircle
//            is Checking -> Icons.Default.Sync
//            is Available -> Icons.Default.CloudDownload
//            is Downloading -> Icons.Default.CloudDownload
//            is Downloaded -> Icons.Default.SystemUpdate
//            is ReadyToInstall -> Icons.Default.SystemUpdate
//            is Error -> Icons.Default.Error
//        }
//
//        fun UpdateState.getDescription(): String = when (this) {
//            is NoUpdateAvailable -> StringProvider.updateNotAvailable
//            is Checking -> StringProvider.updateChecking
//            is Available -> StringProvider.updateAvailable
//            is Downloading -> StringProvider.updateDownloading
//            is Downloaded -> StringProvider.updateDownloaded
//            is ReadyToInstall -> StringProvider.updateReadyToInstall
//            is Error -> message
//        }
//    }
//}
//
//class OtaUpdateManager(
//    private val supabase: SupabaseClient,
//    private val connectivityManager: ConnectivityManager,
//) : ViewModel() {
//
//    private companion object {
//        private const val TAG = "UpdatesRepository"
//        private const val UPDATES_TABLE = "admin_app_updates"
//    }
//
//    val currentVersion: String = BuildConfig.APP_VERSION
//
//    private val _state = MutableStateFlow<UpdateState>(UpdateState.NoUpdateAvailable)
//    val state: StateFlow<UpdateState> = _state.asStateFlow()
//
//    private var downloadedFilePath: String? = null
//    private var currentUpdateInfo: UpdateInfo? = null
//
//    init {
//        startUpdates()
//    }
//
//    fun retryCheck() = startUpdates()
//
//    fun installUpdate() {
//        val filePath = downloadedFilePath ?: return
//        val updateInfo = currentUpdateInfo ?: return
//
//        viewModelScope.launch {
//            try {
//                installUpdateForPlatform(filePath, updateInfo)
//
//                _state.value = UpdateState.NoUpdateAvailable
//                currentUpdateInfo = null
//                downloadedFilePath = null
//            } catch (e: Exception) {
//                _state.value = UpdateState.Error("Installation failed: ${e.message}")
//                Napier.e(tag = TAG) { e.stackTraceToString() }
//            }
//        }
//    }
//
//    private fun startUpdates() {
//        viewModelScope.launch {
//            try {
//                _state.value = UpdateState.Checking
//                delay(1.seconds)
//                val updateInfo = if (connectivityManager.isInternetAvailable()) getUpdates(currentVersion) else null
//
//                if (updateInfo == null) {
//                    checkForPendingOrLeftoverUpdates()
//                    return@launch
//                }
//
//                currentUpdateInfo = updateInfo
//                _state.value = UpdateState.Available(updateInfo)
//
//                downloadUpdate(updateInfo)
//
//            } catch (e: Exception) {
//                _state.value = UpdateState.Error("Update check failed: ${e.message}")
//                Napier.e(tag = TAG) { e.stackTraceToString() }
//            }
//        }
//    }
//
//    private suspend fun checkForPendingOrLeftoverUpdates() {
////        try {
////            val folderName = "updates"
////
////            val directory = PlatformFile(provider = getAppDirectory())
////            if (!ensureDirectoryExists(directory)) return
////
////            val rootFolder = directory.resolve(folderName)
////            if (!ensureDirectoryExists(rootFolder)) return
////
////            val fileExtension = getInstallerExtension()
////            val files = rootFolder.list().filter { file ->
////                file.name.startsWith("update_") && file.name.endsWith(fileExtension)
////            }
////
////            if (files.isEmpty()) {
////                _state.value = UpdateState.NoUpdateAvailable
////                return
////            }
////
////            // Знаходимо останній файл за версією
////            val latestFile = files.maxByOrNull { file ->
////                val versionStr = file.name.removePrefix("update_").removeSuffix(fileExtension)
////                compareVersions(versionStr, "0.0.0") // для сортування
////            } ?: return
////
////            val fileVersionStr = latestFile.name.removePrefix("update_").removeSuffix(fileExtension)
////            val compareResult = compareVersions(fileVersionStr, currentVersion)
////
////            when {
////                compareResult > 0 -> {
////                    // Pending: версія > поточної, встановлення не вдалося, повторюємо
////                    val pendingInfo = if (connectivityManager.isInternetAvailable()) {
////                        getUpdateInfoByVersion(fileVersionStr, currentPlatform())
////                    } else {
////                        UpdateInfo(
////                            id = "",
////                            version = fileVersionStr,
////                            changelog = "Update ready to install",
////                            filePath = "",
////                            fileHash = "",
////                            uploadTime = 0L,
////                            platform = currentPlatform()
////                        )
////                    }
////
////                    if (pendingInfo?.fileHash.isNullOrBlank() || verifyHash(latestFile, pendingInfo.fileHash)) {
////                        currentUpdateInfo = pendingInfo
////                        downloadedFilePath = latestFile.path
////                        _state.value = UpdateState.ReadyToInstall(latestFile.path, pendingInfo!!)
////                    } else {
////                        _state.value = UpdateState.Error("Hash verification failed. File may be corrupted.")
////                        latestFile.delete()
////                        downloadedFilePath = null
////                    }
////                }
////
////                compareResult == 0 -> {
////                    // Встановлено: версія == поточної, видаляємо файл
////                    latestFile.delete()
////                    _state.value = UpdateState.NoUpdateAvailable
////                }
////
////                else -> {
////                    // Старий файл, видаляємо
////                    latestFile.delete()
////                    _state.value = UpdateState.NoUpdateAvailable
////                }
////            }
////        } catch (e: Exception) {
////            _state.value = UpdateState.Error("Pending check failed: ${e.message}")
////            Napier.e(tag = TAG) { e.stackTraceToString() }
////        }
//    }
//
//    private fun parseVersion(version: String): List<Int> =
//        version.split(".").map { it.toIntOrNull() ?: 0 }
//
//    private suspend fun getUpdateInfoByVersion(version: String, platform: Platform): UpdateInfo? {
//        return try {
//            val response = supabase.postgrest
//                .from(UPDATES_TABLE)
//                .select {
//                    filter {
//                        eq("version", version)
//                        eq("platform", platform)
//                    }
//                }
//                .decodeList<UpdateInfo>()
//                .firstOrNull()
//            response
//        } catch (e: Exception) {
//            Napier.e(tag = TAG) { e.stackTraceToString() }
//            null
//        }
//    }
//
//    private suspend fun getUpdates(currentVersion: String): UpdateInfo? {
//        try {
//            val response = supabase.postgrest.from(UPDATES_TABLE).select {
//                order("upload_time", Order.DESCENDING)
//                filter { eq("platform", currentPlatform()) }
//            }.decodeList<UpdateInfo>()
//
//            if (response.isEmpty()) return null
//
//            val newerUpdate = response.firstOrNull { update ->
//                compareVersions(update.version, currentVersion) > 0
//            }
//
//            return newerUpdate
//        } catch (e: Exception) {
//            _state.value = UpdateState.Error("Failed to fetch updates: ${e.message}")
//            Napier.e(tag = TAG) { e.stackTraceToString() }
//            return null
//        }
//    }
//
//    private fun compareVersions(v1: String, v2: String): Int {
//        val parts1 = parseVersion(v1)
//        val parts2 = parseVersion(v2)
//        val maxLength = max(parts1.size, parts2.size)
//        for (i in 0 until maxLength) {
//            val p1 = parts1.getOrNull(i) ?: 0
//            val p2 = parts2.getOrNull(i) ?: 0
//            if (p1 != p2) return p1 - p2
//        }
//        return 0
//    }
//
//    private suspend fun downloadUpdate(updateInfo: UpdateInfo) {
////        try {
////            val bucket = supabase.storage.from("app_updates")
////            val fileObject = bucket.info(updateInfo.filePath)
////            val size = fileObject.size
////            Napier.d(tag = TAG) { "Expected size: $size bytes for ${updateInfo.filePath}" }
////            if (size <= 0) {
////                _state.value = UpdateState.Error("Empty file in storage: ${updateInfo.filePath}")
////                return
////            }
////
////            _state.value = UpdateState.Downloading
////
////            val folderName = "updates"
////            val directory = io.github.vinceglb.filekit.PlatformFile(getAppDirectory())
////            if (!ensureDirectoryExists(directory)) {
////                _state.value = UpdateState.Error("Failed to create app directory")
////                return
////            }
////
////            val rootFolder = directory.resolve(folderName)
////            if (!ensureDirectoryExists(rootFolder)) {
////                _state.value = UpdateState.Error("Failed to create updates directory")
////                return
////            }
////
////            val fileExtension = getInstallerExtension()
////            val fileName = "update_${updateInfo.version}$fileExtension"
////            val updateFile = rootFolder.resolve(fileName)
////
////            val response: ByteArray = supabase.storage.from("app_updates").downloadPublic(updateInfo.filePath)
////            updateFile.write(response)
////
////            _state.value = UpdateState.Downloaded(updateFile.path, updateInfo)
////            downloadedFilePath = updateFile.path
////
////            if (verifyHash(updateFile, updateInfo.fileHash)) {
////                _state.value = UpdateState.ReadyToInstall(updateFile.path, updateInfo)
////            } else {
////                _state.value = UpdateState.Error("Hash verification failed. File may be corrupted.")
////                updateFile.delete()
////                downloadedFilePath = null
////            }
////        } catch (e: Exception) {
////            _state.value = UpdateState.Error("Download failed: ${e.message}")
////            Napier.e(tag = TAG) { e.stackTraceToString() }
////        }
//    }
//
//    private suspend fun verifyHash(file: PlatformFile, expectedHash: String): Boolean {
//        return try {
//            val computedHash = computeFileHashSHA256(file)
//            Napier.d(tag = TAG) { "HASH===============\nCheck:$computedHash\nExpect:$expectedHash\nHASH===============" }
//            computedHash.equals(expectedHash, ignoreCase = true)
//        } catch (e: Exception) {
//            Napier.e(tag = TAG) { e.stackTraceToString() }
//            false
//        }
//    }
//
//    private suspend fun computeFileHashSHA256(file: PlatformFile): String {
//        val bytes = file.readBytes()
//        val digest = SHA256()
//        digest.update(bytes)
//        val hash = digest.digest()
//        return hash.joinToString("") { byte ->
//            val unsigned = byte.toInt() and 0xFF
//            unsigned.toString(16).padStart(2, '0')
//        }
//    }
//
//    private fun getInstallerExtension(): String {
//        return when (currentPlatform()) {
//            Platform.ANDROID -> ".apk"
//            Platform.WINDOWS -> ".msi"
//            Platform.LINUX -> ".deb"
//            Platform.IOS -> ".ipa"
//            Platform.MACOS -> ".dmg"
//            Platform.WEB -> "" //TODO
//        }
//    }
//
//    private suspend fun ensureDirectoryExists(directory: PlatformFile): Boolean {
////        if (!directory.exists()) {
////            directory.createDirectories()
////        }
////        repeat(4) {
////            if (directory.exists()) return true
////            delay(500)
////        }
////        return directory.exists()
//        return false
//    }
//}
//@Serializable
//enum class Platform {
//    ANDROID,
//    IOS,
//    WINDOWS,
//    LINUX,
//    MACOS,
//    WEB,
//}
//
//expect fun currentPlatform(): Platform
//expect suspend fun installUpdateForPlatform(filePath: String, updateInfo: UpdateInfo)