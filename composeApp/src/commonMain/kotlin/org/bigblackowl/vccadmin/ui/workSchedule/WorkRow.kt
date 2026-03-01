package org.bigblackowl.vccadmin.ui.workSchedule

sealed interface WorkRow {
    data class CityHeader(val cityName: String) : WorkRow
    data class Shop(val shopId: String) : WorkRow
}