package org.bigblackowl.vccadmin.playground

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import org.bigblackowl.vccadmin.theme.PreviewDarkMaterialTheme
import org.bigblackowl.vccadmin.theme.PreviewLightMaterialTheme

@Composable
private fun PlaygroundContent() {
    val path = "G:/Завантаження/Telegram Desktop/Графік роботи по 01.03.2026.xls"

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