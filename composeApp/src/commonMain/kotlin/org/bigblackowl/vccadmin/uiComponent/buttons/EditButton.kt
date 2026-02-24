package org.bigblackowl.vccadmin.uiComponent.buttons

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.bigblackowl.vccadmin.uiComponent.icons.DefaultIcon
import org.bigblackowl.vccadmin.uiComponent.text.BodyText
import org.bigblackowl.vccadmin.utils.isWideScreen
import org.jetbrains.compose.resources.stringResource
import vccadministrator.composeapp.generated.resources.Res
import vccadministrator.composeapp.generated.resources.edit

@Composable
fun EditButton(
    modifier: Modifier = Modifier,
    showLabel: Boolean = isWideScreen(),
    onEdit: () -> Unit,
) {
    OutlinedButton(
        onClick = onEdit,
        modifier = modifier,
        colors = ButtonDefaults.outlinedButtonColors(),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface)
    ) {
        DefaultIcon(Icons.Default.Edit)

        if (showLabel) {
            Spacer(Modifier.width(ButtonDefaults.IconSpacing))
            BodyText(text = stringResource(Res.string.edit))
        }
    }
}

