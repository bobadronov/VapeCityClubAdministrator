package org.bigblackowl.vccadmin.uiComponent.dialog

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import io.github.alexzhirkevich.qrose.options.QrBrush
import io.github.alexzhirkevich.qrose.options.QrColors
import io.github.alexzhirkevich.qrose.options.QrErrorCorrectionLevel
import io.github.alexzhirkevich.qrose.options.QrFrameShape
import io.github.alexzhirkevich.qrose.options.QrLogo
import io.github.alexzhirkevich.qrose.options.QrLogoPadding
import io.github.alexzhirkevich.qrose.options.QrPixelShape
import io.github.alexzhirkevich.qrose.options.QrShapes
import io.github.alexzhirkevich.qrose.options.roundCorners
import io.github.alexzhirkevich.qrose.options.solid
import io.github.alexzhirkevich.qrose.rememberQrCodePainter
import org.bigblackowl.vccadmin.resourses.DefaultValues
import org.bigblackowl.vccadmin.theme.PreviewDarkMaterialTheme
import org.bigblackowl.vccadmin.theme.PreviewLightMaterialTheme
import org.bigblackowl.vccadmin.theme.rememberIsDarkTheme
import org.bigblackowl.vccadmin.uiComponent.buttons.CancelButton
import org.bigblackowl.vccadmin.uiComponent.container.ButtonRowContainer
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import vccadministrator.composeapp.generated.resources.Res
import vccadministrator.composeapp.generated.resources.copy_value
import vccadministrator.composeapp.generated.resources.main_logo
import vccadministrator.composeapp.generated.resources.main_logo_white_theme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QRCodeDialog(
    data: String,
    showConfirmMessage: () -> Unit= {},
    onDismiss: () -> Unit,
) {
// TODO: make share for different platforms
//    val uriHandler = LocalUriHandler.current

    @Suppress("DEPRECATION") val clipboardManager = LocalClipboardManager.current


    BasicAlertDialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Card(
            modifier = Modifier.padding(8.dp),
            shape = MaterialTheme.shapes.medium,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background),
            border = BorderStroke(2.dp, MaterialTheme.colorScheme.surfaceContainer)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically)
            ) {

                QrCodeImage(data = data)

                Text(
                    text = AnnotatedString(
//                        if (isDesktop)
                        stringResource(Res.string.copy_value, data)
//                        else StringProvider.call(data)
                    ),
                    modifier = Modifier.clickable {
                        // uri handler to open the phone app with the number
//                        if (isDesktop) {
                        clipboardManager.setText(
                            AnnotatedString(
                                data
                            )
                        )
                        showConfirmMessage()
//                        } else
//                            uriHandler.openUri("tel:$data")

                    },
                    textAlign = TextAlign.Center,
                    textDecoration = TextDecoration.Underline,
                    color = MaterialTheme.colorScheme.onSurface,
                    autoSize = DefaultValues.Size.textBodyAutoSize,
                    softWrap = false,
                )

                ButtonRowContainer {
                    CancelButton(modifier = Modifier.weight(.5f), color = ButtonDefaults.outlinedButtonColors()) {
                        onDismiss()
                    }
                }
            }
        }
    }
}

@Composable
private fun QrCodeImage(
    data: String,
    modifier: Modifier = Modifier,
    size: Dp = 300.dp,
) {

    val centerLogo = painterResource(
        if (rememberIsDarkTheme())
            Res.drawable.main_logo
        else
            Res.drawable.main_logo_white_theme
    )

    val qrcodePainter = rememberQrCodePainter(
        data = if (data.startsWith("+38")) "tel:$data" else data,
        shapes = QrShapes(
            darkPixel = QrPixelShape.roundCorners(.3f),
            frame = QrFrameShape.roundCorners(.1f),
        ),
        colors = QrColors(
            dark = QrBrush.solid(MaterialTheme.colorScheme.secondary),
        ),
        logo = QrLogo(painter = centerLogo, padding = QrLogoPadding.Natural(.05f)),
        errorCorrectionLevel = QrErrorCorrectionLevel.MediumHigh
    )

    Image(
        painter = qrcodePainter,
        contentDescription = null,
        modifier = modifier.size(size),
    )
}


@Preview
@Composable
private fun QRCodeDialogPreview1() = PreviewDarkMaterialTheme {
    QRCodeDialog(
        "29084028",
    ) {}
}


@Preview
@Composable
private fun QRCodeDialogPreview2() = PreviewLightMaterialTheme {
    QRCodeDialog(
        "29084028",
    ) {}
}


