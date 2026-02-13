package org.bigblackowl.vccadmin.uiComponent.icons

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage
import org.jetbrains.compose.resources.painterResource
import vccadministrator.composeapp.generated.resources.Res
import vccadministrator.composeapp.generated.resources.main_logo
import vccadministrator.composeapp.generated.resources.main_logo_white_theme

@Composable
fun OnlineIcon(
    model: Any?,
    contentDescription: String? = null,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit,
) {
    val isDark = isSystemInDarkTheme()
    val placeholderLogo = painterResource(
        if (isDark) Res.drawable.main_logo else Res.drawable.main_logo_white_theme
    )

    val fallbackPainter = remember(isDark) { placeholderLogo }

    AsyncImage(
        model = model,
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = contentScale,
        placeholder = fallbackPainter,
        error = fallbackPainter,
    )
}