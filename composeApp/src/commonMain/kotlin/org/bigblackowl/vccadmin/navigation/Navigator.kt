package org.bigblackowl.vccadmin.navigation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.scene.Scene
import androidx.navigation3.scene.SceneStrategy
import androidx.navigation3.scene.SceneStrategyScope
import androidx.navigation3.ui.NavDisplay
import androidx.window.core.layout.WindowSizeClass
import io.github.aakira.napier.Napier
import org.bigblackowl.vccadmin.ui.addEditShop.ShopAddEditScreen
import org.bigblackowl.vccadmin.ui.addEditSlideScreen.AddEditSlideScreen
import org.bigblackowl.vccadmin.ui.city.addEdit.AddEditCityScreen
import org.bigblackowl.vccadmin.ui.city.list.CitiesListScreen
import org.bigblackowl.vccadmin.ui.editSlidesSettings.EditSlidesSettingsScreen
import org.bigblackowl.vccadmin.ui.fileGenerator.FileGenerationScreen
import org.bigblackowl.vccadmin.ui.login.LoginScreen
import org.bigblackowl.vccadmin.ui.main.MainScreen
import org.bigblackowl.vccadmin.ui.shopDetail.ShopDetailsScreen
import org.bigblackowl.vccadmin.ui.slideAiGeneration.SlideAiGenerationScreen
import org.bigblackowl.vccadmin.ui.slidesList.SlidesListScreen
import org.bigblackowl.vccadmin.ui.users.addEdit.AddEditUserScreen
import org.bigblackowl.vccadmin.ui.users.detail.UserDetailScreen
import org.bigblackowl.vccadmin.ui.users.list.UsersScreen

@Composable
fun Navigator(
    padding: PaddingValues,
    snackbarHostState: SnackbarHostState,
    navigationViewModel: NavigationViewModel,
) {

    val sceneStrategy = rememberSceneStrategy<Route>()

    NavDisplay(
        backStack = navigationViewModel.backStack,
        modifier = Modifier.padding(padding).consumeWindowInsets(WindowInsets.statusBars),
        onBack = navigationViewModel::popBackStack,
        sceneStrategy = sceneStrategy,
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator(),
        ),
        entryProvider = entryProvider {

            entry<Route.Login> {
                LoginScreen(snackbarHostState = snackbarHostState)
            }

            entry<Route.Main>(
                metadata = ListDetailSceneStrategy.listPane()
            ) {
                MainScreen(
                    snackbarHostState = snackbarHostState,
                    navigationViewModel = navigationViewModel,
                )
            }

            entry<Route.SlidesList>(
                metadata = ListDetailSceneStrategy.listPane()
            ) {
                SlidesListScreen(
                    snackbarHostState = snackbarHostState,
                    navigationViewModel = navigationViewModel,
                )
            }

            entry<Route.ShopDetails>(
                metadata = ListDetailSceneStrategy.detailPane()
            ) { param ->
                ShopDetailsScreen(
                    shopId = param.shopID,
                    snackbarHostState = snackbarHostState,
                    navigationViewModel = navigationViewModel,
                )
            }

            entry<Route.AddEditShop> { param ->
                ShopAddEditScreen(
                    shopId = param.shopID,
                    snackbarHostState = snackbarHostState,
                    navigationViewModel = navigationViewModel,
                )
            }

            entry<Route.AddEditSlide>(
                metadata = ListDetailSceneStrategy.detailPane()
            ) { param ->
                AddEditSlideScreen(
                    slideId = param.id,
                    snackbarHostState = snackbarHostState,
                    navigationViewModel = navigationViewModel,
                )
            }

            entry<Route.EditSlidesSettings>(
                metadata = ListDetailSceneStrategy.detailPane()
            ) {
                EditSlidesSettingsScreen(
                    snackbarHostState = snackbarHostState,
                    navigationViewModel = navigationViewModel,
                )
            }

            entry<Route.CityList>(metadata = ListDetailSceneStrategy.listPane()) {
                CitiesListScreen(
                    snackbarHostState = snackbarHostState,
                    navigationViewModel = navigationViewModel,
                )
            }

            entry<Route.AddEditCity>(
                metadata = ListDetailSceneStrategy.detailPane()
            ) { param ->
                AddEditCityScreen(
                    cityId = param.id,
                    snackbarHostState = snackbarHostState,
                    navigationViewModel = navigationViewModel,
                )
            }

            entry<Route.UsersList>(
                metadata = ListDetailSceneStrategy.listPane()
            ) {
                UsersScreen(
                    snackbarHostState = snackbarHostState,
                    navigationViewModel = navigationViewModel,
                )
            }

            entry<Route.UserDetail>(
                metadata = ListDetailSceneStrategy.detailPane()
            ) { param ->
                UserDetailScreen(
                    userId = param.userId,
                    snackbarHostState = snackbarHostState,
                    navigationViewModel = navigationViewModel,
                )
            }

            entry<Route.AddEditUser> { param ->
                AddEditUserScreen(
                    userId = param.userId,
                    snackbarHostState = snackbarHostState,
                    navigationViewModel = navigationViewModel,
                )
            }

            entry<Route.FileGenerator> {
                FileGenerationScreen(
                    snackbarHostState = snackbarHostState,
                    navigationViewModel = navigationViewModel,
                )
            }

            entry<Route.SlideAiGeneration> {
                SlideAiGenerationScreen(
                    snackbarHostState = snackbarHostState,
                    navigationViewModel = navigationViewModel,
                )
            }
        }
    )
}

