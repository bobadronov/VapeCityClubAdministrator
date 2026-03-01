@file:Suppress("AssignedValueIsNeverRead")

package org.bigblackowl.vccadmin.ui.city.addEdit

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.bigblackowl.vccadmin.data.events.UIEvents
import org.bigblackowl.vccadmin.data.repository.FakeBackend
import org.bigblackowl.vccadmin.navigation.NavigationViewModel
import org.bigblackowl.vccadmin.theme.DefaultValues
import org.bigblackowl.vccadmin.theme.PreviewDarkMaterialTheme
import org.bigblackowl.vccadmin.theme.PreviewLightMaterialTheme
import org.bigblackowl.vccadmin.uiComponent.buttons.AddButton
import org.bigblackowl.vccadmin.uiComponent.buttons.BackButton
import org.bigblackowl.vccadmin.uiComponent.buttons.CancelButton
import org.bigblackowl.vccadmin.uiComponent.buttons.DeleteButton
import org.bigblackowl.vccadmin.uiComponent.buttons.SaveButton
import org.bigblackowl.vccadmin.uiComponent.container.ButtonRowContainer
import org.bigblackowl.vccadmin.uiComponent.dialog.UnsavedChangesDialog
import org.bigblackowl.vccadmin.uiComponent.icons.OnlineIcon
import org.bigblackowl.vccadmin.uiComponent.indicators.LoadingComponent
import org.bigblackowl.vccadmin.uiComponent.listItems.DefaultVerticalScrollbar
import org.bigblackowl.vccadmin.uiComponent.text.BodyText
import org.bigblackowl.vccadmin.uiComponent.text.SmallText
import org.bigblackowl.vccadmin.utils.isWideScreen
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import vccadministrator.composeapp.generated.resources.Res
import vccadministrator.composeapp.generated.resources.change_logo
import vccadministrator.composeapp.generated.resources.city
import vccadministrator.composeapp.generated.resources.delete_confirmation_message
import vccadministrator.composeapp.generated.resources.edit_city
import vccadministrator.composeapp.generated.resources.enter_city_name
import vccadministrator.composeapp.generated.resources.invalid_city_name
import vccadministrator.composeapp.generated.resources.no_matches_with_existing_list
import vccadministrator.composeapp.generated.resources.select_logo

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun AddEditCityScreen(
    cityId: Int?,
    snackbarHostState: SnackbarHostState,
    navigationViewModel: NavigationViewModel,
    addEditCityScreenViewModel: AddEditCityScreenViewModel = koinInject(),
) {
    val isEdit = cityId != null
    var showUnsavedDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (isEdit) addEditCityScreenViewModel.onIntent(AddEditCityScreenIntent.GetCity(cityId))
    }

    DisposableEffect(Unit) { onDispose { addEditCityScreenViewModel.onIntent(AddEditCityScreenIntent.Clear) } }

    val autocomplete by addEditCityScreenViewModel.cityAutocomplete.collectAsStateWithLifecycle()

    // Обробка одноразових подій (наприклад, помилок у Snackbar)
    LaunchedEffect(Unit) {
        addEditCityScreenViewModel.uiEvent.collect { event ->
            when (event) {
                is UIEvents.ShowMessage -> snackbarHostState.showSnackbar(event.message)
                is UIEvents.NavigateBack -> {
                    navigationViewModel.popBackStack()
                }
                is UIEvents.ShowUnsavedChangesDialog -> showUnsavedDialog = true
                else -> {}
            }
        }
    }

    val state by addEditCityScreenViewModel.state.collectAsStateWithLifecycle()

    AddEditCityScreenContent(
        uiState = state,
        autocomplete = autocomplete,
        onIntent = addEditCityScreenViewModel::onIntent,
    )

    UnsavedChangesDialog(
        show = showUnsavedDialog,
        onSave = {
            addEditCityScreenViewModel.onIntent(AddEditCityScreenIntent.Save)
            showUnsavedDialog = false
        },
        onDiscard = {
            addEditCityScreenViewModel.onIntent(AddEditCityScreenIntent.DiscardAndBack)
            showUnsavedDialog = false
        },
        onCancel = { showUnsavedDialog = false },
        onDismissRequest = { showUnsavedDialog = false },
    )
}

