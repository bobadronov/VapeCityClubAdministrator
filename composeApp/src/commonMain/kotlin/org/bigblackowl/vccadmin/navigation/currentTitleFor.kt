package org.bigblackowl.vccadmin.navigation

import androidx.compose.runtime.Composable
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
    else -> BuildConfig.APP_NAME
}