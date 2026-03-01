package org.bigblackowl.vccadmin.uiComponent.text

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

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

private fun TextStyle.scaled(k: Float): TextStyle {
    fun TextUnit.scale(): TextUnit = (this.value * k).sp
    val fs = this.fontSize
    val lh = this.lineHeight
    return this.copy(
        fontSize = if (fs != TextUnit.Unspecified) fs.scale() else fs,
        lineHeight = if (lh != TextUnit.Unspecified) lh.scale() else lh,
    )
}
