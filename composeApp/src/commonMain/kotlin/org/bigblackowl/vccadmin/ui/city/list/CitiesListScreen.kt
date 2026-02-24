@file:Suppress("SpellCheckingInspection")

package org.bigblackowl.vccadmin.ui.city.list

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import org.bigblackowl.vccadmin.data.entity.City
import org.bigblackowl.vccadmin.data.entity.UserRole
import org.bigblackowl.vccadmin.data.events.UIEvents
import org.bigblackowl.vccadmin.data.repository.FakeBackend
import org.bigblackowl.vccadmin.navigation.NavigationViewModel
import org.bigblackowl.vccadmin.navigation.Route
import org.bigblackowl.vccadmin.resourses.DefaultValues
import org.bigblackowl.vccadmin.theme.PreviewDarkMaterialTheme
import org.bigblackowl.vccadmin.theme.PreviewLightMaterialTheme
import org.bigblackowl.vccadmin.uiComponent.buttons.AddButton
import org.bigblackowl.vccadmin.uiComponent.buttons.BackButton
import org.bigblackowl.vccadmin.uiComponent.buttons.EditButton
import org.bigblackowl.vccadmin.uiComponent.buttons.RetryButton
import org.bigblackowl.vccadmin.uiComponent.container.ButtonRowContainer
import org.bigblackowl.vccadmin.uiComponent.container.PlatformPullToRefreshBox
import org.bigblackowl.vccadmin.uiComponent.icons.OnlineIcon
import org.bigblackowl.vccadmin.uiComponent.indicators.LoadingComponent
import org.bigblackowl.vccadmin.uiComponent.listItems.DefaultScrollbar
import org.bigblackowl.vccadmin.uiComponent.text.BodyText
import org.bigblackowl.vccadmin.utils.isWideScreen
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import vccadministrator.composeapp.generated.resources.Res
import vccadministrator.composeapp.generated.resources.no_cities

