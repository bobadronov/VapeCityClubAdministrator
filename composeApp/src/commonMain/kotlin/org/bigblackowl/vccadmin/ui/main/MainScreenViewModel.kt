// File: MainViewModel.kt
package org.bigblackowl.vccadmin.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.bigblackowl.vccadmin.data.entity.ShopsFilter
import org.bigblackowl.vccadmin.data.entity.toUiShops
import org.bigblackowl.vccadmin.data.errorManager.ErrorCode
import org.bigblackowl.vccadmin.data.errorManager.ErrorManager
import org.bigblackowl.vccadmin.data.repository.CityRepository
import org.bigblackowl.vccadmin.data.repository.NetworkMonitorProvider
import org.bigblackowl.vccadmin.data.repository.ShopRepository
import org.bigblackowl.vccadmin.data.events.UIEvents
import org.bigblackowl.vccadmin.data.utils.ShopGroup
import org.bigblackowl.vccadmin.data.utils.getGroupedShops
import org.jetbrains.compose.resources.getString
import org.koin.core.component.KoinComponent
import vccadministrator.composeapp.generated.resources.Res
import vccadministrator.composeapp.generated.resources.error_load_data
import vccadministrator.composeapp.generated.resources.no_internet

class MainScreenViewModel(
    private val shopRepository: ShopRepository,
    private val cityRepository: CityRepository,
    private val networkMonitorProvider: NetworkMonitorProvider,
    private val errorManager: ErrorManager,
) : ViewModel(), KoinComponent {

    private val _uiEvent = MutableSharedFlow<UIEvents>(replay = 0)
    val uiEvent: SharedFlow<UIEvents> = _uiEvent.asSharedFlow()

    private val _uiState = MutableStateFlow(MainScreenState())
    val uiState: StateFlow<MainScreenState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            networkMonitorProvider.isConnected.collect { connected ->
                if (connected) loadAllData()
            }
        }
    }

    fun onIntent(intent: MainScreenIntent) {
        when (intent) {
            MainScreenIntent.Refresh -> loadAllData()

            is MainScreenIntent.ToggleCity -> {
                _uiState.update { st ->
                    val nextFilter = st.filter.toggleCity(intent.cityId)
                    st.copy(filter = nextFilter, filteredGroupedShops = applyFilter(st.groupedShops, nextFilter))
                }
            }

            is MainScreenIntent.ToggleStatus -> {
                _uiState.update { st ->
                    val nextFilter = st.filter.toggleStatus(intent.status)
                    st.copy(filter = nextFilter, filteredGroupedShops = applyFilter(st.groupedShops, nextFilter))
                }
            }

            MainScreenIntent.ClearFilters -> {
                _uiState.update { st ->
                    val nextFilter = st.filter.clear()
                    st.copy(filter = nextFilter, filteredGroupedShops = applyFilter(st.groupedShops, nextFilter))
                }
            }
        }
    }

    private fun loadAllData() {
        val hasData = _uiState.value.groupedShops.isNotEmpty() || _uiState.value.cities.isNotEmpty()

        _uiState.update {
            it.copy(
                isInitialLoading = !hasData,
                isRefreshing = hasData,
            )
        }

        viewModelScope.launch {
            if (networkMonitorProvider.isConnected.value.not()) {
                _uiEvent.emit(UIEvents.ShowMessage(getString(Res.string.no_internet)))
                _uiState.update { it.copy(isInitialLoading = false, isRefreshing = false) }
                return@launch
            }

            try {
                coroutineScope {
                    val shopsDeferred = async { shopRepository.getStores() }
                    val citiesDeferred = async { cityRepository.getCities() }

                    val supabaseShops = shopsDeferred.await()
                    val cities = citiesDeferred.await().sortedBy { it.name }

                    val uiShops = supabaseShops.toUiShops(cities)
                    val grouped = getGroupedShops(uiShops, cities)

                    _uiState.update { st ->
                        val filtered = applyFilter(grouped, st.filter)
                        st.copy(
                            groupedShops = grouped,
                            cities = cities,
                            filteredGroupedShops = filtered,
                        )
                    }
                }
            } catch (exception: Exception) {
                val message = exception.message ?: getString(Res.string.error_load_data)
                _uiEvent.emit(UIEvents.ShowMessage(message))
                errorManager.report(message = exception.message.orEmpty(), errorCode = ErrorCode.MainScreenViewModel.LOAD_DATA)
            } finally {
                _uiState.update { it.copy(isInitialLoading = false, isRefreshing = false) }
            }
        }
    }

    private fun applyFilter(
        groups: List<ShopGroup>,
        filter: ShopsFilter
    ): List<ShopGroup> {
        val cityIds = filter.selectedCityIds
        val statuses = filter.selectedStatuses

        return groups
            .asSequence()
            .filter { group ->
                // якщо міста не вибрані — всі; інакше тільки вибрані міста
                cityIds.isEmpty() || group.city.id in cityIds
            }
            .map { group ->
                val shops = group.shops.filter { shop ->
                    statuses.isEmpty() || shop.status in statuses
                }
                group.copy(shops = shops)
            }
            .filter { it.shops.isNotEmpty() }
            .toList()
    }
}
