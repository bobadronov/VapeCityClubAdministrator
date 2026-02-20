package org.bigblackowl.vccadmin.ota

actual class OtaUpdateManager {
    actual val state: kotlinx.coroutines.flow.StateFlow<org.bigblackowl.vccadmin.ota.UpdateState>
        get() = TODO("Not yet implemented")

    actual fun check() {
    }

    actual fun download() {
    }

    actual fun install() {
    }
}