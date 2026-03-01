// LoginScreen.kt
package org.bigblackowl.vccadmin.ui.login

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.autofill.contentType
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import org.bigblackowl.vccadmin.BuildConfig
import org.bigblackowl.vccadmin.data.entity.rememberIsDarkTheme
import org.bigblackowl.vccadmin.theme.DefaultValues
import org.bigblackowl.vccadmin.theme.PreviewDarkMaterialTheme
import org.bigblackowl.vccadmin.theme.PreviewLightMaterialTheme
import org.bigblackowl.vccadmin.uiComponent.checkbox.DefaultCheckbox
import org.bigblackowl.vccadmin.uiComponent.icons.DefaultIcon
import org.bigblackowl.vccadmin.uiComponent.text.BodyText
import org.bigblackowl.vccadmin.uiComponent.text.HelperText
import org.bigblackowl.vccadmin.uiComponent.text.TitleText
import org.bigblackowl.vccadmin.utils.PlatformFunctionProvider
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import vccadministrator.composeapp.generated.resources.Res
import vccadministrator.composeapp.generated.resources.email
import vccadministrator.composeapp.generated.resources.enable_auto_login
import vccadministrator.composeapp.generated.resources.invalid_email
import vccadministrator.composeapp.generated.resources.login_button
import vccadministrator.composeapp.generated.resources.main_logo
import vccadministrator.composeapp.generated.resources.main_logo_white_theme
import vccadministrator.composeapp.generated.resources.min_8_symbols
import vccadministrator.composeapp.generated.resources.no_internet
import vccadministrator.composeapp.generated.resources.open_settings
import vccadministrator.composeapp.generated.resources.password


