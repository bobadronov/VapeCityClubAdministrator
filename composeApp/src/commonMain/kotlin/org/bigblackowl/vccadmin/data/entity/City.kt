package org.bigblackowl.vccadmin.data.entity

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class City(
    @SerialName("id") val id: Int,
    @SerialName("name") val name: String,
    @SerialName("last_modified") val lastModified: Long,
    @SerialName("last_modified_user_id") val lastModifiedUserId: String,
    @SerialName("logo_url") val logoUrl: String? = null,
)
