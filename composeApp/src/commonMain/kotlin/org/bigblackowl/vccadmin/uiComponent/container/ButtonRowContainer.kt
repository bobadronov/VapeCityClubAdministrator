package org.bigblackowl.vccadmin.uiComponent.container

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.bigblackowl.vccadmin.resourses.DefaultValues

@Composable
fun ButtonRowContainer(modifier: Modifier = Modifier, content: @Composable RowScope.() -> Unit) {
//    val isWide = isWideScreen()
//    val width = remember(isWide) { if (isWide) .6f else 1f }

    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = DefaultValues.Padding.mainBoxPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(DefaultValues.Padding.rowItemPadding, Alignment.CenterHorizontally),
        content = content,
    )
}