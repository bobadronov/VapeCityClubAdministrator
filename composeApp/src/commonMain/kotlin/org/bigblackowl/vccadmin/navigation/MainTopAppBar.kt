// file: org/bigblackowl/vccadmin/MainTopAppBar.kt
package org.bigblackowl.vccadmin.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.SignalWifi4Bar
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import org.bigblackowl.vccadmin.data.entity.ThemeMode
import org.bigblackowl.vccadmin.data.entity.User
import org.bigblackowl.vccadmin.data.entity.rememberIsDarkTheme
import org.bigblackowl.vccadmin.data.repository.FakeBackend
import org.bigblackowl.vccadmin.data.utils.NetworkMonitorProvider
import org.bigblackowl.vccadmin.domain.repository.AuthRepository
import org.bigblackowl.vccadmin.domain.repository.LocalRepository
import org.bigblackowl.vccadmin.ota.OtaUiComponent
import org.bigblackowl.vccadmin.theme.DefaultValues
import org.bigblackowl.vccadmin.theme.PreviewDarkMaterialTheme
import org.bigblackowl.vccadmin.theme.PreviewLightMaterialTheme
import org.bigblackowl.vccadmin.theme.locals.LocalThemeMode
import org.bigblackowl.vccadmin.ui.login.LoginScreenIntent
import org.bigblackowl.vccadmin.ui.login.LoginScreenViewModel
import org.bigblackowl.vccadmin.uiComponent.icons.DefaultIcon
import org.bigblackowl.vccadmin.uiComponent.listItems.CurrentUserHeader
import org.bigblackowl.vccadmin.uiComponent.text.BodyText
import org.bigblackowl.vccadmin.utils.PlatformFunctionProvider
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import vccadministrator.composeapp.generated.resources.Res
import vccadministrator.composeapp.generated.resources.exit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainTopAppBar(
    snackbarHostState: SnackbarHostState,
    navigationViewModel: NavigationViewModel,
    authRepository: AuthRepository = koinInject(),
    localRepository: LocalRepository = koinInject(),
    loginScreenViewModel: LoginScreenViewModel = koinInject(),
    networkMonitorProvider: NetworkMonitorProvider = koinInject(),
) {
    var themeMode by LocalThemeMode.current
    val haptic = LocalHapticFeedback.current
    val systemDark = rememberIsDarkTheme()

    val currentUser by authRepository.currentUser.collectAsStateWithLifecycle()
    val isConnected by networkMonitorProvider.isConnected.collectAsStateWithLifecycle()

    val lastRoute = navigationViewModel.backStack.lastOrNull()
    val isLoginScreen = lastRoute is Route.Login
    val isMainScreen = lastRoute is Route.Main

    var showMenu by remember { mutableStateOf(false) }

    if (isLoginScreen) showMenu = false

    val menuItems = remember(themeMode, currentUser?.role, isLoginScreen, systemDark) {
        buildMenuItems(
            themeMode = themeMode,
            setThemeMode = {
                themeMode = it
                localRepository.setThemeState(
                    when (it) {
                        ThemeMode.AUTO -> systemDark
                        ThemeMode.DARK -> true
                        ThemeMode.LIGHT -> false
                    }
                )
                haptic.performHapticFeedback(HapticFeedbackType.Confirm)
            },
            currentUserRole = currentUser?.role,
            isLoginScreen = isLoginScreen,
            navigate = { route ->
                showMenu = false
                navigationViewModel.navigateTo(route)
                haptic.performHapticFeedback(HapticFeedbackType.Confirm)
            },
            logout = {
                showMenu = false
                loginScreenViewModel.onIntent(LoginScreenIntent.LogoutClicked)
                navigationViewModel.logout()
                haptic.performHapticFeedback(HapticFeedbackType.Confirm)
            },
        )
    }

    TopAppBar(
        title = { Text(if (isLoginScreen) "" else currentTitleFor(lastRoute)) },
        navigationIcon = {
            if (!isLoginScreen && !isMainScreen) {
                IconButton(onClick = {
                    navigationViewModel.requestBack()
                    haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                }) {
                    DefaultIcon(Icons.AutoMirrored.Filled.ArrowBack)
                }
            }
        },
        actions = {
            NetworkStatusIcon(isConnected)
            OtaUiComponent(snackbarHostState)
            Crossfade(isLoginScreen) { isLoginScreen ->

                if (isLoginScreen) {

                    val themeItem = menuItems.first()

                    OutlinedButton(
                        onClick = themeItem.onClick,
                        modifier = Modifier.padding(end = 10.dp)
                    ) {
                        DefaultIcon(themeItem.icon)
                    }

                } else {
                    IconButton(onClick = { showMenu = !showMenu }) {
                        DefaultIcon(Icons.Default.MoreVert)
                    }

                    AppMenu(
                        visible = showMenu,
                        onDismiss = { showMenu = false },
                        onUserSelected = { id ->
                            showMenu = false
                            navigationViewModel.navigateTo(Route.UserDetail(id))
                            haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                        },
                        currentRoute = lastRoute,
                        currentUser = currentUser,
                        items = menuItems
                    )
                }
            }
        }
    )
}

