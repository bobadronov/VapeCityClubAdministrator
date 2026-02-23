package org.bigblackowl.vccadmin.ui.settings

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import org.bigblackowl.vccadmin.theme.PreviewDarkMaterialTheme
import org.bigblackowl.vccadmin.uiComponent.icons.DefaultIcon
import org.jetbrains.compose.resources.stringResource
import vccadministrator.composeapp.generated.resources.Res
import vccadministrator.composeapp.generated.resources.clear_cache

@Composable
fun OutlinedCardWithLabel(
    label: String,
    onTap: ((Offset) -> Unit)? = null,
    onLongPress: ((Offset) -> Unit)? = null,
    modifier: Modifier = Modifier,
    textColor: Color = MaterialTheme.colorScheme.primary,
    borderColor: Color = MaterialTheme.colorScheme.primary,
    titleFontSize: TextUnit = MaterialTheme.typography.bodyMedium.fontSize,
    fontSize: TextUnit = TextUnit.Unspecified,
    verticalArrangement: Arrangement.Vertical = Arrangement.Center,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    content: @Composable (ColumnScope.() -> Unit),
) {
    val textMeasurer: TextMeasurer = rememberTextMeasurer()
    Box(
        modifier = modifier.drawWithContent {
            val canvasWidth = size.width
            val canvasHeight = size.height
            val lineThickness = 3f
            val radius = 52f
            val paddingBetweenTextAndLine = 23f
            val textLayoutResult = textMeasurer.measure(
                text = label, style = TextStyle(fontSize = fontSize, color = borderColor)
            )
            val textWidth = textLayoutResult.size.width.toFloat()
            val textHeight = textLayoutResult.size.height.toFloat()

            val textCenterY = textHeight / 2

            // Текст на початку
            drawText(
                textMeasurer = textMeasurer,
                text = label,
                topLeft = Offset(radius + paddingBetweenTextAndLine, 0f),
                style = TextStyle(fontSize = titleFontSize, color = textColor)
            )

            // Верхня горизонтальна лінія
            drawLine(
                color = borderColor,
                start = Offset(textWidth + paddingBetweenTextAndLine * 2 + radius, textCenterY),
                end = Offset(canvasWidth - radius, textCenterY),
                strokeWidth = lineThickness
            )

            // Бокові вертикальні лінії
            drawLine(
                color = borderColor, start = Offset(0f, 0f + textCenterY + radius), end = Offset(0f, canvasHeight - radius), strokeWidth = lineThickness
            )
            drawLine(
                color = borderColor,
                start = Offset(canvasWidth, 0f + textCenterY + radius),
                end = Offset(canvasWidth, canvasHeight - radius),
                strokeWidth = lineThickness
            )

            // Нижня горизонтальна лінія
            drawLine(
                color = borderColor, start = Offset(radius, canvasHeight), end = Offset(canvasWidth - radius, canvasHeight), strokeWidth = lineThickness
            )

            // Кути
            // Верхній лівий
            drawArc(
                color = borderColor,
                startAngle = 180f,
                sweepAngle = 90f,
                useCenter = false,
                topLeft = Offset(0f, 0f + textCenterY),
                size = Size(2 * radius, 2 * radius),
                style = Stroke(lineThickness)
            )
            // Верхній правий
            drawArc(
                color = borderColor,
                startAngle = 270f,
                sweepAngle = 90f,
                useCenter = false,
                topLeft = Offset(canvasWidth - radius * 2, 0f + textCenterY),
                size = Size(2 * radius, 2 * radius),
                style = Stroke(lineThickness)
            )
            // Нижній лівий
            drawArc(
                color = borderColor,
                startAngle = 90f,
                sweepAngle = 90f,
                useCenter = false,
                topLeft = Offset(0f, canvasHeight - 2 * radius),
                size = Size(2 * radius, 2 * radius),
                style = Stroke(lineThickness)
            )
            // Нижній правий
            drawArc(
                color = borderColor,
                startAngle = 0f,
                sweepAngle = 90f,
                useCenter = false,
                topLeft = Offset(canvasWidth - 2 * radius, canvasHeight - 2 * radius),
                size = Size(2 * radius, 2 * radius),
                style = Stroke(lineThickness)
            )

            drawContent()
        }.pointerInput(Unit) {
            detectTapGestures(
                onTap = onTap, onLongPress = onLongPress
            )
        }, contentAlignment = Alignment.CenterStart
    ) {
        Column(
            modifier = Modifier
                .heightIn(min = 110.dp)
                .fillMaxSize()
                .padding(20.dp)
                .padding(top = 10.dp), verticalArrangement = verticalArrangement, horizontalAlignment = horizontalAlignment
        ) {
            content()
        }
    }
}


@Preview
@Composable
fun OutlinedCardWithLabelPreview() = PreviewDarkMaterialTheme {
    LazyColumn {
        item {
            OutlinedCardWithLabel(
                label = stringResource(Res.string.clear_cache),
            ) {
                DefaultIcon(Icons.Default.Info)
            }
        }
    }
}

@Preview(device = Devices.DESKTOP)
@Composable
fun OutlinedCardWithLabelPreviewPC() = PreviewDarkMaterialTheme {
    LazyColumn {
        item {
            OutlinedCardWithLabel(
                label = stringResource(Res.string.clear_cache),
            ) {

            }
        }
    }
}