package org.bigblackowl.vccadmin.uiComponent.text

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import org.bigblackowl.vccadmin.resourses.DefaultValues
import org.bigblackowl.vccadmin.theme.PreviewDarkMaterialTheme
import org.bigblackowl.vccadmin.theme.PreviewLightMaterialTheme

@Composable
private fun AppText(
    text: String,
    autoSize: TextAutoSize,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    textAlign: TextAlign? = null,
    maxLines: Int = Int.MAX_VALUE,
    letterSpacing: TextUnit = TextUnit.Unspecified,
    fontWeight: FontWeight? = null,
) {
    Text(
        text = text,
        modifier = modifier,
        color = color,
        textAlign = textAlign,
        fontWeight = fontWeight,
        maxLines = maxLines,
        softWrap = false,
        autoSize = autoSize,
        letterSpacing = letterSpacing,
    )
}

/** 1) Заголовки — великий текст */
@Composable
fun TitleText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    textAlign: TextAlign? = null,
    maxLines: Int = 2,
    letterSpacing: TextUnit = TextUnit.Unspecified,

    fontWeight: FontWeight? = FontWeight.SemiBold,
) {
    AppText(
        text = text,
        autoSize = DefaultValues.Size.textTitleAutoSize,
        modifier = modifier,
        color = color,
        textAlign = textAlign,
        maxLines = maxLines,
        fontWeight = fontWeight,
        letterSpacing = letterSpacing,
    )
}

/** 2) Основний текст */
@Composable
fun BodyText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    textAlign: TextAlign? = null,
    maxLines: Int = Int.MAX_VALUE,
    fontWeight: FontWeight? = null,
) {
    AppText(
        text = text,
        autoSize = DefaultValues.Size.textBodyAutoSize,
        modifier = modifier,
        color = color,
        textAlign = textAlign,
        maxLines = maxLines,
        fontWeight = fontWeight,
    )
}

/** 3) Допоміжний текст (пояснення/лейбли) */
@Composable
fun HelperText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    textAlign: TextAlign? = null,
    maxLines: Int = Int.MAX_VALUE,
    fontWeight: FontWeight? = null,
) {
    AppText(
        text = text,
        autoSize = DefaultValues.Size.textHelperAutoSize,
        modifier = modifier,
        color = color,
        textAlign = textAlign,
        maxLines = maxLines,
        fontWeight = fontWeight,
    )
}

/** 4) Маленький текст (підписи/дрібні деталі) */
@Composable
fun SmallText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    textAlign: TextAlign? = null,
    maxLines: Int = Int.MAX_VALUE,
    fontWeight: FontWeight? = null,
) {
    AppText(
        text = text,
        autoSize = DefaultValues.Size.textSmallAutoSize,
        modifier = modifier,
        color = color,
        textAlign = textAlign,
        maxLines = maxLines,
        fontWeight = fontWeight,
    )
}

@Composable
private fun TextPreviewContent() {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            TitleText(
                text = "Заголовок екрану або великого блоку"
            )

            BodyText(
                text = "Основний текст. Тут розміщується головна інформація, опис або контент."
            )

            HelperText(
                text = "Допоміжний текст для пояснення або уточнення деталей."
            )

            SmallText(
                text = "Маленький текст — дата, підпис або технічна інформація."
            )
        }
    }
}

@Preview
@Composable
private fun TextPreviewLight() = PreviewLightMaterialTheme {
    TextPreviewContent()
}


@Preview
@Composable
private fun TextPreviewDark() = PreviewDarkMaterialTheme {
    TextPreviewContent()
}
