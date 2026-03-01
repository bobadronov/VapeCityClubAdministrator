package org.bigblackowl.vccadmin.uiComponent.icons

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import org.bigblackowl.vccadmin.theme.DefaultValues

@Composable
fun DefaultIcon(
    image: ImageVector,
    tint: Color = MaterialTheme.colorScheme.primary,
    size: Dp = DefaultValues.Size.iconSize,
) {
    Icon(imageVector = image, contentDescription = null, modifier = Modifier.size(size), tint = tint)
}

@Composable
fun DefaultIcon(
    icon: Painter,
    color: Color = Color.Unspecified,
    size: Dp = DefaultValues.Size.iconSize
) {
    Icon(painter = icon, contentDescription = null, modifier = Modifier.size(size), tint = color)
}

