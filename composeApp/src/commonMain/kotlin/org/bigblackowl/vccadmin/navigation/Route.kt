package org.bigblackowl.vccadmin.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

// Маршрути навігації:
// використовуємо @Serializable для збереження стану.
@Serializable
sealed interface Route : NavKey {

    @Serializable
    data object Login : Route

    @Serializable
    data object Main : Route

    @Serializable
    data class ShopDetails(val shopID: String) : Route

    @Serializable
    data class AddEditShop(val shopID: String? = null) : Route

    @Serializable
    data object SlidesList : Route

    @Serializable
    data class AddEditSlide(val id: String? = null) : Route

    @Serializable
    data object EditSlidesSettings : Route

    @Serializable
    data object CityList : Route

    @Serializable
    data class AddEditCity(val id: Int? = null) : Route

    @Serializable
    data object UsersList : Route

    @Serializable
    data class UserDetail(val userId: String? = null) : Route

    @Serializable
    data class AddEditUser(val userId: String? = null) : Route

    @Serializable
    data object FileGenerator : Route

    @Serializable
    data object SlideAiGeneration : Route
}