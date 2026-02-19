@file:Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
@file:OptIn(ExperimentalWasmJsInterop::class)

package org.bigblackowl.vccadmin.utils

import io.github.aakira.napier.Napier
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.download
import kotlinx.browser.window
import org.bigblackowl.vccadmin.resourses.DefaultValues
import org.bigblackowl.vccadmin.ui.fileGenerator.GeneratedFile
import org.jetbrains.compose.resources.getString
import org.khronos.webgl.Uint8Array
import vccadministrator.composeapp.generated.resources.Res
import vccadministrator.composeapp.generated.resources.download_failed
import vccadministrator.composeapp.generated.resources.download_started
import vccadministrator.composeapp.generated.resources.no_files_to_share
import vccadministrator.composeapp.generated.resources.share_pdf_files_as_zip_file_name

//wasm/js
actual object PlatformFileProvider {
    private val memoryCache = mutableMapOf<String, ByteArray>() // name -> bytes

    actual fun openFile(fileName: String) {
        val bytes = memoryCache[fileName]
        if (bytes == null) {
            Napier.w(tag = "PlatformFileProvider") { "File not found in memory cache: $fileName" }
            return
        }
        openBytesInNewTab(bytes.toJsNumberArray(), "application/pdf", fileName)
    }

    actual fun openDownloadFolder() {
        // Працює не всюди/не завжди. Викликати тільки з onClick!
        val candidates = listOf(
            "chrome://downloads/",
            "edge://downloads/all",
            "about:downloads"
        )

        for (url in candidates) {
            val w = window.open(url, "_blank")
            if (w != null) return
        }

        // fallback
        window.alert("Не вдалося відкрити сторінку завантажень.\nВідкрий вручну: Ctrl+J (⌘+J на macOS).")
    }

    actual suspend fun downloadFile(name: String, content: ByteArray) {
        // зберігаємо, щоб openFile міг відкрити
        memoryCache[name] = content
//        FileKit.download(bytes = content, fileName = name)
    }

    actual fun isFileExist(fileName: String): Boolean? =
        memoryCache.containsKey(fileName)

    actual suspend fun shareWithTelegram(data: String) {
        val encodedText = encodeURIComponent(data)
        window.open(
            "https://t.me/share?url=Деталі магазину&text=$encodedText",
            "_blank",
            "noopener,noreferrer"
        )
    }

    actual suspend fun deleteFile(fileName: String): Boolean = false

    actual suspend fun shareFilesAsZip(files: List<GeneratedFile>): ShareResult {
        val okFiles = files.asSequence().filter { it.error == null && it.content != null }.toList()
        if (okFiles.isEmpty()) {
            return ShareResult(false, getString(Res.string.no_files_to_share))
        }

        val zipName = getString(
            Res.string.share_pdf_files_as_zip_file_name,
            DefaultValues.Time.date
        ) + ".zip"

        val zipBytes = createZipBytesWasm(okFiles)

        return runCatching {
            FileKit.download(bytes = zipBytes, fileName = zipName)
            ShareResult(true, getString(Res.string.download_started))
        }.getOrElse {
            ShareResult(false, getString(Res.string.download_failed))
        }
    }

    @OptIn(ExperimentalWasmJsInterop::class)
    private fun createZipBytesWasm(files: List<GeneratedFile>): ByteArray {
        val zipObj: JsAny = newZipObject()

        for (f in files) {
            val bytes = f.content ?: continue
            if (bytes.isEmpty()) continue
            if (f.error != null) continue

            val entryName = f.name.safeZipEntryName()
            putZipEntry(zipObj, entryName, bytes.toUint8ArrayWasm()) // <-- ВАЖЛИВО: entryName, не zipName
        }

        val zippedU8: Uint8Array = Fflate.zipSync(zipObj)
        return zippedU8.toByteArrayWasm()
    }

    actual val downloadFolderPath: String = ""
    private fun String.safeZipEntryName(): String =
        replace("\\", "/").substringAfterLast("/").ifBlank { "file.pdf" }
}

@OptIn(ExperimentalWasmJsInterop::class)
@JsFun(
    """
    (function(bytes, mime, fileName) {
      const u8 = new Uint8Array(bytes.length);
      for (let i = 0; i < bytes.length; i++) u8[i] = bytes[i] & 255;

      const blob = new Blob([u8], { type: mime });
      const url = URL.createObjectURL(blob);

      // open in new tab
      window.open(url, "_blank", "noopener,noreferrer");

      // optional: revoke later
      setTimeout(() => URL.revokeObjectURL(url), 60_000);
    })
"""
)
external fun openBytesInNewTab(bytes: JsAny, mime: String, fileName: String)

@OptIn(ExperimentalWasmJsInterop::class)
@JsFun("""(function(len){ return new Array(len); })""")
external fun jsNewNumberArray(len: Int): JsAny

@OptIn(ExperimentalWasmJsInterop::class)
@JsFun("""(function(arr, i, v){ arr[i] = v; })""")
external fun jsSetNumberArray(arr: JsAny, i: Int, v: Int)

@OptIn(ExperimentalWasmJsInterop::class)
private fun ByteArray.toJsNumberArray(): JsAny {
    val arr = jsNewNumberArray(size)
    for (i in indices) jsSetNumberArray(arr, i, this[i].toInt() and 0xFF)
    return arr
}

@JsFun(
    """
    function encode(value) { return encodeURIComponent(value); }
"""
)
private external fun encodeURIComponent(value: String): String

@JsModule("fflate")
private external object Fflate {
    fun zipSync(data: JsAny): Uint8Array
}


@JsFun(""" function newZipObject() { return {}; } """)
private external fun newZipObject(): JsAny

@JsFun(""" function putZipEntry(obj, name, data) { obj[name] = data; } """)
private external fun putZipEntry(obj: JsAny, name: String, data: Uint8Array)

@JsFun(""" function u8Alloc(len) { return new Uint8Array(len); } """)
private external fun u8Alloc(len: Int): Uint8Array

@JsFun(""" function u8Set(arr, idx, value) { arr[idx] = value; } """)
private external fun u8Set(arr: Uint8Array, idx: Int, value: Int)

@JsFun(""" function u8Get(arr, idx) { return arr[idx]; } """)
private external fun u8Get(arr: Uint8Array, idx: Int): Int

@JsFun(""" function u8Len(arr) { return arr.length; } """)
private external fun u8Len(arr: Uint8Array): Int

private fun ByteArray.toUint8ArrayWasm(): Uint8Array {
    val u8 = u8Alloc(size)
    for (i in indices) {
        u8Set(u8, i, this[i].toInt() and 0xFF)
    }
    return u8
}

private fun Uint8Array.toByteArrayWasm(): ByteArray {
    val n = u8Len(this)
    val out = ByteArray(n)
    for (i in 0 until n) {
        out[i] = (u8Get(this, i) and 0xFF).toByte()
    }
    return out
}