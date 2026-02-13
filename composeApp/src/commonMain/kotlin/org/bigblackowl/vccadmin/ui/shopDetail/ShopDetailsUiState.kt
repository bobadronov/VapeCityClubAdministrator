package org.bigblackowl.vccadmin.ui.shopDetail

import org.bigblackowl.vccadmin.data.entity.Shop
import org.bigblackowl.vccadmin.data.entity.UserRole

data class ShopDetailsUiState(
    val isInitialLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val shop: Shop? = null,
    val userRole: UserRole = UserRole.USER,
)
