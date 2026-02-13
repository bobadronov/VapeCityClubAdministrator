package org.bigblackowl.vccadmin.ui.city.addEdit

import io.github.vinceglb.filekit.PlatformFile
import org.bigblackowl.vccadmin.data.entity.City

data class AddEditCityScreenUiState(
    val isLoading: Boolean = false,
    val selectedCity: City? = null,
    val initialName: String = "",
    val newCityName: String = "",
    val newCityLogoFile: PlatformFile? = null,
)