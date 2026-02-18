package org.bigblackowl.vccadmin.data.errorManager

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.bigblackowl.vccadmin.BuildConfig
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@Serializable
data class ErrorReport(
    @SerialName("error_code") val errorCode: Int,
    val message: String,
    val device: String,
    val version: String,
    val timestamp: Long,
    @SerialName("app_id") val appId: String,
    val model: String?,
    @SerialName("screen_size") val screenSize: String?,
    val product: String?,
    @SerialName("android_version") val androidVersion: String?, // todo: можеш перейменувати в osVersion, але лишаю як у тебе
    @SerialName("store_code") val storeCode: String?,
) {
    companion object {
        @OptIn(ExperimentalTime::class)
        fun create(
            message: String,
            errorCode: Int,
            storeCode: String? = null,
        ): ErrorReport {
            val sys = SystemInfoProvider.systemInfo()

            return ErrorReport(
                errorCode = errorCode,
                message = message,
                device = sys.device.ifBlank { "unknown_device" },
                version = BuildConfig.APP_VERSION,
                timestamp = Clock.System.now().toEpochMilliseconds(),
                appId = BuildConfig.APP_NAME.ifBlank { "unknown_app_id" },
                model = sys.model,
                screenSize = sys.screenSize,
                product = sys.product,
                androidVersion = sys.osVersion, // тут фактично OS version (Android/iOS/Desktop/Browser)
                storeCode = storeCode,
            )
        }
    }
}