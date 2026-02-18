package org.bigblackowl.vccadmin.ui.main

import org.bigblackowl.vccadmin.data.entity.ShopStatus

sealed class MainScreenIntent {
    data object Refresh : MainScreenIntent()

    data object ClearFilters : MainScreenIntent()
    data class ToggleCity(val cityId: Int) : MainScreenIntent()
    data class ToggleStatus(val status: ShopStatus) : MainScreenIntent()
}