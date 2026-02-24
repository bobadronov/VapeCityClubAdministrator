package org.bigblackowl.vccadmin.theme.locals

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import org.bigblackowl.vccadmin.data.entity.ShopStatusColors
import org.bigblackowl.vccadmin.data.entity.UserRoleColors
import org.bigblackowl.vccadmin.theme.ShopStatusColorsDark
import org.bigblackowl.vccadmin.theme.ShopStatusColorsLight
import org.bigblackowl.vccadmin.theme.UserRoleColorsDark
import org.bigblackowl.vccadmin.theme.UserRoleColorsLight

val LocalUserRoleColors = staticCompositionLocalOf {
    UserRoleColors(
        admin = Color.Unspecified,
        user = Color.Unspecified,
    )
}
fun shopStatusColors(isDark: Boolean): ShopStatusColors =
    if (isDark) ShopStatusColorsDark else ShopStatusColorsLight

fun userRoleColors(isDark: Boolean): UserRoleColors =
    if (isDark) UserRoleColorsDark else UserRoleColorsLight

@Suppress("UnusedReceiverParameter")
val MaterialTheme.userRoleColors: UserRoleColors
    @Composable get() = LocalUserRoleColors.current