package org.bigblackowl.vccadmin.uiComponent.loading

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.bigblackowl.vccadmin.theme.PreviewDarkMaterialTheme
import org.bigblackowl.vccadmin.theme.PreviewLightMaterialTheme
import org.bigblackowl.vccadmin.utils.isWideScreen

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun LoadingComponent(
    modifier: Modifier = Modifier.fillMaxSize()
) {
    val isWide = isWideScreen()

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {

        LoadingIndicator(
            modifier = Modifier
                .size(if (isWide) 160.dp else 100.dp),
            color = MaterialTheme.colorScheme.primary
        )

    }

}


@Preview
@Composable
private fun LoadingPreview1() = PreviewDarkMaterialTheme { LoadingComponent() }

@Preview
@Composable
private fun LoadingPreview2() = PreviewLightMaterialTheme { LoadingComponent() }


@Preview(device = Devices.DESKTOP)
@Composable
private fun LoadingPreview11() = PreviewDarkMaterialTheme { LoadingComponent() }

@Preview(device = Devices.DESKTOP)
@Composable
private fun LoadingPreview22() = PreviewLightMaterialTheme { LoadingComponent() }
