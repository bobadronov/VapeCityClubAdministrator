@file:Suppress("AssignedValueIsNeverRead")

package org.bigblackowl.vccadmin.ui.users.addEdit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CopyAll
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import org.bigblackowl.vccadmin.data.entity.UserRole
import org.bigblackowl.vccadmin.data.events.UIEvents
import org.bigblackowl.vccadmin.navigation.NavigationViewModel
import org.bigblackowl.vccadmin.resourses.DefaultValues
import org.bigblackowl.vccadmin.theme.PreviewDarkMaterialTheme
import org.bigblackowl.vccadmin.theme.PreviewLightMaterialTheme
import org.bigblackowl.vccadmin.uiComponent.buttons.BackButton
import org.bigblackowl.vccadmin.uiComponent.buttons.DeleteButton
import org.bigblackowl.vccadmin.uiComponent.buttons.RetryButton
import org.bigblackowl.vccadmin.uiComponent.buttons.SaveButton
import org.bigblackowl.vccadmin.uiComponent.container.ButtonRowContainer
import org.bigblackowl.vccadmin.uiComponent.dialog.UnsavedChangesDialog
import org.bigblackowl.vccadmin.uiComponent.icons.DefaultIcon
import org.bigblackowl.vccadmin.uiComponent.indicators.LoadingComponent
import org.bigblackowl.vccadmin.uiComponent.text.BodyText
import org.bigblackowl.vccadmin.utils.AppStringProvider
import org.bigblackowl.vccadmin.utils.UkrainianPhoneVisualTransformation
import org.bigblackowl.vccadmin.utils.isWideScreen
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import vccadministrator.composeapp.generated.resources.Res
import vccadministrator.composeapp.generated.resources.delete_confirmation_message
import vccadministrator.composeapp.generated.resources.email
import vccadministrator.composeapp.generated.resources.enter_user_role
import vccadministrator.composeapp.generated.resources.first_name
import vccadministrator.composeapp.generated.resources.invalid_email
import vccadministrator.composeapp.generated.resources.last_name
import vccadministrator.composeapp.generated.resources.password
import vccadministrator.composeapp.generated.resources.phone_number
import vccadministrator.composeapp.generated.resources.this_user

@Composable
fun AddEditUserScreen(
    userId: String? = null,
    snackbarHostState: SnackbarHostState,
    navigationViewModel: NavigationViewModel,
    viewModel: AddEditUserScreenViewModel = koinInject(),
) {
    val isEditMode = !userId.isNullOrBlank()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val uiEvent by viewModel.uiEvent.collectAsStateWithLifecycle(null)
    var showUnsavedDialog by remember { mutableStateOf(false) }

    DisposableEffect(userId) {
        if (isEditMode) viewModel.onIntent(AddEditUserScreenIntent.LoadUser(userId))
        onDispose {
            viewModel.onIntent(AddEditUserScreenIntent.DiscardAndBack)
        }
    }

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

    AddEditUserScreenContent(
        state = state,
        isEditMode = isEditMode,
        onIntent = viewModel::onIntent,
    )

    UnsavedChangesDialog(
        show = showUnsavedDialog,
        onSave = {
            showUnsavedDialog = false
            viewModel.onIntent(AddEditUserScreenIntent.Save)
        },
        onDiscard = {
            showUnsavedDialog = false
            viewModel.onIntent(AddEditUserScreenIntent.DiscardAndBack)
        },
        onCancel = { showUnsavedDialog = false },
        onDismissRequest = { showUnsavedDialog = false }
    )
}

