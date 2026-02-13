package org.bigblackowl.vccadmin.data.utils

import org.bigblackowl.vccadmin.data.entity.City
import org.bigblackowl.vccadmin.data.entity.Shop

/**
 * Групує магазини по містах та сортує:
 * 1. Міста — за назвою (алфавітно)
 * 2. Магазини в кожному місті — за кодом (алфавітно)
 */
fun getGroupedShops(
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

data class ShopGroup(
    val city: City,
    val shops: List<Shop>
)