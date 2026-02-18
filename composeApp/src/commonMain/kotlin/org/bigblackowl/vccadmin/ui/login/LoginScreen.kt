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
import androidx.compose.material3.Checkbox
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
import org.bigblackowl.vccadmin.BuildConfig
import org.bigblackowl.vccadmin.resourses.DefaultValues
import org.bigblackowl.vccadmin.theme.PreviewDarkMaterialTheme
import org.bigblackowl.vccadmin.theme.PreviewLightMaterialTheme
import org.bigblackowl.vccadmin.theme.rememberIsDarkTheme
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
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { snackbarHostState.showSnackbar(it) }
    }

    LoginScreenContent(
        uiState = uiState,
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
    uiState: LoginUiState,
    onEmailChanged: (String) -> Unit,
    onPasswordChanged: (String) -> Unit,
    onTogglePasswordVisibility: () -> Unit,
    onAutoLoginChanged: (Boolean) -> Unit,
    onLogin: () -> Unit,
) {
    val loginButtonWidth by animateDpAsState(
        targetValue = if (uiState.isLoading) 80.dp else 200.dp,
        animationSpec = tween(
            durationMillis = 400,
            easing = FastOutSlowInEasing
        ),
        label = "login_button_width"
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
            modifier = Modifier
                .alpha(if (isDarkTheme) .05f else .2f)
                .padding(DefaultValues.Padding.mainBoxPadding)
                .fillMaxSize()
        )
    }

    Crossfade(uiState.networkState) { state ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(DefaultValues.Padding.mainBoxPadding)
                .imePadding()
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            if (state) {
                TitleText(
                    text = BuildConfig.APP_NAME,
                    textAlign = TextAlign.Center,
                    letterSpacing = 2.5.sp
                )

                Spacer(Modifier.height(48.dp))

                EmailField(
                    value = uiState.email,
                    onValueChange = onEmailChanged,
                    isError = uiState.isEmailError,
                    enabled = !uiState.isLoading,
                    modifier = Modifier.width(270.dp)
                )

                Spacer(Modifier.height(20.dp))

                PasswordField(
                    value = uiState.password,
                    onValueChange = onPasswordChanged,
                    isError = uiState.isPasswordError,
                    isVisible = uiState.isPasswordVisible,
                    onVisibilityToggle = onTogglePasswordVisibility,
                    enabled = !uiState.isLoading,
                    onDone = { if (!uiState.isLoading && uiState.canLogin) onLogin() },
                    modifier = Modifier.width(270.dp)
                )

                Spacer(Modifier.height(15.dp))

                AnimatedVisibility(visible = uiState.canLogin) {

                    Row(verticalAlignment = Alignment.CenterVertically) {

                        Checkbox(
                            checked = uiState.autoLoginState,
                            onCheckedChange = onAutoLoginChanged,
                            enabled = !uiState.isLoading,
                        )

                        BodyText(stringResource(Res.string.enable_auto_login))

                    }
                }

                Spacer(Modifier.height(48.dp))

                OutlinedButton(
                    onClick = onLogin,
                    modifier = Modifier.size(height = 50.dp, width = loginButtonWidth),
                    enabled = !uiState.isLoading && uiState.canLogin,
                    colors = ButtonDefaults.outlinedButtonColors(),
                    border = BorderStroke(1.2.dp, MaterialTheme.colorScheme.onSurface)
                ) {
                    Crossfade(
                        targetState = uiState.isLoading,
                        animationSpec = tween(300),
                        label = "login_button_content"
                    ) { loading ->
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            if (loading) {
                                LoadingIndicator(
                                    modifier = Modifier
                                        .fillMaxSize(),
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
                    onClick = { PlatformFunctionProvider.openNetwork() },
                ) {
                    DefaultIcon(icon = Icons.Default.Settings, tint = MaterialTheme.colorScheme.onPrimary)
                    Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                    BodyText(stringResource(Res.string.open_settings))
                }
            }
        }
    }
}

@Composable
private fun EmailField(
    value: String,
    onValueChange: (String) -> Unit,
    isError: Boolean,
    enabled: Boolean,
    modifier: Modifier = Modifier
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
fun LoginScreenPreview1() = PreviewDarkMaterialTheme {
    LoginScreenContent(
        uiState = LoginUiState(
            email = "admin@example.com",
            password = "12345678",
            isPasswordVisible = true,
            autoLoginState = true,
            networkState = true,
//                    isLoading = true,
        ),
        onEmailChanged = {},
        onPasswordChanged = {},
        onTogglePasswordVisibility = {},
        onAutoLoginChanged = {},
        onLogin = {},
    )
}

@Preview
@Composable
fun LoginScreenPreview2() = PreviewLightMaterialTheme {
    LoginScreenContent(
        uiState = LoginUiState(
            email = "admin@example.com",
            password = "12345678",
            isPasswordVisible = true,
            autoLoginState = true,
            networkState = false,
//                    isLoading = true,
        ),
        onEmailChanged = {},
        onPasswordChanged = {},
        onTogglePasswordVisibility = {},
        onAutoLoginChanged = {},
        onLogin = {},
    )
}

@Preview(
    device = Devices.DESKTOP
)
@Composable
fun LoginScreenPreview11() = PreviewDarkMaterialTheme {
    LoginScreenContent(
        uiState = LoginUiState(
            email = "admin@example.com",
            password = "12345678",
            isPasswordVisible = true,
            autoLoginState = true,
            networkState = true,
            isLoading = true,
        ),
        onEmailChanged = {},
        onPasswordChanged = {},
        onTogglePasswordVisibility = {},
        onAutoLoginChanged = {},
        onLogin = {},
    )
}

@Preview(
    device = Devices.DESKTOP
)
@Composable
fun LoginScreenPreview21() = PreviewLightMaterialTheme {
    LoginScreenContent(
        uiState = LoginUiState(
            email = "admin@example.com",
            password = "12345678",
            isPasswordVisible = false,
            autoLoginState = false,
            networkState = true,
//                    isLoading = true,
        ),
        onEmailChanged = {},
        onPasswordChanged = {},
        onTogglePasswordVisibility = {},
        onAutoLoginChanged = {},
        onLogin = {},
    )
}