@Composable
fun CitiesListScreen(
    snackbarHostState: SnackbarHostState,
    navigationViewModel: NavigationViewModel,
    citiesListScreenViewModel: CitiesListScreenViewModel = koinInject(),
) {
    // 1) Load один раз
    LaunchedEffect(Unit) {
        citiesListScreenViewModel.onIntent(CitiesListScreenIntent.Load)
    }

    // 2) UI events — один collector
    LaunchedEffect(Unit) {
        citiesListScreenViewModel.uiEvent.collect { event ->
            when (event) {
                is UIEvents.ShowMessage -> snackbarHostState.showSnackbar(event.message)
                else -> Unit
            }
        }
    }

    // 3) State слайсами (щоб не рекомпозити все при кожній зміні)
    val cities by citiesListScreenViewModel.state
        .map { it.cities }
        .distinctUntilChanged()
        .collectAsStateWithLifecycle(initialValue = emptyList())

    val isInitialLoading by citiesListScreenViewModel.state
        .map { it.isInitialLoading }
        .distinctUntilChanged()
        .collectAsStateWithLifecycle(initialValue = true)

    val isRefreshing by citiesListScreenViewModel.state
        .map { it.isRefreshing }
        .distinctUntilChanged()
        .collectAsStateWithLifecycle(initialValue = false)

    val isAdmin by citiesListScreenViewModel.state
        .map { it.currentUserRole == UserRole.ADMIN }
        .distinctUntilChanged()
        .collectAsStateWithLifecycle(initialValue = false)

    CitiesListScreenContent(
        cities = cities,
        isInitialLoading = isInitialLoading,
        isRefreshing = isRefreshing,
        isAdmin = isAdmin,
        onIntent = citiesListScreenViewModel::onIntent,
        onBack = { navigationViewModel.requestBack() },
        onEditClicked = { id -> navigationViewModel.navigateTo(Route.AddEditCity(id)) },
        onAddCity = { navigationViewModel.navigateTo(Route.AddEditCity(null)) },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CitiesListScreenContent(
    cities: List<City>,
    isInitialLoading: Boolean,
    isRefreshing: Boolean,
    isAdmin: Boolean,
    onIntent: (CitiesListScreenIntent) -> Unit,
    onBack: () -> Unit,
    onEditClicked: (Int) -> Unit,
    onAddCity: () -> Unit,
) {
    val listState = rememberLazyGridState()

    if (isInitialLoading) {
        LoadingComponent()
        return
    }

    PlatformPullToRefreshBox(
        isRefreshing = isRefreshing, onRefresh = { onIntent(CitiesListScreenIntent.Refresh) }) {
        Column(
            modifier = Modifier.padding(DefaultValues.Padding.mainBoxPadding).fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(DefaultValues.Padding.verticalListItemPadding)
        ) {
            Row(
                Modifier.weight(1f)
            ) {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(DefaultValues.Size.gridItemMinSize),
                    modifier = Modifier.weight(1f),
                    state = listState,
                    verticalArrangement = Arrangement.spacedBy(DefaultValues.Padding.lazyVerticalGridContentPadding),
                    horizontalArrangement = Arrangement.spacedBy(DefaultValues.Padding.lazyVerticalGridContentPadding),
                ) {
                    if (cities.isEmpty()) {
                        item {
                            BodyText(
                                text = stringResource(Res.string.no_cities),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxSize().wrapContentSize(Alignment.Center)
                            )
                        }
                    } else {
                        items(cities, key = { it.id }) { city ->
                            CityCard(
                                city = city, isAdmin = isAdmin, onEditClicked = { onEditClicked(city.id) })
                        }
                    }
                }

                DefaultScrollbar(scrollState = listState)
            }

            ButtonRowContainer {
                if (isWideScreen()) {
                    BackButton(modifier = Modifier.weight(1f)) { onBack() }

                    RetryButton(modifier = Modifier.weight(1f)) {
                        onIntent(CitiesListScreenIntent.Refresh)
                    }
                }

                AddButton(modifier = Modifier.weight(1f)) {
                    onAddCity()
                }
            }
        }
    }
}

@Composable
private fun CityCard(
    city: City,
    isAdmin: Boolean,
    onEditClicked: () -> Unit,
) {
    val iconSize by animateDpAsState(if (isAdmin) 75.dp else 55.dp)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(DefaultValues.Shape.defaultShape),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            modifier = Modifier.padding(DefaultValues.Padding.cardContentPadding),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxWidth(),
            ) {
                OnlineIcon(
                    model = city.logoUrl,
                    contentDescription = city.name,
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .size(iconSize),
                )

                BodyText(
                    text = city.name,
                    modifier = Modifier.align(Alignment.Center),
                    textAlign = TextAlign.Center,
                )
            }
            AnimatedVisibility(
                visible = isAdmin,
                modifier = Modifier.fillMaxWidth(),
                enter = slideInHorizontally { it } + fadeIn(),
                exit = slideOutHorizontally { it } + fadeOut()
            ) {
                EditButton(Modifier.fillMaxWidth()) {
                    onEditClicked()
                }
            }
        }
    }
}


@Preview
@Composable
private fun CitiesListScreenContentPreview1() = PreviewDarkMaterialTheme {
    CitiesListScreenContent(
        cities = FakeBackend.cities,
        isInitialLoading = false,
        isRefreshing = false,
        isAdmin = true,
        onIntent = {},
        onBack = {},
        onEditClicked = {},
        onAddCity = {}
    )
}

@Preview
@Composable
private fun CitiesListScreenContentPreview2() = PreviewLightMaterialTheme {
    CitiesListScreenContent(
        cities = FakeBackend.cities,
        isInitialLoading = false,
        isRefreshing = false,
        isAdmin = false,
        onIntent = {},
        onBack = {},
        onEditClicked = {},
        onAddCity = {}
    )
}

@Preview(device = Devices.DESKTOP)
@Composable
private fun CitiesListScreenContentPreview1PC() = PreviewDarkMaterialTheme {
    CitiesListScreenContent(
        cities = FakeBackend.cities,
        isInitialLoading = false,
        isRefreshing = false,
        isAdmin = true,
        onIntent = {},
        onBack = {},
        onEditClicked = {},
        onAddCity = {}
    )
}

@Preview(device = Devices.DESKTOP)
@Composable
private fun CitiesListScreenContentPreview2PC() = PreviewLightMaterialTheme {
    CitiesListScreenContent(
        cities = FakeBackend.cities,
        isInitialLoading = false,
        isRefreshing = false,
        isAdmin = false,
        onIntent = {},
        onBack = {},
        onEditClicked = {},
        onAddCity = {}
    )
}