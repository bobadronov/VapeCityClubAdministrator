package org.bigblackowl.vccadmin.navigation

import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable
import org.bigblackowl.vccadmin.BuildConfig
import org.jetbrains.compose.resources.stringResource
import vccadministrator.composeapp.generated.resources.Res
import vccadministrator.composeapp.generated.resources.add_city
import vccadministrator.composeapp.generated.resources.add_shop
import vccadministrator.composeapp.generated.resources.add_slide
import vccadministrator.composeapp.generated.resources.cities
import vccadministrator.composeapp.generated.resources.edit_city
import vccadministrator.composeapp.generated.resources.edit_shop
import vccadministrator.composeapp.generated.resources.edit_slide
import vccadministrator.composeapp.generated.resources.login
import vccadministrator.composeapp.generated.resources.settings
import vccadministrator.composeapp.generated.resources.shop_details
import vccadministrator.composeapp.generated.resources.shops
import vccadministrator.composeapp.generated.resources.slides
import vccadministrator.composeapp.generated.resources.slides_ai_generation


@Composable
fun currentTitleFor(route: Route?): String = when (route) {
    is Route.Login -> stringResource(Res.string.login)
    is Route.Main -> stringResource(Res.string.shops)
    is Route.ShopDetails -> stringResource(Res.string.shop_details)

    is Route.AddEditShop ->
        if (route.shopID == null) stringResource(Res.string.add_shop) else stringResource(Res.string.edit_shop)

    is Route.SlidesList -> stringResource(Res.string.slides)
    is Route.AddEditSlide ->
        if (route.id == null) stringResource(Res.string.add_slide) else stringResource(Res.string.edit_slide)

    is Route.CityList -> stringResource(Res.string.cities)
    is Route.AddEditCity ->
        if (route.id == null) stringResource(Res.string.add_city) else stringResource(Res.string.edit_city)

    is Route.SlideAiGeneration -> stringResource(Res.string.slides_ai_generation)
    is Route.Settings -> stringResource(Res.string.settings)
    else -> BuildConfig.APP_NAME
}


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

    @Serializable
    data object Settings : Route
}