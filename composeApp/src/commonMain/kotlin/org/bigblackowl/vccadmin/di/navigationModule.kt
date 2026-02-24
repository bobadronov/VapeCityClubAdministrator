package org.bigblackowl.vccadmin.di

import androidx.lifecycle.SavedStateHandle
import org.bigblackowl.vccadmin.navigation.NavigationViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val navigationModule = module {
    viewModel<NavigationViewModel> { (handle: SavedStateHandle) ->
        NavigationViewModel(
            authRepository = get(),
            addEditSlideViewModel = get(),
            shopAddEditScreenViewModel = get(),
            addEditCityScreenViewModel = get(),
            addEditUserScreenViewModel = get(),
            editSlidesSettingsScreenViewModel = get(),
            fileGeneratorScreenViewModel = get(),
            savedStateHandle = handle,
        )
    }
}