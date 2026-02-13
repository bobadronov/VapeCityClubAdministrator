// File: src/commonMain/kotlin/org/bigblackowl/vccadmin/ui/shopDetail/ShopDetailsViewModel.kt
package org.bigblackowl.vccadmin.ui.shopDetail

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
import org.bigblackowl.vccadmin.data.entity.Shop
import org.bigblackowl.vccadmin.data.entity.UserRole
import org.bigblackowl.vccadmin.data.errorManager.ErrorCode
import org.bigblackowl.vccadmin.data.errorManager.ErrorManager
import org.bigblackowl.vccadmin.data.repository.CityRepository
import org.bigblackowl.vccadmin.data.repository.NetworkMonitorProvider
import org.bigblackowl.vccadmin.data.repository.ShopRepository
import org.bigblackowl.vccadmin.data.repository.UserRepository
import org.bigblackowl.vccadmin.data.state.UIEvents
import org.bigblackowl.vccadmin.utils.PlatformFileProvider
import org.bigblackowl.vccadmin.utils.formatTimestamp
import org.bigblackowl.vccadmin.utils.withRetry
import org.jetbrains.compose.resources.getString
import org.koin.core.component.KoinComponent
import vccadministrator.composeapp.generated.resources.Res
import vccadministrator.composeapp.generated.resources.error_load_shop
import vccadministrator.composeapp.generated.resources.no_internet
import vccadministrator.composeapp.generated.resources.not_specified
import vccadministrator.composeapp.generated.resources.shop_not_found

class ShopDetailsScreenViewModel(
    private val shopRepository: ShopRepository,
    private val cityRepository: CityRepository,
    private val userRepository: UserRepository,
    private val networkMonitorProvider: NetworkMonitorProvider,
    private val errorManager: ErrorManager,
) : ViewModel(), KoinComponent {

    private val _uiEvent = MutableSharedFlow<UIEvents>(replay = 0)
    val uiEvent: SharedFlow<UIEvents> = _uiEvent.asSharedFlow()

    private val _uiState = MutableStateFlow(ShopDetailsUiState())
    val uiState: StateFlow<ShopDetailsUiState> = _uiState.asStateFlow()

    fun onIntent(intent: ShopDetailsScreenIntent) {
        when (intent) {
            is ShopDetailsScreenIntent.Load -> loadShop(intent.id)
            is ShopDetailsScreenIntent.Refresh -> loadShop(intent.id)
            is ShopDetailsScreenIntent.ShareShop -> shareWithTelegram(intent.data)
        }
    }

    private fun loadShop(shopId: String) {
        val hasData = _uiState.value.shop != null

        _uiState.update {
            it.copy(
                isInitialLoading = !hasData,
                isRefreshing = hasData,
            )
        }

        viewModelScope.launch {
            if (!networkMonitorProvider.isConnected.value) {
                _uiEvent.emit(UIEvents.ShowMessage(getString(Res.string.no_internet)))
                _uiState.update { it.copy(isInitialLoading = false, isRefreshing = false) }
                return@launch
            }

            try {
                val shop = withRetry {
                    val sShop = shopRepository.getShopById(shopId)
                    val city = cityRepository.getCities().find { it.id == sShop?.cityId }

                    if (sShop != null) {
                        Shop(
                            cityName = city?.name ?: getString(Res.string.not_specified),
                            logoUrl = city?.logoUrl ?: getString(Res.string.not_specified),
                            street = sShop.street,
                            houseNumber = sShop.houseNumber ?: getString(Res.string.not_specified),
                            status = sShop.status,
                            statusComment = sShop.statusComment ?: getString(Res.string.not_specified),
                            phoneNumber = sShop.phoneNumber ?: getString(Res.string.not_specified),
                            cameraCodes = sShop.cameraCodes,
                            lastModified = formatTimestamp(sShop.lastModified),
                            lastModifiedUser = userRepository.getUserNameById(sShop.lastModifiedUserId),
                            internetProvider = sShop.internetProvider ?: getString(Res.string.not_specified),
                            internetProviderPersonalAccount = sShop.internetProviderPersonalAccount,
                            internetReplenishmentDay = sShop.internetReplenishmentDay ?: 1,
                            remoteNumber = sShop.remoteNumber ?: getString(Res.string.not_specified),
                            id = sShop.id,
                            internetReplenishmentAmount = sShop.internetReplenishmentAmount ?: "0",
                            addressComment = sShop.addressComment ?: getString(Res.string.not_specified),
                            code = sShop.code,
                            cityId = sShop.cityId,
                            deviceType = sShop.deviceType,
                        )
                    } else {
                        throw Exception(getString(Res.string.shop_not_found))
                    }
                }

                _uiState.update {
                    it.copy(
                        shop = shop,
                        userRole = userRepository.getCurrentUser()?.role ?: UserRole.USER,
                    )
                }
            } catch (exception: Exception) {
                _uiEvent.emit(UIEvents.ShowMessage(exception.message ?: getString(Res.string.error_load_shop)))
                errorManager.report(message = exception.message.orEmpty(), errorCode = ErrorCode.ShopDetailsScreenViewModel.LOAD_SHOP)

            } finally {
                _uiState.update { it.copy(isInitialLoading = false, isRefreshing = false) }
            }
        }
    }

    private fun shareWithTelegram(data: String) = viewModelScope.launch {
        PlatformFileProvider.shareWithTelegram(data)
    }
}
