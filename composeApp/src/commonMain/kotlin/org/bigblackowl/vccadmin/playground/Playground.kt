package org.bigblackowl.vccadmin.playground

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import org.bigblackowl.vccadmin.theme.PreviewDarkMaterialTheme
import org.bigblackowl.vccadmin.theme.PreviewLightMaterialTheme

@Composable
private fun PlaygroundContent() {
   
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