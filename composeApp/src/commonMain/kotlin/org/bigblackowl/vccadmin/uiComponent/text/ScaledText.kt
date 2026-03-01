package org.bigblackowl.vccadmin.uiComponent.text

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import org.bigblackowl.vccadmin.ui.workSchedule.view.scaled

@Composable
fun ScaledText(
    text: String,
    scale: Float,
    modifier: Modifier = Modifier.Companion,
    color: Color = Color.Unspecified,
    textAlign: TextAlign? = null,
) {
    Text(
        text = text,
        modifier = modifier,
        style = MaterialTheme.typography.titleSmall.scaled(scale),
        color = color,
        textAlign = textAlign,
        maxLines = 1
    )
}