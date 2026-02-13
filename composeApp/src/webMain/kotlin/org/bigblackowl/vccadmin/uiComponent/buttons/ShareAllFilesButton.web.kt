package org.bigblackowl.vccadmin.uiComponent.buttons

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.bigblackowl.vccadmin.uiComponent.icons.DefaultIcon
import org.bigblackowl.vccadmin.uiComponent.text.BodyText
import org.jetbrains.compose.resources.stringResource
import vccadministrator.composeapp.generated.resources.Res
import vccadministrator.composeapp.generated.resources.download_all_files

@Composable
actual fun ShareAllFilesButton( onClick: () -> Unit,){
    OutlinedButton(onClick = onClick) {
        DefaultIcon(Icons.Default.Download)
        Spacer(Modifier.width(ButtonDefaults.IconSpacing))
        BodyText(text = stringResource(Res.string.download_all_files),)
    }
}