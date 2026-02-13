package org.bigblackowl.vccadmin.uiComponent.buttons

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.bigblackowl.vccadmin.uiComponent.icons.DefaultIcon
import org.bigblackowl.vccadmin.uiComponent.text.BodyText
import org.bigblackowl.vccadmin.utils.isWideScreen
import org.jetbrains.compose.resources.stringResource
import vccadministrator.composeapp.generated.resources.Res
import vccadministrator.composeapp.generated.resources.add

@Composable
fun AddButton(
    modifier: Modifier = Modifier,
    title: String = stringResource(Res.string.add),
    enabled: Boolean = true,
    showLabel: Boolean = isWideScreen(),
    onAdd: () -> Unit,
) {

    OutlinedButton(
        onClick = onAdd,
        modifier = modifier,
        enabled = enabled,
    ) {
        DefaultIcon(Icons.Default.Add)

        if (showLabel) {
            Spacer(Modifier.width(ButtonDefaults.IconSpacing))
            BodyText(title,)
        }
    }
}

