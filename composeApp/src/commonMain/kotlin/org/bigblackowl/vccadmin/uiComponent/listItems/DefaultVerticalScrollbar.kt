package org.bigblackowl.vccadmin.uiComponent.listItems

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp
import io.github.oikvpqya.compose.fastscroller.ScrollbarStyle
import io.github.oikvpqya.compose.fastscroller.ThumbStyle
import io.github.oikvpqya.compose.fastscroller.TrackStyle
import io.github.oikvpqya.compose.fastscroller.VerticalScrollbar
import io.github.oikvpqya.compose.fastscroller.rememberScrollbarAdapter

@Composable
fun DefaultVerticalScrollbar(scrollState: LazyGridState) {
    if (!scrollState.canScrollForward && !scrollState.canScrollBackward) return


    VerticalScrollbar(
        adapter = rememberScrollbarAdapter(scrollState = scrollState),
        style = ScrollbarStyle(
            minimalHeight = 16.dp,
            thickness = 8.dp,
            hoverDurationMillis = 300,
            thumbStyle = ThumbStyle(
                shape = RoundedCornerShape(4.dp),
                unhoverColor = MaterialTheme.colorScheme.secondary,
                hoverColor = MaterialTheme.colorScheme.primary,
            ),
            trackStyle = TrackStyle(
                shape = RectangleShape,
                unhoverColor = Color.Transparent,
                hoverColor = Color.Transparent,
            ),
        ),
        modifier = Modifier.fillMaxHeight(),
        enablePressToScroll = true,
    )
}

@Composable
fun DefaultVerticalScrollbar(scrollState: LazyListState) {

    if (!scrollState.canScrollForward && !scrollState.canScrollBackward) return


    VerticalScrollbar(
        adapter = rememberScrollbarAdapter(scrollState = scrollState),
        style = ScrollbarStyle(
            minimalHeight = 16.dp,
            thickness = 8.dp,
            hoverDurationMillis = 300,
            thumbStyle = ThumbStyle(
                shape = RoundedCornerShape(4.dp),
                unhoverColor = MaterialTheme.colorScheme.secondary,
                hoverColor = MaterialTheme.colorScheme.primary,
            ),
            trackStyle = TrackStyle(
                shape = RectangleShape,
                unhoverColor = Color.Transparent,
                hoverColor = Color.Transparent,
            ),
        ),
        modifier = Modifier.fillMaxHeight(),
        enablePressToScroll = true,
    )
}

@Composable
fun DefaultVerticalScrollbar(scrollState: ScrollState) {

    if (!scrollState.canScrollForward && !scrollState.canScrollBackward) return


    VerticalScrollbar(
        adapter = rememberScrollbarAdapter(scrollState = scrollState),
        style = ScrollbarStyle(
            minimalHeight = 16.dp,
            thickness = 8.dp,
            hoverDurationMillis = 300,
            thumbStyle = ThumbStyle(
                shape = RoundedCornerShape(4.dp),
                unhoverColor = MaterialTheme.colorScheme.secondary,
                hoverColor = MaterialTheme.colorScheme.primary,
            ),
            trackStyle = TrackStyle(
                shape = RectangleShape,
                unhoverColor = Color.Transparent,
                hoverColor = Color.Transparent,
            ),
        ),
        modifier = Modifier.fillMaxHeight(),
        enablePressToScroll = true,
    )
}