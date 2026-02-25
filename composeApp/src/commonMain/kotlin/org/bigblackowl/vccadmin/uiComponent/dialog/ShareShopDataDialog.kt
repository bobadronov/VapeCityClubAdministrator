package org.bigblackowl.vccadmin.uiComponent.dialog

import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import io.github.aakira.napier.Napier
import kotlinx.coroutines.launch
import org.bigblackowl.vccadmin.data.entity.Shop
import org.bigblackowl.vccadmin.data.repository.FakeBackend
import org.bigblackowl.vccadmin.theme.PreviewDarkMaterialTheme
import org.bigblackowl.vccadmin.theme.PreviewLightMaterialTheme
import org.bigblackowl.vccadmin.uiComponent.buttons.CancelButton
import org.bigblackowl.vccadmin.uiComponent.container.ButtonRowContainer
import org.bigblackowl.vccadmin.uiComponent.icons.DefaultIcon
import org.bigblackowl.vccadmin.uiComponent.listItems.DefaultVerticalScrollbar
import org.bigblackowl.vccadmin.uiComponent.text.BodyText
import org.bigblackowl.vccadmin.uiComponent.text.TitleText
import org.bigblackowl.vccadmin.utils.UkrainianPhoneVisualTransformation
import org.bigblackowl.vccadmin.utils.isWideScreen
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import vccadministrator.composeapp.generated.resources.Res
import vccadministrator.composeapp.generated.resources.address
import vccadministrator.composeapp.generated.resources.address_comment
import vccadministrator.composeapp.generated.resources.camera_codes
import vccadministrator.composeapp.generated.resources.city
import vccadministrator.composeapp.generated.resources.copy
import vccadministrator.composeapp.generated.resources.internet_provider
import vccadministrator.composeapp.generated.resources.internet_provider_personal_account
import vccadministrator.composeapp.generated.resources.internet_replenishment_amount
import vccadministrator.composeapp.generated.resources.internet_replenishment_day
import vccadministrator.composeapp.generated.resources.last_modified
import vccadministrator.composeapp.generated.resources.last_modified_by_user
import vccadministrator.composeapp.generated.resources.not_specified
import vccadministrator.composeapp.generated.resources.phone_number
import vccadministrator.composeapp.generated.resources.remote_number
import vccadministrator.composeapp.generated.resources.select_all
import vccadministrator.composeapp.generated.resources.select_fields_for_share
import vccadministrator.composeapp.generated.resources.send
import vccadministrator.composeapp.generated.resources.slide_code
import vccadministrator.composeapp.generated.resources.status
import vccadministrator.composeapp.generated.resources.status_comment
import vccadministrator.composeapp.generated.resources.telegram_logo

