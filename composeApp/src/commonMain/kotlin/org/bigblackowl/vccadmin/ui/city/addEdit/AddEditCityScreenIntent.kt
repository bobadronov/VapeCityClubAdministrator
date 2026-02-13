package org.bigblackowl.vccadmin.ui.city.addEdit

import org.bigblackowl.vccadmin.data.entity.City

sealed interface AddEditCityScreenIntent {
    object Save : AddEditCityScreenIntent
    object DiscardAndBack : AddEditCityScreenIntent
    object GoBack : AddEditCityScreenIntent
    data class GetCity(val cityId: Int) : AddEditCityScreenIntent
    data class EditName(val newName: String) : AddEditCityScreenIntent
    object EditLogo : AddEditCityScreenIntent
    data class DeleteCity(val city: City) : AddEditCityScreenIntent
    object Clear : AddEditCityScreenIntent
}