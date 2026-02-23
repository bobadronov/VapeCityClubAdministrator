package org.bigblackowl.vccadmin.data.entity

import androidx.compose.runtime.Immutable
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Immutable
@Serializable
data class AdminAppUpdate(
    @SerialName("id")
    val id: String? = null, // uuid у Postgres; якщо вставляєш — можна null, БД згенерує

    @SerialName("version")
    val version: String? = null,

    @SerialName("published_at")
    val publishedAt: Long? = null, // bigint (epoch millis, як у тебе)

    @SerialName("release_notes")
    val releaseNotes: String? = null,

    @SerialName("windows")
    val windows: AssetInfo? = null,

    @SerialName("macos")
    val macos: AssetInfo? = null,

    @SerialName("linux")
    val linux: AssetInfo? = null,

    @SerialName("android")
    val android: AssetInfo? = null,

    @SerialName("created_at")
    val createdAt: String? = null, // timestamp
)

@Serializable
data class AssetInfo(
    @SerialName("name")
    val name: String,

    @SerialName("url")
    val url: String,

    @SerialName("size")
    val size: Long,

    @SerialName("sha256")
    val sha256: String? = null,
)
data class UpdateInfo(
    val manifest: AdminAppUpdate,
    val asset: AssetInfo
)