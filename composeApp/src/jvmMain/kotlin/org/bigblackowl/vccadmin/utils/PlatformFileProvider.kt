@file:Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")

package org.bigblackowl.vccadmin.utils

import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.createDirectories
import io.github.vinceglb.filekit.delete
import io.github.vinceglb.filekit.dialogs.openFileWithDefaultApplication
import io.github.vinceglb.filekit.exists
import io.github.vinceglb.filekit.readBytes
import io.github.vinceglb.filekit.withScopedAccess
import io.github.vinceglb.filekit.write
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.bigblackowl.vccadmin.BuildConfig
import org.bigblackowl.vccadmin.resourses.DefaultValues
import org.bigblackowl.vccadmin.ui.fileGenerator.GeneratedFile
import org.jetbrains.compose.resources.getString
import vccadministrator.composeapp.generated.resources.Res
import vccadministrator.composeapp.generated.resources.copied_to_clipboard
import vccadministrator.composeapp.generated.resources.copy_failed
import vccadministrator.composeapp.generated.resources.no_files_to_share
import vccadministrator.composeapp.generated.resources.share_pdf_files_as_zip_file_name
import java.awt.Desktop
import java.awt.Toolkit
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.Transferable
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.net.URI
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.time.Duration.Companion.seconds

//jvm
actual object PlatformFileProvider {
//    private const val TAG = "PlatformFileProvider"
    private val userHome = System.getProperty("user.home")
    actual val downloadFolderPath = "$userHome${File.separator}${BuildConfig.APP_NAME}DownloadedFiles"

    actual fun openFile(fileName: String) {
        val file = PlatformFile("$downloadFolderPath${File.separator}$fileName")
        FileKit.openFileWithDefaultApplication(file)
    }

    fun startUpdates(fileName: String) {
        val file = File(downloadFolderPath, fileName)
        require(file.exists()) { "Update file not found: ${file.absolutePath}" }

        val os = System.getProperty("os.name").lowercase()
        when {
            os.contains("win") -> startOnWindows(file)
            os.contains("mac") -> startOnMac(file)
            else -> startOnLinux(file)
        }
    }

    fun startOnWindows(file: File) {
        val path = file.absolutePath
        val ext = file.extension.lowercase()

        when (ext) {
            "msi" -> {
                // Варіант 1 (рекомендовано): стандартний GUI інсталер
                // Це найменше ламається і нормально просить UAC якщо треба.
                runCmd(listOf("cmd", "/c", "start", "", "\"$path\""))
            }
            "exe" -> {
                runCmd(listOf("cmd", "/c", "start", "", "\"$path\""))
            }
            else -> {
                // fallback: просто відкрити як файл
                openWithDesktop(file)
            }
        }
    }

    private fun startOnMac(file: File) {
        val path = file.absolutePath
        val ext = file.extension.lowercase()

        when (ext) {
            "dmg" -> {
                // 1) приєднати dmg
                runCmd(listOf("hdiutil", "attach", path, "-nobrowse"))
                // 2) відкрити Finder на змонтованому volume (користувач натисне installer)
                // Зазвичай volume в /Volumes/<Name>
                runCmd(listOf("open", "/Volumes"))
            }
            "pkg" -> {
                runCmd(listOf("open", path))
            }
            else -> openWithDesktop(file)
        }
    }

    private fun startOnLinux(file: File) {
        val path = file.absolutePath
        val ext = file.extension.lowercase()

        when (ext) {
            "deb" -> {
                // Спочатку пробуємо xdg-open (GUI installer, якщо є)
                if (!runCmdNoThrow(listOf("xdg-open", path))) {
                    // Якщо pkexec доступний — спробувати встановити (попросить пароль)
                    // Не всі дистрибутиви мають pkexec (PolicyKit).
                    if (!runCmdNoThrow(listOf("pkexec", "sh", "-c", "dpkg -i '${path.replace("'", "'\\''")}'"))) {
                        // Fallback: просто відкриємо теку
                        runCmdNoThrow(listOf("xdg-open", file.parentFile.absolutePath))
                    }
                }
            }
            else -> openWithDesktop(file)
        }
    }

    private fun openWithDesktop(file: File) {
        if (Desktop.isDesktopSupported()) {
            runCatching { Desktop.getDesktop().open(file) }
                .getOrElse { throw IllegalStateException("Cannot open file: ${file.absolutePath}", it) }
        } else {
            throw IllegalStateException("Desktop API is not supported on this system")
        }
    }

    private fun runCmd(cmd: List<String>) {
        val ok = runCmdNoThrow(cmd)
        if (!ok) error("Command failed: ${cmd.joinToString(" ")}")
    }

    private fun runCmdNoThrow(cmd: List<String>): Boolean =
        runCatching {
            val p = ProcessBuilder(cmd)
                .redirectErrorStream(true)
                .start()
            // Не чекаємо завершення інсталятора, але можемо зачекати старт
            val exit = p.waitFor()
            exit == 0
        }.getOrDefault(false)
    suspend fun saveFile(name: String, content: ByteArray) {
        if (!PlatformFile(downloadFolderPath).exists()) PlatformFile(downloadFolderPath).createDirectories(true)
        val file = PlatformFile(PlatformFile(downloadFolderPath), child = name)
        file.withScopedAccess { file ->
            file.write(content)
        }
    }
    suspend fun sha256OfSavedFile(name: String): String {
        val file = PlatformFile(PlatformFile(downloadFolderPath), child = name)
        return file.withScopedAccess { f ->
            val md = MessageDigest.getInstance("SHA-256")
            val bytes = f.readBytes()
            val digest = md.digest(bytes)
            digest.joinToString("") { "%02x".format(it) }
        }
    }
    actual suspend fun downloadFile(name: String, content: ByteArray) {
        if (!PlatformFile(downloadFolderPath).exists()) PlatformFile(downloadFolderPath).createDirectories(true)
        val file = PlatformFile(PlatformFile(downloadFolderPath), child = name)
        file.withScopedAccess { file ->
            file.write(content)
        }
    }
    actual fun isFileExist(fileName: String): Boolean? = PlatformFile("$downloadFolderPath${File.separator}$fileName").exists()
    actual suspend fun shareWithTelegram(data: String) {
        withContext(Dispatchers.IO) {
            val encodedText = URLEncoder.encode(data, StandardCharsets.UTF_8.toString())
            Desktop.getDesktop().browse(URI("https://t.me/share/url?url=$encodedText"))
        }
    }
    actual suspend fun shareFilesAsZip(files: List<GeneratedFile>): ShareResult {
        val okFiles = files.asSequence().filter { it.error == null && it.content != null }.toList()
        if (okFiles.isEmpty()) {
            return ShareResult(
                state = false,
                message = getString(Res.string.no_files_to_share)
            )
        }
        val zipFile = withContext(Dispatchers.IO) { createZipTemp(okFiles) }
        val copiedOk = runCatching {
            copyFileToClipboard(zipFile)
            true
        }.getOrDefault(false)
        // telegram open: best-effort
        runCatching { Desktop.getDesktop().browse(URI("tg://resolve?domain=telegram")) }
            .onFailure { runCatching { Desktop.getDesktop().browse(URI("https://web.telegram.org")) } }
        return if (copiedOk) {
            ShareResult(
                state = true,
                message = getString(Res.string.copied_to_clipboard)
            )
        } else {
            ShareResult(
                state = false,
                message = getString(Res.string.copy_failed)
            )
        }
    }
    actual suspend fun deleteFile(fileName: String): Boolean {
        val file = PlatformFile(PlatformFile(downloadFolderPath), child = fileName)
        file.delete()
        delay(1.seconds)
        return file.exists()
    }
    actual fun openDownloadFolder() {
        FileKit.openFileWithDefaultApplication(PlatformFile(downloadFolderPath))
    }
    
    
    
    private fun copyFileToClipboard(file: File) {
        val clipboard = Toolkit.getDefaultToolkit().systemClipboard

        val transferable = object : Transferable {
            override fun getTransferDataFlavors(): Array<DataFlavor> =
                arrayOf(DataFlavor.javaFileListFlavor)

            override fun isDataFlavorSupported(flavor: DataFlavor): Boolean =
                flavor == DataFlavor.javaFileListFlavor

            override fun getTransferData(flavor: DataFlavor): Any {
                if (!isDataFlavorSupported(flavor)) {
                    throw UnsupportedOperationException("Unsupported flavor: $flavor")
                }
                return listOf(file)
            }
        }

        clipboard.setContents(transferable, null)
    }
    private suspend fun createZipTemp(files: List<GeneratedFile>): File {
        val zipName: String = getString(Res.string.share_pdf_files_as_zip_file_name, DefaultValues.Time.date) + ".zip"
        val safeName = zipName.ensureZipExt()
        val tmpDir = System.getProperty("java.io.tmpdir")
        val outFile = File(tmpDir, safeName)
        if (outFile.exists()) outFile.delete()

        withContext(Dispatchers.IO) {
            ZipOutputStream(BufferedOutputStream(FileOutputStream(outFile))).use { zipOut ->
                files.asSequence()
                    .filter { it.error == null && it.content != null }
                    .forEach { f ->
                        val entryName = f.name.safeZipEntryName()
                        zipOut.putNextEntry(ZipEntry(entryName))
                        zipOut.write(f.content!!)
                        zipOut.closeEntry()
                    }
            }
        }
        return outFile
    }
    private fun String.ensureZipExt(): String = if (lowercase().endsWith(".zip")) this else "$this.zip"
    private fun String.safeZipEntryName(): String = replace("\\", "/").substringAfterLast("/").ifBlank { "file.bin" }

}