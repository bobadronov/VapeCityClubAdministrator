package org.bigblackowl.vccadmin.uiComponent.dialog

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import net.engawapg.lib.zoomable.rememberZoomState
import net.engawapg.lib.zoomable.zoomable
import org.bigblackowl.vccadmin.theme.PreviewDarkMaterialTheme
import org.bigblackowl.vccadmin.theme.PreviewLightMaterialTheme
import org.bigblackowl.vccadmin.uiComponent.icons.DefaultIcon
import org.bigblackowl.vccadmin.uiComponent.icons.OnlineIcon

@Composable
fun FullscreenZoomableImageViewer(
    model: Any,
    onClose: () -> Unit,
) {
    val zoomState = rememberZoomState()

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.scrim)
        ) {
            // Саме зображення: zoom/pan
            OnlineIcon(
                model = model,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(10.dp)
                    .zoomable(zoomState),
            )

            // Кнопка закриття
            FilledIconButton(
                onClick = onClose,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp),
                colors = IconButtonDefaults.iconButtonColors(containerColor = MaterialTheme.colorScheme.error.copy(.5f))
            ) {
                DefaultIcon(Icons.Default.Close, tint = MaterialTheme.colorScheme.onError)
            }
        }
    }
}

@Preview
@Composable
private fun FullscreenZoomableImageViewerPreviewDark() = PreviewDarkMaterialTheme {
    FullscreenZoomableImageViewer(model = "", onClose = {})
}

@Preview
@Composable
private fun FullscreenZoomableImageViewerPreviewLight() = PreviewLightMaterialTheme {
    FullscreenZoomableImageViewer(model = "", onClose = {})
}