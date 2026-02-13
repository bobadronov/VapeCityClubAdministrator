package org.bigblackowl.vccadmin.uiComponent.buttons

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.bigblackowl.vccadmin.uiComponent.icons.DefaultIcon
import org.bigblackowl.vccadmin.uiComponent.text.BodyText
import org.bigblackowl.vccadmin.utils.isWideScreen
import org.jetbrains.compose.resources.stringResource
import vccadministrator.composeapp.generated.resources.Res
import vccadministrator.composeapp.generated.resources.confirm

@Composable
fun ConfirmButton(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    showLabel: Boolean = isWideScreen(),
    color: ButtonColors = ButtonDefaults.outlinedButtonColors(),
    onClick: () -> Unit,
) {
    OutlinedButton(
        onClick = onClick,
        colors = color,
        modifier = modifier,
        enabled = enabled,
    ) {
        DefaultIcon(Icons.Default.Check)
        if (showLabel) {
            Spacer(Modifier.width(ButtonDefaults.IconSpacing))
            BodyText(text = stringResource(resource = Res.string.confirm),)
        }
    }
}