@Composable
fun LoginScreen(
    snackbarHostState: SnackbarHostState,
    viewModel: LoginScreenViewModel = koinInject(),
) {
    val email by viewModel.uiState.map { it.email }.distinctUntilChanged().collectAsStateWithLifecycle(initialValue = "")

    val password by viewModel.uiState.map { it.password }.distinctUntilChanged().collectAsStateWithLifecycle(initialValue = "")

    val isEmailError by viewModel.uiState.map { it.isEmailError }.distinctUntilChanged().collectAsStateWithLifecycle(initialValue = false)

    val isPasswordError by viewModel.uiState.map { it.isPasswordError }.distinctUntilChanged().collectAsStateWithLifecycle(initialValue = false)

    val isPasswordVisible by viewModel.uiState.map { it.isPasswordVisible }.distinctUntilChanged().collectAsStateWithLifecycle(initialValue = false)

    val canLogin by viewModel.uiState.map { it.canLogin }.distinctUntilChanged().collectAsStateWithLifecycle(initialValue = false)

    val isLoading by viewModel.uiState.map { it.isLoading }.distinctUntilChanged().collectAsStateWithLifecycle(initialValue = false)

    val autoLoginState by viewModel.uiState.map { it.autoLoginState }.distinctUntilChanged().collectAsStateWithLifecycle(initialValue = false)

    val networkState by viewModel.uiState.map { it.networkState }.distinctUntilChanged().collectAsStateWithLifecycle(initialValue = true)

    val errorMessage by viewModel.uiState.map { it.errorMessage }.distinctUntilChanged().collectAsStateWithLifecycle(initialValue = null)

    LaunchedEffect(errorMessage) {
        errorMessage?.let { snackbarHostState.showSnackbar(it) }
    }

    LoginScreenContent(
        networkState = networkState,
        email = email,
        password = password,
        isEmailError = isEmailError,
        isPasswordError = isPasswordError,
        isPasswordVisible = isPasswordVisible,
        autoLoginState = autoLoginState,
        canLogin = canLogin,
        isLoading = isLoading,
        onEmailChanged = { viewModel.onIntent(LoginScreenIntent.EmailChanged(it)) },
        onPasswordChanged = { viewModel.onIntent(LoginScreenIntent.PasswordChanged(it)) },
        onTogglePasswordVisibility = { viewModel.onIntent(LoginScreenIntent.TogglePasswordVisibility) },
        onAutoLoginChanged = { viewModel.onIntent(LoginScreenIntent.AutoLoginChanged(it)) },
        onLogin = { viewModel.onIntent(LoginScreenIntent.LoginClicked) },
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun LoginScreenContent(
    networkState: Boolean,
    email: String,
    password: String,
    isEmailError: Boolean,
    isPasswordError: Boolean,
    isPasswordVisible: Boolean,
    autoLoginState: Boolean,
    canLogin: Boolean,
    isLoading: Boolean,
    onEmailChanged: (String) -> Unit,
    onPasswordChanged: (String) -> Unit,
    onTogglePasswordVisibility: () -> Unit,
    onAutoLoginChanged: (Boolean) -> Unit,
    onLogin: () -> Unit,
) {
    val haptic = LocalHapticFeedback.current

    val loginButtonWidth by animateDpAsState(
        targetValue = if (isLoading) 80.dp else 200.dp, animationSpec = tween(
            durationMillis = 400, easing = FastOutSlowInEasing
        ), label = "login_button_width"
    )
    val isDarkTheme = rememberIsDarkTheme()
    val darkLogo = painterResource(Res.drawable.main_logo)
    val lightLogo = painterResource(Res.drawable.main_logo_white_theme)
    val logo = remember(isDarkTheme) {
        if (isDarkTheme) darkLogo else lightLogo
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter,
    ) {
        Icon(
            painter = logo,
            contentDescription = null,
            modifier = Modifier.alpha(if (isDarkTheme) .05f else .2f).padding(DefaultValues.Padding.mainBoxPadding).fillMaxSize()
        )
    }

    Crossfade(networkState) { state ->
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(DefaultValues.Padding.mainBoxPadding).imePadding().navigationBarsPadding(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            if (state) {
                TitleText(
                    text = BuildConfig.APP_NAME, textAlign = TextAlign.Center, letterSpacing = 2.5.sp
                )

                Spacer(Modifier.height(48.dp))

                EmailField(
                    value = email, onValueChange = onEmailChanged, isError = isEmailError, enabled = !isLoading, modifier = Modifier.width(270.dp)
                )

                Spacer(Modifier.height(20.dp))

                PasswordField(
                    value = password,
                    onValueChange = onPasswordChanged,
                    isError = isPasswordError,
                    isVisible = isPasswordVisible,
                    onVisibilityToggle = onTogglePasswordVisibility,
                    enabled = !isLoading,
                    onDone = { if (!isLoading && canLogin) onLogin() },
                    modifier = Modifier.width(270.dp)
                )

                Spacer(Modifier.height(15.dp))

                AnimatedVisibility(visible = canLogin) {

                    Row(verticalAlignment = Alignment.CenterVertically) {

                        DefaultCheckbox(
                            checked = autoLoginState,
                            onCheckedChange = {
                                onAutoLoginChanged(it)
                                haptic.performHapticFeedback(HapticFeedbackType.SegmentFrequentTick)
                            },
                            enabled = !isLoading,
                        )

                        BodyText(stringResource(Res.string.enable_auto_login))

                    }
                }

                Spacer(Modifier.height(48.dp))

                OutlinedButton(
                    onClick = {
                        onLogin()
                        haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                    },
                    modifier = Modifier.size(height = 50.dp, width = loginButtonWidth),
                    enabled = !isLoading && canLogin,
                    colors = ButtonDefaults.outlinedButtonColors(),
                    border = BorderStroke(1.2.dp, MaterialTheme.colorScheme.onSurface)
                ) {
                    Crossfade(
                        targetState = isLoading, animationSpec = tween(300), label = "login_button_content"
                    ) { loading ->
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            if (loading) {
                                LoadingIndicator(
                                    modifier = Modifier.fillMaxSize(),
                                )
                            } else {
                                BodyText(stringResource(Res.string.login_button))
                            }
                        }
                    }
                }
            } else {
                TitleText(stringResource(Res.string.no_internet))

                Spacer(Modifier.height(15.dp))

                Button(
                    onClick = {
                        PlatformFunctionProvider.openNetwork()
                        haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                    },
                ) {
                    DefaultIcon(image = Icons.Default.Settings, tint = MaterialTheme.colorScheme.onPrimary)
                    Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                    BodyText(stringResource(Res.string.open_settings))
                }
            }
        }
    }
}

@Composable
private fun EmailField(
    value: String, onValueChange: (String) -> Unit, isError: Boolean, enabled: Boolean, modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.contentType(ContentType.EmailAddress),
        label = { Text(stringResource(Res.string.email)) },
        singleLine = true,
        enabled = enabled,
        isError = isError,
        supportingText = if (isError) {
            { HelperText(stringResource(Res.string.invalid_email)) }
        } else null,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Email, imeAction = ImeAction.Next
        ),
        shape = RoundedCornerShape(DefaultValues.Shape.defaultShape),
    )
}

