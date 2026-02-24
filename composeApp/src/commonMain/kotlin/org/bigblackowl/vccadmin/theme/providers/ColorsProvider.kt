package org.bigblackowl.vccadmin.theme.providers

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import org.bigblackowl.vccadmin.data.entity.ShopStatusColors
import org.bigblackowl.vccadmin.data.entity.UserRoleColors
import org.bigblackowl.vccadmin.theme.locals.LocalShopStatusColors
import org.bigblackowl.vccadmin.theme.locals.LocalUserRoleColors

@Composable
fun ColorsProvider(
    shopStatusColors: ShopStatusColors,
    userRoleColors: UserRoleColors,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalShopStatusColors provides shopStatusColors,
        LocalUserRoleColors provides userRoleColors,
        content = content
    )
}