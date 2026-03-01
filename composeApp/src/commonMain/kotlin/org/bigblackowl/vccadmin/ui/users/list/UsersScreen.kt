package org.bigblackowl.vccadmin.ui.users.list

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import org.bigblackowl.vccadmin.data.entity.User
import org.bigblackowl.vccadmin.data.events.UIEvents
import org.bigblackowl.vccadmin.data.repository.FakeBackend
import org.bigblackowl.vccadmin.navigation.NavigationViewModel
import org.bigblackowl.vccadmin.navigation.Route
import org.bigblackowl.vccadmin.theme.DefaultValues
import org.bigblackowl.vccadmin.theme.PreviewDarkMaterialTheme
import org.bigblackowl.vccadmin.theme.PreviewLightMaterialTheme
import org.bigblackowl.vccadmin.uiComponent.buttons.AddButton
import org.bigblackowl.vccadmin.uiComponent.buttons.BackButton
import org.bigblackowl.vccadmin.uiComponent.buttons.EditButton
import org.bigblackowl.vccadmin.uiComponent.container.ButtonRowContainer
import org.bigblackowl.vccadmin.uiComponent.container.PlatformPullToRefreshBox
import org.bigblackowl.vccadmin.uiComponent.indicators.LoadingComponent
import org.bigblackowl.vccadmin.uiComponent.listItems.DefaultVerticalScrollbar
import org.bigblackowl.vccadmin.uiComponent.text.BodyText
import org.bigblackowl.vccadmin.uiComponent.text.TitleText
import org.bigblackowl.vccadmin.utils.isWideScreen
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import vccadministrator.composeapp.generated.resources.Res
import vccadministrator.composeapp.generated.resources.users_not_found

@Composable
fun UsersScreen(
    snackbarHostState: SnackbarHostState,
    navigationViewModel: NavigationViewModel,
    viewModel: UsersScreenViewModel = koinInject(),
) {
    LaunchedEffect(Unit) {
        viewModel.onIntent(UsersScreenIntent.Load)
    }

    // ✅ UI events: один collector, без collectAsState
    LaunchedEffect(Unit) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                is UIEvents.ShowMessage -> snackbarHostState.showSnackbar(event.message)
                else -> Unit
            }
        }
    }

    // ✅ State slices
    val isLoading by viewModel.uiState
        .map { it.isLoading }
        .distinctUntilChanged()
        .collectAsStateWithLifecycle(initialValue = true)

    val isRefreshing by viewModel.uiState
        .map { it.isRefreshing }
        .distinctUntilChanged()
        .collectAsStateWithLifecycle(initialValue = false)

    val userList by viewModel.uiState
        .map { it.userList }
        .distinctUntilChanged()
        .collectAsStateWithLifecycle(initialValue = emptyList())

    val currentUserId by viewModel.uiState
        .map { it.currentUser?.id }
        .distinctUntilChanged()
        .collectAsStateWithLifecycle(initialValue = null)

    if (isLoading) {
        LoadingComponent()
    } else {
        UsersScreenContent(
            isRefreshing = isRefreshing,
            userList = userList,
            currentUserId = currentUserId,
            addNewUser = { navigationViewModel.navigateTo(Route.AddEditUser(null)) },
            onSelect = { id -> navigationViewModel.navigateTo(Route.UserDetail(id)) },
            onEdit = { id -> navigationViewModel.navigateTo(Route.AddEditUser(id)) },
            onRefresh = { viewModel.onIntent(UsersScreenIntent.Refresh) },
            onBack = { navigationViewModel.requestBack() },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UsersScreenContent(
    isRefreshing: Boolean,
    userList: List<User>,         // заміни User на твій тип
    currentUserId: String?,
    addNewUser: () -> Unit,
    onSelect: (String) -> Unit,
    onEdit: (String) -> Unit,
    onRefresh: () -> Unit,
    onBack: () -> Unit,
) {
    val listState = rememberLazyGridState()

    val haptic = LocalHapticFeedback.current

    Column(
        modifier = Modifier.padding(DefaultValues.Padding.mainBoxPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(DefaultValues.Padding.verticalListItemPadding)
    ) {
        PlatformPullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = onRefresh,
            modifier = Modifier.weight(1f),
        ) {
            if (userList.isEmpty()) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Text(stringResource(Res.string.users_not_found))
                }
            } else {
                Row {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(DefaultValues.Size.gridItemMinSize),
                        modifier = Modifier.weight(1f),
                        state = listState,
                        verticalArrangement = Arrangement.spacedBy(DefaultValues.Padding.lazyVerticalGridContentPadding),
                        horizontalArrangement = Arrangement.spacedBy(DefaultValues.Padding.lazyVerticalGridContentPadding),
                    ) {
                        items(userList, key = { it.id }) { user ->
                            UserCard(
                                user = user,
                                isCurrentUser = (currentUserId == user.id),
                                onEdit = onEdit,
                                onClick = {
                                    onSelect(user.id)
                                    haptic.performHapticFeedback(HapticFeedbackType.Confirm)

                                }
                            )
                        }
                    }
                    DefaultVerticalScrollbar(scrollState = listState)
                }
            }
        }

        ButtonRowContainer {
            if (isWideScreen()) {
                BackButton(modifier = Modifier.weight(1f)) {
                    onBack()
                    haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                }
            }
            AddButton(modifier = Modifier.weight(1f)) {
                addNewUser()
                haptic.performHapticFeedback(HapticFeedbackType.Confirm)

            }
        }
    }
}

@Composable
private fun UserCard(
    user: User,
    isCurrentUser: Boolean,
    onClick: () -> Unit,
    onEdit: (String) -> Unit,
) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        colors = CardDefaults.outlinedCardColors(
            containerColor = user.role.color.copy(alpha = .07f),
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
        border = BorderStroke(2.dp, user.role.color.copy(alpha = .3f))
    ) {

        Column(
            modifier = Modifier.padding(DefaultValues.Padding.cardContentPadding),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TitleText(user.fullName)
            BodyText(
                text = "${stringResource(user.role.getName)} | ${user.email}",
                modifier = Modifier.basicMarquee()
            )
            if (!isCurrentUser) {
                EditButton(
                    modifier = Modifier.fillMaxWidth(),
                    showLabel = true
                ) { onEdit(user.id) }
            }
        }
    }
}


@Preview
@Composable
fun UsersScreenContentPreview1() = PreviewDarkMaterialTheme {
    UsersScreenContent(
        isRefreshing = false,
        userList = FakeBackend.users,
        currentUserId = FakeBackend.singleUser.id,
        addNewUser = {},
        onSelect = {},
        onEdit = {},
        onRefresh = {},
        onBack = {}
    )
}

@Preview
@Composable
fun UsersScreenContentPreview2() = PreviewLightMaterialTheme {
    UsersScreenContent(
        isRefreshing = false,
        userList = FakeBackend.users,
        currentUserId = null,
        addNewUser = {},
        onSelect = {},
        onEdit = {},
        onRefresh = {},
        onBack = {}
    )
}

@Preview(device = Devices.DESKTOP)
@Composable
fun UsersScreenContentPreview1PC() = PreviewDarkMaterialTheme {
    UsersScreenContent(
        isRefreshing = false,
        userList = FakeBackend.users,
        currentUserId = FakeBackend.singleUser.id,
        addNewUser = {},
        onSelect = {},
        onEdit = {},
        onRefresh = {},
        onBack = {}
    )
}