@Composable
private fun NetworkStatusIcon(
    isConnected: Boolean,
    hideDelayMs: Long = 1200L,
) {
    val haptic = LocalHapticFeedback.current

    var visible by remember { mutableStateOf(!isConnected) }

    LaunchedEffect(isConnected) {
        if (!isConnected) {
            // немає інтернету — показуємо кнопку
            visible = true
        } else {
            // інтернет з’явився — ще 3 сек показуємо, потім ховаємо
            visible = true
            delay(hideDelayMs)
            visible = false
        }
    }

    val color by animateColorAsState(
        if (isConnected)
            Color.Green.copy(alpha = 0.3f)
        else
            MaterialTheme.colorScheme.error
    )

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(easing = LinearEasing)),
        exit = fadeOut(tween(easing = LinearEasing)),
        modifier = Modifier.padding(end = 8.dp)
    ) {
        IconButton(onClick = {
            PlatformFunctionProvider.openNetwork()
            haptic.performHapticFeedback(HapticFeedbackType.Confirm)
        }) {
            DefaultIcon(
                image = if (isConnected) Icons.Default.SignalWifi4Bar else Icons.Default.WifiOff,
                tint = color
            )
        }
    }
}

@Composable
private fun AppMenu(
    visible: Boolean,
    onDismiss: () -> Unit,
    onUserSelected: (id: String) -> Unit,
    currentRoute: Route?,
    currentUser: User?,
    items: List<MenuItem>,
) {
    if (!visible) return

    Dialog(
        onDismissRequest = onDismiss, // Back / системні сценарії
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false, // важливо: ми робимо outside самі
            usePlatformDefaultWidth = false
        )
    ) {
        Box(Modifier.fillMaxSize()) {

            // 1) Scrim на весь екран: клік по ньому = dismiss
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) { onDismiss() }
                    .background(Color.Black.copy(.5f))
            )

            // 2) Меню (кліки всередині НЕ мають закривати через scrim)

            Column(
                modifier = Modifier.align(Alignment.TopEnd)
                    .padding(top = 60.dp, end = 20.dp)
                    .pointerInput(Unit) { detectTapGestures(onTap = {}) } // поглинути тап
                    .width(230.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                CurrentUserHeader(currentUser, onClick = onUserSelected)

                items.forEach { item ->
                    val selected = item.isSelected(currentRoute)
                    val isLogout = item.text == Res.string.exit

                    val contentColor = when {
                        selected -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                        isLogout -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.onSurface
                    }

                    Card(
                        onClick = {
                            if (!selected) item.onClick()
                        },
                        modifier = Modifier
                            .fillMaxWidth(),
                        enabled = !selected,
                        shape = RoundedCornerShape(DefaultValues.Shape.defaultShape),
                        colors = CardDefaults.cardColors(
                            containerColor = if (selected)
                                MaterialTheme.colorScheme.surfaceVariant
                            else MaterialTheme.colorScheme.surfaceBright
                        ),
                        elevation = CardDefaults.cardElevation(
                            defaultElevation = if (selected) 0.dp else 3.dp
                        ),
                        border = if (selected || isLogout) BorderStroke(
                            width = 2.dp,
                            color = MaterialTheme.colorScheme.outline
                        ) else null
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            DefaultIcon(image = item.icon, tint = contentColor)

                            BodyText(
                                text = stringResource(item.text),
                                color = contentColor,
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
private fun AppMenuPreview1() = PreviewDarkMaterialTheme(
    topBar = {
        TopAppBar(
            title = {},
            actions = {
                AppMenu(
                    visible = true,
                    onDismiss = {},
                    onUserSelected = {},
                    currentRoute = Route.FileGenerator,
                    currentUser = FakeBackend.users.first(),
                    items = buildMenuItems(
                        themeMode = ThemeMode.DARK,
                        setThemeMode = { },
                        currentUserRole = FakeBackend.users.first().role,
                        isLoginScreen = false,
                        navigate = {},
                        logout = {},
                    )
                )
            }
        )
    }
) {}

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
private fun AppMenuPreview2() = PreviewLightMaterialTheme(
    topBar = {
        TopAppBar(
            title = {},
            actions = {
                AppMenu(
                    visible = true,
                    onDismiss = {},
                    onUserSelected = {},
                    currentRoute = Route.FileGenerator,
                    currentUser = FakeBackend.users.first(),
                    items = buildMenuItems(
                        themeMode = ThemeMode.LIGHT,
                        setThemeMode = { },
                        currentUserRole = FakeBackend.users.first().role,
                        isLoginScreen = false,
                        navigate = {},
                        logout = {},
                    )
                )
            }
        )
    }
) {}