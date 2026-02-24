package org.bigblackowl.vccadmin.theme

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import org.bigblackowl.vccadmin.resourses.DefaultValues
import org.bigblackowl.vccadmin.theme.locals.LocalShopStatusColors
import org.bigblackowl.vccadmin.theme.locals.LocalUserRoleColors
import org.bigblackowl.vccadmin.theme.locals.shopStatusColors
import org.bigblackowl.vccadmin.theme.locals.userRoleColors

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PreviewLightMaterialTheme(topBar: @Composable (() -> Unit) = {}, content: @Composable (() -> Unit)) {
    val isDark = false
    val shopStatusColors = remember(isDark) { shopStatusColors(isDark) }
    val userRoleColors = remember(isDark) { userRoleColors(isDark) }

    CompositionLocalProvider(
        LocalShopStatusColors provides shopStatusColors,
        LocalUserRoleColors provides userRoleColors,
    ) {
        MaterialExpressiveTheme(colorScheme = LightColorScheme, content = {
            Scaffold(topBar = topBar) { paddingValues ->
                Box(modifier = Modifier.padding(paddingValues).padding(DefaultValues.Padding.mainBoxPadding)) {
                    content()
                }
            }
        })
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PreviewDarkMaterialTheme(topBar: @Composable (() -> Unit) = {}, content: @Composable (() -> Unit)) {
    val isDark = true
    val shopStatusColors = remember(isDark) { shopStatusColors(isDark) }
    val userRoleColors = remember(isDark) { userRoleColors(isDark) }

    CompositionLocalProvider(
        LocalShopStatusColors provides shopStatusColors,
        LocalUserRoleColors provides userRoleColors,
    ) {
        MaterialExpressiveTheme(colorScheme = DarkColorScheme, content = {
            Scaffold(topBar = topBar) { paddingValues ->
                Box(modifier = Modifier.padding(paddingValues).padding(DefaultValues.Padding.mainBoxPadding)) {
                    content()
                }
            }
        })
    }
}