@Composable
private fun PasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    isError: Boolean,
    isVisible: Boolean,
    onVisibilityToggle: () -> Unit,
    enabled: Boolean,
    onDone: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.contentType(ContentType.Password),
        label = { Text(stringResource(Res.string.password)) },
        singleLine = true,
        enabled = enabled,
        isError = isError,
        supportingText = if (isError) {
            { HelperText(stringResource(Res.string.min_8_symbols)) }
        } else null,
        visualTransformation = if (isVisible) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Password, imeAction = ImeAction.Done
        ),
        keyboardActions = KeyboardActions(onDone = { onDone() }),
        trailingIcon = {
            IconButton(
                onClick = onVisibilityToggle, enabled = enabled
            ) {
                DefaultIcon(if (isVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff)
            }
        },
        shape = RoundedCornerShape(DefaultValues.Shape.defaultShape),
    )
}


@Preview
@Composable
private fun LoginScreenPreview1() = PreviewDarkMaterialTheme {
    LoginScreenContent(
        networkState = true,
        email = "admin@example.com",
        password = "12345678",
        isEmailError = false,
        isPasswordError = false,
        isPasswordVisible = true,
        autoLoginState = true,
        canLogin = true,
        isLoading = false,
        onEmailChanged = {},
        onPasswordChanged = {},
        onTogglePasswordVisibility = {},
        onAutoLoginChanged = {},
        onLogin = {},
    )
}

@Preview
@Composable
private fun LoginScreenPreview2() = PreviewLightMaterialTheme {
    LoginScreenContent(
        networkState = false,
        email = "admin@example.com",
        password = "12345678",
        isEmailError = false,
        isPasswordError = false,
        isPasswordVisible = true,
        autoLoginState = true,
        canLogin = true,
        isLoading = false,
        onEmailChanged = {},
        onPasswordChanged = {},
        onTogglePasswordVisibility = {},
        onAutoLoginChanged = {},
        onLogin = {},
    )
}

@Preview(device = Devices.DESKTOP)
@Composable
private fun LoginScreenPreview11() = PreviewDarkMaterialTheme {
    LoginScreenContent(
        networkState = true,
        email = "admin@example.com",
        password = "12345678",
        isEmailError = false,
        isPasswordError = false,
        isPasswordVisible = true,
        autoLoginState = true,
        canLogin = true,
        isLoading = true,
        onEmailChanged = {},
        onPasswordChanged = {},
        onTogglePasswordVisibility = {},
        onAutoLoginChanged = {},
        onLogin = {},
    )
}

@Preview(device = Devices.DESKTOP)
@Composable
private fun LoginScreenPreview21() = PreviewLightMaterialTheme {
    LoginScreenContent(
        networkState = true,
        email = "admin@example.com",
        password = "12345678",
        isEmailError = false,
        isPasswordError = false,
        isPasswordVisible = false,
        autoLoginState = false,
        canLogin = true,
        isLoading = false,
        onEmailChanged = {},
        onPasswordChanged = {},
        onTogglePasswordVisibility = {},
        onAutoLoginChanged = {},
        onLogin = {},
    )
}