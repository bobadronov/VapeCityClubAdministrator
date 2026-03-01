// File: src/commonMain/kotlin/org/bigblackowl/vccadmin/ui/shopDetail/ShopDetailsScreen.kt
package org.bigblackowl.vccadmin.ui.shopDetail

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Comment
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Money
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Slideshow
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import org.bigblackowl.vccadmin.data.entity.Shop
import org.bigblackowl.vccadmin.data.entity.ShopStatus
import org.bigblackowl.vccadmin.data.entity.UserRole
import org.bigblackowl.vccadmin.data.entity.rememberIsDarkTheme
import org.bigblackowl.vccadmin.data.events.UIEvents
import org.bigblackowl.vccadmin.data.repository.FakeBackend
import org.bigblackowl.vccadmin.navigation.NavigationViewModel
import org.bigblackowl.vccadmin.navigation.Route
import org.bigblackowl.vccadmin.theme.DefaultValues
import org.bigblackowl.vccadmin.theme.PreviewDarkMaterialTheme
import org.bigblackowl.vccadmin.theme.PreviewLightMaterialTheme
import org.bigblackowl.vccadmin.uiComponent.buttons.BackButton
import org.bigblackowl.vccadmin.uiComponent.buttons.EditButton
import org.bigblackowl.vccadmin.uiComponent.buttons.QRCodeButton
import org.bigblackowl.vccadmin.uiComponent.buttons.ShareButton
import org.bigblackowl.vccadmin.uiComponent.container.ButtonRowContainer
import org.bigblackowl.vccadmin.uiComponent.container.PlatformPullToRefreshBox
import org.bigblackowl.vccadmin.uiComponent.dialog.QRCodeDialog
import org.bigblackowl.vccadmin.uiComponent.dialog.ShareShopDataDialog
import org.bigblackowl.vccadmin.uiComponent.icons.DefaultIcon
import org.bigblackowl.vccadmin.uiComponent.icons.OnlineIcon
import org.bigblackowl.vccadmin.uiComponent.indicators.LoadingComponent
import org.bigblackowl.vccadmin.uiComponent.listItems.DefaultVerticalScrollbar
import org.bigblackowl.vccadmin.uiComponent.text.BodyText
import org.bigblackowl.vccadmin.uiComponent.text.HelperText
import org.bigblackowl.vccadmin.uiComponent.text.SmallText
import org.bigblackowl.vccadmin.uiComponent.text.TitleText
import org.bigblackowl.vccadmin.utils.UkrainianPhoneVisualTransformation
import org.bigblackowl.vccadmin.utils.isWideScreen
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import vccadministrator.composeapp.generated.resources.Res
import vccadministrator.composeapp.generated.resources.address
import vccadministrator.composeapp.generated.resources.camera_codes
import vccadministrator.composeapp.generated.resources.city
import vccadministrator.composeapp.generated.resources.copied_to_clipboard
import vccadministrator.composeapp.generated.resources.device_for_slides
import vccadministrator.composeapp.generated.resources.internet_provider
import vccadministrator.composeapp.generated.resources.internet_provider_personal_account
import vccadministrator.composeapp.generated.resources.internet_replenishment_amount
import vccadministrator.composeapp.generated.resources.internet_replenishment_day
import vccadministrator.composeapp.generated.resources.last_modified
import vccadministrator.composeapp.generated.resources.last_modified_by_user
import vccadministrator.composeapp.generated.resources.not_specified
import vccadministrator.composeapp.generated.resources.phone_number
import vccadministrator.composeapp.generated.resources.remote_number
import vccadministrator.composeapp.generated.resources.shop_not_found
import vccadministrator.composeapp.generated.resources.slide_code
import vccadministrator.composeapp.generated.resources.status_comment

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShopDetailsScreen(
    shopId: String,
    snackbarHostState: SnackbarHostState,
    navigationViewModel: NavigationViewModel,
    viewModel: ShopDetailsScreenViewModel = koinInject(),
) {
    LaunchedEffect(shopId) {
        viewModel.onIntent(ShopDetailsScreenIntent.Load(shopId))
    }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle(initialValue = ShopDetailsUiState())

    LaunchedEffect(Unit) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                is UIEvents.ShowMessage -> snackbarHostState.showSnackbar(event.message)
                else -> {}
            }
        }
    }

    @Suppress("DEPRECATION") val clipboardManager = LocalClipboardManager.current
    val scope = rememberCoroutineScope()

    ShopDetailsScreenContent(
        shop = uiState.shop,
        isInitialLoading = uiState.isInitialLoading,
        isRefreshing = uiState.isRefreshing,
        onBack = { navigationViewModel.requestBack() },
        onEdit = { id -> navigationViewModel.navigateTo(Route.AddEditShop(id)) },
        onCopy = { data ->
            scope.launch {
                clipboardManager.setText(AnnotatedString(data))
                snackbarHostState.showSnackbar(getString(Res.string.copied_to_clipboard))
            }
        },
        onShare = { data -> viewModel.onIntent(ShopDetailsScreenIntent.ShareShop(data)) },
        showConfirmMessage = {
            scope.launch {
                snackbarHostState.showSnackbar(getString(Res.string.copied_to_clipboard))
            }
        },
        onRefresh = { viewModel.onIntent(ShopDetailsScreenIntent.Refresh(shopId)) },
        userRole = uiState.userRole,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ShopDetailsScreenContent(
    shop: Shop?,
    isInitialLoading: Boolean,
    isRefreshing: Boolean,
    onBack: () -> Unit,
    onEdit: (String) -> Unit,
    onCopy: (data: String) -> Unit,
    onShare: (data: String) -> Unit,
    showConfirmMessage: () -> Unit,
    onRefresh: () -> Unit,
    userRole: UserRole,
) {
    var showQRCodeDialog by remember { mutableStateOf(false) }
    var showShareDialog by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current

    PlatformPullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        if (isInitialLoading) {
            LoadingComponent()
            return@PlatformPullToRefreshBox
        }

        when {
            shop == null -> {
                Box(modifier = Modifier.fillMaxSize()) {
                    TitleText(
                        text = stringResource(Res.string.shop_not_found),
                        modifier = Modifier.align(Alignment.Center)
                    )

                    ButtonRowContainer(modifier = Modifier.align(Alignment.BottomCenter)) {
                        BackButton(Modifier.weight(1f)) {
                            onBack()
                            haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                        }
                    }
                }
            }

            else -> {
                val listState = rememberLazyListState()

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(DefaultValues.Padding.mainBoxPadding)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(DefaultValues.Padding.verticalListItemPadding)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OnlineIcon(
                            model = shop.logoUrl,
                            modifier = Modifier.heightIn(max = 70.dp),
                        )

                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            TitleText(
                                text = "${shop.street}, ${shop.houseNumber}",
                                fontWeight = FontWeight.Bold
                            )
                            StatusBadge(status = shop.status)
                        }
                    }

                    Card(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        shape = MaterialTheme.shapes.medium,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Row {
                            LazyColumn(
                                modifier = Modifier.padding(16.dp).weight(1f),
                                state = listState,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                item {
                                    InfoRow(
                                        icon = Icons.Default.Business,
                                        label = stringResource(Res.string.city),
                                        value = shop.cityName
                                    )
                                }

                                item {
                                    InfoRow(
                                        icon = Icons.Default.LocationOn,
                                        label = stringResource(Res.string.address),
                                        value = "${shop.street}, ${shop.houseNumber}\n${shop.addressComment}"
                                    )
                                }

                                item {
                                    InfoRow(
                                        icon = Icons.Default.Phone,
                                        label = stringResource(Res.string.phone_number),
                                        value = if (shop.phoneNumber != stringResource(Res.string.not_specified)) {
                                            UkrainianPhoneVisualTransformation()
                                                .filter(AnnotatedString(shop.phoneNumber))
                                                .text.text
                                        } else {
                                            stringResource(Res.string.not_specified)
                                        },
                                    )
                                }

                                item {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center,
                                    ) {
                                        QRCodeButton(modifier = Modifier.fillMaxWidth(.7f), onClick = {
                                            showQRCodeDialog = true
                                            haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                                        })
                                    }
                                }

                                if (userRole == UserRole.ADMIN) {
                                    item {
                                        InfoRow(
                                            icon = Icons.Default.Tv,
                                            label = stringResource(Res.string.device_for_slides),
                                            value = stringResource(shop.deviceType.title)
                                        )
                                    }
                                    item {
                                        InfoRow(
                                            icon = Icons.Default.Slideshow,
                                            label = stringResource(Res.string.slide_code),
                                            value = shop.code
                                        )
                                    }
                                    item {
                                        InfoRow(
                                            icon = Icons.AutoMirrored.Filled.Comment,
                                            label = stringResource(Res.string.status_comment),
                                            value = shop.statusComment
                                        )
                                    }
                                    item {
                                        InfoRow(
                                            icon = Icons.Default.Videocam,
                                            label = stringResource(Res.string.camera_codes),
                                            value = if (shop.cameraCodes.isNotEmpty())
                                                shop.cameraCodes.joinToString(", ")
                                            else stringResource(Res.string.not_specified)
                                        )
                                    }
                                    item {
                                        InfoRow(
                                            icon = Icons.Default.Security,
                                            label = stringResource(Res.string.remote_number),
                                            value = shop.remoteNumber
                                        )
                                    }
                                }

                                item {
                                    InfoRow(
                                        icon = Icons.Default.Wifi,
                                        label = stringResource(Res.string.internet_provider),
                                        value = shop.internetProvider
                                    )
                                }

                                item {
                                    InfoRow(
                                        icon = Icons.Default.AccountCircle,
                                        label = stringResource(Res.string.internet_provider_personal_account),
                                        value = if (shop.internetProviderPersonalAccount.isNotEmpty())
                                            shop.internetProviderPersonalAccount.joinToString(", ")
                                        else stringResource(Res.string.not_specified)
                                    )
                                }

                                item {
                                    InfoRow(
                                        icon = Icons.Default.Event,
                                        label = stringResource(Res.string.internet_replenishment_day),
                                        value = if (shop.internetReplenishmentDay > 0)
                                            shop.internetReplenishmentDay.toString()
                                        else stringResource(Res.string.not_specified)
                                    )
                                }

                                item {
                                    InfoRow(
                                        icon = Icons.Default.Money,
                                        label = stringResource(Res.string.internet_replenishment_amount),
                                        value = "${shop.internetReplenishmentAmount} грн.",
                                    )
                                }

                                item {
                                    Column {
                                        SmallText(
                                            stringResource(Res.string.last_modified, shop.lastModified),
                                        )
                                        SmallText(
                                            text = stringResource(Res.string.last_modified_by_user, shop.lastModifiedUser),
                                            modifier = Modifier.basicMarquee(),
                                        )
                                    }
                                }
                            }

                            DefaultVerticalScrollbar(scrollState = listState)
                        }
                    }

                    ButtonRowContainer {
                        if (isWideScreen()) {
                            BackButton(modifier = Modifier.weight(1f), onBack = {
                                onBack()
                                haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                            })
                        }

                        ShareButton(
                            modifier = Modifier.weight(1f),
                            onShare = {
                                showShareDialog = true
                                haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                            },
                        )

                        if (userRole == UserRole.ADMIN) {
                            EditButton(modifier = Modifier.weight(1f), onEdit = {
                                onEdit(shop.id)
                                haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                            })
                        }
                    }

                    AnimatedVisibility(showShareDialog) {
                        ShareShopDataDialog(
                            shop = shop,
                            onDismiss = {
                                showShareDialog = false
                                haptic.performHapticFeedback(HapticFeedbackType.Reject)
                            },
                            onShareClick = onShare,
                            onCopy = onCopy,
                        )
                    }

                    AnimatedVisibility(showQRCodeDialog) {
                        QRCodeDialog(
                            data = UkrainianPhoneVisualTransformation()
                                .filter(AnnotatedString(shop.phoneNumber))
                                .text.text,
                            showConfirmMessage = showConfirmMessage,
                            onDismiss = {
                                showQRCodeDialog = false
                                haptic.performHapticFeedback(HapticFeedbackType.Reject)
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoRow(
    icon: ImageVector,
    label: String,
    value: String,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        DefaultIcon(icon, tint = MaterialTheme.colorScheme.secondary)
        Spacer(Modifier.width(12.dp))
        SelectionContainer {
            Column {
                HelperText(
                    text = label,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                BodyText(
                    text = value,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun StatusBadge(status: ShopStatus) {
    val color = status.color
    val text = stringResource(status.title)
    val isDarkTheme = rememberIsDarkTheme()
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(DefaultValues.Shape.defaultShape))
            .background(color.copy(alpha = 0.15f))
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        BodyText(
            text = text,
            color = if (isDarkTheme) color else Color.Unspecified,
            fontWeight = FontWeight.Bold,
        )
    }
}


@Preview
@Composable
private fun PreviewShopDetails1() = PreviewDarkMaterialTheme {
    ShopDetailsScreenContent(
        shop = FakeBackend.singleShop,
        isInitialLoading = false,
        isRefreshing = false,
        onBack = {},
        onEdit = {},
        onCopy = {},
        onShare = {},
        onRefresh = {},
        userRole = UserRole.USER,
        showConfirmMessage = {},
    )
}

@Preview
@Composable
private fun PreviewShopDetails2() = PreviewLightMaterialTheme {
    ShopDetailsScreenContent(
        shop = FakeBackend.singleShop,
        isInitialLoading = false,
        isRefreshing = false,
        onBack = {},
        onEdit = {},
        onCopy = {},
        onShare = {},
        onRefresh = {},
        userRole = UserRole.ADMIN,
        showConfirmMessage = {},
    )
}

@Preview
@Composable
private fun StatusBadgePreview1() = PreviewDarkMaterialTheme {
    Column(
        modifier = Modifier.padding(DefaultValues.Padding.mainBoxPadding),
        verticalArrangement = Arrangement.spacedBy(15.dp)
    ) {
        ShopStatus.all().forEach { shopStatusInfo ->
            StatusBadge(status = shopStatusInfo.status)
        }
    }
}

@Preview
@Composable
private fun StatusBadgePreview2() = PreviewLightMaterialTheme {
    Column(
        modifier = Modifier.padding(DefaultValues.Padding.mainBoxPadding),
        verticalArrangement = Arrangement.spacedBy(15.dp)
    ) {
        ShopStatus.all().forEach { shopStatusInfo ->
            StatusBadge(status = shopStatusInfo.status)
        }
    }
}