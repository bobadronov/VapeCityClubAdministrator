@file:Suppress("AssignedValueIsNeverRead")

package org.bigblackowl.vccadmin.ui.city.addEdit

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.bigblackowl.vccadmin.data.repository.FakeBackend
import org.bigblackowl.vccadmin.data.state.UIEvents
import org.bigblackowl.vccadmin.navigation.NavigationViewModel
import org.bigblackowl.vccadmin.resourses.DefaultValues
import org.bigblackowl.vccadmin.theme.PreviewDarkMaterialTheme
import org.bigblackowl.vccadmin.theme.PreviewLightMaterialTheme
import org.bigblackowl.vccadmin.uiComponent.LoadingComponent
import org.bigblackowl.vccadmin.uiComponent.buttons.AddButton
import org.bigblackowl.vccadmin.uiComponent.buttons.BackButton
import org.bigblackowl.vccadmin.uiComponent.buttons.CancelButton
import org.bigblackowl.vccadmin.uiComponent.buttons.DeleteButton
import org.bigblackowl.vccadmin.uiComponent.buttons.SaveButton
import org.bigblackowl.vccadmin.uiComponent.container.ButtonRowContainer
import org.bigblackowl.vccadmin.uiComponent.dialog.UnsavedChangesDialog
import org.bigblackowl.vccadmin.uiComponent.icons.OnlineIcon
import org.bigblackowl.vccadmin.uiComponent.listItems.DefaultScrollbar
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

    DisposableEffect(Unit) { onDispose { addEditCityScreenViewModel.onIntent(AddEditCityScreenIntent.Clear) } }

    LaunchedEffect(Unit) {
        if (isEdit) addEditCityScreenViewModel.onIntent(AddEditCityScreenIntent.GetCity(cityId))
    }

    val uiEvent by addEditCityScreenViewModel.uiEvent.collectAsStateWithLifecycle(null)

    // Обробка одноразових подій (наприклад, помилок у Snackbar)
    LaunchedEffect(uiEvent) {
        uiEvent?.let { event ->
            when (event) {
                is UIEvents.ShowMessage -> snackbarHostState.showSnackbar(event.message)
                is UIEvents.NavigateBack -> navigationViewModel.popBackStack()
                is UIEvents.ShowUnsavedChangesDialog -> showUnsavedDialog = true
                else -> {}
            }
        }
    }

    val state by addEditCityScreenViewModel.state.collectAsStateWithLifecycle()

    AddEditCityScreenContent(
        uiState = state,
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
    onIntent: (AddEditCityScreenIntent) -> Unit,
) {

    val isAddMode = uiState.selectedCity == null

    val listState = rememberLazyListState()

    if (uiState.isLoading) {
        LoadingComponent()
        return
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(DefaultValues.Padding.verticalListItemPadding),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val imageModel = uiState.newCityLogoFile?.toString() ?: uiState.selectedCity?.logoUrl
        Row(Modifier.weight(1f)) {
            LazyColumn(
                modifier = Modifier.weight(1f),
                state = listState,
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(DefaultValues.Padding.verticalListItemPadding)
            ) {
                item {
                    Crossfade(targetState = imageModel) { model ->
                        OnlineIcon(
                            model = model,
                            contentDescription = stringResource(Res.string.select_logo),
                            modifier = Modifier.size(250.dp),
                        )
                    }
                }
                item {
                    OutlinedButton(
                        onClick = { onIntent(AddEditCityScreenIntent.EditLogo) }, modifier = Modifier.fillMaxWidth(0.8f)
                    ) {
                        BodyText(text = if (imageModel != null) stringResource(Res.string.change_logo) else stringResource(Res.string.select_logo))
                    }
                }

                item {
                    OutlinedTextField(
                        value = uiState.newCityName,
                        onValueChange = { onIntent(AddEditCityScreenIntent.EditName(it)) },
                        label = { Text(if (isAddMode) stringResource(Res.string.enter_city_name) else stringResource(Res.string.edit_city)) },
                        modifier = Modifier.fillMaxWidth(0.8f),
                        singleLine = true,
                        isError = uiState.newCityName.length < 2,
                        supportingText = {
                            if (uiState.newCityName.length < 2) SmallText(stringResource(Res.string.invalid_city_name))
                        })
                }
            }

            DefaultScrollbar(scrollState = listState)
        }

        ButtonRowContainer {
            if (isWideScreen()) {
                BackButton(modifier = Modifier.weight(1f)) { onIntent(AddEditCityScreenIntent.GoBack) }
            }
            if (isAddMode) {
                AddButton(
                    enabled = uiState.newCityName.length >= 2, modifier = Modifier.weight(1f)
                ) { onIntent(AddEditCityScreenIntent.Save) }
                CancelButton(modifier = Modifier.weight(1f)) { onIntent(AddEditCityScreenIntent.GoBack) }
            } else {
                DeleteButton(
                    message = stringResource(Res.string.delete_confirmation_message, "${stringResource(Res.string.city).lowercase()}: ${uiState.selectedCity.name}"),
                    modifier = Modifier.weight(1f)
                ) { onIntent(AddEditCityScreenIntent.DeleteCity(uiState.selectedCity)) }
                SaveButton(
                    enabled = uiState.newCityName.length >= 2, modifier = Modifier.weight(1f)
                ) { onIntent(AddEditCityScreenIntent.Save) }
            }
        }
    }
}

@Preview
@Composable
private fun AddEditCityScreenContentPreview1() = PreviewLightMaterialTheme {
    AddEditCityScreenContent(
        uiState = AddEditCityScreenUiState(),
        onIntent = {},
    )
}

@Preview
@Composable
private fun AddEditCityScreenContentPreview2() = PreviewDarkMaterialTheme {
    AddEditCityScreenContent(
        uiState = AddEditCityScreenUiState(
            selectedCity = FakeBackend.singleCity,
        ),
        onIntent = {},
    )
}