package org.bigblackowl.vccadmin.ui.city.addEdit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.openFilePicker
import kotlinx.coroutines.Job
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
import org.bigblackowl.vccadmin.data.repository.CitySearchRepository
import org.bigblackowl.vccadmin.data.repository.NetworkMonitorProvider
import org.bigblackowl.vccadmin.data.events.UIEvents
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
    private val citySearchRepository: CitySearchRepository,
) : ViewModel(), KoinComponent {

    private val _uiEvent = MutableSharedFlow<UIEvents>(extraBufferCapacity = 1)
    val uiEvent: SharedFlow<UIEvents> = _uiEvent.asSharedFlow()

    private val _state = MutableStateFlow(AddEditCityScreenUiState())
    val state: StateFlow<AddEditCityScreenUiState> = _state.asStateFlow()

    private val _isDirty = MutableStateFlow(false)

    private val _cityAutocomplete = MutableStateFlow(CityAutocompleteUiState(isLoading = true))
    val cityAutocomplete: StateFlow<CityAutocompleteUiState> = _cityAutocomplete.asStateFlow()

    private var searchJob: Job? = null
    private var existingCityNames: Set<String> = emptySet()

    private companion object {
        const val MIN_CHARS = 2
        const val DEBOUNCE_MS = 150L
        const val MAX_SUGGESTIONS = 20
    }

    init {
        viewModelScope.launch {
            _cityAutocomplete.update { it.copy(isLoading = true) }
            runCatching { citySearchRepository.preload() }
            _cityAutocomplete.update { it.copy(isLoading = false) }
            refreshExistingCityNames()
        }
    }

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

            is AddEditCityScreenIntent.EditName -> {
                _state.update { it.copy(newCityName = intent.newName) }
                _isDirty.value = computeHasUnsavedChanges()
                triggerCitySearch(intent.newName)
            }

            is AddEditCityScreenIntent.Clear -> clearAll()

            is AddEditCityScreenIntent.GoBack -> handleBackOrCancel()

            is AddEditCityScreenIntent.ExpandCityDropdown -> triggerCitySearch(_state.value.newCityName)

            is AddEditCityScreenIntent.HighlightNextCity -> highlight(+1)
            is AddEditCityScreenIntent.HighlightPrevCity -> highlight(-1)
            is AddEditCityScreenIntent.SelectHighlightedCity -> selectHighlighted()

            is AddEditCityScreenIntent.CitySelected -> {
                if (intent.suggestion.exists) return
                _state.update { it.copy(newCityName = intent.suggestion.city.name) }
                _isDirty.value = computeHasUnsavedChanges()
                _cityAutocomplete.update { it.copy( highlightedIndex = -1) }
                // важливо: можна не тригерити пошук тут, бо ми закрили dropdown і вже маємо фінальний текст
            }
        }
    }

    private fun clearAll() {
        searchJob?.cancel()
        _state.value = AddEditCityScreenUiState()
        _cityAutocomplete.value = CityAutocompleteUiState(isLoading = false,suggestions = emptyList(), highlightedIndex = -1)
        _isDirty.value = false
    }

    private fun highlight(dir: Int) {
        _cityAutocomplete.update { st ->
            val n = st.suggestions.size
            if (n == 0) return@update st

            var i = st.highlightedIndex
            repeat(n) {
                i = when {
                    i < 0 && dir > 0 -> 0
                    i < 0 && dir < 0 -> n - 1
                    else -> (i + dir + n) % n
                }
                if (!st.suggestions[i].exists) return@update st.copy(highlightedIndex = i)
            }
            st.copy(highlightedIndex = -1)
        }
    }

    private fun selectHighlighted() {
        val st = _cityAutocomplete.value
        val idx = st.highlightedIndex
        if (idx !in st.suggestions.indices) return

        val suggestion = st.suggestions[idx]
        if (suggestion.exists) return

        _state.update { it.copy(newCityName = suggestion.city.name) }
        _isDirty.value = computeHasUnsavedChanges()
        _cityAutocomplete.update { it.copy( highlightedIndex = -1) }
    }

    private fun triggerCitySearch(text: String) {
        val q = text.trim()

        searchJob?.cancel()
        if (q.length < MIN_CHARS) {
            _cityAutocomplete.update { it.copy(suggestions = emptyList(), isLoading = false, highlightedIndex = -1) }
            return
        }

        searchJob = viewModelScope.launch {
            _cityAutocomplete.update { it.copy(isLoading = true) }
            delay(DEBOUNCE_MS)

            val result = runCatching { citySearchRepository.search(q, limit = MAX_SUGGESTIONS) }
                .getOrDefault(emptyList())

            val selfName = _state.value.initialName
            val suggestions = result.map { dto ->
                val isSelf = selfName.isNotBlank() && dto.name == selfName
                CitySuggestion(
                    city = dto,
                    exists = !isSelf && existingCityNames.contains(dto.name)
                )
            }

            val prevIndex = _cityAutocomplete.value.highlightedIndex
            val nextIndex = when {
                suggestions.isEmpty() -> -1
                prevIndex in suggestions.indices && !suggestions[prevIndex].exists -> prevIndex
                else -> suggestions.indexOfFirst { !it.exists }.takeIf { it >= 0 } ?: -1
            }

            _cityAutocomplete.update {
                it.copy(
                    suggestions = suggestions,
                    isLoading = false,
                    highlightedIndex = nextIndex
                )
            }
        }
    }

    private fun editLogo() = viewModelScope.launch {
        val logo = FileKit.openFilePicker(type = FileKitType.Image)
        _state.update { it.copy(newCityLogoFile = logo) }
        _isDirty.value = computeHasUnsavedChanges()
    }

    private fun computeHasUnsavedChanges(): Boolean {
        val s = _state.value
        return if (s.selectedCity == null) {
            s.newCityName.isNotBlank() || s.newCityLogoFile != null
        } else {
            s.newCityName != s.initialName || s.newCityLogoFile != null
        }
    }

    private fun handleBackOrCancel() {
        if (_isDirty.value) showEvent(UIEvents.ShowUnsavedChangesDialog)
        else {
            cancelChanges()
            showEvent(UIEvents.NavigateBack)
        }
    }

    private fun saveChanges() = viewModelScope.launch {
        if (!networkMonitorProvider.isConnected.value) {
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

            refreshExistingCityNames()
            _isDirty.value = false
            showEvent(UIEvents.NavigateBack)
        } catch (e: Exception) {
            showMessage(e.message ?: getString(Res.string.add_city_error))
            errorManager.report(
                message = e.message.orEmpty(),
                errorCode = ErrorCode.AddEditCityScreenViewModel.SAVE_CHANGES
            )
        } finally {
            _state.update { it.copy(isLoading = false) }
        }
    }

    private fun cancelChanges() {
        val s = _state.value
        _state.update {
            if (s.selectedCity != null) it.copy(newCityName = s.initialName, newCityLogoFile = null)
            else it.copy(newCityName = "", newCityLogoFile = null)
        }
        _isDirty.value = false
    }

    private fun getCity(cityId: Int) = viewModelScope.launch {
        if (!networkMonitorProvider.isConnected.value) {
            _uiEvent.emit(UIEvents.ShowMessage(getString(Res.string.no_internet)))
            return@launch
        }
        try {
            _state.update { it.copy(isLoading = true) }
            val city = cityRepository.getCities().firstOrNull { it.id == cityId }
            if (city != null) {
                _state.update { it.copy(selectedCity = city, initialName = city.name, newCityName = city.name) }
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
        if (!networkMonitorProvider.isConnected.value) {
            _uiEvent.emit(UIEvents.ShowMessage(getString(Res.string.no_internet)))
            return@launch
        }
        try {
            _state.update { it.copy(isLoading = true) }
            cityRepository.deleteCity(city)
            showMessage(getString(Res.string.success_deleted))
            refreshExistingCityNames()
            showEvent(UIEvents.NavigateBack)
        } catch (e: Exception) {
            showMessage(e.message?.take(30) ?: getString(Res.string.delete_error))
            errorManager.report(message = e.message.orEmpty(), errorCode = ErrorCode.AddEditCityScreenViewModel.DELETE_CITY)
        } finally {
            _state.update { it.copy(isLoading = false) }
        }
    }

    private suspend fun refreshExistingCityNames() {
        existingCityNames = cityRepository.getCities().map { it.name }.toSet()
    }

    private fun showEvent(event: UIEvents) = _uiEvent.tryEmit(event)
    private fun showMessage(message: String) = _uiEvent.tryEmit(UIEvents.ShowMessage(message))

    override fun onCleared() {
        searchJob?.cancel()
        super.onCleared()
    }
}