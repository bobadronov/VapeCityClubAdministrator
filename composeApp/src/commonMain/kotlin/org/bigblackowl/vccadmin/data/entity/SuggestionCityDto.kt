package org.bigblackowl.vccadmin.data.entity

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SuggestionCityDto(
    @SerialName("name") val name: String,
    @SerialName("oblast") val oblast: String,
)