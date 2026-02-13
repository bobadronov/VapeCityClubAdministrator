package org.bigblackowl.vccadmin.ui.addEditShop

import org.bigblackowl.vccadmin.data.entity.DeviceType
import org.bigblackowl.vccadmin.data.entity.ShopStatus

// Наміри для обробки подій
sealed interface ShopAddEditIntent {

    object Init : ShopAddEditIntent
    object Dismiss : ShopAddEditIntent
    object ClearData : ShopAddEditIntent
    object DiscardChanges : ShopAddEditIntent
    data class LoadShopAddDetails(val shopId: String) : ShopAddEditIntent
    data class UpdateStreet(val street: String) : ShopAddEditIntent
    data class UpdateHouseNumber(val houseNumber: String) : ShopAddEditIntent
    data class UpdateAddressComment(val comment: String) : ShopAddEditIntent
    data class UpdatePhoneNumber(val phoneNumber: String) : ShopAddEditIntent
    data class UpdateDeviceType(val deviceType: DeviceType) : ShopAddEditIntent
    data class UpdateDeviceDropdownExpanded(val expanded: Boolean) : ShopAddEditIntent
    data class UpdateSelectedCityId(val cityId: Int) : ShopAddEditIntent
    data class UpdateCityDropdownExpanded(val expanded: Boolean) : ShopAddEditIntent
    data class UpdateStatus(val status: ShopStatus) : ShopAddEditIntent
    data class UpdateStatusDropdownExpanded(val expanded: Boolean) : ShopAddEditIntent
    data class UpdateStatusComment(val comment: String) : ShopAddEditIntent
    data class UpdateCameraCodeInput(val code: String) : ShopAddEditIntent
    data class AddCameraCode(val code: String) : ShopAddEditIntent
    data class RemoveCameraCode(val code: String) : ShopAddEditIntent
    data class UpdateInternetProvider(val provider: String) : ShopAddEditIntent
    data class UpdateInternetProviderAccountInput(val account: String) : ShopAddEditIntent
    data class AddInternetProviderPersonalAccount(val account: String) : ShopAddEditIntent
    data class RemoveInternetProviderPersonalAccount(val account: String) : ShopAddEditIntent
    data class UpdateInternetReplenishmentDay(val day: Int) : ShopAddEditIntent
    data class UpdateInternetReplenishmentAmountInput(val amount: String) : ShopAddEditIntent
    data class UpdateRemoteNumberInput(val remoteNumber: String) : ShopAddEditIntent
    object ValidateAndSaveShopAddEdit : ShopAddEditIntent
    data class DeleteShopAddEdit(val shopId: String?) : ShopAddEditIntent
    object GoBack : ShopAddEditIntent
}