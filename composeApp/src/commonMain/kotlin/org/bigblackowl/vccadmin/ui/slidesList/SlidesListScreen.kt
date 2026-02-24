// File: src/commonMain/kotlin/org/bigblackowl/vccadmin/ui/slidesList/SlidesListScreen.kt
package org.bigblackowl.vccadmin.ui.slidesList

import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import org.bigblackowl.vccadmin.data.entity.Slide
import org.bigblackowl.vccadmin.data.events.UIEvents
import org.bigblackowl.vccadmin.data.repository.FakeBackend
import org.bigblackowl.vccadmin.navigation.NavigationViewModel
import org.bigblackowl.vccadmin.navigation.Route
import org.bigblackowl.vccadmin.resourses.DefaultValues
import org.bigblackowl.vccadmin.theme.PreviewDarkMaterialTheme
import org.bigblackowl.vccadmin.theme.PreviewLightMaterialTheme
import org.bigblackowl.vccadmin.uiComponent.buttons.AddButton
import org.bigblackowl.vccadmin.uiComponent.buttons.BackButton
import org.bigblackowl.vccadmin.uiComponent.buttons.RetryButton
import org.bigblackowl.vccadmin.uiComponent.buttons.SettingsButton
import org.bigblackowl.vccadmin.uiComponent.container.ButtonRowContainer
import org.bigblackowl.vccadmin.uiComponent.container.PlatformPullToRefreshBox
import org.bigblackowl.vccadmin.uiComponent.icons.OnlineIcon
import org.bigblackowl.vccadmin.uiComponent.indicators.LoadingComponent
import org.bigblackowl.vccadmin.uiComponent.listItems.DefaultScrollbar
import org.bigblackowl.vccadmin.uiComponent.text.BodyText
import org.bigblackowl.vccadmin.uiComponent.text.HelperText
import org.bigblackowl.vccadmin.uiComponent.text.SmallText
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import vccadministrator.composeapp.generated.resources.Res
import vccadministrator.composeapp.generated.resources.no_slides
import vccadministrator.composeapp.generated.resources.slide_active_shops
import vccadministrator.composeapp.generated.resources.slide_last_modified
import vccadministrator.composeapp.generated.resources.slide_not_show
import vccadministrator.composeapp.generated.resources.slide_position
import vccadministrator.composeapp.generated.resources.status_active
import vccadministrator.composeapp.generated.resources.status_inactive

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SlidesListScreen(
    snackbarHostState: SnackbarHostState,
    navigationViewModel: NavigationViewModel,
    viewModel: SlidesListScreenViewModel = koinInject(),
) {
    // ✅ Load один раз
    LaunchedEffect(Unit) {
        viewModel.onIntent(SlidesListScreenIntent.Load)
    }

    // ✅ UI events: один collector, без collectAsState
    LaunchedEffect(Unit) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                is UIEvents.ShowMessage -> snackbarHostState.showSnackbar(event.message)
                else -> Unit
            }
        }
    }

    // ✅ State slices
    val isRefreshing by viewModel.uiState
        .map { it.isRefreshing }
        .distinctUntilChanged()
        .collectAsStateWithLifecycle(initialValue = false)

    val isInitialLoading by viewModel.uiState
        .map { it.isInitialLoading }
        .distinctUntilChanged()
        .collectAsStateWithLifecycle(initialValue = true)

    val slides by viewModel.uiState
        .map { it.slides }
        .distinctUntilChanged()
        .collectAsStateWithLifecycle(initialValue = emptyList())

    SlidesListScreenContent(
        isRefreshing = isRefreshing,
        isInitialLoading = isInitialLoading,
        slides = slides,
        onIntent = viewModel::onIntent,
        onBack = { navigationViewModel.requestBack() },
        onOpenSettings = { navigationViewModel.navigateTo(Route.EditSlidesSettings) },
        onEditSlide = { id -> navigationViewModel.navigateTo(Route.AddEditSlide(id)) },
        onAddSlide = { navigationViewModel.navigateTo(Route.AddEditSlide()) },
    )
}

