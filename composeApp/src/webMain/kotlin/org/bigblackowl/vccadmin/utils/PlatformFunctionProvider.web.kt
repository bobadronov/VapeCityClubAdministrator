@file:Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
package org.bigblackowl.vccadmin.utils

import kotlinx.browser.window
import kotlin.js.ExperimentalWasmJsInterop

internal actual object PlatformFunctionProvider {
    @OptIn(ExperimentalWasmJsInterop::class)
    actual fun openNetwork() {
        window.alert("Not available on web")
    }
}