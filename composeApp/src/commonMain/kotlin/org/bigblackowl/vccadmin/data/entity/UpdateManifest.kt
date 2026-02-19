package org.bigblackowl.vccadmin.data.entity

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UpdateManifest(
    val tag: String? = null,
    val publishedAt: String? = null,
    val versionName: String? = null,

    @SerialName("desktop_version")
    val desktopVersion: String? = null,

    val releaseNotes: String? = null,

    @SerialName("android_version")
    val androidVersion: String? = null,

    @SerialName("android_version_code")
    val androidVersionCode: Long? = null,

    val assets: Assets = Assets(), // ✅ важливо
) {
    fun pickAsset(os: DesktopOs): AssetInfo? = when (os) {
        DesktopOs.WINDOWS -> assets.windows
        DesktopOs.MACOS -> assets.macos
        DesktopOs.LINUX -> assets.linux
    }

    fun pickAndroidAsset(): AssetInfo? = assets.android
}

@Serializable
data class Assets(
    val windows: AssetInfo? = null,
    val macos: AssetInfo? = null,
    val linux: AssetInfo? = null,

    // NEW: android apk
    val android: AssetInfo? = null
)

@Serializable
data class AssetInfo(
    val name: String,
    val url: String,
    val size: Long,
    val sha256: String = ""
)

data class UpdateInfo(
    val manifest: UpdateManifest,
    val asset: AssetInfo,
)