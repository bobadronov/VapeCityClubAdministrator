package org.bigblackowl.vccadmin.ui.city.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.bigblackowl.vccadmin.data.entity.UserRole
import org.bigblackowl.vccadmin.data.errorManager.ErrorCode
import org.bigblackowl.vccadmin.data.errorManager.ErrorManager
import org.bigblackowl.vccadmin.data.events.UIEvents
import org.bigblackowl.vccadmin.data.utils.NetworkMonitorProvider
import org.bigblackowl.vccadmin.domain.repository.CityRepository
import org.bigblackowl.vccadmin.domain.repository.UserRepository
import org.jetbrains.compose.resources.getString
import org.koin.core.component.KoinComponent
import vccadministrator.composeapp.generated.resources.Res
import vccadministrator.composeapp.generated.resources.no_internet

class CitiesListScreenViewModel(
    private val errorManager: ErrorManager,
    private val cityRepository: CityRepository,
    private val userRepository: UserRepository,
    private val networkMonitorProvider: NetworkMonitorProvider,
) : ViewModel(), KoinComponent {

    private val _state = MutableStateFlow(CitiesListScreenUiState())
    val state: StateFlow<CitiesListScreenUiState> = _state.asStateFlow()

    private val _uiEvent = MutableSharedFlow<UIEvents>(replay = 0)
    val uiEvent: SharedFlow<UIEvents> = _uiEvent.asSharedFlow()


    fun onIntent(intent: CitiesListScreenIntent) {
        when (intent) {
            CitiesListScreenIntent.Load -> loadCities(forceRefresh = false)
            CitiesListScreenIntent.Refresh -> loadCities(forceRefresh = true)
        }
    }

    private fun loadCities(forceRefresh: Boolean) = viewModelScope.launch {
        val hasData = _state.value.cities.isNotEmpty()

        // Виставляємо режими
        _state.update {
            it.copy(
                isInitialLoading = !hasData && !forceRefresh,
                isRefreshing = forceRefresh || hasData
            )
        }

        if (networkMonitorProvider.isConnected.value.not()) {
            _uiEvent.emit(UIEvents.ShowMessage(getString(Res.string.no_internet)))
            _state.update { it.copy(isInitialLoading = false, isRefreshing = false) }
            return@launch
        }

        try {
            val cityList = cityRepository.getCities().sortedBy { it.name }
            val userRole = userRepository.getCurrentUser()?.role ?: UserRole.USER

            _state.update {
                it.copy(
                    cities = cityList,
                    currentUserRole = userRole
                )
            }
        } catch (exception: Exception) {
            showMessage(exception.stackTraceToString())
            errorManager.report(message = exception.message.orEmpty(), errorCode = ErrorCode.CitiesListScreenViewModel.LOAD_CITIES)
        } finally {
            _state.update { it.copy(isInitialLoading = false, isRefreshing = false) }
        }
    }

    private fun showMessage(message: String) = viewModelScope.launch {
        _uiEvent.emit(UIEvents.ShowMessage(message))
    }
}

