package org.bigblackowl.vccadmin.ui.editSlidesSettings

import org.bigblackowl.vccadmin.data.entity.TransitionEffect

// Стан екрану налаштувань слайдів
data class SlidesSettingsState(
    val isLoading: Boolean = false,
    val settingsId: String? = null,
    val slideDuration: Int = 5,
    val transitionDuration: Int = 1200,
    val transitionEffect: TransitionEffect = TransitionEffect.FADE,
    val autoReloadTime: Int = 15,
    val slides: List<SlideOrderItem> = emptyList(),
    val lastModified: String = "",
    val lastModifiedByUser: String = "",
)