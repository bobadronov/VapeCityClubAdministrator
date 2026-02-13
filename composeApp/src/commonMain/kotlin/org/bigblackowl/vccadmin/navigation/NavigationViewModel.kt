package org.bigblackowl.vccadmin.navigation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.launch
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import org.bigblackowl.vccadmin.data.repository.AuthRepository
import org.bigblackowl.vccadmin.ui.addEditShop.ShopAddEditIntent
import org.bigblackowl.vccadmin.ui.addEditShop.ShopAddEditScreenViewModel
import org.bigblackowl.vccadmin.ui.addEditSlideScreen.AddEditSlideIntent
import org.bigblackowl.vccadmin.ui.addEditSlideScreen.AddEditSlideViewModel
import org.bigblackowl.vccadmin.ui.city.addEdit.AddEditCityScreenIntent
import org.bigblackowl.vccadmin.ui.city.addEdit.AddEditCityScreenViewModel
import org.bigblackowl.vccadmin.ui.editSlidesSettings.EditSlidesSettingsScreenViewModel
import org.bigblackowl.vccadmin.ui.editSlidesSettings.SlidesSettingsIntent
import org.bigblackowl.vccadmin.ui.fileGenerator.FileGenerationIntent
import org.bigblackowl.vccadmin.ui.fileGenerator.FileGeneratorScreenViewModel
import org.bigblackowl.vccadmin.ui.users.addEdit.AddEditUserScreenIntent
import org.bigblackowl.vccadmin.ui.users.addEdit.AddEditUserScreenViewModel

class NavigationViewModel(
    private val authRepository: AuthRepository,
    private val addEditSlideViewModel: AddEditSlideViewModel,
    private val shopAddEditScreenViewModel: ShopAddEditScreenViewModel,
    private val addEditCityScreenViewModel: AddEditCityScreenViewModel,
    private val addEditUserScreenViewModel: AddEditUserScreenViewModel,
    private val editSlidesSettingsScreenViewModel: EditSlidesSettingsScreenViewModel,
    private val fileGeneratorScreenViewModel: FileGeneratorScreenViewModel,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private companion object {
        const val STACK_KEY = "nav_back_stack"
    }

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        classDiscriminator = "type"
    }

    var backStack by mutableStateOf(loadStack())
        private set

    private fun loadStack(): List<Route> {
        val raw = savedStateHandle.get<String>(STACK_KEY) ?: return listOf(Route.Login)
        return runCatching {
            json.decodeFromString(ListSerializer(Route.serializer()), raw)
        }.getOrElse { listOf(Route.Login) }
    }

    private fun persist() {
        savedStateHandle[STACK_KEY] =
            json.encodeToString(ListSerializer(Route.serializer()), backStack)
    }

    init {
        viewModelScope.launch {
            authRepository.sessionStatus.collect { sessionStatus ->
                when (sessionStatus) {
                    is SessionStatus.NotAuthenticated -> replaceRoot(Route.Login)

                    is SessionStatus.Authenticated -> {
                        val last = backStack.lastOrNull()
                        val hasMeaningfulStack = backStack.size > 1 || (last != null && last != Route.Login)
                        if (!hasMeaningfulStack) replaceRoot(Route.Main) else persist()
                    }

                    else -> Unit
                }
            }
        }
    }

    fun navigateTo(route: Route) {
        val last = backStack.lastOrNull()
        backStack = when {
            last == null -> listOf(route)
            shouldReplaceSameScreen(last, route) -> backStack.dropLast(1) + route
            last == route -> backStack
            else -> backStack + route
        }
        persist()
    }

    fun popBackStack() {
        backStack = backStack.dropLast(1).ifEmpty { listOf(Route.Login) }
        persist()
    }

    fun requestBack() {
        when (backStack.lastOrNull()) {
            is Route.AddEditSlide -> addEditSlideViewModel.onIntent(AddEditSlideIntent.GoBack)
            is Route.EditSlidesSettings -> editSlidesSettingsScreenViewModel.onIntent(SlidesSettingsIntent.GoBack)
            is Route.AddEditCity -> addEditCityScreenViewModel.onIntent(AddEditCityScreenIntent.GoBack)
            is Route.AddEditUser -> addEditUserScreenViewModel.onIntent(AddEditUserScreenIntent.GoBack)
            is Route.AddEditShop -> shopAddEditScreenViewModel.onIntent(ShopAddEditIntent.GoBack)
            is Route.FileGenerator -> fileGeneratorScreenViewModel.onIntent(FileGenerationIntent.GoBack)
            else -> popBackStack()
        }
    }

    fun logout() = replaceRoot(Route.Login)

    private fun replaceRoot(route: Route) {
        backStack = listOf(route)
        persist()
    }

    private fun shouldReplaceSameScreen(from: Route, to: Route): Boolean =
        (from is Route.ShopDetails && to is Route.ShopDetails) ||
                (from is Route.UserDetail && to is Route.UserDetail) ||
                (from is Route.AddEditSlide && to is Route.AddEditSlide) ||
                (from is Route.AddEditShop && to is Route.AddEditShop) ||
                (from is Route.AddEditUser && to is Route.AddEditUser) ||
                (from is Route.AddEditCity && to is Route.AddEditCity)
}