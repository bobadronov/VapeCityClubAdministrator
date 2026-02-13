package org.bigblackowl.vccadmin.uiComponent.dialog

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.window.DialogProperties
import org.bigblackowl.vccadmin.theme.PreviewDarkMaterialTheme
import org.bigblackowl.vccadmin.theme.PreviewLightMaterialTheme
import org.bigblackowl.vccadmin.uiComponent.buttons.CancelButton
import org.bigblackowl.vccadmin.uiComponent.buttons.SaveButton
import org.bigblackowl.vccadmin.uiComponent.text.BodyText
import org.bigblackowl.vccadmin.uiComponent.text.TitleText
import org.jetbrains.compose.resources.stringResource
import vccadministrator.composeapp.generated.resources.Res
import vccadministrator.composeapp.generated.resources.dismiss
import vccadministrator.composeapp.generated.resources.unsaved_changes_message
import vccadministrator.composeapp.generated.resources.unsaved_changes_title

@Composable
fun UnsavedChangesDialog(
    show: Boolean,
    onSave: () -> Unit,
    onDiscard: () -> Unit,
    onCancel: () -> Unit = { /* default: do nothing */ },
    onDismissRequest: () -> Unit = onCancel,
) {
    AnimatedVisibility(visible = show, modifier = Modifier.fillMaxSize()) {
        AlertDialog(
            onDismissRequest = onDismissRequest,
            title = { TitleText(stringResource(Res.string.unsaved_changes_title)) },
            text = { BodyText(stringResource(Res.string.unsaved_changes_message)) },
            confirmButton = {
                SaveButton { onSave() }
            },
            dismissButton = {
                CancelButton { onCancel() }
                OutlinedButton(onClick = onDiscard) { BodyText(stringResource(Res.string.dismiss)) }
            },
            properties = DialogProperties(usePlatformDefaultWidth = false),
        )
    }
}

@Preview
@Composable
fun UnsavedChangesDialogPreview1() = PreviewLightMaterialTheme {
    UnsavedChangesDialog(
        show = true,
        onSave = {},
        onDiscard = {},
        onDismissRequest = {}
    )
}

@Preview
@Composable
fun UnsavedChangesDialogPreview2() = PreviewDarkMaterialTheme {
    UnsavedChangesDialog(
        show = true,
        onSave = {},
        onDiscard = {},
        onDismissRequest = {}
    )
}