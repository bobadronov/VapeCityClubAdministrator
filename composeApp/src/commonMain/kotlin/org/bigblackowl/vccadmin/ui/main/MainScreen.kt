// File: src/commonMain/kotlin/org/bigblackowl/vccadmin/ui/main/MainScreen.kt

package org.bigblackowl.vccadmin.ui.main

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Spring.StiffnessHigh
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.staggeredgrid.LazyHorizontalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowRight
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.bigblackowl.vccadmin.data.entity.City
import org.bigblackowl.vccadmin.data.entity.Shop
import org.bigblackowl.vccadmin.data.entity.ShopGroup
import org.bigblackowl.vccadmin.data.entity.ShopStatus
import org.bigblackowl.vccadmin.data.events.UIEvents
import org.bigblackowl.vccadmin.data.repository.FakeBackend
import org.bigblackowl.vccadmin.navigation.NavigationViewModel
import org.bigblackowl.vccadmin.navigation.Route
import org.bigblackowl.vccadmin.resourses.DefaultValues
import org.bigblackowl.vccadmin.theme.PreviewDarkMaterialTheme
import org.bigblackowl.vccadmin.theme.PreviewLightMaterialTheme
import org.bigblackowl.vccadmin.uiComponent.container.PlatformPullToRefreshBox
import org.bigblackowl.vccadmin.uiComponent.icons.DefaultIcon
import org.bigblackowl.vccadmin.uiComponent.listItems.DefaultScrollbar
import org.bigblackowl.vccadmin.uiComponent.listItems.StickyCityHeader
import org.bigblackowl.vccadmin.uiComponent.indicators.LoadingComponent
import org.bigblackowl.vccadmin.uiComponent.text.BodyText
import org.bigblackowl.vccadmin.uiComponent.text.HelperText
import org.bigblackowl.vccadmin.utils.isWideScreen
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import vccadministrator.composeapp.generated.resources.Res
import vccadministrator.composeapp.generated.resources.empty_list

