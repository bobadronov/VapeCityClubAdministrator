package org.bigblackowl.vccadmin.uiComponent.buttons

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowSizeClass.Companion.WIDTH_DP_MEDIUM_LOWER_BOUND
import org.bigblackowl.vccadmin.theme.PreviewDarkMaterialTheme
import org.bigblackowl.vccadmin.theme.PreviewLightMaterialTheme
import org.jetbrains.compose.resources.stringResource
import vccadministrator.composeapp.generated.resources.Res
import vccadministrator.composeapp.generated.resources.delete_confirmation_message

@Preview(showSystemUi = true)
@Composable
private fun ButtonsGallery_Light1() = PreviewLightMaterialTheme {
    ButtonsColumn(
        title = "Light",
    )
}
@Preview(showSystemUi = true)
@Composable
private fun ButtonsGallery_Dark1() = PreviewDarkMaterialTheme {
    ButtonsColumn(
        title = "Dark",
    )
}

@Preview(showSystemUi = true, device = Devices.DESKTOP)
@Composable
private fun ButtonsGallery_Light2() = PreviewLightMaterialTheme {
    ButtonsColumn(
        title = "Light",
    )
}
@Preview(showSystemUi = true, device = Devices.DESKTOP)
@Composable
private fun ButtonsGallery_Dark2() = PreviewDarkMaterialTheme {
    ButtonsColumn(
        title = "Dark",
    )
}


private data class ButtonItem(
    val content: @Composable () -> Unit
)

@Composable
private fun buttonList(): List<ButtonItem> {
    val deleteMsg = stringResource(Res.string.delete_confirmation_message)

    return listOf(
        ButtonItem { AddButton { } },
        ButtonItem { BackButton { } },
        ButtonItem { CancelButton { } },
        ButtonItem { ChangeButton { } },
        ButtonItem { ConfirmButton { } },
        ButtonItem { DeleteButton(message = deleteMsg, onDeleteConfirmed = {}) },
        ButtonItem { EditButton { } },
        ButtonItem { OpenButton { } },
        ButtonItem { RetryButton { } },
        ButtonItem { QRCodeButton { } },
        ButtonItem { SaveButton { } },
        ButtonItem { SettingsButton { } },
        ButtonItem { ShareAllFilesButton { } },
        ButtonItem { ShareButton { } },
    )
}


@Composable
private fun ButtonsColumn(
    title: String,
    modifier: Modifier = Modifier,
) {
    val list = buttonList()
    Column(modifier = modifier.padding(5.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge
        )

        LazyVerticalGrid(
            columns = GridCells.Adaptive(WIDTH_DP_MEDIUM_LOWER_BOUND.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(
                items = list,
            ) { item ->
                item.content()
            }
        }
    }
}
