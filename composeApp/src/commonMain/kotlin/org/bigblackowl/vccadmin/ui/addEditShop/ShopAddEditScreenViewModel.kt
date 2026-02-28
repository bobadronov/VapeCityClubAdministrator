package org.bigblackowl.vccadmin.ui.addEditShop

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.aakira.napier.Napier
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import org.bigblackowl.vccadmin.data.entity.DeviceType
import org.bigblackowl.vccadmin.data.entity.NewShop
import org.bigblackowl.vccadmin.data.entity.ShopStatus
import org.bigblackowl.vccadmin.data.entity.SupabaseShop
import org.bigblackowl.vccadmin.data.errorManager.ErrorCode
import org.bigblackowl.vccadmin.data.errorManager.ErrorManager
import org.bigblackowl.vccadmin.data.events.UIEvents
import org.bigblackowl.vccadmin.data.utils.NetworkMonitorProvider
import org.bigblackowl.vccadmin.domain.repository.CityRepository
import org.bigblackowl.vccadmin.domain.repository.ShopRepository
import org.bigblackowl.vccadmin.theme.DefaultValues
import org.jetbrains.compose.resources.getString
import vccadministrator.composeapp.generated.resources.Res
import vccadministrator.composeapp.generated.resources.error_delete_shop
import vccadministrator.composeapp.generated.resources.error_enter_comment
import vccadministrator.composeapp.generated.resources.error_enter_house_number
import vccadministrator.composeapp.generated.resources.error_enter_street
import vccadministrator.composeapp.generated.resources.error_load_data
import vccadministrator.composeapp.generated.resources.error_save_shop
import vccadministrator.composeapp.generated.resources.error_select_city
import vccadministrator.composeapp.generated.resources.error_shop_id_not_found
import vccadministrator.composeapp.generated.resources.no_internet
import vccadministrator.composeapp.generated.resources.shop_added_success
import vccadministrator.composeapp.generated.resources.shop_updated_success
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
class ShopAddEditScreenViewModel(
    private val cityRepository: CityRepository,
    private val shopRepository: ShopRepository,
    private val networkMonitorProvider: NetworkMonitorProvider,
    private val errorManager: ErrorManager,
) : ViewModel() {
    private val _uiState = MutableStateFlow(ShopAddEditState())
    val uiState: StateFlow<ShopAddEditState> = _uiState.asStateFlow()

    private val _isDirty = MutableStateFlow(false)

    private val _uiEvent = MutableSharedFlow<UIEvents>(replay = 0)
    val uiEvent: SharedFlow<UIEvents> = _uiEvent.asSharedFlow()

    private var originalState: OriginalShopState? = null
    private var isEditModeInternal: Boolean = false

    private fun computeHasUnsavedChanges(): Boolean {
        val current = _uiState.value
        val orig = originalState
        return if (orig == null) {
            // Add mode
            current.street.isNotBlank() || current.houseNumber.isNotBlank() || current.addressComment?.isNotBlank() == true || // НОВЕ
                    current.phoneNumber?.isNotBlank() == true || current.selectedCityId != null || current.status != ShopStatus.ACTIVE || current.statusComment?.isNotBlank() == true || current.cameraCodes?.isNotEmpty() == true || current.internetProvider?.isNotBlank() == true || current.internetProviderPersonalAccount?.isNotEmpty() == true || current.internetReplenishmentDay != 1 || current.internetReplenishmentAmount != null || // НОВЕ
                    current.remoteNumber?.isNotBlank() == true
        } else {
            // Edit mode
            current.street != orig.street || current.houseNumber != orig.houseNumber || current.addressComment != orig.addressComment || // НОВЕ
                    current.phoneNumber != orig.phoneNumber || current.selectedCityId != orig.selectedCityId || current.status != orig.status || current.statusComment != orig.statusComment || current.cameraCodes != orig.cameraCodes || current.internetProvider != orig.internetProvider || current.internetProviderPersonalAccount != orig.internetProviderPersonalAccount || current.internetReplenishmentDay != orig.internetReplenishmentDay || current.internetReplenishmentAmount != orig.internetReplenishmentAmount || // НОВЕ
                    current.remoteNumber != orig.remoteNumber
        }
    }

    fun onIntent(intent: ShopAddEditIntent) = viewModelScope.launch {
        when (intent) {

            is ShopAddEditIntent.UpdateDeviceType -> {
                _uiState.update { it.copy(deviceType = intent.deviceType) }
                _isDirty.update { computeHasUnsavedChanges() }
            }

            is ShopAddEditIntent.UpdateStreet -> {
                _uiState.update { it.copy(street = intent.street) }
                _isDirty.update { computeHasUnsavedChanges() }
            }

            is ShopAddEditIntent.UpdateHouseNumber -> {
                _uiState.update { it.copy(houseNumber = intent.houseNumber.trim()) }
                _isDirty.update { computeHasUnsavedChanges() }
            }

            is ShopAddEditIntent.UpdatePhoneNumber -> {
                _uiState.update { it.copy(phoneNumber = intent.phoneNumber) }
                _isDirty.update { computeHasUnsavedChanges() }
            }

            is ShopAddEditIntent.UpdateSelectedCityId -> {
                _uiState.update { it.copy(selectedCityId = intent.cityId) }
                _isDirty.update { computeHasUnsavedChanges() }
            }

            is ShopAddEditIntent.UpdateCityDropdownExpanded -> {
                _uiState.update { it.copy(cityDropdownExpanded = intent.expanded) }
            }

            is ShopAddEditIntent.UpdateStatus -> {
                _uiState.update { it.copy(status = intent.status) }
                _isDirty.update { computeHasUnsavedChanges() }
            }

            is ShopAddEditIntent.UpdateStatusDropdownExpanded -> {
                _uiState.update { it.copy(statusDropdownExpanded = intent.expanded) }
                _isDirty.update { computeHasUnsavedChanges() }
            }

            is ShopAddEditIntent.UpdateStatusComment -> {
                _uiState.update { it.copy(statusComment = intent.comment) }
                _isDirty.update { computeHasUnsavedChanges() }
            }

            is ShopAddEditIntent.UpdateCameraCodeInput -> {
                _uiState.update { it.copy(cameraCodeInput = intent.code) }
                _isDirty.update { computeHasUnsavedChanges() }
            }

            is ShopAddEditIntent.AddCameraCode -> {
                if (intent.code.isNotBlank()) {
                    _uiState.update { it.copy(cameraCodes = it.cameraCodes?.plus(intent.code.trim()), cameraCodeInput = "") }
                    _isDirty.update { computeHasUnsavedChanges() }
                }
            }

            is ShopAddEditIntent.RemoveCameraCode -> {
                _uiState.update { it.copy(cameraCodes = it.cameraCodes?.minus(intent.code)) }
                _isDirty.update { computeHasUnsavedChanges() }
            }

            is ShopAddEditIntent.UpdateAddressComment -> {
                _uiState.update { it.copy(addressComment = intent.comment) }
                _isDirty.update { computeHasUnsavedChanges() }
            }

            is ShopAddEditIntent.UpdateInternetReplenishmentAmountInput -> {
                _uiState.update { it.copy(internetReplenishmentAmount = filterDecimalInput(intent.amount)) }
                _isDirty.update { computeHasUnsavedChanges() }
            }

            is ShopAddEditIntent.UpdateInternetProvider -> {
                _uiState.update { it.copy(internetProvider = intent.provider) }
                _isDirty.update { computeHasUnsavedChanges() }
            }

            is ShopAddEditIntent.UpdateInternetReplenishmentDay -> {
                _uiState.update { it.copy(internetReplenishmentDay = intent.day) }
                _isDirty.update { computeHasUnsavedChanges() }
            }

            is ShopAddEditIntent.ValidateAndSaveShopAddEdit -> {
                validateAndSaveShop()
            }

            is ShopAddEditIntent.DeleteShopAddEdit -> {
                deleteShop(intent.shopId)
            }

            is ShopAddEditIntent.UpdateInternetProviderAccountInput -> {
                _uiState.update { it.copy(internetProviderAccountInput = intent.account.trim()) }
                _isDirty.update { computeHasUnsavedChanges() }
            }


            is ShopAddEditIntent.AddInternetProviderPersonalAccount -> {
                if (intent.account.isNotBlank()) {
                    _uiState.update {
                        it.copy(
                            internetProviderPersonalAccount = it.internetProviderPersonalAccount?.plus(intent.account.trim()),
                            internetProviderAccountInput = "",
                        )
                    }
                    _isDirty.update { computeHasUnsavedChanges() }
                }
            }

            is ShopAddEditIntent.RemoveInternetProviderPersonalAccount -> {
                _uiState.update {
                    it.copy(
                        internetProviderPersonalAccount = it.internetProviderPersonalAccount?.minus(
                            intent.account
                        )
                    )
                }
                _isDirty.update { computeHasUnsavedChanges() }
            }

            is ShopAddEditIntent.UpdateRemoteNumberInput -> {
                _uiState.update { it.copy(remoteNumber = intent.remoteNumber) }
                _isDirty.update { computeHasUnsavedChanges() }
            }

            is ShopAddEditIntent.LoadShopAddDetails -> {
                loadShopDetails(intent.shopId)
            }

            is ShopAddEditIntent.ClearData -> {
                originalState = null
                isEditModeInternal = false
                _uiState.update { ShopAddEditState() }
                _isDirty.update { false }
            }

            is ShopAddEditIntent.DiscardChanges -> {
                if (isEditModeInternal && originalState != null) {
                    val orig = originalState!!
                    _uiState.update {
                        it.copy(
                            street = orig.street,
                            houseNumber = orig.houseNumber,
                            addressComment = orig.addressComment,
                            phoneNumber = orig.phoneNumber,
                            selectedCityId = orig.selectedCityId,
                            status = orig.status,
                            statusComment = orig.statusComment,
                            cameraCodes = orig.cameraCodes,
                            cameraCodeInput = "",
                            internetProvider = orig.internetProvider,
                            internetProviderPersonalAccount = orig.internetProviderPersonalAccount,
                            internetProviderAccountInput = "",
                            internetReplenishmentDay = orig.internetReplenishmentDay,
                            internetReplenishmentAmount = orig.internetReplenishmentAmount,
                            remoteNumber = orig.remoteNumber
                        )
                    }
                } else {
                    _uiState.update { ShopAddEditState() }
                }
                _isDirty.value = false
                _uiEvent.emit(UIEvents.NavigateBack)
            }

            is ShopAddEditIntent.Dismiss -> {
                if (computeHasUnsavedChanges()) {
                    _uiEvent.emit(UIEvents.ShowUnsavedChangesDialog)
                } else {
                    _uiEvent.emit(UIEvents.NavigateBack)
                }
            }

            is ShopAddEditIntent.Init -> {
                viewModelScope.launch {
                    val cityList = cityRepository.getCities()
                    _uiState.update { state ->
                        state.copy(
                            cities = cityList,
                        )
                    }
                }
            }

            is ShopAddEditIntent.GoBack -> {
                if (computeHasUnsavedChanges()) {
                    _uiEvent.emit(UIEvents.ShowUnsavedChangesDialog)
                } else {
                    _uiEvent.emit(UIEvents.NavigateBack)
                    originalState = null
                    isEditModeInternal = false
                    _uiState.update { ShopAddEditState() }
                    _isDirty.update { false }
                }
            }

            is ShopAddEditIntent.UpdateDeviceDropdownExpanded -> {
                _uiState.update { it.copy(deviceDropdownExpanded = intent.expanded) }
            }
        }
    }

    private fun filterDecimalInput(input: String): String {
        // лише цифри + одна крапка
        val cleaned = input.replace(Regex("[^0-9.]"), "")

        // не більше однієї крапки
        val parts = cleaned.split(".")
        if (parts.size > 2) return parts[0] + "." + parts[1]

        // максимум 2 знаки після точки
        return if (parts.size == 2) {
            parts[0] + "." + parts[1].take(2)
        } else {
            cleaned
        }
    }

    private fun loadShopDetails(shopId: String) {
        viewModelScope.launch {
            if (networkMonitorProvider.isConnected.value.not()) {
                showMessage(message = getString(Res.string.no_internet))
                return@launch
            }
            try {
                _uiState.update { it.copy(isLoading = true) }
                val shop = shopRepository.getShopById(shopId)
                originalState = OriginalShopState(
                    street = shop?.street.orEmpty(),
                    houseNumber = shop?.houseNumber.orEmpty(),
                    phoneNumber = shop?.phoneNumber.orEmpty(),
                    selectedCityId = shop?.cityId,
                    addressComment = shop?.addressComment, // НОВЕ
                    status = shop?.status ?: ShopStatus.INACTIVE,
                    statusComment = shop?.statusComment,
                    cameraCodes = shop?.cameraCodes.orEmpty(),
                    internetProvider = shop?.internetProvider,
                    internetProviderPersonalAccount = shop?.internetProviderPersonalAccount ?: emptyList(),
                    internetReplenishmentAmount = shop?.internetReplenishmentAmount, // НОВЕ
                    internetReplenishmentDay = shop?.internetReplenishmentDay ?: 1,
                    remoteNumber = shop?.remoteNumber.orEmpty(),
                    code = shop?.code.orEmpty(),
                    deviceType = shop?.deviceType ?: DeviceType.NONE,
                )
                isEditModeInternal = true
                _uiState.update { state ->
                    state.copy(
                        shopId = shop?.id,
                        street = shop?.street.orEmpty(),
                        houseNumber = shop?.houseNumber.orEmpty(),
                        addressComment = shop?.addressComment,
                        phoneNumber = shop?.phoneNumber.orEmpty(),
                        selectedCityId = shop?.cityId,
                        status = shop?.status ?: ShopStatus.INACTIVE,
                        statusComment = shop?.statusComment,
                        cameraCodes = shop?.cameraCodes.orEmpty(),
                        cameraCodeInput = "",
                        internetProvider = shop?.internetProvider.orEmpty(),
                        internetProviderPersonalAccount = shop?.internetProviderPersonalAccount ?: emptyList(),
                        internetProviderAccountInput = "",
                        internetReplenishmentDay = shop?.internetReplenishmentDay ?: 1,
                        internetReplenishmentAmount = shop?.internetReplenishmentAmount,
                        remoteNumber = shop?.remoteNumber.orEmpty(),
                        isLoading = false,
                        code = shop?.code.orEmpty(),
                        deviceType = shop?.deviceType ?: DeviceType.NONE,
                    )
                }
                _isDirty.value = false
            } catch (exception: Exception) {
                showMessage(message = "${getString(Res.string.error_load_data)}: ${exception.message}")
                errorManager.report(message = exception.message.orEmpty(), errorCode = ErrorCode.ShopAddEditScreenViewModel.LOAD_SHOP_DETAILS)
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    private fun deleteShop(shopId: String?) {
        viewModelScope.launch {
            if (networkMonitorProvider.isConnected.value.not()) {
                showMessage(message = getString(Res.string.no_internet))
                return@launch
            }
            if (shopId == null) {
                showMessage(message = getString(Res.string.error_shop_id_not_found))
                return@launch
            }
            try {
                _uiState.update { it.copy(isLoading = true) }
                shopRepository.deleteShop(shopId)
                delay(1.seconds)
                originalState = null
                isEditModeInternal = false
                _isDirty.value = false
                _uiEvent.emit(UIEvents.NavigateBack)
            } catch (exception: Exception) {
                showMessage(message = "${getString(Res.string.error_delete_shop)}: ${exception.message}")
                errorManager.report(message = exception.message.orEmpty(), errorCode = ErrorCode.ShopAddEditScreenViewModel.DELETE_SHOP)
                Napier.e("Error deleting shop: ${exception.message}", tag = "ShopEditScreenViewModel")
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    private fun validateAndSaveShop() {
        viewModelScope.launch {
            try {
                if (networkMonitorProvider.isConnected.value.not()) {
                    showMessage(message = getString(Res.string.no_internet))
                    return@launch
                }
                _uiState.update { it.copy(isLoading = true) }
                val state = _uiState.value
                val errorMessage = when {
                    state.street.isBlank() -> getString(Res.string.error_enter_street)
                    state.houseNumber.isBlank() -> getString(Res.string.error_enter_house_number)
                    state.selectedCityId == null -> getString(Res.string.error_select_city)
                    (state.status == ShopStatus.RELOCATING || state.status == ShopStatus.UNDER_REPAIR) && state.statusComment?.isBlank() == true -> getString(Res.string.error_enter_comment)
                    else -> null
                }

                if (errorMessage != null) {
                    showMessage(message = errorMessage)
                    return@launch
                }

                val userId = shopRepository.getCurrentUserId()

                if (state.shopId == null) {
                    val shopToInsert = NewShop(
                        cityId = state.selectedCityId!!,
                        street = state.street.trim(),
                        houseNumber = state.houseNumber.trim(),
                        phoneNumber = state.phoneNumber,
                        status = state.status,
                        statusComment = if (state.statusComment.isNullOrBlank()) null else state.statusComment,
                        cameraCodes = state.cameraCodes?.let { codes ->
                            buildJsonArray { codes.forEach { add(it) } }
                        },
                        lastModified = DefaultValues.Time.now,
                        lastModifiedUserId = userId.orEmpty(),
                        internetProvider = if (state.internetProvider.isNullOrBlank()) null else state.internetProvider,
                        internetProviderPersonalAccount = state.internetProviderPersonalAccount?.let { accounts ->
                            buildJsonArray { accounts.forEach { add(it) } }
                        },
                        addressComment = if (state.addressComment.isNullOrBlank()) null else state.addressComment,
                        internetReplenishmentAmount = if (state.internetReplenishmentAmount.isNullOrBlank()) null else state.internetReplenishmentAmount,
                        internetReplenishmentDay = state.internetReplenishmentDay,
                        remoteNumber = if (state.remoteNumber.isNullOrBlank()) null else state.remoteNumber,
                        deviceType = state.deviceType,
                    )

                    shopRepository.addShop(shopToInsert)
                    showMessage(message = getString(Res.string.shop_added_success))
                    originalState = null
                    isEditModeInternal = false
                    _uiState.update { ShopAddEditState() }
                    _isDirty.value = false
                    _uiEvent.emit(UIEvents.NavigateBack)
                } else {
                    val shopToInsert = SupabaseShop(
                        id = state.shopId,
                        cityId = state.selectedCityId!!,
                        street = state.street.trim(),
                        houseNumber = state.houseNumber.trim(),
                        addressComment = if (state.addressComment.isNullOrBlank()) null else state.addressComment,
                        phoneNumber = state.phoneNumber,
                        status = state.status,
                        statusComment = if (state.statusComment.isNullOrBlank()) null else state.statusComment,
                        cameraCodes = state.cameraCodes.orEmpty(),
                        lastModified = DefaultValues.Time.now,
                        lastModifiedUserId = userId.orEmpty(),
                        internetProvider = if (state.internetProvider.isNullOrBlank()) null else state.internetProvider,
                        internetProviderPersonalAccount = state.internetProviderPersonalAccount.orEmpty(),
                        internetReplenishmentAmount = state.internetReplenishmentAmount,
                        internetReplenishmentDay = state.internetReplenishmentDay,
                        remoteNumber = if (state.remoteNumber.isNullOrBlank()) null else state.remoteNumber,
                        code = state.code,
                        deviceType = state.deviceType,
                    )
                    shopRepository.updateShop(shopToInsert)
                    originalState = OriginalShopState(
                        street = state.street.trim(),
                        houseNumber = state.houseNumber.trim(),
                        addressComment = state.addressComment,
                        phoneNumber = state.phoneNumber,
                        status = state.status,
                        statusComment = state.statusComment,
                        cameraCodes = state.cameraCodes.orEmpty(),
                        internetProvider = if (state.internetProvider.isNullOrBlank()) null else state.internetProvider,
                        internetProviderPersonalAccount = state.internetProviderPersonalAccount.orEmpty(),
                        internetReplenishmentAmount = state.internetReplenishmentAmount,
                        internetReplenishmentDay = state.internetReplenishmentDay,
                        remoteNumber = if (state.remoteNumber.isNullOrBlank()) null else state.remoteNumber,
                        deviceType = state.deviceType,

                        )
                    _isDirty.value = false
                    showMessage(message = getString(Res.string.shop_updated_success))
                    _uiEvent.emit(UIEvents.NavigateBack)
                }
            } catch (exception: Exception) {
                showMessage(message = "${getString(Res.string.error_save_shop)}: ${exception.stackTraceToString()}")
                errorManager.report(message = exception.message.orEmpty(), errorCode = ErrorCode.ShopAddEditScreenViewModel.SAVE_SHOP)
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    private fun showMessage(message: String) = viewModelScope.launch {
        Napier.d(tag = "ShopAddEditScreenViewModel") { message }
        _uiEvent.emit(UIEvents.ShowMessage(message))
    }
}