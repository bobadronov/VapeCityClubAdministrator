package org.bigblackowl.vccadmin.ui.settings

import org.bigblackowl.vccadmin.BuildConfig
import org.bigblackowl.vccadmin.ota.UpdateState

data class SettingsUiState(
    val isInitialLoading: Boolean = true,
    val isDarkEffective: Boolean = false,
    val cacheSizeBytes: Long = 0L,
    val currentAppVersionLabel: String = BuildConfig.APP_VERSION,
    val newAppVersionLabel: String = "", // on init set from OtaUpdateManager.state
    val appBuildLabel: String = "",
    val updateState: UpdateState = UpdateState.NotAvailable,
    val logoutDialogVisible: Boolean = false,
    val clearCacheDialogVisible: Boolean = false,
)