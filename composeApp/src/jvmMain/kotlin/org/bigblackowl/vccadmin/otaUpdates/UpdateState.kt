package org.bigblackowl.vccadmin.otaUpdates

import org.bigblackowl.vccadmin.data.entity.UpdateInfo

sealed class UpdateState {
    object Idle : UpdateState()
    object Checking : UpdateState()
    object NoUpdate : UpdateState()

    data class Available(val info: UpdateInfo) : UpdateState()

    data class Downloading(
        val progress: Float? = null,
        val total: String? = null,
        val downloaded: String? = null,
    ) : UpdateState() // 0..1 або null
    data class Verifying(val info: UpdateInfo) : UpdateState()
    object ReadyToInstall : UpdateState()

    data class Installing(val info: UpdateInfo) : UpdateState()
    data class Error(val message: String, val cause: Throwable? = null) : UpdateState()
}