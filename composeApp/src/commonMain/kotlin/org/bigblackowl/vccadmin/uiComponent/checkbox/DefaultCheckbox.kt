package org.bigblackowl.vccadmin.uiComponent.checkbox

import androidx.compose.material3.Checkbox
import androidx.compose.runtime.Composable

@Composable
fun DefaultCheckbox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true
) {
    Checkbox(
        checked = checked,
        onCheckedChange = onCheckedChange,
        enabled = enabled
    )
}