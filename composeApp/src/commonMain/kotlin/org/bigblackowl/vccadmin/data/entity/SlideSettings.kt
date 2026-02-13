package org.bigblackowl.vccadmin.data.entity

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SlideSettings(
    val id: String = "",
    @SerialName("slide_duration") val slideDuration: Int = 0,
    // час анімації переходу між слайдами (с)
    @SerialName("transition_duration") val transitionDuration: Int = 0,
    // ефект переходу
    @SerialName("transition_effect") val transitionEffect: TransitionEffect = TransitionEffect.SLIDE_FADE,
    @SerialName("auto_reload_time") val autoReloadTime: Int = 15,
    @SerialName("last_modified") val lastModified: Long? = null,
    @SerialName("last_modified_user_id") val lastModifiedByUser: String? = null,
)