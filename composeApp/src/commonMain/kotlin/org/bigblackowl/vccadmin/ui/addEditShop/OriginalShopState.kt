package org.bigblackowl.vccadmin.ui.addEditShop

import org.bigblackowl.vccadmin.data.entity.DeviceType
import org.bigblackowl.vccadmin.data.entity.ShopStatus

data class OriginalShopState(
    val street: String = "",
    val houseNumber: String = "",
    val addressComment: String? = null, // НОВЕ
    val phoneNumber: String? = null,
    val selectedCityId: Int? = null,
    val status: ShopStatus = ShopStatus.ACTIVE,
    val statusComment: String? = null,
    val cameraCodes: List<String> = emptyList(),
    val internetProvider: String? = null,
    val internetProviderPersonalAccount: List<String> = emptyList(),
    val internetReplenishmentDay: Int = 1,
    val internetReplenishmentAmount: String? = null, // НОВЕ
    val remoteNumber: String? = null,
    val code: String = "",
    val deviceType: DeviceType = DeviceType.NONE,
)