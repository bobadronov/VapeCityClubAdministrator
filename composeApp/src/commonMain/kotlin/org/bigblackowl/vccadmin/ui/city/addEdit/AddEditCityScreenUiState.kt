package org.bigblackowl.vccadmin.ui.city.addEdit

import io.github.vinceglb.filekit.PlatformFile
import org.bigblackowl.vccadmin.data.entity.City
import org.bigblackowl.vccadmin.data.entity.SuggestionCityDto

data class AddEditCityScreenUiState(
    val isLoading: Boolean = false,
    val selectedCity: City? = null,
    val initialName: String = "",
    val newCityName: String = "",
    val newCityLogoFile: PlatformFile? = null,
)

data class CitySuggestion(
    val city: SuggestionCityDto,
    val exists: Boolean,
) {
    val key: String get() = "${city.oblast}|${city.name}"
}

data class CityAutocompleteUiState(
    val isLoading: Boolean = false,
    val suggestions: List<CitySuggestion> = emptyList(),
    val highlightedIndex: Int = -1,
)