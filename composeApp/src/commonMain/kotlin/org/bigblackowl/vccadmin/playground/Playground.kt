package org.bigblackowl.vccadmin.playground

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.tooling.preview.Preview
import kotlinx.coroutines.launch
import org.bigblackowl.vccadmin.theme.PreviewDarkMaterialTheme
import org.bigblackowl.vccadmin.theme.PreviewLightMaterialTheme
import org.bigblackowl.vccadmin.utils.PlatformFileProvider

@Composable
private fun PlaygroundContent() {

    val fileName = "test_kb.txt"
    val bytes = ByteArray(204_800) { i ->
        val ch = 'A'.code + (i % 26)
        ch.toByte()
    }
    val scope = rememberCoroutineScope()
    Column {
        Button(
            onClick = {
                scope.launch {

                    PlatformFileProvider.downloadFile(fileName, bytes)
                }
            }
        ) {
            Text("Create file")
        }

        Button(
            onClick = { PlatformFileProvider.openDownloadFolder() }
        ) {
            Text("Open Downloads folder")
        }
    }
}


@Preview
@Composable
private fun PlaygroundPreviewDark() = PreviewDarkMaterialTheme {
    PlaygroundContent()
}

@Preview
@Composable
private fun PlaygroundPreviewLight() = PreviewLightMaterialTheme {
    PlaygroundContent()
}