@Composable
fun MainScreen(
    snackbarHostState: SnackbarHostState,
    navigationViewModel: NavigationViewModel,
    viewModel: MainScreenViewModel = koinInject(),
) {
    LaunchedEffect(Unit) { viewModel.onIntent(MainScreenIntent.Refresh) }

    LaunchedEffect(Unit) {
        viewModel.uiEvent.collect { event ->
            if (event is UIEvents.ShowMessage) snackbarHostState.showSnackbar(event.message)
        }
    }

    val cities by viewModel.uiState.map { it.cities }
        .distinctUntilChanged()
        .collectAsStateWithLifecycle(initialValue = emptyList())

    val selectedCityIds by viewModel.uiState.map { it.filter.selectedCityIds }
        .distinctUntilChanged()
        .collectAsStateWithLifecycle(initialValue = emptySet())

    val selectedStatuses by viewModel.uiState.map { it.filter.selectedStatuses }
        .distinctUntilChanged()
        .collectAsStateWithLifecycle(initialValue = emptySet())

    val groupedShops by viewModel.uiState.map { it.filteredGroupedShops }
        .distinctUntilChanged()
        .collectAsStateWithLifecycle(initialValue = emptyList())

    val isInitialLoading by viewModel.uiState.map { it.isInitialLoading }
        .distinctUntilChanged()
        .collectAsStateWithLifecycle(initialValue = true)

    val isRefreshing by viewModel.uiState.map { it.isRefreshing }
        .distinctUntilChanged()
        .collectAsStateWithLifecycle(initialValue = false)

    MainScreenContent(
        cities = cities,
        selectedCityIds = selectedCityIds,
        selectedStatuses = selectedStatuses,
        groupedShops = groupedShops,
        isInitialLoading = isInitialLoading,
        isRefreshing = isRefreshing,
        onShopClick = { shopId -> navigationViewModel.navigateTo(Route.ShopDetails(shopId)) },
        onRefresh = { viewModel.onIntent(MainScreenIntent.Refresh) },
        onToggleCity = { viewModel.onIntent(MainScreenIntent.ToggleCity(it)) },
        onToggleStatus = { viewModel.onIntent(MainScreenIntent.ToggleStatus(it)) },
        onClearFilters = { viewModel.onIntent(MainScreenIntent.ClearFilters) },
    )
}
@Composable
private fun MainScreenContent(
    cities: List<City>,
    selectedCityIds: Set<Int>,
    selectedStatuses: Set<ShopStatus>,
    groupedShops: List<ShopGroup>,
    isInitialLoading: Boolean,
    isRefreshing: Boolean,
    onShopClick: (String) -> Unit,
    onRefresh: () -> Unit,
    onToggleCity: (Int) -> Unit,
    onToggleStatus: (ShopStatus) -> Unit,
    onClearFilters: () -> Unit,
) {
    val lazyGridState = rememberLazyGridState()
    val scope = rememberCoroutineScope()

    val showScrollToTop by remember {
        derivedStateOf { lazyGridState.firstVisibleItemIndex > 5 }
    }

    PlatformPullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        if (isInitialLoading) {
            LoadingComponent()
            return@PlatformPullToRefreshBox
        }

        if (cities.isEmpty()) {
            BodyText(stringResource(Res.string.empty_list), modifier = Modifier.align(Alignment.Center), textAlign = TextAlign.Center)
            return@PlatformPullToRefreshBox
        }

        Row(
            Modifier
                .fillMaxSize()
                .padding(DefaultValues.Padding.mainBoxPadding)
        ) {
            Column(Modifier.weight(1f)) {

                ShopsFiltersBar(
                    cities = cities,
                    selectedCityIds = selectedCityIds,      // краще якщо це Set<Int>
                    selectedStatuses = selectedStatuses,    // краще якщо це Set<ShopStatus>
                    onToggleCity = onToggleCity,
                    onToggleStatus = onToggleStatus,
                    onClear = onClearFilters,
                )

                Spacer(Modifier.height(12.dp))

                if (groupedShops.isEmpty()) {
                    BodyText(
                        "Нічого не знайдено за поточними фільтрами",
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(DefaultValues.Size.gridItemMinSize),
                        state = lazyGridState,
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(DefaultValues.Padding.lazyVerticalGridContentPadding),
                        horizontalArrangement = Arrangement.spacedBy(DefaultValues.Padding.lazyVerticalGridContentPadding),
                    ) {
                        groupedShops.forEach { (city, shops) ->
                            stickyHeader(key = "header_${city.id}") {
                                StickyCityHeader(city = city)
                            }
                            items(items = shops, key = { it.id }) { shop ->
                                ShopCardItem(shop = shop, onClick = { onShopClick(shop.id) })
                            }
                        }
                    }
                }
            }

            DefaultScrollbar(scrollState = lazyGridState)
        }

        AnimatedVisibility(
            visible = showScrollToTop,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            FloatingActionButton(
                onClick = { scope.launch { lazyGridState.animateScrollToItem(0) } },
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
            ) {
                DefaultIcon(Icons.Default.ArrowUpward, tint = MaterialTheme.colorScheme.primary)
            }
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ShopsFiltersBar(
    cities: List<City>,
    selectedCityIds: Set<Int>,
    selectedStatuses: Set<ShopStatus>,
    onToggleCity: (Int) -> Unit,
    onToggleStatus: (ShopStatus) -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val statuses = remember { ShopStatus.entries } // стабільно
    var expanded by rememberSaveable { mutableStateOf(false) } // щоб не скидалося при конфіг-змінах
    val height by animateDpAsState(if (expanded) 110.dp else 35.dp, label = "filters_height")

    val gridState = rememberLazyStaggeredGridState()
    val scope = rememberCoroutineScope()


    Box(modifier = modifier.fillMaxWidth().height(height)) {

        FiltersGrid(
            expanded = expanded,
            gridState = gridState,
            scope = scope,
            cities = cities,
            statuses = statuses,
            selectedCityIds = selectedCityIds,
            selectedStatuses = selectedStatuses,
            onToggleCity = onToggleCity,
            onToggleStatus = onToggleStatus,
            onClear = onClear,
        )

        ExpandButton(
            expanded = expanded,
            onToggle = { expanded = !expanded },
            modifier = Modifier.align(Alignment.CenterEnd)
        )
    }
}
@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun FiltersGrid(
    expanded: Boolean,
    gridState: LazyStaggeredGridState,
    wheelSpeed: Float = 130f,
    scope: CoroutineScope,
    cities: List<City>,
    statuses: List<ShopStatus>,
    selectedCityIds: Set<Int>,
    selectedStatuses: Set<ShopStatus>,
    onToggleCity: (Int) -> Unit,
    onToggleStatus: (ShopStatus) -> Unit,
    onClear: () -> Unit,
) {
    val canClear = selectedCityIds.isNotEmpty() || selectedStatuses.isNotEmpty()

    LazyHorizontalStaggeredGrid(
        rows = StaggeredGridCells.Fixed(if (expanded) 3 else 1),
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(gridState) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        if (event.type == PointerEventType.Scroll) {
                            val dy = event.changes.firstOrNull()?.scrollDelta?.y ?: 0f
                            if (dy != 0f) {
                                scope.launch {
                                    gridState.animateScrollBy(
                                        value = dy * wheelSpeed,
                                        animationSpec = spring(stiffness = StiffnessHigh)
                                    )
                                }
                                event.changes.forEach { it.consume() }
                            }
                        }
                    }
                }
            },
        state = gridState,
        verticalArrangement = Arrangement.spacedBy(DefaultValues.Padding.flowRowPadding),
        horizontalItemSpacing = DefaultValues.Padding.flowRowPadding,
    ) {
        item(span = StaggeredGridItemSpan.FullLine) {
            AssistChip(
                onClick = onClear,
                label = { HelperText("Скинути") },
                enabled = canClear,
                shape = RoundedCornerShape(DefaultValues.Shape.defaultShape),
            )
        }

        items(statuses, key = { it.name }, span = { StaggeredGridItemSpan.SingleLane }) { st ->
            StatusChip(
                status = st,
                selected = st in selectedStatuses,
                onClick = { onToggleStatus(st) },
            )
        }

        items(cities, key = { it.id }, span = { StaggeredGridItemSpan.SingleLane }) { city ->
            CityChip(
                city = city,
                selected = city.id in selectedCityIds,
                onClick = { onToggleCity(city.id) },
            )
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StatusChip(
    status: ShopStatus,
    selected: Boolean,
    onClick: () -> Unit,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { HelperText(stringResource(status.title)) },
        leadingIcon = { Icon(status.icon, contentDescription = null) },
        shape = RoundedCornerShape(DefaultValues.Shape.defaultShape),
    )
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CityChip(
    city: City,
    selected: Boolean,
    onClick: () -> Unit,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { HelperText(city.name) },
        shape = RoundedCornerShape(DefaultValues.Shape.defaultShape),
    )
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExpandButton(
    expanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    IconButton(
        onClick = onToggle,
        modifier = modifier,
        colors = IconButtonDefaults.iconButtonColors()
            .copy(containerColor = MaterialTheme.colorScheme.primary),
    ) {
        ExposedDropdownMenuDefaults.TrailingIcon(expanded)
    }
}
@Composable
private fun ShopCardItem(
    shop: Shop,
    onClick: () -> Unit,
) {
    val statusColor = shop.status.color
    val isWide = isWideScreen() // <-- один раз

    OutlinedCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.outlinedCardColors(
            containerColor = statusColor.copy(alpha = .1f)
        ),
        border = BorderStroke(width = 2.dp, color = statusColor.copy(alpha = .3f)),
    ) {
        Crossfade(isWide) { isW ->
            Box(
                modifier = Modifier
                    .padding(DefaultValues.Padding.cardContentPadding)
                    .fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(if (isW) Alignment.Center else Alignment.CenterStart),
                    horizontalAlignment = if (isW) Alignment.CenterHorizontally else Alignment.Start
                ) {
                    BodyText(
                        text = buildString {
                            append(shop.street)
                            append(", ${shop.houseNumber}")
                            shop.addressComment.let { if (it.isNotBlank()) append("\n($it)") }
                        },
                        textAlign = if (isW) TextAlign.Center else TextAlign.Start
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        Icon(
                            imageVector = shop.status.icon,
                            contentDescription = null,
                            modifier = Modifier.sizeIn(maxHeight = 20.dp)
                        )
                        HelperText(
                            text = stringResource(shop.status.title),
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }

                this@OutlinedCard.AnimatedVisibility(
                    visible = !isW,
                    modifier = Modifier.align(Alignment.CenterEnd),
                    enter = fadeIn(),
                    exit = fadeOut(),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }
    }
}





@Preview(device = Devices.PHONE)
@Composable
private fun MainScreenContentPreview1() = PreviewDarkMaterialTheme {
    val cities = FakeBackend.cities.sortedBy { it.name }

    MainScreenContent(
        cities = cities,
        selectedCityIds = emptySet(),                 // без фільтрів
        selectedStatuses = emptySet(),                // без фільтрів
        groupedShops = FakeBackend.groupedShops,      // повний список
        isInitialLoading = false,
        isRefreshing = false,
        onShopClick = {},
        onRefresh = {},
        onToggleCity = {},
        onToggleStatus = {},
        onClearFilters = {},
    )
}

@Preview(showSystemUi = true, device = Devices.DESKTOP)
@Composable
private fun MainScreenContentPreview12() = PreviewLightMaterialTheme {
    val cities = FakeBackend.cities.sortedBy { it.name }

    // приклад: прев’ю з активними фільтрами
    val selectedCityIds = cities.take(2).map { it.id }.toSet()
    val selectedStatuses = setOf(ShopStatus.ACTIVE)

    MainScreenContent(
        cities = cities,
        selectedCityIds = selectedCityIds,
        selectedStatuses = selectedStatuses,
        groupedShops = FakeBackend.groupedShops, // або підстав "вручну" відфільтрований список для прев’ю
        isInitialLoading = false,
        isRefreshing = false,
        onShopClick = {},
        onRefresh = {},
        onToggleCity = {},
        onToggleStatus = {},
        onClearFilters = {},
    )
}
