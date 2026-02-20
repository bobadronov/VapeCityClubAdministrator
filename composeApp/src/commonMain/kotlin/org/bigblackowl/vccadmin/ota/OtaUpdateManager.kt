@file:Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")

package org.bigblackowl.vccadmin.ota

import kotlinx.coroutines.flow.StateFlow


expect class OtaUpdateManager {
    val state: StateFlow<UpdateState>
    fun check()
    fun download()
    fun install()
}
