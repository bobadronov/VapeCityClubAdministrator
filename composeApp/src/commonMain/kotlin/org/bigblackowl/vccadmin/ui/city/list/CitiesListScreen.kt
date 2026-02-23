@file:Suppress("SpellCheckingInspection")

package org.bigblackowl.vccadmin.ui.city.list

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
import org.bigblackowl.vccadmin.uiComponent.listItems.DefaultScrollbar
import org.bigblackowl.vccadmin.uiComponent.loading.LoadingComponent
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
    val state by citiesListScreenViewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        citiesListScreenViewModel.onIntent(CitiesListScreenIntent.Load)
        citiesListScreenViewModel.uiEvent.collect { event ->
            when (event) {
                is UIEvents.ShowMessage -> snackbarHostState.showSnackbar(event.message)
                else -> {}
            }
        }
    }

    CitiesListScreenContent(
        state = state,
        isAdmin = state.currentUserRole == UserRole.ADMIN,
        onIntent = citiesListScreenViewModel::onIntent,
        onBack = { navigationViewModel.requestBack() },
        onEditClicked = { id -> navigationViewModel.navigateTo(Route.AddEditCity(id)) },
        onAddCity = { navigationViewModel.navigateTo(Route.AddEditCity(null)) },
    )

}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CitiesListScreenContent(
    state: CitiesListScreenUiState,
    isAdmin: Boolean,
    onIntent: (CitiesListScreenIntent) -> Unit,
    onBack: () -> Unit,
    onEditClicked: (Int) -> Unit,
    onAddCity: () -> Unit,
) {
    val listState = rememberLazyListState()

    if (state.isInitialLoading) {
        LoadingComponent()
        return
    }

    PlatformPullToRefreshBox(
        isRefreshing = state.isRefreshing, onRefresh = { onIntent(CitiesListScreenIntent.Refresh) }) {
        Column(
            modifier = Modifier.padding(DefaultValues.Padding.mainBoxPadding).fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(DefaultValues.Padding.verticalListItemPadding)
        ) {
            Row(
                Modifier.weight(1f)
            ) {
                LazyColumn(
                    modifier = Modifier.weight(1f).padding(vertical = 10.dp),
                    state = listState,
                    verticalArrangement = Arrangement.spacedBy(DefaultValues.Padding.verticalListItemPadding)
                ) {
                    if (state.cities.isEmpty()) {
                        item {
                            BodyText(
                                text = stringResource(Res.string.no_cities),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxSize().wrapContentSize(Alignment.Center)
                            )
                        }
                    } else {
                        items(state.cities, key = { it.id }) { city ->
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
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Row(
            modifier = Modifier.padding(10.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(DefaultValues.Padding.rowItemPadding)
        ) {
            OnlineIcon(
                model = city.logoUrl,
                contentDescription = city.name,
                modifier = Modifier.size(if (isAdmin) 75.dp else 60.dp),
            )

            BodyText(
                text = city.name,
                modifier = Modifier.basicMarquee().weight(1f),
            )

            AnimatedVisibility(isAdmin) {
                EditButton {
                    onEditClicked()
                }
            }
        }
    }
}

@Preview
@Composable
fun CitiesListScreenContentPreview1() = PreviewDarkMaterialTheme {
    CitiesListScreenContent(
        state = CitiesListScreenUiState(
            cities = FakeBackend.cities
        ),
        isAdmin = true,
        onIntent = {},
        onBack = {},
        onEditClicked = {},
        onAddCity = {}
    )
}

@Preview
@Composable
fun CitiesListScreenContentPreview2() = PreviewLightMaterialTheme {
    CitiesListScreenContent(
        state = CitiesListScreenUiState(
            cities = FakeBackend.cities
        ),
        isAdmin = false,
        onIntent = {},
        onBack = {},
        onEditClicked = {},
        onAddCity = {}
    )
}

@Preview
@Composable
private fun CityCardPreview1() = PreviewDarkMaterialTheme {
    CityCard(
        city = FakeBackend.singleCity, isAdmin = true, onEditClicked = {})
}


@Preview
@Composable
private fun CityCardPreview2() = PreviewLightMaterialTheme {
    CityCard(
        city = FakeBackend.singleCity, isAdmin = false, onEditClicked = {})
}
