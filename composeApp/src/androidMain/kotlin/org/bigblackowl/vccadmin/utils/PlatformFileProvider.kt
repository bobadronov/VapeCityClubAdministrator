@file:Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")

package org.bigblackowl.vccadmin.utils

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.os.Environment
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.delete
import io.github.vinceglb.filekit.dialogs.openFileWithDefaultApplication
import io.github.vinceglb.filekit.dialogs.shareFile
import io.github.vinceglb.filekit.exists
import io.github.vinceglb.filekit.withScopedAccess
import io.github.vinceglb.filekit.write
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
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
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.time.Duration.Companion.seconds

actual object PlatformFileProvider {
    private val context: Context by inject(Context::class.java)
    actual val downloadFolderPath: String = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).absolutePath

    actual fun openFile(fileName: String) {
        val file = PlatformFile("$downloadFolderPath/$fileName")
        FileKit.openFileWithDefaultApplication(file)
    }

    actual fun openDownloadFolder() {
        val intent = Intent(DownloadManager.ACTION_VIEW_DOWNLOADS)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(intent)
    }

    actual suspend fun downloadFile(name: String, content: ByteArray) {
        val file = PlatformFile(PlatformFile(downloadFolderPath), child = name)
        file.withScopedAccess { file ->
            file.write(content)
        }
    }

    actual fun isFileExist(fileName: String): Boolean? = PlatformFile("$downloadFolderPath${File.separator}$fileName").exists()

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

    actual suspend fun deleteFile(fileName: String): Boolean {
        val file = PlatformFile(PlatformFile(downloadFolderPath), child = fileName)
        file.delete()
        delay(1.seconds)
        return file.exists()
    }

    private fun String.ensureZipExt(): String =
        if (lowercase().endsWith(".zip")) this else "$this.zip"

    private fun String.safeZipEntryName(): String =
        replace("\\", "/").substringAfterLast("/").ifBlank { "file.pdf" }
}