@Composable
private fun AddEditCityScreenContent(
    uiState: AddEditCityScreenUiState,
    autocomplete: CityAutocompleteUiState,
    onIntent: (AddEditCityScreenIntent) -> Unit,
) {
    val haptic = LocalHapticFeedback.current

    val isAddMode = uiState.selectedCity == null

    if (uiState.isLoading) {
        LoadingComponent()
        return
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(DefaultValues.Padding.mainBoxPadding),
        verticalArrangement = Arrangement.spacedBy(DefaultValues.Padding.verticalListItemPadding),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val imageModel = uiState.newCityLogoFile?.toString() ?: uiState.selectedCity?.logoUrl

        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(DefaultValues.Padding.verticalListItemPadding)
        ) {

            Crossfade(targetState = imageModel) { model ->
                OnlineIcon(
                    model = model,
                    contentDescription = stringResource(Res.string.select_logo),
                    modifier = Modifier.size(250.dp),
                )
            }

            OutlinedButton(
                onClick = {
                    onIntent(AddEditCityScreenIntent.EditLogo)
                    haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                }, modifier = Modifier.fillMaxWidth(0.8f)
            ) {
                BodyText(text = if (imageModel != null) stringResource(Res.string.change_logo) else stringResource(Res.string.select_logo))
            }

            CityAutocompleteField(
                value = uiState.newCityName,
                onValueChange = {
                    onIntent(AddEditCityScreenIntent.EditName(it))
                    haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                },
                onSuggestionSelected = {
                    onIntent(AddEditCityScreenIntent.CitySelected(it))
                    haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                },
                onHighlightNext = {
                    onIntent(AddEditCityScreenIntent.HighlightNextCity)
                    haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                },
                onHighlightPrev = {
                    onIntent(AddEditCityScreenIntent.HighlightPrevCity)
                    haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                },
                onSelectHighlighted = {
                    onIntent(AddEditCityScreenIntent.SelectHighlightedCity)
                    haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                },
                isLoading = autocomplete.isLoading,
                suggestions = autocomplete.suggestions,
                highlightedIndex = autocomplete.highlightedIndex,
                label = { Text(if (isAddMode) stringResource(Res.string.enter_city_name) else stringResource(Res.string.edit_city)) },
                modifier = Modifier.fillMaxWidth(0.8f),
                isError = uiState.newCityName.length < 2,
                supportingText = {
                    if (uiState.newCityName.length < 2) SmallText(stringResource(Res.string.invalid_city_name))
                },
            )
        }

        ButtonRowContainer {
            if (isWideScreen()) {
                BackButton(modifier = Modifier.weight(1f)) {
                    onIntent(AddEditCityScreenIntent.GoBack)
                    haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                }
            }
            if (isAddMode) {
                AddButton(
                    enabled = uiState.newCityName.length >= 2, modifier = Modifier.weight(1f)
                ) {
                    onIntent(AddEditCityScreenIntent.Save)
                    haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                }
                CancelButton(modifier = Modifier.weight(1f)) {
                    onIntent(AddEditCityScreenIntent.GoBack)
                    haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                }
            } else {
                DeleteButton(
                    message = stringResource(Res.string.delete_confirmation_message, "${stringResource(Res.string.city).lowercase()}: ${uiState.selectedCity.name}"),
                    modifier = Modifier.weight(1f)
                ) {
                    onIntent(AddEditCityScreenIntent.DeleteCity(uiState.selectedCity))
                    haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                }
                SaveButton(
                    enabled = uiState.newCityName.length >= 2, modifier = Modifier.weight(1f)
                ) {
                    onIntent(AddEditCityScreenIntent.Save)
                    haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                }
            }
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun CityAutocompleteField(
    value: String,
    onValueChange: (String) -> Unit,
    onSuggestionSelected: (CitySuggestion) -> Unit,

    onHighlightNext: () -> Unit,
    onHighlightPrev: () -> Unit,
    onSelectHighlighted: () -> Unit,

    isLoading: Boolean,
    suggestions: List<CitySuggestion>,
    highlightedIndex: Int,

    modifier: Modifier = Modifier,
    label: @Composable (() -> Unit)? = null,
    isError: Boolean = false,
    supportingText: @Composable (() -> Unit)? = null,
) {
    val tfFocus = remember { FocusRequester() }

    val scrollState = rememberLazyListState()

    LaunchedEffect(suggestions.size, highlightedIndex) {
        if (suggestions.isEmpty()) return@LaunchedEffect
        if (highlightedIndex !in suggestions.indices) return@LaunchedEffect
        scrollState.animateScrollToItem(highlightedIndex)
    }

    Column(modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = { onValueChange(it) },
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(tfFocus)
                .onPreviewKeyEvent { e ->
                    if (e.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                    when (e.key) {
                        Key.DirectionDown -> {
                            if (suggestions.isNotEmpty()) {
                                onHighlightNext()
                                true
                            } else false
                        }

                        Key.DirectionUp -> {
                            if (suggestions.isNotEmpty()) {
                                onHighlightPrev()
                                true
                            } else false
                        }

                        Key.Enter, Key.NumPadEnter -> {
                            if (suggestions.isNotEmpty()) {
                                onSelectHighlighted()
                                true
                            } else false
                        }

                        Key.Escape -> {
                            tfFocus.requestFocus(); true
                        }

                        else -> false
                    }
                },
            label = label,
            singleLine = true,
            isError = isError,
            supportingText = supportingText,
        )

        // ВАЖЛИВО: це НЕ Popup. Це звичайний блок під полем -> фокус не зникає.

        Surface(
            tonalElevation = 2.dp,
            shape = RoundedCornerShape(DefaultValues.Shape.defaultShape),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp)
        ) {
            when {
                suggestions.isEmpty() -> {
                    BodyText(
                        text = stringResource(Res.string.no_matches_with_existing_list),
                        modifier = Modifier
                            .padding(DefaultValues.Padding.cardContentPadding),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }

                else -> {
                    Column {
                        Row(Modifier.fillMaxWidth().weight(1f)) {
                            LazyColumn(
                                modifier = Modifier.weight(1f),
                                state = scrollState,
                            ) {
                                items(suggestions.size) { index ->
                                    val s = suggestions[index]
                                    val selected = index == highlightedIndex

                                    val bg = if (selected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent
                                    val fg = if (selected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurface

                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(10.dp),
                                        colors = CardDefaults.cardColors().copy(containerColor = bg),
                                        onClick = { if (!s.exists) onSuggestionSelected(s) }
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(DefaultValues.Padding.cardContentPadding)
                                        ) {
                                            Text(
                                                text = s.city.name,
                                                style = if (selected) MaterialTheme.typography.titleMedium
                                                else MaterialTheme.typography.bodyLarge,
                                                color = fg
                                            )
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = s.city.oblast,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = if (selected) fg else MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                if (s.exists) {
                                                    Text(
                                                        text = "  •  вже додано",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.error
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            DefaultVerticalScrollbar(scrollState = scrollState)
                        }

                        AnimatedVisibility(
                            visible = isLoading && value.trim().length >= 2,
                            modifier = Modifier.fillMaxWidth(),
                            enter = slideInVertically { it },
                            exit = slideOutVertically { -it },
                        ) {
                            LinearWavyProgressIndicator(Modifier.fillMaxWidth())
                        }
                    }
                }
            }
        }
    }
}

// ---------- Previews ----------
@Preview
@Composable
private fun AddEditCityScreenContentPreview_AddMode_Empty() = PreviewLightMaterialTheme {
    AddEditCityScreenContent(
        uiState = AddEditCityScreenUiState(
            isLoading = false,
            selectedCity = null,
            newCityName = "",
        ),
        onIntent = {},
        autocomplete = CityAutocompleteUiState(
            isLoading = false,
            suggestions = emptyList(),
            highlightedIndex = -1
        )
    )
}

@Preview
@Composable
private fun AddEditCityScreenContentPreview_AddMode_LoadingDropdown() = PreviewLightMaterialTheme {
    AddEditCityScreenContent(
        uiState = AddEditCityScreenUiState(
            isLoading = false,
            selectedCity = null,
            newCityName = "Ки",
        ),
        onIntent = {},
        autocomplete = CityAutocompleteUiState(
            isLoading = true,
            suggestions = emptyList(),
            highlightedIndex = -1
        )
    )
}

@Preview
@Composable
private fun AddEditCityScreenContentPreview_AddMode_WithSuggestions() = PreviewLightMaterialTheme {
    AddEditCityScreenContent(
        uiState = AddEditCityScreenUiState(
            isLoading = false,
            selectedCity = null,
            newCityName = "Ки",
        ),
        onIntent = {},
        autocomplete = CityAutocompleteUiState(
            isLoading = false,
            suggestions = FakeBackend.previewSuggestionsList,
            highlightedIndex = 1 // підсвітимо другий доступний (exists=false)
        )
    )
}

@Preview
@Composable
private fun AddEditCityScreenContentPreview_EditMode_WithSuggestions() = PreviewDarkMaterialTheme {
    AddEditCityScreenContent(
        uiState = AddEditCityScreenUiState(
            isLoading = false,
            selectedCity = FakeBackend.singleCity,
            initialName = FakeBackend.singleCity.name,
            newCityName = "Ір",
        ),
        onIntent = {},
        autocomplete = CityAutocompleteUiState(
            isLoading = true,
            suggestions = FakeBackend.previewSuggestionsList,
            highlightedIndex = 0
        ),
    )
}