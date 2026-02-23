@file:Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")

package org.bigblackowl.vccadmin.ota

import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import org.bigblackowl.vccadmin.data.events.UIEvents


expect class OtaUpdateManager {
    val state: StateFlow<UpdateState>
    val uiEvent: SharedFlow<UIEvents>
    fun check()
    fun download()
    fun install()
}
