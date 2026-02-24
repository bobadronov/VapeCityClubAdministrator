@file:Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")

package org.bigblackowl.vccadmin.utils

import io.github.aakira.napier.Napier
import kotlinx.browser.window
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.await
import kotlinx.coroutines.launch
import org.w3c.dom.events.KeyboardEvent
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
@OptIn(ExperimentalWasmJsInterop::class)
@JsFun("() => navigator.storage && navigator.storage.estimate ? navigator.storage.estimate() : null")
private external fun storageEstimateOrNull(): Promise<JsAny?>?
@OptIn(ExperimentalWasmJsInterop::class)
@JsFun("(o) => o && (o.usage ?? -1)")
private external fun estimateUsage(o: JsAny): Double

actual object PlatformFunctionProvider {
    private val scope = MainScope()

    @OptIn(ExperimentalWasmJsInterop::class)
    actual fun openNetwork() {
        window.alert("Not available on web")
    }

    @OptIn(ExperimentalWasmJsInterop::class)
    actual suspend fun getCacheSize(): Long {
        val p = storageEstimateOrNull() ?: return -1L
        val obj = p.await<JsAny?>() ?: return -1L
        val usage = estimateUsage(obj)
        return if (usage < 0) -1L else usage.toLong()
    }

    @OptIn(ExperimentalWasmJsInterop::class)
    actual fun clearCache() {
        scope.launch {
            // важливо: привести до JsAny, інакше interop може дати Promise<*>
            val cachesObj: JsAny = window.caches

            // важливо: явний тип для await
            val keysAny: JsAny = cachesKeys(cachesObj).await() ?: return@launch

            val len = jsLength(keysAny)
            for (i in 0 until len) {
                val key = jsGetString(keysAny, i) ?: continue
                cachesDelete(cachesObj, key).await<JsAny?>()
            }
        }
    }
    fun installReloadBlocker() {
        window.addEventListener("keydown", { e ->
            val ke = e as KeyboardEvent
            Napier.d { ke.key }
            val isF5 = ke.key == "F5" || ke.keyCode == 116
            if (isF5) {
                ke.preventDefault()
                ke.stopPropagation()
            }
        }, true) // capture=true — краще перехоплює раніше
    }
}