@Composable
private fun SlidesListScreenContent(
    isRefreshing: Boolean,
    isInitialLoading: Boolean,
    slides: List<Slide>,
    onIntent: (SlidesListScreenIntent) -> Unit,
    onBack: () -> Unit,
    onOpenSettings: () -> Unit,
    onEditSlide: (String) -> Unit,
    onAddSlide: () -> Unit,
) {
    val listState = rememberLazyGridState()
    val toggle: (String) -> Unit = remember(onIntent) {
        { id -> onIntent(SlidesListScreenIntent.ToggleSlideVisibility(id)) }
    }
    PlatformPullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = { onIntent(SlidesListScreenIntent.Refresh) },
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        if (isInitialLoading) {
            LoadingComponent()
            return@PlatformPullToRefreshBox
        }

        if (slides.isEmpty()) {
            Text(stringResource(Res.string.no_slides), modifier = Modifier.align(Alignment.Center))
            return@PlatformPullToRefreshBox
        }

        Column(modifier = Modifier.padding(DefaultValues.Padding.mainBoxPadding)) {
            Row(Modifier.weight(1f)) {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 280.dp),
                    modifier = Modifier.weight(1f),
                    state = listState,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(items = slides, key = { it.id }) { slide ->
                        SlideGridItem(
                            slide = slide,
                            onClick = { onEditSlide(slide.id) },
                            onToggleActive = {
                                toggle(slide.id)
                            },
                        )
                    }
                }

                DefaultScrollbar(scrollState = listState)
            }

            ButtonRowContainer {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    SettingsButton { onOpenSettings() }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(
                            DefaultValues.Padding.rowItemPadding,
                            Alignment.CenterHorizontally
                        ),
                    ) {
                        BackButton(Modifier.weight(1f)) { onBack() }
                        AddButton(Modifier.weight(1f)) { onAddSlide() }
                        RetryButton(Modifier.weight(1f)) { onIntent(SlidesListScreenIntent.Refresh) }
                    }
                }
            }
        }
    }
}

@Composable
private fun SlideGridItem(
    slide: Slide,
    onClick: () -> Unit,
    onToggleActive: (Boolean) -> Unit,
) {

    ElevatedCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().heightIn(min = 250.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            OnlineIcon(
                model = slide.publicUrl,
                contentDescription = slide.fileName,
                modifier = Modifier
                    .heightIn(max = 180.dp)
                    .aspectRatio(16f / 9f)
                    .clip(CardDefaults.elevatedShape),
            )

            Spacer(Modifier.height(12.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    BodyText(
                        text = slide.fileName,
                        modifier = Modifier.basicMarquee(),
                    )

                    HelperText(
                        text = stringResource(Res.string.slide_position, slide.position),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    HelperText(
                        text = if (slide.shopCodes.isNotEmpty()) pluralStringResource(Res.plurals.slide_active_shops, slide.shopCodes.size, slide.shopCodes.size)
                        else stringResource(Res.string.slide_not_show),
                        modifier = Modifier.basicMarquee(),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    SmallText(
                        text = stringResource(Res.string.slide_last_modified, slide.lastModified),
                        modifier = Modifier.basicMarquee(),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(Modifier.width(10.dp))

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Switch(
                        checked = slide.isActive,
                        onCheckedChange = onToggleActive
                    )
                    SmallText(
                        text = if (slide.isActive) stringResource(Res.string.status_active) else stringResource(Res.string.status_inactive),
                        color = if (slide.isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
    }
}

@Preview
@Composable
private fun SlideGridItemPreview1() = PreviewDarkMaterialTheme {
    SlideGridItem(FakeBackend.singleSlide, onClick = {}) {}
}

@Preview
@Composable
private fun SlideGridItemPreview2() = PreviewLightMaterialTheme {
    SlideGridItem(FakeBackend.singleSlide, onClick = {}) {}
}

@Preview(device = Devices.DESKTOP)
@Composable
private fun SlidesListScreenContentPreview1() = PreviewDarkMaterialTheme {
    SlidesListScreenContent(
        isRefreshing = false,
        isInitialLoading = false,
        slides = FakeBackend.slides,
        onIntent = { },
        onBack = {},
        onOpenSettings = {},
        onEditSlide = {},
        onAddSlide = {}
    )
}

@Preview(device = Devices.DESKTOP)
@Composable
private fun SlidesListScreenContentPreview2() = PreviewLightMaterialTheme {
    SlidesListScreenContent(
        isRefreshing = false,
        isInitialLoading = false,
        slides = FakeBackend.slides,
        onIntent = { },
        onBack = {},
        onOpenSettings = {},
        onEditSlide = {},
        onAddSlide = {}
    )
}