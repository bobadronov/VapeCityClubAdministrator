package org.bigblackowl.vccadmin.uiComponent.buttons

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.bigblackowl.vccadmin.uiComponent.icons.DefaultIcon
import org.bigblackowl.vccadmin.uiComponent.text.BodyText
import org.bigblackowl.vccadmin.utils.isWideScreen
import org.jetbrains.compose.resources.stringResource
import vccadministrator.composeapp.generated.resources.Res
import vccadministrator.composeapp.generated.resources.settings

@Composable
fun SettingsButton(
    modifier: Modifier = Modifier,
    title: String = stringResource(Res.string.settings),
    enabled: Boolean = true,
    showLabel: Boolean = isWideScreen(),
    onSettingsClicked: () -> Unit,
) {
    OutlinedButton(
        onClick = onSettingsClicked,
        modifier = modifier,
        enabled = enabled,
    ) {
        DefaultIcon(Icons.Default.Settings)

        if (showLabel) {
            Spacer(Modifier.width(ButtonDefaults.IconSpacing))
            BodyText(text = title,)
        }
    }
}