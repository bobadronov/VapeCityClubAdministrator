@file:Suppress("AssignedValueIsNeverRead", "UnusedExpression")

package org.bigblackowl.vccadmin.ui.addEditShop

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.window.core.layout.WindowSizeClass.Companion.WIDTH_DP_MEDIUM_LOWER_BOUND
import kotlinx.coroutines.launch
import org.bigblackowl.vccadmin.data.entity.DeviceType
import org.bigblackowl.vccadmin.data.entity.ShopStatus
import org.bigblackowl.vccadmin.data.events.UIEvents
import org.bigblackowl.vccadmin.navigation.NavigationViewModel
import org.bigblackowl.vccadmin.navigation.Route
import org.bigblackowl.vccadmin.resourses.DefaultValues
import org.bigblackowl.vccadmin.theme.PreviewDarkMaterialTheme
import org.bigblackowl.vccadmin.theme.PreviewLightMaterialTheme
import org.bigblackowl.vccadmin.uiComponent.buttons.AddButton
import org.bigblackowl.vccadmin.uiComponent.buttons.BackButton
import org.bigblackowl.vccadmin.uiComponent.buttons.CancelButton
import org.bigblackowl.vccadmin.uiComponent.buttons.DeleteButton
import org.bigblackowl.vccadmin.uiComponent.buttons.SaveButton
import org.bigblackowl.vccadmin.uiComponent.container.ButtonRowContainer
import org.bigblackowl.vccadmin.uiComponent.dialog.UnsavedChangesDialog
import org.bigblackowl.vccadmin.uiComponent.icons.DefaultIcon
import org.bigblackowl.vccadmin.uiComponent.icons.OnlineIcon
import org.bigblackowl.vccadmin.uiComponent.listItems.DefaultVerticalScrollbar
import org.bigblackowl.vccadmin.uiComponent.indicators.LoadingComponent
import org.bigblackowl.vccadmin.uiComponent.text.BodyText
import org.bigblackowl.vccadmin.uiComponent.text.TitleText
import org.bigblackowl.vccadmin.utils.UkrainianPhoneVisualTransformation
import org.bigblackowl.vccadmin.utils.isWideScreen
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import vccadministrator.composeapp.generated.resources.Res
import vccadministrator.composeapp.generated.resources.add_new_city
import vccadministrator.composeapp.generated.resources.add_shop
import vccadministrator.composeapp.generated.resources.address_comment_label
import vccadministrator.composeapp.generated.resources.address_comment_placeholder
import vccadministrator.composeapp.generated.resources.camera_codes
import vccadministrator.composeapp.generated.resources.city
import vccadministrator.composeapp.generated.resources.code
import vccadministrator.composeapp.generated.resources.current_value
import vccadministrator.composeapp.generated.resources.delete_confirmation_message
import vccadministrator.composeapp.generated.resources.device_for_slides
import vccadministrator.composeapp.generated.resources.house_number
import vccadministrator.composeapp.generated.resources.internet_provider
import vccadministrator.composeapp.generated.resources.internet_provider_account
import vccadministrator.composeapp.generated.resources.internet_replenishment_day
import vccadministrator.composeapp.generated.resources.phone_number
import vccadministrator.composeapp.generated.resources.remote_number
import vccadministrator.composeapp.generated.resources.replenishment_amount_label
import vccadministrator.composeapp.generated.resources.select_day
import vccadministrator.composeapp.generated.resources.selected_day
import vccadministrator.composeapp.generated.resources.status
import vccadministrator.composeapp.generated.resources.status_comment
import vccadministrator.composeapp.generated.resources.street
import vccadministrator.composeapp.generated.resources.this_shop
import kotlin.time.ExperimentalTime

