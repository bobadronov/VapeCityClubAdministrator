@file:Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")

package org.bigblackowl.vccadmin.ota

actual class OtaUpdateManager {
    actual val state: kotlinx.coroutines.flow.StateFlow<UpdateState>
        get() = TODO("Not yet implemented")

    actual fun check() {
    }

    actual fun download() {
    }

    actual fun install() {
    }
}