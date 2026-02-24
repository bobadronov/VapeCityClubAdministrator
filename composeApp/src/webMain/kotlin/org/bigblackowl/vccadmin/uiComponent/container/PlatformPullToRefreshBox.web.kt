package org.bigblackowl.vccadmin.uiComponent.container

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp
import io.github.aakira.napier.Napier
import kotlinx.browser.window
import org.w3c.dom.events.KeyboardEvent

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
actual fun PlatformPullToRefreshBox(
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier,
    contentAlignment: Alignment,
    content: @Composable (BoxScope.() -> Unit)
) {
    val focusRequester = FocusRequester()

    // щоб listener завжди бачив актуальні isRefreshing/onRefresh
    val refreshingState = rememberUpdatedState(isRefreshing)
    val onRefreshState = rememberUpdatedState(onRefresh)

    // 1) Блокуємо browser reload по F5 на рівні window
    DisposableEffect(Unit) {
        val handler: (org.w3c.dom.events.Event) -> Unit = { e ->
            val ke = e as KeyboardEvent
            val isF5 = ke.key == "F5" || ke.keyCode == 116
            if (isF5) {
                ke.preventDefault()
                ke.stopPropagation()

                if (!refreshingState.value) onRefreshState.value()
                Napier.d(tag = "PlatformPullToRefreshBox") { "F5 blocked (window), refresh triggered" }
            }
        }

        // capture=true — перехоплює раніше за браузерні/DOM-хендлери
        window.addEventListener("keydown", handler, true)

        onDispose {
            window.removeEventListener("keydown", handler, true)
        }
    }

    // 2) Локальний Compose-хендлер можна лишити (він ок, але не завжди перший)
    Box(
        modifier = modifier
            .onPreviewKeyEvent { event ->
                if (event.key == Key.F5 && event.type == KeyEventType.KeyUp) {
                    // тут можна просто true, бо реальну дію вже зробив window listener
                    Napier.d(tag = "PlatformPullToRefreshBox") { "F5 seen (compose)" }
                    true
                } else false
            }
            .focusRequester(focusRequester)
            .focusable(),
        contentAlignment = contentAlignment
    ) {
        content()

        if (isRefreshing) {
            LinearWavyProgressIndicator(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            )
        }
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
}