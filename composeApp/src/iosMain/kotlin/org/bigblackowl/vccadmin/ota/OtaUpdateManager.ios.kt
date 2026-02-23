@file:Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")

package org.bigblackowl.vccadmin.ota

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import org.bigblackowl.vccadmin.data.events.UIEvents

actual class OtaUpdateManager {

    private val _state = MutableStateFlow<UpdateState>(UpdateState.NotAvailable)
    actual val state: StateFlow<UpdateState> = _state.asStateFlow()

    private val _uiEvent = MutableSharedFlow<UIEvents>(replay = 0)
    actual val uiEvent: SharedFlow<UIEvents> = _uiEvent.asSharedFlow()

    actual fun check() {
    }

    actual fun download() {
    }

    actual fun install() {
    }
}