@Composable
private fun AddEditUserScreenContent(
    state: AddEditUserUiState,
    isEditMode: Boolean,
    onIntent: (AddEditUserScreenIntent) -> Unit,
) {
    if (state.isLoading) {
        LoadingComponent()
        return
    }
    var showPassword by remember { mutableStateOf(!isEditMode) }
    val focusManager = LocalFocusManager.current
    val localKeyboard = LocalSoftwareKeyboardController.current

    val icon = remember(showPassword) {
        if (showPassword) Icons.Default.Visibility else Icons.Default.VisibilityOff
    }
    @Suppress("DEPRECATION") val clipboardManager = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    Column(
        modifier = Modifier.fillMaxSize().padding(DefaultValues.Padding.mainBoxPadding),
    ) {
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(DefaultValues.Padding.verticalListItemPadding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                OutlinedTextField(
                    value = state.editableFirstName,
                    onValueChange = { onIntent(AddEditUserScreenIntent.UpdateFirstName(it)) },
                    label = { Text(stringResource(Res.string.first_name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                        .semantics { contentDescription = "Поле для введення імені" },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(
                        onNext = { focusManager.moveFocus(FocusDirection.Down) }
                    ),
                    shape = MaterialTheme.shapes.medium,
                )
            }

            item {
                OutlinedTextField(
                    value = state.editableLastName,
                    onValueChange = { onIntent(AddEditUserScreenIntent.UpdateLastName(it)) },
                    label = { Text(stringResource(Res.string.last_name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                        .semantics { contentDescription = "Поле для введення прізвища" },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(
                        onNext = { focusManager.moveFocus(FocusDirection.Down) }
                    ),
                    shape = MaterialTheme.shapes.medium,
                )
            }

            item {
                OutlinedTextField(
                    value = state.editableEmail,
                    onValueChange = { onIntent(AddEditUserScreenIntent.UpdateEmail(it)) },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Next,
                        keyboardType = KeyboardType.Email
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { focusManager.moveFocus(FocusDirection.Down) }
                    ),
                    label = { Text(stringResource(Res.string.email)) },
                    singleLine = true,
                    isError = state.editableEmail.isNotBlank() && !state.editableEmail.matches(Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")),
                    supportingText = {
                        if (state.editableEmail.isNotBlank() && !state.editableEmail.matches(Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$"))) {
                            BodyText(stringResource(Res.string.invalid_email))
                        }
                    },
                    shape = MaterialTheme.shapes.medium,
                )
            }

            item {
                OutlinedTextField(
                    value = state.editablePhone,
                    onValueChange = { newValue ->
                        val trimmedValue = newValue.filter { it.isDigit() }.take(10)
                        onIntent(AddEditUserScreenIntent.UpdatePhone(trimmedValue))
                    },
                    label = { Text(stringResource(Res.string.phone_number)) },
                    maxLines = 1,
                    visualTransformation = UkrainianPhoneVisualTransformation(),
                    modifier = Modifier.fillMaxWidth().onPreviewKeyEvent { keyEvent ->
                        when (keyEvent.key) {
                            Key.Tab if keyEvent.type == KeyEventType.KeyDown -> {
                                focusManager.moveFocus(FocusDirection.Next)
                                true
                            }

                            Key.Enter if keyEvent.type == KeyEventType.KeyDown -> {
                                focusManager.moveFocus(FocusDirection.Next)
                                true
                            }

                            else -> {
                                false
                            }
                        }
                    },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(
                        onNext = { focusManager.moveFocus(FocusDirection.Next) }),
                    shape = MaterialTheme.shapes.medium,
                )
            }

            if (!isEditMode) {
                item {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(DefaultValues.Padding.rowItemPadding)) {
                        OutlinedTextField(
                            value = state.editablePassword,
                            onValueChange = { onIntent(AddEditUserScreenIntent.UpdatePassword(it)) },
                            label = { Text(stringResource(Res.string.password)) },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password, imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    focusManager.clearFocus()
                                    localKeyboard?.hide()
                                }
                            ),
                            trailingIcon = {
                                IconButton(onClick = { showPassword = !showPassword }) {
                                    DefaultIcon(icon)
                                }
                            },
                            isError = state.editablePassword.isNotBlank() && state.editablePassword.length < 8,
                            shape = MaterialTheme.shapes.medium,
                        )

                        OutlinedIconButton({
                            scope.launch {
                                clipboardManager.setText(AnnotatedString(state.editablePassword))
                            }
                        }) {
                            Icon(Icons.Default.CopyAll, null)
                        }

                        RetryButton(showLabel = false, onClick = { onIntent(AddEditUserScreenIntent.UpdatePassword(AppStringProvider.generatePassword())) })
                    }
                }
            }

            item {
                OutlinedCard(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(DefaultValues.Padding.cardContentPadding),
                        verticalArrangement = Arrangement.spacedBy(DefaultValues.Padding.flowRowPadding)
                    ) {
                        BodyText("${stringResource(Res.string.enter_user_role)}:")
                        UserRole.entries.forEach { role ->
                            OutlinedCard(
                                modifier = Modifier
                                    .fillMaxWidth(),
                                onClick = { onIntent(AddEditUserScreenIntent.UpdateRole(role)) }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = role == state.editableRole, onClick = {
                                            onIntent(AddEditUserScreenIntent.UpdateRole(role))
                                        }, enabled = !state.isLoading
                                    )
                                    BodyText(stringResource(role.getName))
                                }
                            }
                        }
                    }
                }
            }
        }

        ButtonRowContainer {
            if (isWideScreen()) {
                BackButton(modifier = Modifier.weight(1f)) { onIntent(AddEditUserScreenIntent.GoBack) }
            }

            SaveButton(
                enabled = state.isFormValid && !state.isLoading,
                modifier = Modifier.weight(1f)
            ) { onIntent(AddEditUserScreenIntent.Save) }

            if (isEditMode && state.userId != null) {
                DeleteButton(
                    message = stringResource(
                        Res.string.delete_confirmation_message,
                        "${stringResource(Res.string.this_user)}: ${state.editableFirstName}, ${state.editableLastName} який є ${state.editableRole.name}-ом"
                    ),
                    modifier = Modifier.weight(1f)
                ) { onIntent(AddEditUserScreenIntent.DeleteUser(state.userId)) }
            }
        }
    }
}

@Preview
@Composable
private fun AddEditUserScreenContentPreview1() = PreviewDarkMaterialTheme {
    AddEditUserScreenContent(
        state = AddEditUserUiState(),
        isEditMode = false,
        onIntent = {}
    )
}

@Preview
@Composable
private fun AddEditUserScreenContentPreview2() = PreviewLightMaterialTheme {
    AddEditUserScreenContent(
        state = AddEditUserUiState(userId = "afs"),
        isEditMode = true,
        onIntent = {}
    )
}