@Composable
fun ShopAddEditScreen(
    shopId: String?,
    snackbarHostState: SnackbarHostState,
    navigationViewModel: NavigationViewModel,
    viewModel: ShopAddEditScreenViewModel = koinInject(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    var showUnsavedDialog by remember { mutableStateOf(false) }

    val isEditMode = shopId != null

    LaunchedEffect(Unit, shopId) {
        viewModel.onIntent(ShopAddEditIntent.Init)

        if (isEditMode) {
            viewModel.onIntent(ShopAddEditIntent.LoadShopAddDetails(shopId))
        }

        launch {
            viewModel.uiEvent.collect { event ->
                when (event) {
                    is UIEvents.NavigateBack -> navigationViewModel.popBackStack()
                    is UIEvents.ShowMessage -> snackbarHostState.showSnackbar(event.message)
                    is UIEvents.ShowUnsavedChangesDialog -> showUnsavedDialog = true
                    else -> {}
                }
            }
        }
    }

    ShopEditContent(
        isEditMode = isEditMode,
        uiState = state,
        onDismissRequest = { viewModel.onIntent(ShopAddEditIntent.Dismiss) },
        onBack = { viewModel.onIntent(ShopAddEditIntent.Dismiss) },
        onIntent = { intent -> viewModel.onIntent(intent) },
        onAddNewCity = {
            navigationViewModel.navigateTo(Route.CityList)
        },
    )

    UnsavedChangesDialog(
        show = showUnsavedDialog,
        onSave = {
            viewModel.onIntent(ShopAddEditIntent.ValidateAndSaveShopAddEdit)
            showUnsavedDialog = false
        },
        onDiscard = {
            viewModel.onIntent(ShopAddEditIntent.DiscardChanges)
            showUnsavedDialog = false
        },
        onCancel = { showUnsavedDialog = false },
        onDismissRequest = { showUnsavedDialog = false }
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalTime::class)
@Composable
private fun ShopEditContent(
    isEditMode: Boolean,
    uiState: ShopAddEditState,
    onDismissRequest: () -> Unit,
    onBack: () -> Unit,
    onIntent: (ShopAddEditIntent) -> Unit,
    onAddNewCity: () -> Unit,
) {
    val localFocusManager = LocalFocusManager.current
    val state = rememberLazyListState()

    if (uiState.isLoading) {
        LoadingComponent()
        return
    }

    Column(
        modifier = Modifier.padding(DefaultValues.Padding.mainBoxPadding).fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(DefaultValues.Padding.verticalListItemPadding),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            Modifier.weight(1f).sizeIn(maxWidth = WIDTH_DP_MEDIUM_LOWER_BOUND.dp)
        ) {
            LazyColumn(
                modifier = Modifier.weight(1f),
                state = state,
                verticalArrangement = Arrangement.spacedBy(DefaultValues.Padding.verticalListItemPadding)
            ) {
                item {
                    var cityDropdownIndex by remember { mutableIntStateOf(0) }
                    ExposedDropdownMenuBox(
                        expanded = uiState.cityDropdownExpanded,
                        onExpandedChange = { onIntent(ShopAddEditIntent.UpdateCityDropdownExpanded(it)) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = uiState.cities.find { it.id == uiState.selectedCityId }?.name ?: "",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(stringResource(Res.string.city)) },
                            leadingIcon = {
                                val selected = uiState.cities.find { it.id == uiState.selectedCityId }
                                OnlineIcon(
                                    model = selected?.logoUrl,
                                    modifier = Modifier.height(35.dp),
                                )
                            },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = uiState.cityDropdownExpanded)
                            },
                            isError = uiState.selectedCityId == null,
                            modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                            shape = MaterialTheme.shapes.medium,
                        )
                        ExposedDropdownMenu(
                            expanded = uiState.cityDropdownExpanded,
                            onDismissRequest = { onIntent(ShopAddEditIntent.UpdateCityDropdownExpanded(false)) },
                        ) {
                            uiState.cities.forEachIndexed { index, city ->

                                val backgroundColor =
                                    if (index == cityDropdownIndex) MaterialTheme.colorScheme.primary.copy(alpha = DefaultValues.Color.MENU_ALPHA) else Color.Unspecified
                                DropdownMenuItem(
                                    text = {
                                        BodyText(text = city.name)
                                    },
                                    onClick = {
                                        cityDropdownIndex = index
                                        onIntent(ShopAddEditIntent.UpdateSelectedCityId(city.id))
                                        onIntent(ShopAddEditIntent.UpdateCityDropdownExpanded(false))
                                        localFocusManager.moveFocus(FocusDirection.Next)
                                    },
                                    modifier = Modifier.padding(bottom = 10.dp).background(backgroundColor),
                                    leadingIcon = {
                                        OnlineIcon(
                                            model = city.logoUrl,
                                            modifier = Modifier.size(55.dp),
                                            contentScale = ContentScale.FillHeight,
                                        )
                                    }
                                )
                                HorizontalDivider(modifier = Modifier.padding(bottom = 10.dp))
                            }

                            DropdownMenuItem(text = { BodyText(stringResource(Res.string.add_new_city)) }, onClick = {
                                onAddNewCity()
                                onIntent(ShopAddEditIntent.UpdateCityDropdownExpanded(false))
                                localFocusManager.moveFocus(FocusDirection.Next)
                            }, leadingIcon = { DefaultIcon(Icons.Default.Add) })
                        }
                    }
                }
                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = uiState.street,
                            onValueChange = { onIntent(ShopAddEditIntent.UpdateStreet(it)) },
                            modifier = Modifier.weight(1f).padding(end = 4.dp).onPreviewKeyEvent { keyEvent ->
                                when (keyEvent.key) {
                                    Key.Tab if keyEvent.type == KeyEventType.KeyDown -> {
                                        localFocusManager.moveFocus(FocusDirection.Next)
                                        true
                                    }

                                    Key.Enter if keyEvent.type == KeyEventType.KeyDown -> {
                                        localFocusManager.moveFocus(FocusDirection.Next)
                                        true
                                    }

                                    else -> {
                                        false
                                    }
                                }
                            },
                            label = { Text(stringResource(Res.string.street)) },
                            maxLines = 1,
                            isError = uiState.street.isBlank() || uiState.street.contains(".") || uiState.street.contains(","),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                            keyboardActions = KeyboardActions(
                                onNext = { localFocusManager.moveFocus(FocusDirection.Next) }),
                            shape = MaterialTheme.shapes.medium,
                        )
                        OutlinedTextField(
                            value = uiState.houseNumber,
                            onValueChange = { onIntent(ShopAddEditIntent.UpdateHouseNumber(it)) },
                            label = { Text(stringResource(Res.string.house_number), maxLines = 1) },
                            singleLine = true,
                            modifier = Modifier.weight(.8f).onPreviewKeyEvent { keyEvent ->
                                when (keyEvent.key) {
                                    Key.Tab if keyEvent.type == KeyEventType.KeyDown -> {
                                        localFocusManager.moveFocus(FocusDirection.Next)
                                        true
                                    }

                                    Key.Enter if keyEvent.type == KeyEventType.KeyDown -> {
                                        localFocusManager.moveFocus(FocusDirection.Next)
                                        true
                                    }

                                    else -> {
                                        false
                                    }
                                }
                            },
                            isError = uiState.houseNumber.contains(".") || uiState.houseNumber.contains(","),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                            keyboardActions = KeyboardActions(
                                onNext = { localFocusManager.moveFocus(FocusDirection.Next) }),
                            shape = MaterialTheme.shapes.medium,
                        )
                    }
                }
                item {
                    OutlinedTextField(
                        value = uiState.addressComment.orEmpty(),
                        onValueChange = { onIntent(ShopAddEditIntent.UpdateAddressComment(it)) },
                        label = { Text(stringResource(Res.string.address_comment_label)) },
                        placeholder = { Text(stringResource(Res.string.address_comment_placeholder)) },
                        maxLines = 2,
                        modifier = Modifier.fillMaxWidth().onPreviewKeyEvent { keyEvent ->
                            if (keyEvent.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                            when (keyEvent.key) {
                                Key.Tab, Key.Enter, Key.NumPadEnter -> {
                                    localFocusManager.moveFocus(FocusDirection.Next)
                                    true
                                }

                                else -> false
                            }
                        },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(onNext = { localFocusManager.moveFocus(FocusDirection.Next) }),
                        shape = MaterialTheme.shapes.medium
                    )
                }
                item {
                    OutlinedTextField(
                        value = uiState.phoneNumber.orEmpty(),
                        onValueChange = { newValue ->
                            val trimmedValue = newValue.filter { it.isDigit() }.take(10)
                            onIntent(ShopAddEditIntent.UpdatePhoneNumber(trimmedValue))
                        },
                        label = { Text(stringResource(Res.string.phone_number)) },
                        isError = uiState.phoneNumber.isNullOrBlank() || uiState.phoneNumber.length < 10,
                        maxLines = 1,
                        visualTransformation = UkrainianPhoneVisualTransformation(),
                        modifier = Modifier.fillMaxWidth().onPreviewKeyEvent { keyEvent ->
                            when (keyEvent.key) {
                                Key.Tab if keyEvent.type == KeyEventType.KeyDown -> {
                                    localFocusManager.moveFocus(FocusDirection.Next)
                                    true
                                }

                                Key.Enter if keyEvent.type == KeyEventType.KeyDown -> {
                                    localFocusManager.moveFocus(FocusDirection.Next)
                                    onIntent(ShopAddEditIntent.UpdateStatusDropdownExpanded(true))
                                    true
                                }

                                else -> {
                                    false
                                }
                            }
                        },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(
                            onNext = { localFocusManager.moveFocus(FocusDirection.Next) }),
                        shape = MaterialTheme.shapes.medium,
                    )
                }
                item {
                    var deviceDropdownIndex by remember { mutableIntStateOf(0) }

                    ExposedDropdownMenuBox(
                        expanded = uiState.deviceDropdownExpanded,
                        onExpandedChange = {
                            onIntent(ShopAddEditIntent.UpdateDeviceDropdownExpanded(!uiState.deviceDropdownExpanded))
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = stringResource(uiState.deviceType.title),
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(stringResource(Res.string.device_for_slides)) },
                            leadingIcon = { Icon(uiState.deviceType.icon, null) },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = uiState.deviceDropdownExpanded)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                            shape = MaterialTheme.shapes.medium
                        )

                        // Випадаюче меню
                        ExposedDropdownMenu(
                            expanded = uiState.deviceDropdownExpanded,
                            onDismissRequest = {
                                onIntent(ShopAddEditIntent.UpdateDeviceDropdownExpanded(false))
                            }
                        ) {
                            DeviceType.all().forEach { info ->

                                val isSelected = info.index == deviceDropdownIndex
                                val backgroundColor =
                                    if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = DefaultValues.Color.MENU_ALPHA)
                                    else Color.Unspecified

                                DropdownMenuItem(
                                    text = { BodyText(text = info.name) },
                                    leadingIcon = { DefaultIcon(info.icon) },
                                    onClick = {
                                        deviceDropdownIndex = info.index
                                        onIntent(ShopAddEditIntent.UpdateDeviceType(info.type))
                                        onIntent(ShopAddEditIntent.UpdateDeviceDropdownExpanded(false))
                                    },
                                    modifier = Modifier
                                        .padding(bottom = 10.dp)
                                        .background(backgroundColor)
                                )

                                if (info.index < DeviceType.entries.size - 1) {
                                    HorizontalDivider(modifier = Modifier.padding(bottom = 10.dp))
                                }
                            }
                        }
                    }
                }
                item {
                    var statusDropdownIndex by remember { mutableIntStateOf(0) }
                    ExposedDropdownMenuBox(
                        expanded = uiState.statusDropdownExpanded,
                        onExpandedChange = { onIntent(ShopAddEditIntent.UpdateStatusDropdownExpanded(!uiState.statusDropdownExpanded)) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = stringResource(uiState.status.title),
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(stringResource(Res.string.status)) },
                            leadingIcon = { Icon(uiState.status.icon, null) },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = uiState.statusDropdownExpanded)
                            },
                            modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                            shape = MaterialTheme.shapes.medium
                        )

                        // Випадаюче меню
                        ExposedDropdownMenu(
                            expanded = uiState.statusDropdownExpanded, onDismissRequest = {
                                onIntent(ShopAddEditIntent.UpdateStatusDropdownExpanded(false))
                            }) {
                            ShopStatus.all().forEach { info ->

                                val isSelected = info.index == statusDropdownIndex
                                val backgroundColor =
                                    if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = DefaultValues.Color.MENU_ALPHA) else Color.Unspecified

                                DropdownMenuItem(
                                    text = { BodyText(text = info.name) },
                                    leadingIcon = { DefaultIcon(info.icon) },
                                    onClick = {
                                        statusDropdownIndex = info.index
                                        onIntent(ShopAddEditIntent.UpdateStatus(info.status))
                                        onIntent(ShopAddEditIntent.UpdateStatusDropdownExpanded(false))
                                    },
                                    modifier = Modifier.padding(bottom = 10.dp).background(backgroundColor)
                                )
                                if (info.index < ShopStatus.entries.size - 1) {
                                    HorizontalDivider(modifier = Modifier.padding(bottom = 10.dp))
                                }
                            }
                        }
                    }
                }
                item {
                    OutlinedTextField(
                        value = uiState.statusComment.orEmpty(),
                        onValueChange = { onIntent(ShopAddEditIntent.UpdateStatusComment(it)) },
                        label = { Text(stringResource(Res.string.status_comment)) },
                        maxLines = 4,
                        isError = (uiState.status == ShopStatus.RELOCATING || uiState.status == ShopStatus.UNDER_REPAIR) && uiState.statusComment?.isBlank() == true,
                        modifier = Modifier.fillMaxWidth().onPreviewKeyEvent { keyEvent ->
                            when (keyEvent.key) {
                                Key.Tab if keyEvent.type == KeyEventType.KeyDown -> {
                                    localFocusManager.moveFocus(FocusDirection.Next)
                                    true
                                }

                                Key.Enter if keyEvent.type == KeyEventType.KeyDown -> {
                                    localFocusManager.moveFocus(FocusDirection.Next)
                                    true
                                }

                                else -> {
                                    false
                                }
                            }
                        },
                        shape = MaterialTheme.shapes.medium,
                    )
                }
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = uiState.internetProvider.orEmpty(),
                            onValueChange = { onIntent(ShopAddEditIntent.UpdateInternetProvider(it)) },
                            label = { Text(stringResource(Res.string.internet_provider)) },
                            maxLines = 1,
                            modifier = Modifier.weight(1f).padding(end = 4.dp).onPreviewKeyEvent { keyEvent ->
                                when (keyEvent.key) {
                                    Key.Tab if keyEvent.type == KeyEventType.KeyDown -> {
                                        localFocusManager.moveFocus(FocusDirection.Next)
                                        true
                                    }

                                    Key.Enter if keyEvent.type == KeyEventType.KeyDown -> {
                                        localFocusManager.moveFocus(FocusDirection.Next)
                                        true
                                    }

                                    else -> {
                                        false
                                    }
                                }
                            },
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                            keyboardActions = KeyboardActions(
                                onNext = { localFocusManager.moveFocus(FocusDirection.Next) }),
                            shape = MaterialTheme.shapes.medium,
                        )

                        val prefix = "UAH"
                        val suffix = ".00"

                        OutlinedTextField(
                            value = uiState.internetReplenishmentAmount ?: "",
                            onValueChange = { newValue ->
                                onIntent(ShopAddEditIntent.UpdateInternetReplenishmentAmountInput(newValue))
                            },
                            label = { Text(stringResource(Res.string.replenishment_amount_label)) },
                            prefix = { if (uiState.internetReplenishmentAmount?.isNotBlank() == true) Text(prefix) },
                            suffix = { if (uiState.internetReplenishmentAmount?.isNotBlank() == true) Text(suffix) },
                            maxLines = 1,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next
                            ),
                            keyboardActions = KeyboardActions(
                                onNext = { localFocusManager.moveFocus(FocusDirection.Next) }),
                            modifier = Modifier.weight(.8f).onPreviewKeyEvent { keyEvent ->
                                if (keyEvent.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                                when (keyEvent.key) {
                                    Key.Tab, Key.Enter, Key.NumPadEnter -> {
                                        localFocusManager.moveFocus(FocusDirection.Next)
                                        true
                                    }

                                    else -> false
                                }
                            },
                            shape = MaterialTheme.shapes.medium
                        )
                    }
                }
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = uiState.internetProviderAccountInput,
                            onValueChange = {
                                onIntent(
                                    ShopAddEditIntent.UpdateInternetProviderAccountInput(
                                        it
                                    )
                                )
                            },
                            label = { Text(stringResource(Res.string.internet_provider_account)) },
                            maxLines = 1,
                            modifier = Modifier.weight(1f).onPreviewKeyEvent { keyEvent ->
                                if (keyEvent.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false

                                if (keyEvent.isCtrlPressed && keyEvent.key == Key.V) {
                                    if (uiState.internetProviderAccountInput.isNotBlank()) {
                                        onIntent(ShopAddEditIntent.AddInternetProviderPersonalAccount(uiState.internetProviderAccountInput))
                                    }
                                    true
                                } else false

                                when (keyEvent.key) {
                                    Key.Tab -> {
                                        localFocusManager.moveFocus(FocusDirection.Next)
                                        true
                                    }

                                    Key.Enter, Key.NumPadEnter -> {
                                        if (uiState.internetProviderAccountInput.isBlank()) return@onPreviewKeyEvent false
                                        onIntent(
                                            ShopAddEditIntent.AddInternetProviderPersonalAccount(
                                                uiState.internetProviderAccountInput
                                            )
                                        )
                                        localFocusManager.moveFocus(FocusDirection.Next)
                                        true
                                    }

                                    else -> {
                                        false
                                    }
                                }
                            },
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                            keyboardActions = KeyboardActions(
                                onNext = { localFocusManager.moveFocus(FocusDirection.Next) }),
                            shape = MaterialTheme.shapes.medium,
                        )

                        AddButton(modifier = Modifier.padding(start = 8.dp), enabled = uiState.internetProviderAccountInput.isNotBlank()) {
                            if (uiState.internetProviderAccountInput.isBlank()) return@AddButton
                            onIntent(
                                ShopAddEditIntent.AddInternetProviderPersonalAccount(
                                    uiState.internetProviderAccountInput
                                )
                            )
                        }
                    }
                }
                items(uiState.internetProviderPersonalAccount ?: emptyList(), key = { it }) { account ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp).animateItem()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically
                        ) {
                            BodyText(
                                text = "${stringResource(Res.string.internet_provider_account)}: $account",
                                modifier = Modifier.weight(1f)
                            )
                            DeleteButton(
                                message = stringResource(Res.string.delete_confirmation_message, "${stringResource(Res.string.internet_provider_account)} $account")
                            ) {
                                onIntent(ShopAddEditIntent.RemoveInternetProviderPersonalAccount(account))
                            }
                        }
                    }
                }
                item {
                    var showDialog by remember { mutableStateOf(false) }

                    ExposedDropdownMenuBox(
                        expanded = showDialog, onExpandedChange = { showDialog = true }, modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = uiState.internetReplenishmentDay.toString(),
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(stringResource(Res.string.internet_replenishment_day)) },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = false)
                            },
                            modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                            shape = MaterialTheme.shapes.medium
                        )
                    }

                    if (showDialog) {
                        var selectedDay by remember { mutableStateOf(uiState.internetReplenishmentDay) }

                        AlertDialog(
                            onDismissRequest = { showDialog = false },
                            title = { TitleText(stringResource(Res.string.select_day)) },
                            text = {
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(DefaultValues.Padding.verticalListItemPadding)
                                ) {
                                    BodyText("${stringResource(Res.string.current_value)}: ${uiState.internetReplenishmentDay}")
                                    BodyText("${stringResource(Res.string.selected_day)}: $selectedDay")
                                    LazyVerticalGrid(
                                        columns = GridCells.Fixed(5),
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp),
                                    ) {
                                        items(31) {
                                            val day = it + 1
                                            val color = if (day == selectedDay) Color.Red else LocalContentColor.current

                                            OutlinedIconButton(
                                                onClick = { selectedDay = day },
                                                modifier = Modifier.size(35.dp),
                                                border = BorderStroke(2.dp, color = color.copy(.5f)),
                                            ) {
                                                BodyText(
                                                    text = day.toString(), color = color
                                                )
                                            }
                                        }
                                    }
                                }
                            },
                            confirmButton = {
                                SaveButton {
                                    onIntent(ShopAddEditIntent.UpdateInternetReplenishmentDay(selectedDay))
                                    showDialog = false
                                }
                            },
                            dismissButton = {
                                CancelButton { showDialog = false }
                            }
                        )
                    }
                }
                item {
                    OutlinedTextField(
                        value = uiState.remoteNumber.orEmpty(),
                        onValueChange = { onIntent(ShopAddEditIntent.UpdateRemoteNumberInput(it)) },
                        label = { Text(stringResource(Res.string.remote_number)) },
                        maxLines = 1,
                        modifier = Modifier.fillMaxWidth().onPreviewKeyEvent { keyEvent ->
                            if (keyEvent.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                            when (keyEvent.key) {
                                Key.Tab -> {
                                    localFocusManager.moveFocus(FocusDirection.Next)
                                    true
                                }

                                Key.Enter, Key.NumPadEnter -> {
                                    localFocusManager.moveFocus(FocusDirection.Next)
                                    true
                                }

                                else -> {
                                    false
                                }
                            }
                        },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(
                            onNext = { localFocusManager.moveFocus(FocusDirection.Next) }),
                        shape = MaterialTheme.shapes.medium,
                    )
                }
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = uiState.cameraCodeInput,
                            onValueChange = { onIntent(ShopAddEditIntent.UpdateCameraCodeInput(it)) },
                            label = { Text(stringResource(Res.string.camera_codes)) },
                            maxLines = 1,
                            modifier = Modifier.weight(1f).onPreviewKeyEvent { keyEvent ->
                                if (keyEvent.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                                if (keyEvent.isCtrlPressed && keyEvent.key == Key.V) {
                                    if (uiState.cameraCodeInput.isNotBlank()) {
                                        onIntent(ShopAddEditIntent.AddCameraCode(uiState.cameraCodeInput))
                                    }
                                    true
                                } else false
                                when (keyEvent.key) {
                                    Key.Tab -> {
                                        localFocusManager.moveFocus(FocusDirection.Next)
                                        true
                                    }

                                    Key.Enter, Key.NumPadEnter -> {
                                        if (uiState.cameraCodeInput.isBlank()) return@onPreviewKeyEvent false
                                        onIntent(ShopAddEditIntent.AddCameraCode(uiState.cameraCodeInput))
                                        localFocusManager.moveFocus(FocusDirection.Next)
                                        true
                                    }

                                    else -> {
                                        false
                                    }
                                }
                            },
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                            keyboardActions = KeyboardActions(
                                onNext = { localFocusManager.moveFocus(FocusDirection.Next) }),
                            shape = MaterialTheme.shapes.medium,
                        )

                        AddButton(modifier = Modifier.padding(start = 8.dp), enabled = uiState.cameraCodeInput.isNotBlank()) {
                            if (uiState.cameraCodeInput.isBlank()) return@AddButton
                            onIntent(ShopAddEditIntent.AddCameraCode(uiState.cameraCodeInput))
                        }
                    }
                }
                items(uiState.cameraCodes.orEmpty(), key = { it }) { code ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp).animateItem()
                    ) {
                        Row(
                            modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically
                        ) {

                            BodyText(
                                text = "${stringResource(Res.string.code)}: $code",
                                modifier = Modifier.weight(1f)
                            )

                            DeleteButton(
                                message = stringResource(Res.string.delete_confirmation_message, "цей код камери: $code")
                            ) { onIntent(ShopAddEditIntent.RemoveCameraCode(code)) }
                        }
                    }
                }
            }

            DefaultVerticalScrollbar(scrollState = state)

        }

        ButtonRowContainer {
            if (isWideScreen()) {
                BackButton(modifier = Modifier.weight(1f), onBack = onBack)
            }

            if (isEditMode) {
                SaveButton(
                    enabled = uiState.selectedCityId != null && uiState.street.isNotBlank() && uiState.phoneNumber?.isNotBlank() ?: false,
                    modifier = Modifier.weight(1f)
                ) {
                    onIntent(ShopAddEditIntent.ValidateAndSaveShopAddEdit)
                }
                DeleteButton(
                    modifier = Modifier.weight(1f),
                    message = stringResource(Res.string.delete_confirmation_message, "${stringResource(Res.string.this_shop)}: ${uiState.street}, ${uiState.houseNumber}")
                ) {
                    onIntent(ShopAddEditIntent.DeleteShopAddEdit(uiState.shopId))
                }
                CancelButton(modifier = Modifier.weight(1f)) { onDismissRequest() }
            } else {
                AddButton(
                    enabled = uiState.selectedCityId != null && uiState.street.isNotBlank() && uiState.phoneNumber?.isNotBlank() ?: false,
                    modifier = Modifier.weight(1f),
                    title = stringResource(Res.string.add_shop)
                ) {
                    onIntent(ShopAddEditIntent.ValidateAndSaveShopAddEdit)
                }
                CancelButton(modifier = Modifier.weight(1f)) {
                    onIntent(ShopAddEditIntent.ClearData)
                }
            }
        }
    }
}

