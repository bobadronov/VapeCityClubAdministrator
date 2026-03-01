package org.bigblackowl.vccadmin.ui.addEditSlideScreen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.aakira.napier.Napier
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.openFilePicker
import io.github.vinceglb.filekit.name
import io.github.vinceglb.filekit.readBytes
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.bigblackowl.vccadmin.data.entity.City
import org.bigblackowl.vccadmin.data.entity.DeviceType
import org.bigblackowl.vccadmin.data.entity.Shop
import org.bigblackowl.vccadmin.data.entity.ShopGroup
import org.bigblackowl.vccadmin.data.entity.SupabaseSlide
import org.bigblackowl.vccadmin.data.entity.toUiShops
import org.bigblackowl.vccadmin.data.errorManager.ErrorCode
import org.bigblackowl.vccadmin.data.errorManager.ErrorManager
import org.bigblackowl.vccadmin.data.events.UIEvents
import org.bigblackowl.vccadmin.data.utils.NetworkMonitorProvider
import org.bigblackowl.vccadmin.domain.repository.CityRepository
import org.bigblackowl.vccadmin.domain.repository.ShopRepository
import org.bigblackowl.vccadmin.domain.repository.SlideRepository
import org.bigblackowl.vccadmin.utils.PlatformFileProvider
import org.jetbrains.compose.resources.getString
import org.koin.core.component.KoinComponent
import vccadministrator.composeapp.generated.resources.Res
import vccadministrator.composeapp.generated.resources.error_add_slide
import vccadministrator.composeapp.generated.resources.error_delete_slide
import vccadministrator.composeapp.generated.resources.error_empty_name
import vccadministrator.composeapp.generated.resources.error_file_name_empty
import vccadministrator.composeapp.generated.resources.error_invalid_file_name
import vccadministrator.composeapp.generated.resources.error_load_file
import vccadministrator.composeapp.generated.resources.error_open_file
import vccadministrator.composeapp.generated.resources.error_select_file
import vccadministrator.composeapp.generated.resources.error_slide_id_missing
import vccadministrator.composeapp.generated.resources.error_unknown_load
import vccadministrator.composeapp.generated.resources.error_update_slide
import vccadministrator.composeapp.generated.resources.file_name_hint
import vccadministrator.composeapp.generated.resources.file_name_invalid
import vccadministrator.composeapp.generated.resources.file_not_selected
import vccadministrator.composeapp.generated.resources.file_saved_success
import vccadministrator.composeapp.generated.resources.no_internet
import vccadministrator.composeapp.generated.resources.preview_create_error
import vccadministrator.composeapp.generated.resources.slide_added_success
import vccadministrator.composeapp.generated.resources.slide_deleted_success
import vccadministrator.composeapp.generated.resources.slide_updated_success

private val FILE_NAME_REGEX = Regex("^[a-zA-Z0-9 _\\-.()]+$")

