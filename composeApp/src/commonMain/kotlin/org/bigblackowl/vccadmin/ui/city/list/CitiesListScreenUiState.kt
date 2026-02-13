package org.bigblackowl.vccadmin.ui.city.list

import org.bigblackowl.vccadmin.data.entity.City
import org.bigblackowl.vccadmin.data.entity.UserRole

data class CitiesListScreenUiState(
    val isInitialLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val cities: List<City> = emptyList(),
    val currentUserRole: UserRole = UserRole.USER,
)

