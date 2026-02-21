@file:Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")

package org.bigblackowl.vccadmin.utils

import kotlinx.browser.window
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.await
import kotlinx.coroutines.launch
import kotlin.js.Promise

@OptIn(ExperimentalWasmJsInterop::class)
@JsFun("(c) => c.keys()")
private external fun cachesKeys(caches: JsAny): Promise<JsAny?>

@OptIn(ExperimentalWasmJsInterop::class)
@JsFun("(c, k) => c.delete(k)")
private external fun cachesDelete(caches: JsAny, key: String): Promise<JsAny?>

@OptIn(ExperimentalWasmJsInterop::class)
@JsFun("(arr) => arr.length")
private external fun jsLength(arr: JsAny): Int

@OptIn(ExperimentalWasmJsInterop::class)
@JsFun("(arr, i) => arr[i]")
private external fun jsGetString(arr: JsAny, i: Int): String?

actual object PlatformFunctionProvider {
    private val scope = MainScope()

    @OptIn(ExperimentalWasmJsInterop::class)
    actual fun openNetwork() {
        window.alert("Not available on web")
    }

    actual fun getCacheSize(): Long = -1L

    @OptIn(ExperimentalWasmJsInterop::class)
    actual fun clearCache() {
        scope.launch {
            val cachesObj = window.caches
            val keysAny = cachesKeys(cachesObj).await() ?: return@launch

            val len = jsLength(keysAny)

            for (i in 0 until len) {
                val key = jsGetString(keysAny, i) ?: continue
                cachesDelete(cachesObj, key).await()
            }
        }
    }
}