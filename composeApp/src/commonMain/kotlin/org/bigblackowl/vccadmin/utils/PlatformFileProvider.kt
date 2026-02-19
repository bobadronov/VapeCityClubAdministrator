package org.bigblackowl.vccadmin.utils

import org.bigblackowl.vccadmin.ui.fileGenerator.GeneratedFile

@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
internal expect object PlatformFileProvider {
    val downloadFolderPath: String
    fun openFile(fileName: String)
    fun openDownloadFolder()
    suspend fun downloadFile(name: String, content: ByteArray)
    fun isFileExist(fileName: String): Boolean?
    suspend fun shareWithTelegram(data:String)
    suspend fun shareFilesAsZip(files: List<GeneratedFile>): ShareResult
    suspend fun deleteFile(fileName: String): Boolean
}

data class ShareResult(
    val state: Boolean,  // return state of
    val message: String, // return getString(Res.string.xxxxxx)
)