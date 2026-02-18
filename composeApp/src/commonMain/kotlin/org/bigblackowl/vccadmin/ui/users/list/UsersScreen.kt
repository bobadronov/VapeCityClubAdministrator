package org.bigblackowl.vccadmin.ui.users.list

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.bigblackowl.vccadmin.data.entity.User
import org.bigblackowl.vccadmin.data.entity.UserRole
import org.bigblackowl.vccadmin.data.repository.FakeBackend
import org.bigblackowl.vccadmin.data.state.UIEvents
import org.bigblackowl.vccadmin.navigation.NavigationViewModel
import org.bigblackowl.vccadmin.navigation.Route
import org.bigblackowl.vccadmin.resourses.DefaultValues
import org.bigblackowl.vccadmin.theme.PreviewDarkMaterialTheme
import org.bigblackowl.vccadmin.theme.PreviewLightMaterialTheme
import org.bigblackowl.vccadmin.uiComponent.loading.LoadingComponent
import org.bigblackowl.vccadmin.uiComponent.buttons.AddButton
import org.bigblackowl.vccadmin.uiComponent.buttons.BackButton
import org.bigblackowl.vccadmin.uiComponent.buttons.EditButton
import org.bigblackowl.vccadmin.uiComponent.container.AdaptiveBox
import org.bigblackowl.vccadmin.uiComponent.container.ButtonRowContainer
import org.bigblackowl.vccadmin.uiComponent.container.PlatformPullToRefreshBox
import org.bigblackowl.vccadmin.uiComponent.listItems.DefaultScrollbar
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
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val uiEvent by viewModel.uiEvent.collectAsStateWithLifecycle(null)

    LaunchedEffect(Unit) {
        viewModel.onIntent(UsersScreenIntent.Load)
    }

    LaunchedEffect(uiEvent) {
        uiEvent.let { event ->
            when (event) {
                is UIEvents.ShowMessage -> snackbarHostState.showSnackbar(event.message)
                else -> {}
            }
        }
    }

    if (state.isLoading) {
        LoadingComponent()
    } else {
        UsersScreenContent(
            uiState = state,
            addNewUser = {
                navigationViewModel.navigateTo(Route.AddEditUser(null))
            },
            onSelect = { id ->
                navigationViewModel.navigateTo(Route.UserDetail(id))
            },
            onEdit = { id ->
                navigationViewModel.navigateTo(Route.AddEditUser(id))
            },
            onRefresh = {
                viewModel.onIntent(UsersScreenIntent.Refresh)
            },
            onBack = {
                navigationViewModel.popBackStack()
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UsersScreenContent(
    uiState: UsersScreenUiState,
    addNewUser: () -> Unit,
    onSelect: (String) -> Unit,
    onEdit: (String) -> Unit,
    onRefresh: () -> Unit,
    onBack: () -> Unit,
) {
    val listState = rememberLazyListState()

    Column(
        modifier = Modifier.padding(DefaultValues.Padding.mainBoxPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(DefaultValues.Padding.verticalListItemPadding)
    ) {
        PlatformPullToRefreshBox(
            isRefreshing = uiState.isRefreshing,
            onRefresh = onRefresh,
            modifier = Modifier.weight(1f),
        ) {
            if (uiState.userList.isEmpty()) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Text(stringResource(Res.string.users_not_found))
                }
            } else {
                Row {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        state = listState,
                        verticalArrangement = Arrangement.spacedBy(DefaultValues.Padding.verticalListItemPadding),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        items(uiState.userList, key = { it.id }) { user ->
                            UserCard(
                                user = user,
                                isCurrentUser = uiState.currentUser?.id == user.id,
                                onEdit = onEdit,
                                onClick = { onSelect(user.id) }
                            )
                        }
                    }

                    DefaultScrollbar(scrollState = listState)
                }
            }
        }
        ButtonRowContainer {
            if (isWideScreen()) {
                BackButton(modifier = Modifier.weight(1f)) { onBack() }
            }

            AddButton(modifier = Modifier.weight(1f)) {
                addNewUser()
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
    val color = when (user.role) {
        UserRole.ADMIN -> Color.Blue
        UserRole.USER -> Color.Green
    }

    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        colors = CardDefaults.outlinedCardColors(
            containerColor = color.copy(alpha = 0.07f),
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
        border = BorderStroke(2.dp, color.copy(alpha = 0.3f))
    ) {
        AdaptiveBox(
            onWide = {
                Row(
                    modifier = Modifier.padding(DefaultValues.Padding.cardContentPadding).fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TitleText("${user.firstName} ${user.lastName}")
                        BodyText("${stringResource(user.role.getName)} | ${user.email}")
                    }
                    if (!isCurrentUser) {
                        EditButton { onEdit(user.id) }
                    }
                }
            },
            onNarrow = {
                Column(
                    modifier = Modifier.padding(DefaultValues.Padding.cardContentPadding),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TitleText("${user.firstName} ${user.lastName}")
                    BodyText("${stringResource(user.role.getName)} | ${user.email}")
                    if (!isCurrentUser) {
                        EditButton(modifier = Modifier.weight(1f), showLabel = true) { onEdit(user.id) }
                    }
                }
            },
        )
    }
}

@Preview
@Composable
fun UsersScreenContentPreview1() = PreviewDarkMaterialTheme {
    UsersScreenContent(
        uiState = UsersScreenUiState(userList = FakeBackend.users),
        addNewUser = {},
        onSelect = {},
        onEdit = {},
        onRefresh = {},
        onBack = {}
    )
}

@Preview(
    widthDp = 600,
)
@Composable
fun UsersScreenContentPreview2() = PreviewLightMaterialTheme {
    UsersScreenContent(
        uiState = UsersScreenUiState(userList = FakeBackend.users),
        addNewUser = {},
        onSelect = {},
        onEdit = {},
        onRefresh = {},
        onBack = {}
    )
}