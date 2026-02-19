@file:Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")

package org.bigblackowl.vccadmin.utils

import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.absolutePath
import io.github.vinceglb.filekit.dialogs.openFileWithDefaultApplication
import io.github.vinceglb.filekit.dialogs.shareFile
import io.github.vinceglb.filekit.exists
import io.github.vinceglb.filekit.filesDir
import io.github.vinceglb.filekit.withScopedAccess
import io.github.vinceglb.filekit.write
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.bigblackowl.vccadmin.resourses.DefaultValues
import org.bigblackowl.vccadmin.ui.fileGenerator.GeneratedFile
import org.jetbrains.compose.resources.getString
import platform.Foundation.*
import platform.UIKit.UIApplication
import vccadministrator.composeapp.generated.resources.Res
import vccadministrator.composeapp.generated.resources.no_files_to_share
import vccadministrator.composeapp.generated.resources.share_failed
import vccadministrator.composeapp.generated.resources.share_pdf_files_as_zip_file_name
import vccadministrator.composeapp.generated.resources.zip_ready_to_share

import cocoapods.ZIPFoundation.Archive
import cocoapods.ZIPFoundation.ArchiveAccessMode
import cocoapods.ZIPFoundation.EntryType
import io.github.vinceglb.filekit.delete
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.seconds

// ios
actual object PlatformFileProvider {
    actual val downloadFolderPath = FileKit.filesDir.absolutePath()

    actual fun openFile(fileName: String) {
        val file = PlatformFile("$downloadFolderPath/$fileName")
        FileKit.openFileWithDefaultApplication(file)
    }
    actual fun openDownloadFolder() {
        FileKit.openFileWithDefaultApplication(PlatformFile(downloadFolderPath))
    }
    actual suspend fun downloadFile(name: String, content: ByteArray) {
        val file = PlatformFile(PlatformFile(downloadFolderPath), child = name)
        file.withScopedAccess { scoped ->
            scoped.write(content)
        }
    }
    actual suspend fun deleteFile(fileName: String): Boolean {
        val file = PlatformFile(PlatformFile(downloadFolderPath), child = fileName)
        file.delete()
        delay(1.seconds)
        return file.exists()
    }
    actual fun isFileExist(fileName: String): Boolean? =
        PlatformFile("$downloadFolderPath/$fileName").exists()

    actual suspend fun shareWithTelegram(data: String) {
        val encoded = (data as NSString)
            .stringByAddingPercentEncodingWithAllowedCharacters(NSCharacterSet.URLQueryAllowedCharacterSet())
            ?: return

        // Telegram share: text=
        val urlString = "https://t.me/share/url?text=$encoded"
        val url = NSURL.URLWithString(urlString) ?: return

        // newer iOS API
        UIApplication.sharedApplication.openURL(url)
    }

    @OptIn(ExperimentalForeignApi::class)
    actual suspend fun shareFilesAsZip(files: List<GeneratedFile>): ShareResult {
        val okFiles = files.asSequence().filter { it.error == null && it.content != null }.toList()
        if (okFiles.isEmpty()) {
            return ShareResult(
                state = false,
                message = getString(Res.string.no_files_to_share)
            )
        }

        val zipName = getString(
            Res.string.share_pdf_files_as_zip_file_name,
            DefaultValues.Time.date
        ).ensureZipExt()
            .sanitizeFileName() // <— важливо

        val zipPath = runCatching {
            withContext(Dispatchers.Default) {
                val fm = NSFileManager.defaultManager
                val tmpDir = NSTemporaryDirectory()

                // НЕ чіпаємо tmpDir, санітизуємо тільки ім’я
                val zipPath = tmpDir + zipName
                fm.removeItemAtPath(zipPath, error = null)

                val zipUrl = NSURL.fileURLWithPath(zipPath)

                val archive = Archive(
                    url = zipUrl,
                    accessMode = ArchiveAccessMode.ArchiveAccessModeCreate
                ) ?: error("Cannot create ZIP archive at $zipPath")

                okFiles.forEach { f ->
                    val entryName = f.name.safeZipEntryName()
                    val data = f.content!!.toNSData()

                    archive.addEntry(
                        path = entryName,
                        type = EntryType.EntryTypeFile,
                        uncompressedSize = data.length,
                        provider = { position, size ->
                            val start = position.toLong()
                            val end = (start + size.toLong()).coerceAtMost(data.length.toLong())
                            if (start >= end) return@addEntry null
                            data.subdataWithRange(NSMakeRange(start, end - start))
                        }
                    )
                }

                zipPath
            }
        }.getOrNull() ?: return ShareResult(
            state = false,
            message = getString(Res.string.share_failed)
        )

        // Share sheet через FileKit (best practice: scoped)
        val shared = runCatching {
            FileKit.shareFile(files = listOf(PlatformFile(zipPath)))
            true
        }.getOrDefault(false)

        return if (shared) {
            ShareResult(true, getString(Res.string.zip_ready_to_share))
        } else {
            ShareResult(false, getString(Res.string.share_failed))
        }
    }

    private fun String.ensureZipExt(): String =
        if (lowercase().endsWith(".zip")) this else "$this.zip"

    private fun String.safeZipEntryName(): String =
        replace("\\", "/").substringAfterLast("/").ifBlank { "file.pdf" }

    // iOS файли не люблять деякі символи в іменах
    private fun String.sanitizeFileName(): String =
        replace("/", "_").replace("\\", "_").replace(":", "_")

    @OptIn(ExperimentalForeignApi::class)
    private fun ByteArray.toNSData(): NSData =
        usePinned { pinned -> NSData.dataWithBytes(pinned.addressOf(0), size.toULong()) }
}
