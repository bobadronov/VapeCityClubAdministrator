package org.bigblackowl.vccadmin.ui.shopDetail

sealed interface ShopDetailsScreenIntent {
    data class Load(val id: String) : ShopDetailsScreenIntent
    data class Refresh(val id: String) : ShopDetailsScreenIntent
    data class ShareShop(val data: String) : ShopDetailsScreenIntent
}