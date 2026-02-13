// File: src/commonMain/kotlin/org/bigblackowl/vccadmin/ui/main/MainScreen.kt

package org.bigblackowl.vccadmin.ui.main

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowRight
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import org.bigblackowl.vccadmin.data.entity.Shop
import org.bigblackowl.vccadmin.data.repository.FakeBackend
import org.bigblackowl.vccadmin.data.state.UIEvents
import org.bigblackowl.vccadmin.data.utils.ShopGroup
import org.bigblackowl.vccadmin.data.utils.getGroupedShops
import org.bigblackowl.vccadmin.navigation.NavigationViewModel
import org.bigblackowl.vccadmin.navigation.Route
import org.bigblackowl.vccadmin.resourses.DefaultValues
import org.bigblackowl.vccadmin.theme.PreviewDarkMaterialTheme
import org.bigblackowl.vccadmin.theme.PreviewLightMaterialTheme
import org.bigblackowl.vccadmin.uiComponent.LoadingComponent
import org.bigblackowl.vccadmin.uiComponent.container.PlatformPullToRefreshBox
import org.bigblackowl.vccadmin.uiComponent.icons.DefaultIcon
import org.bigblackowl.vccadmin.uiComponent.listItems.DefaultScrollbar
import org.bigblackowl.vccadmin.uiComponent.listItems.StickyCityHeader
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
    LaunchedEffect(Unit) {
        viewModel.onIntent(MainScreenIntent.Refresh)
    }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val uiEvent by viewModel.uiEvent.collectAsStateWithLifecycle(null)

    LaunchedEffect(uiEvent) {
        uiEvent?.let { event ->
            when (event) {
                is UIEvents.ShowMessage -> snackbarHostState.showSnackbar(event.message)
                else -> {}
            }
        }
    }

    MainScreenContent(
        groupedShops = uiState.groupedShops,
        isInitialLoading = uiState.isInitialLoading,
        isRefreshing = uiState.isRefreshing,
        onShopClick = { shopId -> navigationViewModel.navigateTo(Route.ShopDetails(shopId)) },
        onRefresh = { viewModel.onIntent(MainScreenIntent.Refresh) },
    )
}

@Composable
private fun MainScreenContent(
    groupedShops: List<ShopGroup>,
    isInitialLoading: Boolean,
    isRefreshing: Boolean,
    onShopClick: (String) -> Unit,
    onRefresh: () -> Unit,
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
        if (groupedShops.isEmpty()) {
            BodyText(stringResource(Res.string.empty_list), modifier = Modifier.align(Alignment.Center), textAlign = TextAlign.Center)
            return@PlatformPullToRefreshBox
        }
        Row(
            Modifier
                .fillMaxSize()
                .padding(DefaultValues.Padding.mainBoxPadding)
        ) {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(DefaultValues.Size.gridItemMinSize),
                modifier = Modifier.weight(1f),
                state = lazyGridState,
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


@Preview
@Composable
private fun MainScreenContentPreview1() = PreviewDarkMaterialTheme {
    MainScreenContent(
        groupedShops = getGroupedShops(
            shops = FakeBackend.shops,
            cities = FakeBackend.cities
        ),
        isInitialLoading = false,
        isRefreshing = false,
        onShopClick = {},
        onRefresh = {}
    )
}

@Preview(
    showSystemUi = true,
    device = Devices.DESKTOP,
)
@Composable
private fun MainScreenContentPreview12() = PreviewLightMaterialTheme {
    MainScreenContent(
        groupedShops = getGroupedShops(
            shops = FakeBackend.shops,
            cities = FakeBackend.cities
        ),
        isInitialLoading = false,
        isRefreshing = false,
        onShopClick = {},
        onRefresh = {}
    )
}

