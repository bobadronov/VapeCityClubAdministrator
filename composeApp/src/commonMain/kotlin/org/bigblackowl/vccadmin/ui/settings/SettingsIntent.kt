package org.bigblackowl.vccadmin.ui.settings

sealed interface SettingsIntent {
    object Init : SettingsIntent
    data class SetTheme(val state: Boolean) : SettingsIntent

    data class SetLogoutDialog(val visible: Boolean) : SettingsIntent
    data class SetClearCacheDialog(val visible: Boolean) : SettingsIntent

    object ClearCache : SettingsIntent

    object CheckUpdates : SettingsIntent
    object DownloadUpdate : SettingsIntent
    object InstallUpdate : SettingsIntent

    object Logout : SettingsIntent
    data class SetLanguage(val iso: String) : SettingsIntent

}