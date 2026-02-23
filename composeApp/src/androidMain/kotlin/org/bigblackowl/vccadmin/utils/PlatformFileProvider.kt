@file:Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")

package org.bigblackowl.vccadmin.utils

import android.app.DownloadManager
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.absolutePath
import io.github.vinceglb.filekit.createDirectories
import io.github.vinceglb.filekit.delete
import io.github.vinceglb.filekit.dialogs.openFileWithDefaultApplication
import io.github.vinceglb.filekit.dialogs.shareFile
import io.github.vinceglb.filekit.exists
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.bigblackowl.vccadmin.BuildConfig
import org.bigblackowl.vccadmin.resourses.DefaultValues
import org.bigblackowl.vccadmin.ui.fileGenerator.GeneratedFile
import org.jetbrains.compose.resources.getString
import org.koin.java.KoinJavaComponent.inject
import vccadministrator.composeapp.generated.resources.Res
import vccadministrator.composeapp.generated.resources.no_files_to_share
import vccadministrator.composeapp.generated.resources.share_failed
import vccadministrator.composeapp.generated.resources.share_pdf_files_as_zip_file_name
import vccadministrator.composeapp.generated.resources.zip_ready_to_share
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

actual object PlatformFileProvider {
    private val context: Context by inject(Context::class.java)

    private fun getDirLegacy(): String {
        val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val path = PlatformFile(PlatformFile(dir), child = BuildConfig.APP_NAME)
        path.createDirectories(mustCreate = false) // не падає, якщо вже існує
        return path.absolutePath()
    }

    actual val downloadFolderPath: String = getDirLegacy()

    actual fun openFile(fileName: String) {
        val relativePath = "${Environment.DIRECTORY_DOWNLOADS}/${BuildConfig.APP_NAME}/"
        val uri = findExistingDownloadUri(fileName, relativePath)

        if (uri != null) {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mimeFor(fileName))
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(intent)
        } else {
            // legacy fallback
            val file = PlatformFile("$downloadFolderPath${File.separator}$fileName")
            FileKit.openFileWithDefaultApplication(file)
        }
    }

    actual fun openDownloadFolder() {
        val dir = File(downloadFolderPath)

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            dir
        )

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "*/*") // інколи краще "*/*"
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        runCatching { context.startActivity(intent) }
            .getOrElse {
                // fallback: хоча б відкрий систему "Downloads"
                val fallback = Intent(DownloadManager.ACTION_VIEW_DOWNLOADS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(fallback)
            }
    }

    private fun mimeFor(name: String): String {
        val ext = name.substringAfterLast('.', "").lowercase()
        return when (ext) {
            "pdf" -> "application/pdf"
            "apk" -> "application/vnd.android.package-archive"
            "zip" -> "application/zip"
            else  -> "application/octet-stream"
        }
    }

    private fun findExistingDownloadUri(
        displayName: String,
        relativePath: String
    ): android.net.Uri? {
        val resolver = context.contentResolver
        val collection = MediaStore.Downloads.EXTERNAL_CONTENT_URI

        val projection = arrayOf(MediaStore.Downloads._ID)
        val selection = "${MediaStore.Downloads.DISPLAY_NAME}=? AND ${MediaStore.Downloads.RELATIVE_PATH}=?"
        val args = arrayOf(displayName, relativePath)

        resolver.query(collection, projection, selection, args, null)?.use { c ->
            if (c.moveToFirst()) {
                val id = c.getLong(c.getColumnIndexOrThrow(MediaStore.Downloads._ID))
                return ContentUris.withAppendedId(collection, id)
            }
        }
        return null
    }

    actual suspend fun downloadFile(name: String, content: ByteArray) {
        withContext(Dispatchers.IO) {
            val resolver = context.contentResolver
            val relativePath = "${Environment.DIRECTORY_DOWNLOADS}/${BuildConfig.APP_NAME}/"

            // 1) delete old if exists
            findExistingDownloadUri(name, relativePath)?.let { oldUri ->
                runCatching { resolver.delete(oldUri, null, null) }
            }

            // 2) insert new
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, name)
                put(MediaStore.Downloads.MIME_TYPE, mimeFor(name))
                put(MediaStore.Downloads.RELATIVE_PATH, relativePath)
                put(MediaStore.Downloads.IS_PENDING, 1)
            }

            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                ?: error("MediaStore insert failed")

            // 3) write bytes
            resolver.openOutputStream(uri, "w")!!.use { it.write(content) }

            // 4) finalize
            val done = ContentValues().apply { put(MediaStore.Downloads.IS_PENDING, 0) }
            resolver.update(uri, done, null, null)
        }
    }

    actual fun isFileExist(fileName: String): Boolean? {
        val relativePath = "${Environment.DIRECTORY_DOWNLOADS}/${BuildConfig.APP_NAME}/"
        return findExistingDownloadUri(fileName, relativePath) != null
    }

    actual suspend fun shareWithTelegram(data: String) {
        val telegramPkg = "org.telegram.messenger"

        val telegramIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, data)
            setPackage(telegramPkg)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        runCatching {
            context.startActivity(telegramIntent)
        }.getOrElse {
            // fallback: share sheet
            val chooser = Intent.createChooser(
                Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, data)
                },
                null
            ).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }

            context.startActivity(chooser)
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

        val zipName = getString(
            Res.string.share_pdf_files_as_zip_file_name,
            DefaultValues.Time.date
        ).ensureZipExt()

        return runCatching {
            val zipFile = withContext(Dispatchers.IO) {
                val outFile = File(context.cacheDir, zipName)

                ZipOutputStream(BufferedOutputStream(FileOutputStream(outFile))).use { zipOut ->
                    okFiles.forEach { f ->
                        val entryName = f.name.safeZipEntryName()
                        zipOut.putNextEntry(ZipEntry(entryName))
                        zipOut.write(f.content!!)
                        zipOut.closeEntry()
                    }
                }
                outFile
            }

            FileKit.shareFile(files = listOf(PlatformFile(zipFile.absolutePath)))

            ShareResult(
                state = true,
                message = getString(Res.string.zip_ready_to_share)
            )
        }.getOrElse {
            ShareResult(
                state = false,
                message = getString(Res.string.share_failed)
            )
        }
    }

    actual suspend fun deleteFile(fileName: String): Boolean = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        val relativePath = "${Environment.DIRECTORY_DOWNLOADS}/${BuildConfig.APP_NAME}/"

        val uri = findExistingDownloadUri(fileName, relativePath)
        if (uri != null) {
            runCatching { resolver.delete(uri, null, null) }.isSuccess
        } else {
            // fallback: якщо старі файли лежать у legacy path
            val file = PlatformFile(PlatformFile(downloadFolderPath), child = fileName)
            runCatching {
                file.delete()
                delay(200)
                !file.exists()
            }.getOrDefault(false)
        }
    }

    suspend fun sha256OfSavedFile(name: String): String = withContext(Dispatchers.IO) {
        val relativePath = "${Environment.DIRECTORY_DOWNLOADS}/${BuildConfig.APP_NAME}/"
        val uri = findExistingDownloadUri(name, relativePath)
            ?: error("File not found in MediaStore: $name")

        val md = MessageDigest.getInstance("SHA-256")
        context.contentResolver.openInputStream(uri)!!.use { input ->
            val buf = ByteArray(8192)
            while (true) {
                val n = input.read(buf)
                if (n <= 0) break
                md.update(buf, 0, n)
            }
        }
        md.digest().joinToString("") { "%02x".format(it) }
    }

    private fun String.ensureZipExt(): String = if (lowercase().endsWith(".zip")) this else "$this.zip"
    private fun String.safeZipEntryName(): String = replace("\\", "${File.separator}").substringAfterLast("${File.separator}").ifBlank { "file.pdf" }
}