@Preview(
    showSystemUi = true,
    device = Devices.DESKTOP,
)
@Composable
private fun ShopAddEditContentPreview1() = PreviewDarkMaterialTheme {
    ShopEditContent(
        isEditMode = false,
        uiState = ShopAddEditState(),
        onDismissRequest = {},
        onBack = {},
        onIntent = {},
        onAddNewCity = {}
    )
}

@Preview(
    showSystemUi = true,
    device = Devices.DESKTOP,
)
@Composable
private fun ShopAddEditContentPreview2() = PreviewLightMaterialTheme {
    ShopEditContent(
        isEditMode = false,
        uiState = ShopAddEditState(),
        onDismissRequest = {},
        onBack = {},
        onIntent = {},
        onAddNewCity = {}
    )
}

@Preview
@Composable
private fun ShopAddEditContentPreview3() = PreviewDarkMaterialTheme {
    ShopEditContent(
        isEditMode = false,
        uiState = ShopAddEditState(),
        onDismissRequest = {},
        onBack = {},
        onIntent = {},
        onAddNewCity = {}
    )
}

@Preview
@Composable
private fun ShopAddEditContentPreview4() = PreviewLightMaterialTheme {
    ShopEditContent(
        isEditMode = false,
        uiState = ShopAddEditState(),
        onDismissRequest = {},
        onBack = {},
        onIntent = {},
        onAddNewCity = {}
    )
}