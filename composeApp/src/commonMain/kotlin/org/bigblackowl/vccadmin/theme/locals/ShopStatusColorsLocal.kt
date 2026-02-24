package org.bigblackowl.vccadmin.theme.locals

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import org.bigblackowl.vccadmin.data.entity.ShopStatusColors

val LocalShopStatusColors = staticCompositionLocalOf {
    ShopStatusColors(
        active = Color.Unspecified,
        inactive = Color.Unspecified,
        closed = Color.Unspecified,
        relocating = Color.Unspecified,
        underRepair = Color.Unspecified,
    )
}

@Suppress("UnusedReceiverParameter")
val MaterialTheme.shopStatusColors: ShopStatusColors
    @Composable get() = LocalShopStatusColors.current