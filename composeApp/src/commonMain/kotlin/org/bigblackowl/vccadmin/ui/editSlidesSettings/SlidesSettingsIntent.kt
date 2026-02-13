package org.bigblackowl.vccadmin.ui.editSlidesSettings

import org.bigblackowl.vccadmin.data.entity.TransitionEffect

// Інтерфейс для інтентів екрану налаштувань слайдів
sealed interface SlidesSettingsIntent {
    object GetAppSettings : SlidesSettingsIntent
    data class ChangeSlideDuration(val value: Int) : SlidesSettingsIntent
    data class ChangeTransitionDuration(val value: Int) : SlidesSettingsIntent
    data class ChangeAutoReloadTime(val value: Int) : SlidesSettingsIntent
    data class ChangeEffect(val value: TransitionEffect) : SlidesSettingsIntent
    object SaveSettings : SlidesSettingsIntent
    object DiscardChanges : SlidesSettingsIntent
    object GoBack : SlidesSettingsIntent
    object Load : SlidesSettingsIntent
}