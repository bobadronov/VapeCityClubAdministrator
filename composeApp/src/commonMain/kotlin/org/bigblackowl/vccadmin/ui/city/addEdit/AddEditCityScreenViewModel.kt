package org.bigblackowl.vccadmin.ui.city.addEdit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.openFilePicker
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.bigblackowl.vccadmin.data.entity.City
import org.bigblackowl.vccadmin.data.errorManager.ErrorCode
import org.bigblackowl.vccadmin.data.errorManager.ErrorManager
import org.bigblackowl.vccadmin.data.repository.CityRepository
import org.bigblackowl.vccadmin.data.repository.NetworkMonitorProvider
import org.bigblackowl.vccadmin.data.state.UIEvents
import org.jetbrains.compose.resources.getString
import org.koin.core.component.KoinComponent
import vccadministrator.composeapp.generated.resources.Res
import vccadministrator.composeapp.generated.resources.add_city_error
import vccadministrator.composeapp.generated.resources.city_not_found
import vccadministrator.composeapp.generated.resources.delete_error
import vccadministrator.composeapp.generated.resources.error_load_shop
import vccadministrator.composeapp.generated.resources.invalid_city_name
import vccadministrator.composeapp.generated.resources.new_city_added_success
import vccadministrator.composeapp.generated.resources.no_internet
import vccadministrator.composeapp.generated.resources.success_deleted
import vccadministrator.composeapp.generated.resources.success_updated
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime

class AddEditCityScreenViewModel(
    private val cityRepository: CityRepository,
    private val errorManager: ErrorManager,
    private val networkMonitorProvider: NetworkMonitorProvider,
) : ViewModel(), KoinComponent {

    private val _uiEvent = MutableSharedFlow<UIEvents>(replay = 0)
    val uiEvent: SharedFlow<UIEvents> = _uiEvent.asSharedFlow()

    private val _state = MutableStateFlow(AddEditCityScreenUiState())
    val state: StateFlow<AddEditCityScreenUiState> = _state.asStateFlow()

    private val _isDirty = MutableStateFlow(false)

    private val _event = MutableSharedFlow<UIEvents>(replay = 0)
    val event: SharedFlow<UIEvents> = _event.asSharedFlow()

    fun onIntent(intent: AddEditCityScreenIntent) {
        when (intent) {
            is AddEditCityScreenIntent.GetCity -> getCity(intent.cityId)
            is AddEditCityScreenIntent.Save -> saveChanges()
            is AddEditCityScreenIntent.DiscardAndBack -> {
                cancelChanges()
                showEvent(UIEvents.NavigateBack)
            }
            is AddEditCityScreenIntent.DeleteCity -> deleteCity(intent.city)
            is AddEditCityScreenIntent.EditLogo -> editLogo()
            is AddEditCityScreenIntent.EditName -> editName(intent.newName)
            is AddEditCityScreenIntent.Clear -> _state.value = AddEditCityScreenUiState()
            is AddEditCityScreenIntent.GoBack -> handleBackOrCancel()
        }
    }

    private fun editName(newName: String) {
        _state.update { it.copy(newCityName = newName) }
        _isDirty.value = computeHasUnsavedChanges()
    }

    private fun editLogo() = viewModelScope.launch {
        val logo = FileKit.openFilePicker(type = FileKitType.Image)
        _state.update { it.copy(newCityLogoFile = logo) }
        _isDirty.value = computeHasUnsavedChanges()
    }

    private fun computeHasUnsavedChanges(): Boolean {
        val current = _state.value
        return if (current.selectedCity == null) {
            current.newCityName.isNotBlank() || current.newCityLogoFile != null
        } else {
            current.newCityName != current.initialName || current.newCityLogoFile != null
        }
    }

    private fun handleBackOrCancel() {
        if (_isDirty.value) {
            showEvent(UIEvents.ShowUnsavedChangesDialog)
        } else {
            cancelChanges()
            showEvent(UIEvents.NavigateBack)
        }
    }

    private fun saveChanges() = viewModelScope.launch {
        if (networkMonitorProvider.isConnected.value.not()) {
            _uiEvent.emit(UIEvents.ShowMessage(getString(Res.string.no_internet)))
            return@launch
        }
        try {
            _state.update { it.copy(isLoading = true) }
            val current = _state.value
            if (current.newCityName.length < 2) {
                showMessage(getString(Res.string.invalid_city_name))
                return@launch
            }
            if (current.selectedCity == null) {
                cityRepository.addCity(current.newCityName, current.newCityLogoFile)
                showMessage(getString(Res.string.new_city_added_success))
            } else {
                cityRepository.updateCity(current.selectedCity, current.newCityName, current.newCityLogoFile)
                showMessage(getString(Res.string.success_updated))
            }
            _isDirty.value = false
            showEvent(UIEvents.NavigateBack)
        } catch (exception: Exception) {
            val msg = exception.message ?: getString(Res.string.add_city_error)
            showMessage(msg)
            errorManager.report(message = exception.message.orEmpty(), errorCode = ErrorCode.AddEditCityScreenViewModel.SAVE_CHANGES)
        } finally {
            _state.update { it.copy(isLoading = false) }
        }
    }

    private fun cancelChanges() {
        val current = _state.value
        _state.update {
            if (current.selectedCity != null) {
                it.copy(newCityName = current.initialName, newCityLogoFile = null)
            } else {
                it.copy(newCityName = "", newCityLogoFile = null)
            }
        }
        _isDirty.value = false
    }

    private fun getCity(cityId: Int) = viewModelScope.launch {
        if (networkMonitorProvider.isConnected.value.not()) {
            _uiEvent.emit(UIEvents.ShowMessage(getString(Res.string.no_internet)))
            return@launch
        }
        try {
            _state.update { it.copy(isLoading = true) }
            val city = cityRepository.getCities().firstOrNull { it.id == cityId }
            if (city != null) {
                _state.update {
                    it.copy(
                        selectedCity = city,
                        initialName = city.name,
                        newCityName = city.name
                    )
                }
                _isDirty.value = false
            } else {
                showMessage(getString(Res.string.city_not_found))
                delay(4.seconds)
                showEvent(UIEvents.NavigateBack)
            }
        } catch (e: Exception) {
            showMessage(e.message ?: getString(Res.string.error_load_shop))
            errorManager.report(message = e.message.orEmpty(), errorCode = ErrorCode.AddEditCityScreenViewModel.GET_CITY)
        } finally {
            _state.update { it.copy(isLoading = false) }
        }
    }

    @OptIn(ExperimentalTime::class)
    private fun deleteCity(city: City) = viewModelScope.launch {
        if (networkMonitorProvider.isConnected.value.not()) {
            _uiEvent.emit(UIEvents.ShowMessage(getString(Res.string.no_internet)))
            return@launch
        }
        try {
            _state.update { it.copy(isLoading = true) }
            cityRepository.deleteCity(city)
            showMessage(getString(Res.string.success_deleted))
            showEvent(UIEvents.NavigateBack)
        } catch (e: Exception) {
            val safeMessage = e.message?.take(30) ?: getString(Res.string.delete_error)
            showMessage(safeMessage)
            errorManager.report(message = e.message.orEmpty(), errorCode = ErrorCode.AddEditCityScreenViewModel.DELETE_CITY)

        } finally {
            _state.update { it.copy(isLoading = false) }
        }
    }

    private fun showEvent(event: UIEvents) = viewModelScope.launch {
        _event.emit(event)
    }
    private fun showMessage(message: String) = viewModelScope.launch {
        println(message)
        _event.emit(UIEvents.ShowMessage(message))
    }
}