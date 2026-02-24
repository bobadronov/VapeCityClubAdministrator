package org.bigblackowl.vccadmin.uiComponent.icons

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage
import org.bigblackowl.vccadmin.data.entity.rememberIsDarkTheme
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

    val fallbackPainter =  if (rememberIsDarkTheme()) Res.drawable.main_logo else Res.drawable.main_logo_white_theme

    AsyncImage(
        model = model,
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = contentScale,
        placeholder = painterResource(fallbackPainter),
        error = painterResource(fallbackPainter),
    )
}