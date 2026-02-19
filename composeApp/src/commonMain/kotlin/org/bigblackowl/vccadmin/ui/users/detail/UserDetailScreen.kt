package org.bigblackowl.vccadmin.ui.users.detail

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.bigblackowl.vccadmin.data.repository.FakeBackend
import org.bigblackowl.vccadmin.data.state.UIEvents
import org.bigblackowl.vccadmin.navigation.NavigationViewModel
import org.bigblackowl.vccadmin.resourses.DefaultValues
import org.bigblackowl.vccadmin.theme.PreviewDarkMaterialTheme
import org.bigblackowl.vccadmin.theme.PreviewLightMaterialTheme
import org.bigblackowl.vccadmin.uiComponent.buttons.BackButton
import org.bigblackowl.vccadmin.uiComponent.buttons.QRCodeButton
import org.bigblackowl.vccadmin.uiComponent.container.ButtonRowContainer
import org.bigblackowl.vccadmin.uiComponent.container.PlatformPullToRefreshBox
import org.bigblackowl.vccadmin.uiComponent.dialog.QRCodeDialog
import org.bigblackowl.vccadmin.uiComponent.icons.DefaultIcon
import org.bigblackowl.vccadmin.uiComponent.listItems.DefaultScrollbar
import org.bigblackowl.vccadmin.uiComponent.loading.LoadingComponent
import org.bigblackowl.vccadmin.uiComponent.text.BodyText
import org.bigblackowl.vccadmin.uiComponent.text.HelperText
import org.bigblackowl.vccadmin.uiComponent.text.TitleText
import org.bigblackowl.vccadmin.utils.UkrainianPhoneVisualTransformation
import org.bigblackowl.vccadmin.utils.isWideScreen
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import vccadministrator.composeapp.generated.resources.Res
import vccadministrator.composeapp.generated.resources.admin
import vccadministrator.composeapp.generated.resources.copied_to_clipboard
import vccadministrator.composeapp.generated.resources.created
import vccadministrator.composeapp.generated.resources.email
import vccadministrator.composeapp.generated.resources.last_modified
import vccadministrator.composeapp.generated.resources.last_modified_by_user
import vccadministrator.composeapp.generated.resources.phone_number
import vccadministrator.composeapp.generated.resources.user_role

@Composable
fun UserDetailScreen(
    userId: String?,
    snackbarHostState: SnackbarHostState,
    navigationViewModel: NavigationViewModel,
    userDetailScreenViewModel: UserDetailScreenViewModel = koinInject(),
) {

    val state by userDetailScreenViewModel.state.collectAsState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(userId) {
        userDetailScreenViewModel.onIntent(UserDetailScreenIntent.Load(userId.orEmpty()))
        launch {
            userDetailScreenViewModel.uiEvent.collect { event ->
                when (event) {
                    is UIEvents.ShowMessage -> snackbarHostState.showSnackbar(event.message)
                    is UIEvents.NavigateBack -> navigationViewModel.popBackStack()
                    else -> {}
                }
            }
        }
    }

    if (state.isLoading) {
        LoadingComponent()
    } else {
        UserDetailContent(
            state = state,
            onBack = { navigationViewModel.requestBack() },
            onRefresh = { userDetailScreenViewModel.onIntent(UserDetailScreenIntent.Refresh(userId.orEmpty())) },
            showConfirmMessage = {
                scope.launch {
                    snackbarHostState.showSnackbar(getString(Res.string.copied_to_clipboard))
                }
            },
        )
    }
}

@Suppress("AssignedValueIsNeverRead")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UserDetailContent(
    state: UserDetailState?,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    showConfirmMessage: () -> Unit,
) {
    if (state?.id == null) return
    var showQRCodeDialog by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    Column(
        modifier = Modifier.fillMaxSize().padding(DefaultValues.Padding.mainBoxPadding),
        verticalArrangement = Arrangement.spacedBy(DefaultValues.Padding.verticalListItemPadding)
    ) {
        TitleText(
            text = buildString {
                append(state.firstName, " ")
                append(state.lastName)
                if (isBlank()) append(stringResource(Res.string.admin))
            }.trim(),
            fontWeight = FontWeight.Bold
        )
        PlatformPullToRefreshBox(
            isRefreshing = state.isRefreshing,
            onRefresh = onRefresh,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .padding(16.dp),
                ) {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f),
                        state = listState,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        item {
                            InfoRow(
                                icon = Icons.Default.Person,
                                label = stringResource(Res.string.user_role),
                                value = stringResource(state.role.getName),
                            )
                        }

                        item {
                            InfoRow(
                                icon = Icons.Default.Email,
                                label = stringResource(Res.string.email),
                                value = state.email,
                            )
                        }

                        item {
                            InfoRow(
                                icon = Icons.Default.Phone,
                                label = stringResource(Res.string.phone_number),
                                value = state.phone.let { UkrainianPhoneVisualTransformation().filter(AnnotatedString(it)) }.text.text,
                            )
                        }

                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                            ) {
                                QRCodeButton(modifier = Modifier.fillMaxWidth(.7f), onClick = { showQRCodeDialog = true })
                            }
                        }

                        item {
                            InfoRow(
                                icon = Icons.Default.CalendarToday,
                                label = stringResource(Res.string.created),
                                value = state.createdAt,
                            )
                        }

                        item {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                DefaultIcon(Icons.Default.Edit)
                                Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                                Column {
                                    HelperText(stringResource(Res.string.last_modified, state.lastModified))
                                    HelperText(stringResource(Res.string.last_modified_by_user, state.lastModifiedByUser))
                                }
                            }
                        }
                    }

                    DefaultScrollbar(scrollState = listState)

                }
            }
        }

        if (isWideScreen()) {
            ButtonRowContainer {
                BackButton { onBack() }
            }
        }
    }

    if (state.phone.isNotBlank()) {
        AnimatedVisibility(showQRCodeDialog) {
            QRCodeDialog(
                data = state.phone.let { UkrainianPhoneVisualTransformation().filter(AnnotatedString(it)) }.text.text,
                showConfirmMessage = showConfirmMessage,
                onDismiss = { showQRCodeDialog = false },
            )
        }
    }
}

@Composable
private fun InfoRow(
    icon: ImageVector,
    label: String,
    value: String,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
    ) {
        DefaultIcon(icon, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(ButtonDefaults.IconSpacing))
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

@Preview
@Composable
private fun UserDetailContentPreview1() = PreviewLightMaterialTheme {
    val user = FakeBackend.singleUser

    UserDetailContent(
        state = UserDetailState(
            isLoading = false,
            isRefreshing = false,
            id = user.id,
            firstName = user.firstName,
            lastName = user.lastName,
            email = user.email,
            phone = user.phone,
            role = user.role,
            createdAt = "user.createdAt",
            lastModified = " user.",
            lastModifiedByUser = "user."
        ),
        onBack = {},
        onRefresh = { },
        showConfirmMessage = {}
    )
}

@Preview
@Composable
private fun UserDetailContentPreview2() = PreviewDarkMaterialTheme {
    val user = FakeBackend.singleUser
    UserDetailContent(
        state = UserDetailState(
            isLoading = false,
            isRefreshing = false,
            id = user.id,
            firstName = user.firstName,
            lastName = user.lastName,
            email = user.email,
            phone = user.phone,
            role = user.role,
            createdAt = "user.createdAt",
            lastModified = " user.",
            lastModifiedByUser = "user."
        ),
        onBack = {},
        onRefresh = { },
        showConfirmMessage = {}
    )
}