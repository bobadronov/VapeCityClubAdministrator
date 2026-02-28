package org.bigblackowl.vccadmin.playground

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fitInside
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.RectRulers
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.bigblackowl.vccadmin.theme.PreviewDarkMaterialTheme
import org.bigblackowl.vccadmin.theme.PreviewLightMaterialTheme
import org.bigblackowl.vccadmin.uiComponent.listItems.DefaultHorizontalScrollbar
import org.bigblackowl.vccadmin.uiComponent.listItems.DefaultVerticalScrollbar

@Composable
private fun PlaygroundContent() {
    val state = rememberLazyGridState()
    Row {
        Column(Modifier.weight(1f).fillMaxWidth(),) {
            LazyHorizontalGrid(
                rows = GridCells.FixedSize(72.5.dp),
                modifier = Modifier
                    .fitInside(RectRulers())
                    .fillMaxWidth(),
                state = state,
            ) {
                items(150) { colIndex ->
                    Card(shape = RectangleShape, border = BorderStroke(1.dp, Color.White)) {
                        Text("Елемент $colIndex",  modifier = Modifier.padding(3.dp))
                    }
                }
            }
            DefaultHorizontalScrollbar(state)
        }
        DefaultVerticalScrollbar(state)
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