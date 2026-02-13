package org.bigblackowl.vccadmin.uiComponent.container

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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

    Box(
        modifier = modifier
            .onPreviewKeyEvent { event ->
                if (event.key == Key.F5 && event.type == KeyEventType.KeyUp) {
                    if (!isRefreshing) onRefresh()
                    Napier.d(tag = "PlatformPullToRefreshBox") { "F5 pressed" }
                    true
                } else {
                    false
                }
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