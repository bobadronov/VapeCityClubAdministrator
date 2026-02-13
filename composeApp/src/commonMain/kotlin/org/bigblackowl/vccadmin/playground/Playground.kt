package org.bigblackowl.vccadmin.playground

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.bigblackowl.vccadmin.data.repository.FakeBackend
import org.bigblackowl.vccadmin.theme.PreviewDarkMaterialTheme

@Preview
@Composable
private fun PlaygroundPreview1() = PreviewDarkMaterialTheme {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(FakeBackend.shops) { shop ->

        }
    }
}