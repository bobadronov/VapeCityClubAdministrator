package org.bigblackowl.vccadmin.uiComponent

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.bigblackowl.vccadmin.theme.PreviewDarkMaterialTheme
import org.bigblackowl.vccadmin.theme.PreviewLightMaterialTheme

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun LoadingComponent(
    modifier: Modifier = Modifier.fillMaxSize()
) {

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterVertically)
    ) {

        LoadingIndicator(
            modifier = Modifier
                .size(130.dp)
        )

    }

}

@Preview
@Composable
private fun LoadingPreview1() = PreviewDarkMaterialTheme { LoadingComponent() }

@Preview
@Composable
private fun LoadingPreview2() = PreviewLightMaterialTheme { LoadingComponent() }