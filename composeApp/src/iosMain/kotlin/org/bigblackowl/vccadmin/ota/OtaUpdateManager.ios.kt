@file:Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")

package org.bigblackowl.vccadmin.ota

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

actual class OtaUpdateManager {
    private val _state = MutableStateFlow<UpdateState>(UpdateState.NotAvailable)

    actual val state: StateFlow<UpdateState> = _state.asStateFlow()

    actual fun check() {
    }

    actual fun download() {
    }

    actual fun install() {
    }
}