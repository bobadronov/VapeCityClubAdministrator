package org.bigblackowl.vccadmin.ui.slidesList

import org.bigblackowl.vccadmin.data.entity.Shop
import org.bigblackowl.vccadmin.data.entity.Slide

data class SlidesListUiState(
    val isInitialLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val slides: List<Slide> = emptyList(),
    val shopList: List<Shop> = emptyList(),
)