package org.bigblackowl.vccadmin.uiComponent.icons

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage
import org.bigblackowl.vccadmin.data.entity.rememberIsDarkTheme
import org.bigblackowl.vccadmin.uiComponent.indicators.LoadingComponent
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
    val fallbackPainter =
        if (rememberIsDarkTheme()) Res.drawable.main_logo else Res.drawable.main_logo_white_theme

    var isLoading by remember { mutableStateOf(true) }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {

        // Картинка (плавна поява)
        AnimatedVisibility(
            visible = !isLoading,
            enter = fadeIn(tween(220)) + scaleIn(initialScale = 0.98f, animationSpec = tween(220)),
            exit = fadeOut(tween(120)) + scaleOut(targetScale = 0.98f, animationSpec = tween(120)),
        ) {
            AsyncImage(
                model = model,
                contentDescription = contentDescription,
                contentScale = contentScale,
                placeholder = painterResource(fallbackPainter),
                error = painterResource(fallbackPainter),
                onLoading = { _ ->
                    isLoading = true
                },
                onSuccess = {
                    isLoading = false
                },
                onError = {
                    isLoading = false
                }
            )
        }

        // Лоадер (плавне зникнення)
        AnimatedVisibility(
            visible = isLoading,
            enter = fadeIn(tween(120)),
            exit = fadeOut(tween(180)),
        ) {
            // якщо хочеш, можеш використати loaderAlpha всередині LoadingComponent (alpha(modifier))
            LoadingComponent()
        }
    }
}