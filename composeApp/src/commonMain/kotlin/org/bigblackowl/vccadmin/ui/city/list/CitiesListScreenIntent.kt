package org.bigblackowl.vccadmin.ui.city.list

sealed interface CitiesListScreenIntent {
    object Load : CitiesListScreenIntent
    object Refresh : CitiesListScreenIntent
}