@Composable
fun ShareShopDataDialog(
    shop: Shop,
    onDismiss: () -> Unit,
    onShareClick: (String) -> Unit,
    onCopy: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    val notSpecified = stringResource(Res.string.not_specified)
    var selectedFields by remember(shop) {
        mutableStateOf(
            mapOf(
                "code" to true,
                "address" to true,
                "addressComment" to (shop.addressComment != notSpecified),
                "phoneNumber" to true,
                "status" to true,
                "statusComment" to (shop.statusComment != notSpecified),
                "internetProvider" to (shop.internetProvider != notSpecified),
                "internetProviderPersonalAccount" to shop.internetProviderPersonalAccount.isNotEmpty(),
                "internetReplenishmentDay" to (shop.internetReplenishmentDay != 0),
                "internetReplenishmentAmount" to (shop.internetReplenishmentAmount != "0"),
                "remoteNumber" to (shop.remoteNumber != notSpecified),
                "cameraCodes" to shop.cameraCodes.isNotEmpty(),
                "lastModified" to false,
                "lastModifiedUser" to false,
            )
        )
    }

    val isWide = isWideScreen()

    val allSelected = selectedFields.values.all { it }

    val scrollState = rememberScrollState()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = isWide)
    ) {
        Surface(
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {

                TitleText(stringResource(Res.string.select_fields_for_share))

                Spacer(Modifier.height(16.dp))

                Row(
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        modifier = Modifier
                            .verticalScroll(scrollState)
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        FieldCheckbox(
                            label = stringResource(Res.string.select_all),
                            checked = allSelected,
                            onCheckedChange = { newValue ->
                                selectedFields = selectedFields.mapValues { newValue }
                            }
                        )
                        Spacer(Modifier.height(8.dp))
                        FieldCheckbox(
                            "${stringResource(Res.string.address)}: м. ${shop.cityName}, вул. ${shop.street}, ${shop.houseNumber}",
                            selectedFields["address"]!!
                        ) { selectedFields = selectedFields + ("address" to it) }
                        FieldCheckbox("${stringResource(Res.string.address_comment)}: ${shop.addressComment}", selectedFields["addressComment"]!!) {
                            selectedFields = selectedFields + ("addressComment" to it)
                        }
                        FieldCheckbox("${stringResource(Res.string.status)}: ${stringResource(shop.status.title)}", selectedFields["status"]!!) {
                            selectedFields = selectedFields + ("status" to it)
                        }
                        FieldCheckbox(
                            "${stringResource(Res.string.phone_number)}: ${
                                UkrainianPhoneVisualTransformation().filter(AnnotatedString(shop.phoneNumber)).text.text.replace(
                                    " ",
                                    ""
                                )
                            }",
                            selectedFields["phoneNumber"]!!
                        ) {
                            selectedFields = selectedFields + ("phoneNumber" to it)
                        }
                        FieldCheckbox("${stringResource(Res.string.slide_code)}: ${shop.code}", selectedFields["code"]!!) {
                            selectedFields = selectedFields + ("code" to it)
                        }
                        FieldCheckbox("${stringResource(Res.string.status_comment)}: ${shop.statusComment}", selectedFields["statusComment"]!!) {
                            selectedFields = selectedFields + ("statusComment" to it)
                        }
                        FieldCheckbox("${stringResource(Res.string.internet_provider)}: ${shop.internetProvider}", selectedFields["internetProvider"]!!) {
                            selectedFields = selectedFields + ("internetProvider" to it)
                        }
                        FieldCheckbox(
                            "${stringResource(Res.string.internet_provider_personal_account)}: ${
                                if (shop.internetProviderPersonalAccount.isEmpty()) stringResource(Res.string.not_specified) else shop.internetProviderPersonalAccount.joinToString(
                                    ", "
                                )
                            }",
                            selectedFields["internetProviderPersonalAccount"]!!
                        ) {
                            selectedFields = selectedFields + ("internetProviderPersonalAccount" to it)
                        }
                        FieldCheckbox(
                            "${stringResource(Res.string.internet_replenishment_day)}: ${shop.internetReplenishmentDay}",
                            selectedFields["internetReplenishmentDay"]!!
                        ) {
                            selectedFields = selectedFields + ("internetReplenishmentDay" to it)
                        }
                        FieldCheckbox(
                            "${stringResource(Res.string.internet_replenishment_amount)}: ${shop.internetReplenishmentAmount}",
                            selectedFields["internetReplenishmentAmount"]!!
                        ) {
                            selectedFields = selectedFields + ("internetReplenishmentAmount" to it)
                        }
                        FieldCheckbox("${stringResource(Res.string.remote_number)}: ${shop.remoteNumber}", selectedFields["remoteNumber"]!!) {
                            selectedFields = selectedFields + ("remoteNumber" to it)
                        }
                        FieldCheckbox(
                            "${stringResource(Res.string.camera_codes)}: ${
                                if (shop.cameraCodes.isEmpty()) stringResource(Res.string.not_specified) else shop.cameraCodes.joinToString(
                                    ", "
                                )
                            }",
                            selectedFields["cameraCodes"]!!
                        ) {
                            selectedFields = selectedFields + ("cameraCodes" to it)
                        }
                        FieldCheckbox(stringResource(Res.string.last_modified, shop.lastModified), selectedFields["lastModified"]!!) {
                            selectedFields = selectedFields + ("lastModified" to it)
                        }
                        FieldCheckbox(stringResource(Res.string.last_modified_by_user, shop.lastModifiedUser), selectedFields["lastModifiedUser"]!!) {
                            selectedFields = selectedFields + ("lastModifiedUser" to it)
                        }
                    }

                    DefaultVerticalScrollbar(scrollState = scrollState)

                }

                ButtonRowContainer {
                    CancelButton(modifier = Modifier.weight(1f), onClick = onDismiss)

                    Button(
                        onClick = {
                            scope.launch {
                                val sharedText = generateSharedText(shop, selectedFields)
                                Napier.d { sharedText }
                                onShareClick(sharedText)
                                onDismiss()
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        DefaultIcon(painterResource(Res.drawable.telegram_logo))
                        if (isWide) {
                            Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                            BodyText(stringResource(Res.string.send))
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                val sharedText = generateSharedText(shop, selectedFields)
                                onCopy(sharedText)
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        DefaultIcon(Icons.Default.ContentCopy, tint = MaterialTheme.colorScheme.primary)
                        if (isWide) {
                            Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                            BodyText(stringResource(Res.string.copy))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FieldCheckbox(
    label: String,
    checked: Boolean,
    modifier: Modifier = Modifier,
    onCheckedChange: (Boolean) -> Unit,
) {
    Card(
        modifier = modifier.then(Modifier.fillMaxWidth()),
        onClick = {
            onCheckedChange(!checked)
        }
    ) {
        Row(
            modifier = Modifier.padding(15.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Checkbox(checked = checked, onCheckedChange = onCheckedChange)
            BodyText(label, modifier = Modifier.basicMarquee(), maxLines = 2)
        }
    }
}


private suspend fun generateSharedText(shop: Shop, selectedFields: Map<String, Boolean>): String {
    return buildString {
        if (selectedFields["address"] == true) {
            appendLine(
                "${getString(Res.string.address)}: " + "${getString(Res.string.city).lowercase()[0]}. ${shop.cityName}, " + "${shop.street}, " + shop.houseNumber
            )
        }
        if (selectedFields["addressComment"] == true) {
            appendLine("${getString(Res.string.address_comment)}: ${shop.addressComment}")
        }
        if (selectedFields["status"] == true) {
            appendLine("${getString(Res.string.status)}: ${getString(shop.status.title)}")
        }
        if (selectedFields["phoneNumber"] == true) {
            appendLine(
                "${getString(Res.string.phone_number)}: ${
                    UkrainianPhoneVisualTransformation().filter(AnnotatedString(shop.phoneNumber)).text.text.replace(
                        " ",
                        ""
                    )
                }"
            )
        }
        if (selectedFields["code"] == true) {
            appendLine("${getString(Res.string.slide_code)}: ${shop.code}")
        }
        if (selectedFields["statusComment"] == true) {
            appendLine("${getString(Res.string.status_comment)}: ${shop.statusComment}")
        }
        if (selectedFields["internetProvider"] == true) {
            appendLine("${getString(Res.string.internet_provider)}: ${shop.internetProvider}")
        }
        if (selectedFields["internetProviderPersonalAccount"] == true) {
            appendLine(
                "${getString(Res.string.internet_provider_personal_account)}: " +
                        if (shop.internetProviderPersonalAccount.isEmpty()) getString(Res.string.not_specified) else shop.internetProviderPersonalAccount.joinToString("\n")
            )
        }
        if (selectedFields["internetReplenishmentDay"] == true) {
            appendLine("${getString(Res.string.internet_replenishment_day)}: ${shop.internetReplenishmentDay}")
        }
        if (selectedFields["internetReplenishmentAmount"] == true) {
            appendLine("${getString(Res.string.internet_replenishment_amount)}: ${shop.internetReplenishmentAmount}")
        }
        if (selectedFields["remoteNumber"] == true) {
            appendLine("${getString(Res.string.remote_number)}: ${shop.remoteNumber}")
        }
        if (selectedFields["cameraCodes"] == true) {
            appendLine(
                "${getString(Res.string.camera_codes)}: " +
                        if (shop.cameraCodes.isEmpty()) getString(Res.string.not_specified) else shop.cameraCodes.joinToString("\n")
            )
        }
        if (selectedFields["lastModified"] == true) {
            appendLine(getString(Res.string.last_modified, shop.lastModified))
        }
        if (selectedFields["lastModifiedUser"] == true) {
            appendLine(getString(Res.string.last_modified_by_user, shop.lastModifiedUser))
        }
    }
}


@Preview
@Composable
fun ShareShopDataDialogPreview1() = PreviewDarkMaterialTheme {
    ShareShopDataDialog(
        shop = FakeBackend.singleShop,
        onDismiss = {},
        onShareClick = {},
        onCopy = {},
    )
}

@Preview
@Composable
fun ShareShopDataDialogPreview2() = PreviewLightMaterialTheme {
    ShareShopDataDialog(
        shop = FakeBackend.singleShop,
        onDismiss = {},
        onShareClick = {},
        onCopy = {},
    )
}