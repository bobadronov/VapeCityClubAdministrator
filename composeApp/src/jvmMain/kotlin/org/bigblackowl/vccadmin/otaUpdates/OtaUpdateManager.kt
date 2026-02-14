//package org.bigblackowl.vccadmin.otaUpdates
//
//import io.github.jan.supabase.SupabaseClient
//import io.github.jan.supabase.postgrest.postgrest
//import io.github.jan.supabase.postgrest.query.Order
//import io.ktor.client.HttpClient
//import io.ktor.client.request.get
//import io.ktor.client.statement.bodyAsChannel
//import io.ktor.http.isSuccess
//import io.ktor.utils.io.readAvailable
//import kotlinx.coroutines.Dispatchers
//import kotlinx.coroutines.flow.MutableStateFlow
//import kotlinx.coroutines.flow.StateFlow
//import kotlinx.coroutines.flow.asStateFlow
//import kotlinx.coroutines.withContext
//import org.bigblackowl.vccadmin.data.repository.NetworkMonitorProvider
//import java.nio.file.Files
//import java.nio.file.Path
//import java.nio.file.StandardCopyOption
//import java.security.MessageDigest
//import kotlin.math.max
//import kotlin.system.exitProcess
//
//sealed class UpdateState {
//    data object Idle : UpdateState()
//    data object Checking : UpdateState()
//    data object NoUpdate : UpdateState()
//
//    data class Available(val info: UpdateInfo) : UpdateState()
//    data class Downloading(val progress: Float?) : UpdateState() // 0..1, може бути null
//    data class Downloaded(val file: Path, val info: UpdateInfo) : UpdateState()
//
//    data class Verifying(val info: UpdateInfo) : UpdateState()
//    data class ReadyToInstall(val file: Path, val info: UpdateInfo) : UpdateState()
//    data class Installing(val info: UpdateInfo) : UpdateState()
//
//    data class Relaunching(val command: List<String>) : UpdateState()
//    data class Error(val message: String, val cause: Throwable? = null) : UpdateState()
//}
//
//class OtaUpdateManager(
//    private val networkMonitorProvider: NetworkMonitorProvider,
//    private val supabase: SupabaseClient,
//    private val httpClient: HttpClient,
//    private val updatesTable: String = "admin_app_updates",
//    private val currentVersion: String,
//    private val platform: Platform, // WINDOWS / LINUX / MACOS
//    private val downloadUrlResolver: (UpdateInfo) -> String, // як отримати URL на файл
//    private val updatesDir: Path, // де зберігати інсталятори/пакети
//    private val appLaunchCommand: List<String>, // як запускати основну програму
//) {
//    private val _state = MutableStateFlow<UpdateState>(UpdateState.Idle)
//    val state: StateFlow<UpdateState> = _state.asStateFlow()
//
//    suspend fun runOnce() {
//        try {
//            _state.value = UpdateState.Checking
//            val update = fetchLatestUpdateIfNewer()
//            if (update == null) {
//                _state.value = UpdateState.NoUpdate
//                relaunchAppAndExit()
//                return
//            }
//
//            _state.value = UpdateState.Available(update)
//
//            val downloaded = downloadUpdate(update)
//            _state.value = UpdateState.Downloaded(downloaded, update)
//
//            _state.value = UpdateState.Verifying(update)
//            val ok = verifySha256(downloaded, update.fileHash)
//            if (!ok) {
//                safeDelete(downloaded)
//                _state.value = UpdateState.Error("SHA256 mismatch. File corrupted or wrong hash in DB.")
//                return
//            }
//
//            _state.value = UpdateState.ReadyToInstall(downloaded, update)
//            _state.value = UpdateState.Installing(update)
//            install(downloaded, update)
//
//            relaunchAppAndExit()
//        } catch (t: Throwable) {
//            _state.value = UpdateState.Error("Updater failed: ${t.message}", t)
//        }
//    }
//
//    private suspend fun fetchLatestUpdateIfNewer(): UpdateInfo? {
//        val list = supabase.postgrest
//            .from(updatesTable)
//            .select {
//                order("upload_time", Order.DESCENDING)
//                filter { eq("platform", platform) }
//            }
//            .decodeList<UpdateInfo>()
//
//        val latest = list.firstOrNull() ?: return null
//        return if (compareVersions(latest.version, currentVersion) > 0) latest else null
//    }
//
//    private suspend fun downloadUpdate(info: UpdateInfo): Path = withContext(Dispatchers.IO) {
//        Files.createDirectories(updatesDir)
//
//        val url = downloadUrlResolver(info)
//        val ext = installerExtension(platform)
//        val tmp = updatesDir.resolve("update_${info.version}$ext.part")
//        val target = updatesDir.resolve("update_${info.version}$ext")
//
//        val response = httpClient.get(url)
//        if (!response.status.isSuccess()) error("Download failed: HTTP ${response.status.value}")
//
//        val channel = response.bodyAsChannel() // <-- ОДИН раз
//        Files.newOutputStream(tmp).use { out ->
//            val buf = ByteArray(64 * 1024)
//            var total = 0L
//            val contentLen = response.headers["Content-Length"]?.toLongOrNull()
//
//            while (!channel.isClosedForRead) {
//                val n = channel.readAvailable(buf, 0, buf.size)
//                if (n <= 0) break
//                out.write(buf, 0, n)
//                total += n
//
//                _state.value =
//                    if (contentLen != null && contentLen > 0)
//                        UpdateState.Downloading(total.toFloat() / contentLen.toFloat())
//                    else
//                        UpdateState.Downloading(null)
//            }
//        }
//
//        // move tmp -> target з fallback, якщо atomic не підтримується
//        runCatching {
//            Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
//        }.getOrElse {
//            Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING)
//        }
//
//        target
//    }
//
//
//    private fun verifySha256(file: Path, expectedHex: String): Boolean {
//        val md = MessageDigest.getInstance("SHA-256")
//        Files.newInputStream(file).use { input ->
//            val buf = ByteArray(64 * 1024)
//            while (true) {
//                val r = input.read(buf)
//                if (r <= 0) break
//                md.update(buf, 0, r)
//            }
//        }
//        val actual = md.digest().toHex()
//        return actual.equals(expectedHex.trim(), ignoreCase = true)
//    }
//
//    private fun install(file: Path, info: UpdateInfo) {
//        when (platform) {
//            Platform.WINDOWS -> installWindowsMsi(file, info)
//            Platform.LINUX -> runAndCheck(
//                listOf("bash", "-lc", "sudo dpkg -i '${file.toAbsolutePath()}'")
//            )
//            Platform.MACOS -> runAndCheck(
//                listOf("bash", "-lc", "open '${file.toAbsolutePath()}'")
//            )
//            else -> error("Unsupported platform in updater: $platform")
//        }
//    }
//
//    private fun installWindowsMsi(file: Path, info: UpdateInfo) {
//        val logFile = updatesDir
//            .resolve("msi_install_${info.version}.log")
//            .toAbsolutePath()
//            .toString()
//
//        val cmd = listOf(
//            "msiexec",
//            "/i", file.toAbsolutePath().toString(),
//            "/qn",
//            "/norestart",
//            "/L*v", logFile
//        )
//
//        // 0 = success
//        // 3010 = success, reboot required
//        when (val exit = run(cmd)) {
//            0, 3010 -> return
//            else -> error("MSI install failed. Exit=$exit. Log=$logFile")
//        }
//    }
//
//    private fun runAndCheck(cmd: List<String>) {
//        val exit = run(cmd)
//        if (exit != 0) error("Installer exit code: $exit")
//    }
//
//    private fun run(cmd: List<String>): Int {
//        val p = ProcessBuilder(cmd)
//            .redirectErrorStream(true)
//            .start()
//
//        // (опційно) можна читати stdout/stderr для дебагу
//        // p.inputStream.bufferedReader().useLines { lines -> lines.forEach { Napier.d { it } } }
//
//        return p.waitFor()
//    }
//
//    private fun relaunchAppAndExit() {
//        _state.value = UpdateState.Relaunching(appLaunchCommand)
//        ProcessBuilder(appLaunchCommand).start()
//        // Updater process завершується, щоб не висіти
//        exitProcess(0)
//    }
//
//    private fun compareVersions(v1: String, v2: String): Int {
//        val p1 = v1.split(".").map { it.toIntOrNull() ?: 0 }
//        val p2 = v2.split(".").map { it.toIntOrNull() ?: 0 }
//        val maxLen = max(p1.size, p2.size)
//        for (i in 0 until maxLen) {
//            val a = p1.getOrNull(i) ?: 0
//            val b = p2.getOrNull(i) ?: 0
//            if (a != b) return a - b
//        }
//        return 0
//    }
//
//    private fun installerExtension(platform: Platform): String = when (platform) {
//        Platform.WINDOWS -> ".msi"
//        Platform.LINUX -> ".deb"
//        Platform.MACOS -> ".dmg" // або ".pkg" — як у тебе
//        else -> ""
//    }
//
//    private fun safeDelete(p: Path) = runCatching { Files.deleteIfExists(p) }
//
//    private fun ByteArray.toHex(): String =
//        joinToString("") { b -> (b.toInt() and 0xFF).toString(16).padStart(2, '0') }
//}
