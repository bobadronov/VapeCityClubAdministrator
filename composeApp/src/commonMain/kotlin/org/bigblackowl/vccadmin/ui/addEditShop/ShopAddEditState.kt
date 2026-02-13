package org.bigblackowl.vccadmin.ui.addEditShop

import org.bigblackowl.vccadmin.data.entity.City
import org.bigblackowl.vccadmin.data.entity.DeviceType
import org.bigblackowl.vccadmin.data.entity.ShopStatus

// Модель стану UI
data class ShopAddEditState(
    val isLoading: Boolean = false,
    val shopId: String? = null,
    val cities: List<City> = emptyList(),

    val selectedCityId: Int? = null,
    val cityDropdownExpanded: Boolean = false,


    val street: String = "",
    val houseNumber: String = "",
    val addressComment: String? = null, // НОВЕ — коментар до адреси (МАФ, ТЦ тощо)

    val phoneNumber: String? = null,

    val status: ShopStatus = ShopStatus.ACTIVE,
    val statusDropdownExpanded: Boolean = false,
    val statusComment: String? = null,

    val cameraCodes: List<String>? = emptyList(),
    val cameraCodeInput: String = "",

    val internetProvider: String? = null,
    val internetProviderPersonalAccount: List<String>? = emptyList(),
    val internetProviderAccountInput: String = "",

    val internetReplenishmentDay: Int = 1,
    val internetReplenishmentAmount: String? = null, // НОВЕ — сума поповнення

    val remoteNumber: String? = null,

    val code: String = "",

    val deviceDropdownExpanded: Boolean = false,
    val deviceType: DeviceType = DeviceType.NONE,
)
