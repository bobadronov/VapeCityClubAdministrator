package org.bigblackowl.vccadmin.ui.main

import org.bigblackowl.vccadmin.data.entity.City
import org.bigblackowl.vccadmin.data.entity.ShopsFilter
import org.bigblackowl.vccadmin.data.utils.ShopGroup

/** Стан головного екрану */
data class MainScreenState(
    val isInitialLoading: Boolean = true,
    val isRefreshing: Boolean = false,

    val groupedShops: List<ShopGroup> = emptyList(),
    val filteredGroupedShops: List<ShopGroup> = emptyList(),

    val cities: List<City> = emptyList(),

    val filter: ShopsFilter = ShopsFilter(),
)