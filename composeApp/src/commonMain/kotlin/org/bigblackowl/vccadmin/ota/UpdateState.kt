package org.bigblackowl.vccadmin.ota

sealed interface UpdateState {
    data object NotAvailable : UpdateState
    data object Idle : UpdateState
    data object Checking : UpdateState
    data object NoUpdate : UpdateState
    data class Available(val info: UpdateInfo) : UpdateState
    data class Downloading(
        val progress: Float? = null,
        val total: String? = null,
        val downloaded: String? = null,
    ) : UpdateState

    data class Verifying(val info: UpdateInfo) : UpdateState
    data class ReadyToInstall(val info: UpdateInfo) : UpdateState
    data class Installing(val info: UpdateInfo) : UpdateState
    data class Error(val message: String, val cause: Throwable? = null) : UpdateState

}
