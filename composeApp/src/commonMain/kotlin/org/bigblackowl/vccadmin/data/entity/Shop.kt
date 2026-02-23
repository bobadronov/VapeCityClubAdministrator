package org.bigblackowl.vccadmin.data.entity

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import org.bigblackowl.vccadmin.utils.AppStringProvider

/**
 * Data class, що представляє магазин з таблиці public.shops.
 * Використовуємо @Serializable для підтримки серіалізації/десеріалізації через kotlinx.serialization.
 * @SerialName вказує ім'я поля в JSON (snake_case, як у базі даних), тоді як у Kotlin використовуємо camelCase для кращої читабельності.
 * Типи полів адаптовано: UUID як String, bigint як Long, jsonb для camera_codes як List<String> (припускаємо масив кодів камер),
 * text[] для internet_provider_personal_account як List<String>.
 * Статус як String з можливими значеннями з CHECK constraint.
 */
@Serializable
data class SupabaseShop(
    @SerialName("city_id")
    val cityId: Int,

    @SerialName("street")
    val street: String,

    @SerialName("house_number")
    val houseNumber: String? = null,

    @SerialName("status")
    val status: ShopStatus,

    @SerialName("status_comment")
    val statusComment: String? = null,

    @SerialName("phone_number")
    val phoneNumber: String? = null,

    @SerialName("camera_codes")
    val cameraCodes: List<String>,

    @SerialName("last_modified")
    val lastModified: Long,

    @SerialName("last_modified_user_id")
    val lastModifiedUserId: String,

    @SerialName("internet_provider")
    val internetProvider: String? = null,

    @SerialName("internet_provider_personal_account")
    val internetProviderPersonalAccount: List<String>,

    @SerialName("internet_replenishment_day")
    val internetReplenishmentDay: Int? = null,

    @SerialName("remote_number")
    val remoteNumber: String? = null,

    @SerialName("id")
    val id: String,

    @SerialName("internet_replenishment_amount")
    val internetReplenishmentAmount: String? = null,

    @SerialName("address_comment")
    val addressComment: String? = null,

    @SerialName("code")
    val code: String,

    @SerialName("device_type")
    val deviceType: DeviceType,
)

data class Shop(
    val id: String,
    val code: String,
    val cityId: Int,                // Додано: потрібно для групування
    val cityName: String,           // Назва міста (з join або окремого запиту)
    val logoUrl: String,
    val street: String,
    val houseNumber: String,
    val addressComment: String,
    val phoneNumber: String,
    val status: ShopStatus,
    val statusComment: String,
    val internetProvider: String,
    val internetProviderPersonalAccount: List<String>,
    val internetReplenishmentDay: Int,
    val internetReplenishmentAmount: String,
    val remoteNumber: String,
    val cameraCodes: List<String>,
    val lastModified: String,
    val lastModifiedUser: String,
    val deviceType: DeviceType,
)

// Додаємо extension для конвертації з SupabaseShop + City
suspend fun List<SupabaseShop>.toUiShops(cities: List<City>): List<Shop> {
    return this.mapNotNull { supabaseShop ->
        val city = cities.find { it.id == supabaseShop.cityId } ?: return@mapNotNull null
        Shop(
            id = supabaseShop.id,
            code = supabaseShop.code,
            deviceType = supabaseShop.deviceType,
            cityId = supabaseShop.cityId,
            cityName = city.name,
            logoUrl = city.logoUrl.orEmpty(),
            street = supabaseShop.street,
            houseNumber = supabaseShop.houseNumber.orEmpty(),
            addressComment = supabaseShop.addressComment.orEmpty(),
            phoneNumber = supabaseShop.phoneNumber.orEmpty(),
            status = supabaseShop.status,
            statusComment = supabaseShop.statusComment.orEmpty(),
            internetProvider = supabaseShop.internetProvider.orEmpty(),
            internetProviderPersonalAccount = supabaseShop.internetProviderPersonalAccount,
            internetReplenishmentDay = supabaseShop.internetReplenishmentDay ?: 1,
            internetReplenishmentAmount = supabaseShop.internetReplenishmentAmount.orEmpty(),
            remoteNumber = supabaseShop.remoteNumber.orEmpty(),
            cameraCodes = supabaseShop.cameraCodes,
            lastModified = AppStringProvider.formatTimestamp(supabaseShop.lastModified), // ваша функція форматування
            lastModifiedUser = supabaseShop.lastModifiedUserId, // або мапінг на ім'я користувача

        )
    }
}

data class ShopStatusInfo(
    val index: Int,
    val status: ShopStatus,
    val name: String,
    val icon: ImageVector,
    val color: Color,
)

@Serializable
data class NewShop(
    // @SerialName("code") val code: String,
    @SerialName("city_id") val cityId: Int,
    @SerialName("street") val street: String,
    @SerialName("house_number") val houseNumber: String? = null,
    @SerialName("address_comment") val addressComment: String? = null, // НОВЕ
    @SerialName("status") val status: ShopStatus = ShopStatus.INACTIVE,
    @SerialName("status_comment") val statusComment: String? = null,
    @SerialName("phone_number") val phoneNumber: String? = null,
    @SerialName("camera_codes") val cameraCodes: JsonArray? = null,
    @SerialName("last_modified") val lastModified: Long,
    @SerialName("last_modified_user_id") val lastModifiedUserId: String,
    @SerialName("internet_provider") val internetProvider: String? = null,
    @SerialName("internet_provider_personal_account") val internetProviderPersonalAccount: JsonArray? = null,
    @SerialName("internet_replenishment_day") val internetReplenishmentDay: Int? = null,
    @SerialName("internet_replenishment_amount") val internetReplenishmentAmount: String? = null, // Double
    @SerialName("remote_number") val remoteNumber: String? = null,
    @SerialName("device_type") val deviceType: DeviceType = DeviceType.NONE,
)

@Immutable
data class ShopsFilter(
    val selectedCityIds: Set<Int> = emptySet(),
    val selectedStatuses: Set<ShopStatus> = emptySet(),
) {
    fun toggleCity(cityId: Int): ShopsFilter =
        copy(
            selectedCityIds = if (cityId in selectedCityIds) selectedCityIds - cityId else selectedCityIds + cityId
        )

    fun toggleStatus(status: ShopStatus): ShopsFilter =
        copy(
            selectedStatuses = if (status in selectedStatuses) selectedStatuses - status else selectedStatuses + status
        )

    fun clear(): ShopsFilter = ShopsFilter()
}