class AddEditSlideViewModel(
    private val errorManager: ErrorManager,
    private val cityRepository: CityRepository,
    private val shopRepository: ShopRepository,
    private val slideRepository: SlideRepository,
    private val networkMonitorProvider: NetworkMonitorProvider,
) : ViewModel(), KoinComponent {

    private val _uiEvent = MutableSharedFlow<UIEvents>(replay = 0)
    val uiEvent: SharedFlow<UIEvents> = _uiEvent.asSharedFlow()

    private val _uiState = MutableStateFlow(AddSlideState())
    val uiState: StateFlow<AddSlideState> = _uiState.asStateFlow()

    private val _isDirty = MutableStateFlow(false)

    companion object {
        const val TAG = "AddEditSlideViewModel"
    }

    fun onIntent(intent: AddEditSlideIntent) {
        when (intent) {
            AddEditSlideIntent.ClearData -> clearData()
            is AddEditSlideIntent.DeleteSlide -> deleteSlide(intent.slideId)
            AddEditSlideIntent.GoBack -> goBack()
            is AddEditSlideIntent.LoadSlide -> loadSlide(intent.slideId)
            is AddEditSlideIntent.OnActiveChanged -> onActiveChanged(intent.state)
            is AddEditSlideIntent.OnFileNameChanged -> onFileNameChanged(intent.newName)
            AddEditSlideIntent.OnSave -> onSave()
            is AddEditSlideIntent.OnShopToggled -> onShopToggled(intent.code)
            AddEditSlideIntent.OnToggleAllShops -> onToggleAllShops()
            AddEditSlideIntent.OnToggleAllShopsWithTablet -> onToggleAllShopsWithTablet()
            AddEditSlideIntent.OnToggleAllShopsWithTv -> onToggleAllShopsWithTv()
            AddEditSlideIntent.SelectFile -> selectFile()
            AddEditSlideIntent.DiscardChanges -> discardChanges()
            AddEditSlideIntent.DownloadIconFile -> downloadIconFile()
            AddEditSlideIntent.OpenFile -> openDownloadedFile()
        }
    }

    // ---------- Derived selection recalculation ----------

    private fun recalcSelectionDerived(current: AddSlideState): AddSlideState {
        val allCodes = current.allShopList.map { it.code }.toSet()

        val tabletCodes = current.allShopList
            .asSequence()
            .filter { it.deviceType == DeviceType.TABLET }
            .map { it.code }
            .toSet()

        val tvCodes = current.allShopList
            .asSequence()
            .filter { it.deviceType == DeviceType.TV }
            .map { it.code }
            .toSet()

        val selected = current.selectedShopCodes

        val isAllSelected = allCodes.isNotEmpty() && selected.containsAll(allCodes)
        val isAllTabletSelected = tabletCodes.isNotEmpty() && selected.containsAll(tabletCodes)
        val isAllTvSelected = tvCodes.isNotEmpty() && selected.containsAll(tvCodes)

        return current.copy(
            allCodes = allCodes,
            tabletCodes = tabletCodes,
            tvCodes = tvCodes,
            isAllSelected = isAllSelected,
            isAllTabletSelected = isAllTabletSelected,
            isAllTvSelected = isAllTvSelected,
        )
    }

    // ---------- File open / download ----------

    private fun openDownloadedFile() {
        viewModelScope.launch {
            try {
                val state = _uiState.value
                val fileNameToDownload = when {
                    state.selectedFile != null -> state.selectedFile.name
                    state.fileName.isNotBlank() -> state.fileName.trim()
                    else -> {
                        onEvent(UIEvents.ShowMessage(getString(Res.string.error_file_name_empty)))
                        return@launch
                    }
                }
                PlatformFileProvider.openFile(fileNameToDownload)
            } catch (exception: Exception) {
                onEvent(UIEvents.ShowMessage(getString(Res.string.error_open_file)))
                errorManager.report(message = exception.message.orEmpty(), errorCode = ErrorCode.AddEditSlideViewModel.OPEN_FILE)

            }
        }
    }

    private fun downloadIconFile() {
        viewModelScope.launch {
            if (networkMonitorProvider.isConnected.value.not()) {
                onEvent(UIEvents.ShowMessage(getString(Res.string.no_internet)))
                return@launch
            }

            try {
                val state = _uiState.value
                val fileNameToDownload = when {
                    state.selectedFile != null -> state.selectedFile.name
                    state.fileName.isNotBlank() -> state.fileName.trim()
                    else -> {
                        onEvent(UIEvents.ShowMessage(getString(Res.string.error_file_name_empty)))
                        return@launch
                    }
                }
                if (!FILE_NAME_REGEX.matches(fileNameToDownload)) {
                    onEvent(UIEvents.ShowMessage(getString(Res.string.error_invalid_file_name)))
                    return@launch
                }
                _uiState.update { it.copy(isLoading = true) }
                val bytes: ByteArray = slideRepository.downloadSlideIcon(fileNameToDownload)
                PlatformFileProvider.downloadFile(name = fileNameToDownload, content = bytes)
                onEvent(UIEvents.ShowMessage(getString(Res.string.file_saved_success, fileNameToDownload)))
                _uiState.update { it.copy(isFileDownloaded = PlatformFileProvider.isFileExist(fileNameToDownload)) }
            } catch (exception: Exception) {
                onEvent(UIEvents.ShowMessage(exception.message ?: getString(Res.string.error_load_file)))
                errorManager.report(message = exception.message.orEmpty(), errorCode = ErrorCode.AddEditSlideViewModel.DOWNLOAD_IMAGE)
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    // ---------- Input changes ----------

    private fun onFileNameChanged(newName: String) {
        viewModelScope.launch {
            val trimmed = newName.trim()
            val error = when {
                trimmed.isBlank() -> getString(Res.string.error_empty_name)
                !FILE_NAME_REGEX.matches(trimmed) -> getString(Res.string.error_invalid_file_name)
                else -> null
            }

            _isDirty.value = true
            _uiState.update { it.copy(fileName = trimmed, fileNameError = error, fileNameHint = null) }
        }
    }

    private fun selectFile() {
        viewModelScope.launch {
            try {
                val file = FileKit.openFilePicker(
                    type = FileKitType.File(listOf("png", "jpg", "jpeg", "gif"))
                )
                Napier.d { file?.name.orEmpty() }

                if (file == null) {
                    onEvent(UIEvents.ShowMessage(getString(Res.string.file_not_selected)))
                    return@launch
                }

                val originalName = file.name
                val suggestedName = sanitizeFileNameSuggestion(originalName)
                val hasInvalidChars = !FILE_NAME_REGEX.matches(originalName)

                _isDirty.value = true
                _uiState.update {
                    it.copy(
                        selectedFile = file,
                        fileName = originalName,
                        fileNameHint = if (hasInvalidChars) getString(Res.string.file_name_hint, suggestedName) else null,
                        fileNameError = if (hasInvalidChars) getString(Res.string.file_name_invalid) else null
                    )
                }
            } catch (exception: Throwable) {
                Napier.e(tag = TAG, throwable = exception) { "Помилка створення preview URL" }
                onEvent(UIEvents.ShowMessage(getString(Res.string.preview_create_error)))
                errorManager.report(message = exception.message.orEmpty(), errorCode = ErrorCode.AddEditSlideViewModel.SELECT_FILE)
            }
        }
    }

    private fun onActiveChanged(active: Boolean) {
        _isDirty.value = true
        _uiState.update { it.copy(isActive = active) }
        Napier.d(tag = TAG) { "onActiveChanged: active=$active, _isDirty=true" }
    }

    // ---------- Shops selection ----------

    private fun onToggleAllShops() {
        _isDirty.value = true
        _uiState.update { current ->
            val nextSelected = if (current.isAllSelected) emptySet() else current.allCodes
            recalcSelectionDerived(current.copy(selectedShopCodes = nextSelected))
        }
        Napier.d(tag = TAG) { "onToggleAllShops: _isDirty=true" }
    }

    private fun onToggleAllShopsWithTablet() {
        _isDirty.value = true
        _uiState.update { current ->
            val selected = current.selectedShopCodes.toMutableSet()
            if (current.isAllTabletSelected) selected.removeAll(current.tabletCodes) else selected.addAll(current.tabletCodes)
            recalcSelectionDerived(current.copy(selectedShopCodes = selected))
        }
        Napier.d(tag = TAG) { "onToggleAllShopsWithTablet: _isDirty=true" }
    }

    private fun onToggleAllShopsWithTv() {
        _isDirty.value = true
        _uiState.update { current ->
            val selected = current.selectedShopCodes.toMutableSet()
            if (current.isAllTvSelected) selected.removeAll(current.tvCodes) else selected.addAll(current.tvCodes)
            recalcSelectionDerived(current.copy(selectedShopCodes = selected))
        }
        Napier.d(tag = TAG) { "onToggleAllShopsWithTv: _isDirty=true" }
    }

    private fun onShopToggled(code: String) {
        _isDirty.value = true
        _uiState.update { current ->
            val newSet = current.selectedShopCodes.toMutableSet()
            if (newSet.contains(code)) newSet.remove(code) else newSet.add(code)
            recalcSelectionDerived(current.copy(selectedShopCodes = newSet))
        }
        Napier.d(tag = TAG) { "onShopToggled: code=$code, _isDirty=true" }
    }
    private fun recalcAllDerived(current: AddSlideState): AddSlideState {
        val grouped = getGroupedShops(current.allShopList, current.cities)
        return recalcSelectionDerived(current.copy(groupedShops = grouped))
    }

    // ---------- Save / Load / Delete ----------
    private fun onSave() {
        viewModelScope.launch {
            if (networkMonitorProvider.isConnected.value.not()) {
                onEvent(UIEvents.ShowMessage(getString(Res.string.no_internet)))
                return@launch
            }
            _uiState.update { it.copy(isLoading = true) }
            try {
                if (!validateBeforeSave()) return@launch
                if (_uiState.value.slideId == null) saveNewSlide() else updateExistingSlide()
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    private fun loadSlide(slideId: String?) {
        viewModelScope.launch {
            Napier.d(tag = TAG) { "loadSlide started, slideId=$slideId" }

            if (networkMonitorProvider.isConnected.value.not()) {
                onEvent(UIEvents.ShowMessage(getString(Res.string.no_internet)))
                return@launch
            }

            try {
                _uiState.update { it.copy(isLoading = true) }

                val shopsDeferred = async { shopRepository.getStores() }
                val citiesDeferred = async { cityRepository.getCities() }

                val allShops = shopsDeferred.await()
                val cities = citiesDeferred.await()

                _uiState.update { current ->
                    recalcAllDerived(
                        current.copy(
                            allShopList = allShops.toUiShops(cities),
                            cities = cities
                        )
                    )
                }

                if (slideId == null) return@launch

                val supabaseSlide: SupabaseSlide = slideRepository.getSlideById(slideId)
                Napier.d(tag = "SUPABASE GET SLIDE") { supabaseSlide.fileName }

                _uiState.update { current ->
                    recalcAllDerived(
                        current.copy(
                            slideId = slideId,
                            fileName = supabaseSlide.fileName,
                            selectedShopCodes = supabaseSlide.shopCodes.toSet(),
                            isActive = supabaseSlide.isActive,
                            currentImageUrl = supabaseSlide.publicUrl,
                            isFileDownloaded = PlatformFileProvider.isFileExist(supabaseSlide.fileName)
                        )
                    )
                }

                _isDirty.value = false
                Napier.d(tag = TAG) { "loadSlide completed, _isDirty reset to false" }
            } catch (exception: Exception) {
                val message = exception.message ?: getString(Res.string.error_unknown_load)
                onEvent(UIEvents.ShowMessage(message))
                Napier.e(tag = TAG, throwable = exception) { "loadSlide error" }
                errorManager.report(message = exception.message.orEmpty(), errorCode = ErrorCode.AddEditSlideViewModel.LOAD_SLIDE)
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    private fun deleteSlide(slideId: String) {
        viewModelScope.launch {
            Napier.d(tag = TAG) { "deleteSlide started, slideId=$slideId" }

            if (networkMonitorProvider.isConnected.value.not()) {
                onEvent(UIEvents.ShowMessage(getString(Res.string.no_internet)))
                return@launch
            }

            _uiState.update { it.copy(isLoading = true) }
            try {
                slideRepository.deleteSlide(slideId)
                onEvent(UIEvents.NotificationAndNavigate(getString(Res.string.slide_deleted_success)))
                Napier.d(tag = TAG) { "deleteSlide completed" }
            } catch (exception: Exception) {
                val message = exception.message ?: getString(Res.string.error_delete_slide)
                onEvent(UIEvents.ShowMessage(message))
                Napier.e(tag = TAG, throwable = exception) { "deleteSlide error" }
                errorManager.report(message = exception.message.orEmpty(), errorCode = ErrorCode.AddEditSlideViewModel.DELETE_SLIDE)

            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    private fun clearData() {
        _uiState.value = AddSlideState()
        _isDirty.value = false
        Napier.d(tag = TAG) { "clearData: state reset, _isDirty=false" }
    }

    private fun saveNewSlide() {
        viewModelScope.launch {
            Napier.d(tag = TAG) { "saveNewSlide started, current state: ${_uiState.value}" }

            if (networkMonitorProvider.isConnected.value.not()) {
                onEvent(UIEvents.ShowMessage(getString(Res.string.no_internet)))
                return@launch
            }

            _uiState.update { it.copy(isLoading = true) }
            try {
                val state = _uiState.value
                if (state.fileName.isBlank()) throw IllegalArgumentException(getString(Res.string.error_file_name_empty))
                if (state.selectedFile == null) throw IllegalArgumentException(getString(Res.string.error_select_file))

                slideRepository.addSlide(
                    fileName = state.fileName,
                    data = state.selectedFile.readBytes(),
                    shopCodes = state.selectedShopCodes.toList(),
                    isActive = state.isActive
                )

                _isDirty.value = false
                onEvent(UIEvents.NotificationAndNavigate(getString(Res.string.slide_added_success)))
                Napier.d(tag = TAG) { "saveNewSlide completed, _isDirty=false" }
            } catch (exception: Exception) {
                val message = exception.message ?: getString(Res.string.error_add_slide)
                onEvent(UIEvents.ShowMessage(message))
                Napier.e(tag = TAG) { "saveNewSlide error: $message" }
                errorManager.report(message = exception.message.orEmpty(), errorCode = ErrorCode.AddEditSlideViewModel.SAVE_NEW_SLIDE)

            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    private fun updateExistingSlide() {
        viewModelScope.launch {
            Napier.d(tag = TAG) { "updateExistingSlide started, current state: ${_uiState.value}" }

            if (networkMonitorProvider.isConnected.value.not()) {
                onEvent(UIEvents.ShowMessage(getString(Res.string.no_internet)))
                return@launch
            }

            _uiState.update { it.copy(isLoading = true) }
            try {
                val state = _uiState.value
                val slideId = state.slideId ?: throw IllegalStateException(getString(Res.string.error_slide_id_missing))
                if (state.fileName.isBlank()) throw IllegalArgumentException(getString(Res.string.error_file_name_empty))

                slideRepository.updateSlide(
                    slideId = slideId,
                    fileName = state.fileName,
                    data = state.selectedFile?.readBytes(),
                    shopCodes = state.selectedShopCodes.toList(),
                    isActive = state.isActive
                )

                _isDirty.value = false
                onEvent(UIEvents.NotificationAndNavigate(getString(Res.string.slide_updated_success)))
                Napier.d(tag = TAG) { "updateExistingSlide completed, _isDirty=false" }
            } catch (exception: Exception) {
                val message = exception.message ?: getString(Res.string.error_update_slide)
                onEvent(UIEvents.ShowMessage(message))
                Napier.e(tag = TAG) { "updateExistingSlide error: $message" }
                errorManager.report(message = exception.message.orEmpty(), errorCode = ErrorCode.AddEditSlideViewModel.UPDATE_EXISTING_SLIDE)
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    private suspend fun validateBeforeSave(): Boolean {
        val state = _uiState.value
        val trimmedName = state.fileName.trim()

        return when {
            trimmedName.isBlank() -> {
                _uiState.update { it.copy(fileNameError = getString(Res.string.error_file_name_empty)) }
                false
            }

            !FILE_NAME_REGEX.matches(trimmedName) -> {
                _uiState.update { it.copy(fileNameError = getString(Res.string.error_invalid_file_name)) }
                false
            }

            state.selectedFile == null && state.slideId == null -> {
                onEvent(UIEvents.ShowMessage(getString(Res.string.error_select_file)))
                false
            }

            else -> true
        }
    }

    private fun sanitizeFileNameSuggestion(original: String): String =
        original.trim()
            .replace(Regex("\\s+"), " ")
            .replace(Regex("[^a-zA-Z0-9 _\\-.()]"), "")
            .takeIf { it.isNotBlank() } ?: "slide_image"

    private fun goBack() {
        if (_isDirty.value) onEvent(UIEvents.ShowUnsavedChangesDialog)
        else onEvent(UIEvents.NavigateBack)
    }

    private fun discardChanges() {
        val slideId = _uiState.value.slideId
        if (slideId != null) loadSlide(slideId) else clearData()
        onEvent(UIEvents.NavigateBack)
    }

    private fun onEvent(event: UIEvents) = viewModelScope.launch {
        _uiEvent.emit(event)
    }

    /**
     * Групує магазини по містах та сортує:
     * 1. Міста — за назвою (алфавітно)
     * 2. Магазини в кожному місті — за кодом (алфавітно)
     */
    private fun getGroupedShops(
        shops: List<Shop>,
        cities: List<City>
    ): List<ShopGroup> {
        // Створюємо map: cityId -> City
        val cityMap = cities.associateBy { it.id }

        return shops
            .groupBy { it.cityId }
            .mapNotNull { (cityId, shopsInCity) ->
                val city = cityMap[cityId] ?: return@mapNotNull null // якщо місто не знайдено — пропускаємо
                ShopGroup(
                    city = city,
                    shops = shopsInCity.sortedBy { it.street }
                )
            }
            .sortedBy { it.city.name } // сортування міст за назвою
    }
}