/**
 * A [androidx.navigation3.scene.Scene] that displays a list and a detail [androidx.navigation3.runtime.NavEntry] side-by-side in a 40/60 split.
 */
private class ListDetailScene<T : Any>(
    override val key: Any,
    override val previousEntries: List<NavEntry<T>>,
    val listEntry: NavEntry<T>,
    val detailEntry: NavEntry<T>,
) : Scene<T> {
    override val entries: List<NavEntry<T>> = listOf(listEntry, detailEntry)
    override val content: @Composable (() -> Unit) = {
        Row(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.weight(0.4f)) {
                listEntry.Content()
            }
            Column(modifier = Modifier.weight(0.6f)) {
                detailEntry.Content()
            }
        }
    }
}

@Composable
private fun <T : Any> rememberSceneStrategy(): ListDetailSceneStrategy<T> {
    val windowSizeClass = currentWindowAdaptiveInfo(supportLargeAndXLargeWidth = true).windowSizeClass
    Napier.d { "windowSizeClass minWidthDp: ${windowSizeClass.minWidthDp}" }
    return remember(windowSizeClass) {
        ListDetailSceneStrategy(windowSizeClass)
    }
}

/**
 * A [androidx.navigation3.scene.SceneStrategy] that returns a [ListDetailScene] if the window is wide enough, the last item
 * is the backstack is a detail, and before it, at any point in the backstack is a list.
 */
private class ListDetailSceneStrategy<T : Any>(val windowSizeClass: WindowSizeClass) : SceneStrategy<T> {

    override fun SceneStrategyScope<T>.calculateScene(entries: List<NavEntry<T>>): Scene<T>? {

        if (!windowSizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND)) {
            return null
        }

        val detailEntry = entries.lastOrNull()?.takeIf { it.metadata.containsKey(DETAIL_KEY) } ?: return null
        val listEntry = entries.findLast { it.metadata.containsKey(LIST_KEY) } ?: return null

        // We use the list's contentKey to uniquely identify the scene.
        // This allows the detail panes to be displayed instantly through recomposition, rather than
        // having NavDisplay animate the whole scene out when the selected detail item changes.
        val sceneKey = listEntry.contentKey

        return ListDetailScene(
            key = sceneKey, previousEntries = entries.dropLast(1), listEntry = listEntry, detailEntry = detailEntry
        )
    }

    companion object {
        const val LIST_KEY = "ListDetailScene-List"
        const val DETAIL_KEY = "ListDetailScene-Detail"

        /**
         * Helper function to add metadata to a [NavEntry] indicating it can be displayed
         * as a list in the [ListDetailScene].
         */
        fun listPane() = mapOf(LIST_KEY to true)

        /**
         * Helper function to add metadata to a [NavEntry] indicating it can be displayed
         * as a list in the [ListDetailScene].
         */
        fun detailPane() = mapOf(DETAIL_KEY to true)
    }
}