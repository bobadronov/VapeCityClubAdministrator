package org.bigblackowl.vccadmin.uiComponent.container

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun AdaptiveBox(onWide: @Composable () -> Unit, onNarrow: @Composable () -> Unit) {
    BoxWithConstraints {
        val width = maxWidth
        if (width > 450.dp) onWide() else onNarrow()
    }
}

@Composable
fun AdaptiveBox(minSize: Dp, onWide: @Composable () -> Unit, onNarrow: @Composable () -> Unit) {
    BoxWithConstraints {
        val width = maxWidth
        if (width > minSize) onWide() else onNarrow()
    }
}