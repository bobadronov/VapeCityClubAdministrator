package org.bigblackowl.vccadmin.data.entity

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.bigblackowl.vccadmin.utils.formatTimestamp


@Serializable
data class SupabaseSlide(
    @SerialName("id") val id: String,
    @SerialName("file_name") val fileName: String,
    @SerialName("public_url") val publicUrl: String,
    @SerialName("shop_codes") val shopCodes: List<String> = emptyList(),
    @SerialName("position") val position: Int = 0,
    @SerialName("is_active") val isActive: Boolean = true,
    @SerialName("last_modified") val lastModified: Long,
    @SerialName("last_modified_user_id") val lastModifiedUserId: String,
    @SerialName("created_at") val createdAt: Long
)
suspend fun SupabaseSlide.toSlide(): Slide =
    Slide(
        id = id,
        fileName = fileName,
        publicUrl = publicUrl,
        shopCodes = shopCodes,
        position = position,
        isActive = isActive,
        lastModified = formatTimestamp(lastModified),
        lastModifiedUserName = lastModifiedUserId,
        createdAt = formatTimestamp(createdAt)
    )

suspend fun List<SupabaseSlide>.toSlides(): List<Slide> =
    map { it.toSlide() }

data class Slide(
    val id: String,
    val fileName: String,
    val publicUrl: String,
    val shopCodes: List<String> = emptyList(),
    val position: Int = 0,
    val isActive: Boolean = true,
    val lastModified: String = "",
    val lastModifiedUserName: String = "",
    val createdAt: String = ""
)