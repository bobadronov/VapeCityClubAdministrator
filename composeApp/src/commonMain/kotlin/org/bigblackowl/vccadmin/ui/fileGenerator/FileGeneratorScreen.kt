package org.bigblackowl.vccadmin.ui.fileGenerator

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColor
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.window.core.layout.WindowSizeClass.Companion.WIDTH_DP_MEDIUM_LOWER_BOUND
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import org.bigblackowl.vccadmin.data.entity.City
import org.bigblackowl.vccadmin.data.entity.Shop
import org.bigblackowl.vccadmin.data.events.UIEvents
import org.bigblackowl.vccadmin.data.repository.FakeBackend
import org.bigblackowl.vccadmin.domain.repository.FileType
import org.bigblackowl.vccadmin.navigation.NavigationViewModel
import org.bigblackowl.vccadmin.theme.DefaultValues
import org.bigblackowl.vccadmin.theme.PreviewDarkMaterialTheme
import org.bigblackowl.vccadmin.uiComponent.buttons.BackButton
import org.bigblackowl.vccadmin.uiComponent.buttons.OpenButton
import org.bigblackowl.vccadmin.uiComponent.buttons.ShareAllFilesButton
import org.bigblackowl.vccadmin.uiComponent.checkbox.DefaultCheckbox
import org.bigblackowl.vccadmin.uiComponent.container.ButtonRowContainer
import org.bigblackowl.vccadmin.uiComponent.container.PlatformPullToRefreshBox
import org.bigblackowl.vccadmin.uiComponent.icons.DefaultIcon
import org.bigblackowl.vccadmin.uiComponent.indicators.LoadingComponent
import org.bigblackowl.vccadmin.uiComponent.listItems.DefaultVerticalScrollbar
import org.bigblackowl.vccadmin.uiComponent.listItems.StickyCityHeader
import org.bigblackowl.vccadmin.uiComponent.text.BodyText
import org.bigblackowl.vccadmin.uiComponent.text.SmallText
import org.bigblackowl.vccadmin.uiComponent.text.TitleText
import org.bigblackowl.vccadmin.utils.PlatformFileProvider
import org.bigblackowl.vccadmin.utils.isWideScreen
import org.jetbrains.compose.resources.stringArrayResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import vccadministrator.composeapp.generated.resources.Res
import vccadministrator.composeapp.generated.resources.all_months
import vccadministrator.composeapp.generated.resources.deselect_all
import vccadministrator.composeapp.generated.resources.empty_list
import vccadministrator.composeapp.generated.resources.generate
import vccadministrator.composeapp.generated.resources.next
import vccadministrator.composeapp.generated.resources.open_file
import vccadministrator.composeapp.generated.resources.select
import vccadministrator.composeapp.generated.resources.select_all
import vccadministrator.composeapp.generated.resources.select_file_types
import vccadministrator.composeapp.generated.resources.select_month
import vccadministrator.composeapp.generated.resources.select_shops
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun FileGenerationScreen(
    snackbarHostState: SnackbarHostState,
    navigationViewModel: NavigationViewModel,
    viewModel: FileGeneratorScreenViewModel = koinInject(),
) {
    val uiState by viewModel.uiState.collectAsState()

    val pagerState = rememberPagerState(initialPage = 0, pageCount = { FileGenerationStage.entries.size })

    // Init один раз
    LaunchedEffect(Unit) {
        viewModel.onIntent(FileGenerationIntent.Init)
    }

    // Effects один collector
    LaunchedEffect(Unit) {
        viewModel.effects.collect { eff ->
            when (eff) {
                is UIEvents.ShowMessage -> snackbarHostState.showSnackbar(eff.message)
                is UIEvents.NavigateBack -> navigationViewModel.popBackStack()
                else -> Unit
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose { viewModel.onIntent(FileGenerationIntent.Exit) }
    }

    // stage -> pager
    // stage slice
    val stage by viewModel.uiState
        .map { it.stage }
        .distinctUntilChanged()
        .collectAsStateWithLifecycle(initialValue = FileGenerationStage.SELECT_FILES)

    LaunchedEffect(pagerState) {
        snapshotFlow { stage.ordinal }
            .distinctUntilChanged()
            .collectLatest { target ->
                if (pagerState.currentPage != target) {
                    pagerState.animateScrollToPage(target)
                }
            }
    }

    FileGenerationScreenContent(
        pagerState = pagerState,
        // SELECT_FILES
        initialLoading = uiState.initialLoading,
        selectedFileTypes = uiState.selectedFileTypes, // Set<FileTypeId>
        requiresMonth = uiState.requiresMonth,
        selectedMonth = uiState.selectedMonth,
        showMonthPicker = uiState.showMonthPicker,
        canGoNextFromFiles = uiState.canGoNextFromFiles,
        needsShops = uiState.needsShops,
        // SELECT_SHOPS
        isRefreshing = uiState.isRefreshing,
        shops = uiState.shops,
        cities = uiState.cities,
        selectedShopIds = uiState.selectedShopIds, // Set<String>
        canGenerate = uiState.canGenerate,
        // GENERATED
        isGenerating = uiState.isGenerating,
        progress = uiState.progress,
        generatedFiles = uiState.generatedFiles,
        // callbacks
        onIntent = viewModel::onIntent,
    )
}

@Composable
private fun FileGenerationScreenContent(
    pagerState: PagerState,
    onIntent: (FileGenerationIntent) -> Unit,

    // SELECT_FILES
    initialLoading: Boolean,
    selectedFileTypes: Set<FileType>, // або Set<FileTypeId> - підстав свій тип
    requiresMonth: Boolean,
    selectedMonth: String,
    showMonthPicker: Boolean,
    canGoNextFromFiles: Boolean,
    needsShops: Boolean,

    // SELECT_SHOPS
    isRefreshing: Boolean,
    shops: List<Shop>,
    cities: List<City>, // підстав свій тип
    selectedShopIds: Set<String>,
    canGenerate: Boolean,

    // GENERATED
    isGenerating: Boolean,
    progress: Float,
    generatedFiles: List<GeneratedFile>,
) {
    Column(
        modifier = Modifier.padding(DefaultValues.Padding.mainBoxPadding),
    ) {
        NumberIndicator(
            currentPage = pagerState.currentPage,
            noOfDots = pagerState.pageCount,
            modifier = Modifier.padding(bottom = 10.dp)
        )

        HorizontalPager(
            state = pagerState,
            userScrollEnabled = false,
        ) { page ->
            when (FileGenerationStage.entries[page]) {
                FileGenerationStage.SELECT_FILES -> {
                    SelectFileContent(
                        initialLoading = initialLoading,
                        selectedFileTypes = selectedFileTypes,
                        requiresMonth = requiresMonth,
                        selectedMonth = selectedMonth,
                        showMonthPicker = showMonthPicker,
                        canGoNextFromFiles = canGoNextFromFiles,
                        needsShops = needsShops,
                        onIntent = onIntent,
                    )
                }

                FileGenerationStage.SELECT_SHOPS -> {
                    ShopPickerContent(
                        initialLoading = initialLoading,
                        isRefreshing = isRefreshing,
                        shops = shops,
                        cities = cities,
                        selectedShopIds = selectedShopIds,
                        canGenerate = canGenerate,
                        onIntent = onIntent,
                    )
                }

                FileGenerationStage.GENERATED -> {
                    GeneratedContent(
                        isGenerating = isGenerating,
                        progress = progress,
                        generatedFiles = generatedFiles,
                        onIntent = onIntent,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun GeneratedContent(
    isGenerating: Boolean,
    progress: Float,
    generatedFiles: List<GeneratedFile>,
    onIntent: (FileGenerationIntent) -> Unit
) {
    val haptic = LocalHapticFeedback.current

    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(300, easing = FastOutLinearInEasing),
        label = "animatedProgress"
    )

    val listState = rememberLazyListState()

    val canShareAll by remember(generatedFiles) {
        derivedStateOf { generatedFiles.any { it.content != null } }
    }

    Crossfade(targetState = isGenerating, label = "generated_crossfade") { generating ->
        if (generating) {
            Box(
                Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                LinearWavyProgressIndicator(progress = { animatedProgress })
            }
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {

                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(DefaultValues.Padding.verticalListItemPadding)
                ) {
                    items(
                        items = generatedFiles,
                        key = { it.name } // або стабільний id
                    ) { file ->
                        FileCardItem(file) {
                            onIntent(FileGenerationIntent.OpenFile(file.name))
                            haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
                        }
                    }
                }

                ButtonRowContainer {
                    BackButton {
                        onIntent(FileGenerationIntent.GoBack)
                        haptic.performHapticFeedback(HapticFeedbackType.Reject)
                    }
                    if (canShareAll) {
                        ShareAllFilesButton {
                            onIntent(FileGenerationIntent.ShareAllFiles)
                            haptic.performHapticFeedback(HapticFeedbackType.Confirm)

                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun NumberIndicator(
    currentPage: Int,
    noOfDots: Int,
    modifier: Modifier = Modifier,
    activeDotColor: Color = MaterialTheme.colorScheme.surface,
    inActiveDotColor: Color = Color.DarkGray,
    indicatorWidth: Dp = 35.dp,
    activeDotSize: Dp = 30.dp,
    spacedBy: Dp = 15.dp,
    inActiveDotSize: Dp = 12.dp,
    // ефекти
    animate: Boolean = true,
    showLabelInsideActive: Boolean = true,
) {
    val labelText = remember(currentPage, noOfDots) { "${currentPage + 1}/$noOfDots" }

    Box(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.align(Alignment.Center),
            horizontalArrangement = Arrangement.spacedBy(spacedBy),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            repeat(noOfDots) { index ->
                val isActive = index == currentPage

                // Синхронна анімація станів
                val transition = updateTransition(
                    targetState = isActive,
                    label = "dot_transition_$index"
                )

                val width by transition.animateDp(
                    label = "dot_width",
                    transitionSpec = { tween(durationMillis = 220, easing = FastOutSlowInEasing) }
                ) { active ->
                    if (active) indicatorWidth else inActiveDotSize
                }

                val height by transition.animateDp(
                    label = "dot_height",
                    transitionSpec = { tween(durationMillis = 220, easing = FastOutSlowInEasing) }
                ) { active ->
                    if (active) activeDotSize else inActiveDotSize
                }

                val bgColor by transition.animateColor(
                    label = "dot_color",
                    transitionSpec = { tween(durationMillis = 220) }
                ) { active ->
                    if (active) activeDotColor else inActiveDotColor
                }

                val scale by transition.animateFloat(
                    label = "dot_scale",
                    transitionSpec = { tween(durationMillis = 220, easing = FastOutSlowInEasing) }
                ) { active ->
                    if (active) 1.05f else 1f
                }

                Box(
                    modifier = Modifier
                        .padding(2.dp)
                        .graphicsLayer {
                            if (animate) {
                                scaleX = scale
                                scaleY = scale
                            }
                        }
                        .size(width = width, height = height)
                        .background(
                            color = bgColor,
                            shape = RoundedCornerShape(DefaultValues.Shape.defaultShape)
                        )
                ) {
                    if (showLabelInsideActive) {
                        this@Row.AnimatedVisibility(
                            visible = isActive,
                            modifier = Modifier.align(Alignment.Center),
                            enter = fadeIn(tween(140)) + scaleIn(
                                initialScale = 0.92f,
                                animationSpec = tween(220, easing = FastOutSlowInEasing)
                            ),
                            exit = fadeOut(tween(100)) + scaleOut(
                                targetScale = 0.96f,
                                animationSpec = tween(140)
                            )
                        ) {
                            OutlinedCard {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 6.dp, vertical = 2.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    SmallText(
                                        text = labelText,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ShopPickerContent(
    initialLoading: Boolean,
    isRefreshing: Boolean,
    shops: List<Shop>,
    cities: List<City>, // підстав свій тип
    selectedShopIds: Set<String>,
    canGenerate: Boolean,
    onIntent: (FileGenerationIntent) -> Unit
) {
    val haptic = LocalHapticFeedback.current

    Column(horizontalAlignment = Alignment.CenterHorizontally) {

        MultiShopPicker(
            initialLoading = initialLoading,
            isRefreshing = isRefreshing,
            shops = shops,
            cities = cities,
            selectedShopIds = selectedShopIds,
            onIntent = onIntent,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        )

        ButtonRowContainer {
            BackButton { onIntent(FileGenerationIntent.GoBack)
                haptic.performHapticFeedback(HapticFeedbackType.Reject)
            }

            OutlinedButton(
                enabled = canGenerate,
                onClick = {
                    onIntent(FileGenerationIntent.Generate)
                    haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                }
            ) {
                BodyText(stringResource(Res.string.generate))
            }
        }
    }
}

@Composable
private fun SelectFileContent(
    initialLoading: Boolean,
    selectedFileTypes: Set<FileType>, // або Set<FileTypeId>
    requiresMonth: Boolean,
    selectedMonth: String,
    showMonthPicker: Boolean,
    canGoNextFromFiles: Boolean,
    needsShops: Boolean,
    onIntent: (FileGenerationIntent) -> Unit,
) {
    val haptic = LocalHapticFeedback.current

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(DefaultValues.Padding.verticalListItemPadding),
    ) {

        FileTypePicker(
            initialLoading = initialLoading,
            selectedFileTypes = selectedFileTypes,
            onFileTypeToggled = { type, isSelected ->
                onIntent(FileGenerationIntent.ToggleFileType(type, isSelected))
            },
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        )

        AnimatedVisibility(
            visible = requiresMonth,
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut(),
        ) {
            MonthPicker(
                selectedMonth = selectedMonth,
                onMonthSelected = { onIntent(FileGenerationIntent.SelectMonth(it)) },
                showMonthPicker = showMonthPicker,
                onShowMonthPickerChange = { show ->
                    onIntent(
                        if (show) FileGenerationIntent.OpenMonthPicker
                        else FileGenerationIntent.CloseMonthPicker
                    )
                }
            )
        }

        ButtonRowContainer {
            if (isWideScreen()) {
                BackButton {
                    onIntent(FileGenerationIntent.GoBack)
                    haptic.performHapticFeedback(HapticFeedbackType.Reject)
                }
            }

            OutlinedButton(
                onClick = {
                    PlatformFileProvider.openDownloadFolder()
                    haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
                },
                modifier = Modifier.sizeIn(minWidth = 150.dp),
            ) {
                if (isWideScreen()) {
                    BodyText(text = stringResource(Res.string.open_file))
                    Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                }
                DefaultIcon(Icons.Default.FolderOpen)
            }

            OutlinedButton(
                enabled = canGoNextFromFiles,
                onClick = {
                    if (needsShops) {
                        onIntent(FileGenerationIntent.NavigateTo(FileGenerationStage.SELECT_SHOPS))
                    } else {
                        onIntent(FileGenerationIntent.Generate)
                    }
                    haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                },
                modifier = Modifier.sizeIn(minWidth = 150.dp),
            ) {
                if (isWideScreen()) {
                    BodyText(text = stringResource(Res.string.next))
                    Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                }
                DefaultIcon(Icons.Default.SkipNext)
            }
        }
    }
}

@Composable
private fun FileCardItem(
    file: GeneratedFile,
    openFile: () -> Unit
) {
    Card(
        Modifier.sizeIn(maxWidth = WIDTH_DP_MEDIUM_LOWER_BOUND.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(DefaultValues.Padding.cardContentPadding)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SelectionContainer(modifier = Modifier.weight(1f)) {
                BodyText(text = file.name)
            }
            OpenButton(onOpen = openFile)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MultiShopPicker(
    initialLoading: Boolean,
    isRefreshing: Boolean,
    shops: List<Shop>,
    cities: List<City>, // підстав свій тип
    selectedShopIds: Set<String>,
    onIntent: (FileGenerationIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptic = LocalHapticFeedback.current
    val gridState = rememberLazyGridState()

    if (initialLoading) {
        LoadingComponent()
        return
    }

    val groupedShops = remember(shops) { shops.groupBy { it.cityName } }
    val citiesByName = remember(cities) { cities.associateBy { it.name } }

    val allSelected by remember(shops, selectedShopIds) {
        derivedStateOf { shops.isNotEmpty() && selectedShopIds.size == shops.size }
    }

    PlatformPullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = {
            onIntent(FileGenerationIntent.Refresh)
            haptic.performHapticFeedback(HapticFeedbackType.Confirm)
        },
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(
                DefaultValues.Padding.verticalListItemPadding,
                Alignment.Top
            )
        ) {
            TitleText(stringResource(Res.string.select_shops))

            if (shops.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    BodyText(stringResource(Res.string.empty_list))
                }
            } else {
                Row(Modifier.fillMaxSize()) {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(DefaultValues.Size.gridItemMinSize),
                        modifier = Modifier.weight(1f),
                        state = gridState,
                        verticalArrangement = Arrangement.spacedBy(DefaultValues.Padding.lazyVerticalGridContentPadding),
                        horizontalArrangement = Arrangement.spacedBy(DefaultValues.Padding.lazyVerticalGridContentPadding),
                    ) {
                        item(span = { GridItemSpan(this.maxLineSpan) }) {
                            SelectAllItem(
                                allSelected = allSelected,
                                onSelectAll = {
                                    onIntent(FileGenerationIntent.SelectAllShops)
                                    haptic.performHapticFeedback(HapticFeedbackType.SegmentFrequentTick)
                                },
                                onDeselectAll = {
                                    onIntent(FileGenerationIntent.DeselectAllShops)
                                    haptic.performHapticFeedback(HapticFeedbackType.Reject)
                                },
                            )
                        }

                        groupedShops.forEach { (city, cityShops) ->
                            stickyHeader(key = city) {
                                val cityEntity = citiesByName[city]
                                if (cityEntity != null) StickyCityHeader(cityEntity)
                                else BodyText(text = city)
                            }

                            items(
                                items = cityShops,
                                key = { it.id }
                            ) { shop ->
                                val checked = selectedShopIds.contains(shop.id)
                                CardListItem(
                                    shop = shop,
                                    checked = checked,
                                    onShopToggled = { id, isSelected ->
                                        onIntent(FileGenerationIntent.ToggleShop(id, isSelected))
                                        haptic.performHapticFeedback(HapticFeedbackType.SegmentFrequentTick)
                                    }
                                )
                            }
                        }
                    }

                    DefaultVerticalScrollbar(scrollState = gridState)
                }
            }
        }
    }
}

@Composable
private fun SelectAllItem(
    allSelected: Boolean,
    onSelectAll: () -> Unit,
    onDeselectAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedCard(
        onClick = { if (allSelected) onDeselectAll() else onSelectAll() },
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(DefaultValues.Padding.cardContentPadding)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(DefaultValues.Padding.rowItemPadding)
        ) {
            DefaultCheckbox(
                checked = allSelected,
                onCheckedChange = { isChecked ->
                    if (isChecked) onSelectAll() else onDeselectAll()
                }
            )

            BodyText(
                text = if (allSelected) stringResource(Res.string.deselect_all)
                else stringResource(Res.string.select_all)
            )
        }
    }
}

@Composable
private fun CardListItem(
    shop: Shop,
    checked: Boolean,
    onShopToggled: (String, Boolean) -> Unit,
) {
    val color = shop.status.color

    OutlinedCard(
        onClick = { onShopToggled(shop.id, !checked) },
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.outlinedCardColors(
            containerColor = color.copy(alpha = 0.1f),
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
        border = BorderStroke(2.dp, color.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier
                .padding(DefaultValues.Padding.cardContentPadding)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(DefaultValues.Padding.rowItemPadding)
        ) {
            DefaultCheckbox(
                checked = checked,
                onCheckedChange = { onShopToggled(shop.id, it) }
            )
            Column {
                BodyText(text = "${shop.street}, ${shop.houseNumber}")
                SmallText(
                    text = stringResource(shop.status.title),
                    modifier = Modifier.padding(top = 2.dp),
                    color = color,
                )
            }
        }
    }
}

@Composable
private fun FileTypePicker(
    initialLoading: Boolean,
    selectedFileTypes: Set<FileType>, // або Set<FileTypeId>
    onFileTypeToggled: (FileType, Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptic = LocalHapticFeedback.current
    val listState = rememberLazyListState()

    if (initialLoading) {
        LoadingComponent()
        return
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(DefaultValues.Padding.verticalListItemPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        TitleText(stringResource(Res.string.select_file_types))

        Row(
            Modifier
                .weight(1f)
                .sizeIn(maxWidth = WIDTH_DP_MEDIUM_LOWER_BOUND.dp)
        ) {
            LazyColumn(
                modifier = Modifier.weight(1f),
                state = listState,
                verticalArrangement = Arrangement.spacedBy(DefaultValues.Padding.verticalListItemPadding)
            ) {
                items(
                    items = FileType.allFileTypes,
                    key = { it.id }
                ) { fileType ->
                    val checked = selectedFileTypes.contains(fileType.id) // якщо Set<FileTypeId>
                    OutlinedCard(
                        onClick = {
                            onFileTypeToggled(fileType.id, !checked)
                            haptic.performHapticFeedback(HapticFeedbackType.SegmentFrequentTick)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(DefaultValues.Padding.cardContentPadding)
                                .fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            DefaultCheckbox(
                                checked = checked,
                                onCheckedChange = {
                                    onFileTypeToggled(fileType.id, it)
                                    haptic.performHapticFeedback(HapticFeedbackType.SegmentFrequentTick)
                                }
                            )
                            DefaultIcon(fileType.icon)
                            BodyText(stringResource(fileType.label), Modifier.basicMarquee())
                        }
                    }
                }
            }

            DefaultVerticalScrollbar(scrollState = listState)
        }
    }
}

@OptIn(ExperimentalTime::class)
@Composable
private fun MonthPicker(
    selectedMonth: String,
    onMonthSelected: (String) -> Unit,
    showMonthPicker: Boolean,
    onShowMonthPickerChange: (Boolean) -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    val monthArray = stringArrayResource(Res.array.all_months)

    val selectedIndex by remember(selectedMonth) {
        derivedStateOf { selectedMonth.toIntOrNull()?.minus(1) }
    }

    val selectedLabel by remember(selectedMonth, selectedIndex) {
        derivedStateOf {
            when {
                selectedMonth.isEmpty() -> null
                selectedIndex == null -> null
                selectedIndex !in monthArray.indices -> null
                else -> monthArray[selectedIndex!!]
            }
        }
    }

    OutlinedCard(
        modifier = Modifier
            .sizeIn(maxWidth = WIDTH_DP_MEDIUM_LOWER_BOUND.dp)
            .fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(DefaultValues.Padding.cardContentPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(DefaultValues.Padding.verticalListItemPadding)
        ) {
            TitleText(stringResource(Res.string.select_month))
            OutlinedButton(
                onClick = {
                    onShowMonthPickerChange(true)
                    haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                },
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .sizeIn(minWidth = 150.dp),
            ) {
                BodyText(
                    text = selectedLabel ?: stringResource(Res.string.select)
                )

                DropdownMenu(
                    expanded = showMonthPicker,
                    onDismissRequest = {
                        onShowMonthPickerChange(false)
                        haptic.performHapticFeedback(HapticFeedbackType.Reject)
                    },
                    modifier = Modifier.fillMaxWidth(.3f)
                ) {
                    monthArray.forEachIndexed { index, monthName ->
                        val monthValue = (index + 1).toString().padStart(2, '0')
                        DropdownMenuItem(
                            text = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    BodyText(
                                        text = monthValue,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                    BodyText(monthName)
                                }
                            },
                            onClick = {
                                onMonthSelected(monthValue)
                                onShowMonthPickerChange(false)
                                haptic.performHapticFeedback(HapticFeedbackType.SegmentFrequentTick)
                            },
                            colors = MenuDefaults.itemColors(
                                textColor = if (selectedMonth == monthValue)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.onSurface
                            ),
                        )

                        if (index < monthArray.size - 1) {
                            HorizontalDivider(modifier = Modifier.fillMaxWidth())
                        }
                    }
                }
            }
        }
    }
}


@Preview(name = "FileGeneration • Dark")
@Composable
private fun FileGenerationScreenContentPreview1() = PreviewDarkMaterialTheme {
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { FileGenerationStage.entries.size })

    FileGenerationScreenContent(
        pagerState = pagerState,
        onIntent = {},
        // SELECT_FILES
        initialLoading = false,
        selectedFileTypes = emptySet(), // Set<FileTypeId> або Set<FileType>
        requiresMonth = true,
        selectedMonth = "02",
        showMonthPicker = false,
        canGoNextFromFiles = true,
        needsShops = true,
        // SELECT_SHOPS
        isRefreshing = false,
        shops = FakeBackend.shops,      // <-- реалізуй нижче під свої моделі
        cities = FakeBackend.cities,    // <-- реалізуй нижче під свої моделі
        selectedShopIds = setOf("1", "3"),
        canGenerate = true,
        // GENERATED
        isGenerating = false,
        progress = 0.65f,
        generatedFiles = FakeBackend.generatedFiles, // <-- реалізуй нижче під свої моделі
    )
}

@Preview(name = "FileGeneration • Dark")
@Composable
private fun FileGenerationScreenContentPreview2() = PreviewDarkMaterialTheme {
    val pagerState = rememberPagerState(initialPage = 1, pageCount = { FileGenerationStage.entries.size })

    FileGenerationScreenContent(
        pagerState = pagerState,
        onIntent = {},
        // SELECT_FILES
        initialLoading = false,
        selectedFileTypes = emptySet(), // Set<FileTypeId> або Set<FileType>
        requiresMonth = true,
        selectedMonth = "02",
        showMonthPicker = false,
        canGoNextFromFiles = true,
        needsShops = true,
        // SELECT_SHOPS
        isRefreshing = false,
        shops = FakeBackend.shops,      // <-- реалізуй нижче під свої моделі
        cities = FakeBackend.cities,    // <-- реалізуй нижче під свої моделі
        selectedShopIds = setOf("1", "3"),
        canGenerate = true,
        // GENERATED
        isGenerating = false,
        progress = 0.65f,
        generatedFiles = FakeBackend.generatedFiles, // <-- реалізуй нижче під свої моделі
    )
}

@Preview(name = "FileGeneration • Dark")
@Composable
private fun FileGenerationScreenContentPreview3() = PreviewDarkMaterialTheme {
    val pagerState = rememberPagerState(initialPage = 2, pageCount = { FileGenerationStage.entries.size })

    FileGenerationScreenContent(
        pagerState = pagerState,
        onIntent = {},
        // SELECT_FILES
        initialLoading = false,
        selectedFileTypes = emptySet(), // Set<FileTypeId> або Set<FileType>
        requiresMonth = true,
        selectedMonth = "02",
        showMonthPicker = false,
        canGoNextFromFiles = true,
        needsShops = true,
        // SELECT_SHOPS
        isRefreshing = false,
        shops = FakeBackend.shops,      // <-- реалізуй нижче під свої моделі
        cities = FakeBackend.cities,    // <-- реалізуй нижче під свої моделі
        selectedShopIds = setOf("1", "3"),
        canGenerate = true,
        // GENERATED
        isGenerating = false,
        progress = 0.65f,
        generatedFiles = FakeBackend.generatedFiles, // <-- реалізуй нижче під свої моделі
    )
}


@Preview(name = "FileGeneration • Desktop Dark", device = Devices.DESKTOP)
@Composable
private fun FileGenerationScreenContentPreview4() = PreviewDarkMaterialTheme {
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { FileGenerationStage.entries.size })

    FileGenerationScreenContent(
        pagerState = pagerState,
        onIntent = {},
        // SELECT_FILES
        initialLoading = false,
        selectedFileTypes = emptySet(),
        requiresMonth = true,
        selectedMonth = "",
        showMonthPicker = false,
        canGoNextFromFiles = true,
        needsShops = true,
        // SELECT_SHOPS
        isRefreshing = false,
        shops = FakeBackend.shops,      // <-- реалізуй нижче під свої моделі
        cities = FakeBackend.cities,    // <-- реалізуй нижче під свої моделі
        selectedShopIds = setOf("2"),
        canGenerate = false,
        // GENERATED
        isGenerating = false,
        progress = 0f,
        generatedFiles = FakeBackend.generatedFiles,
    )
}

@Preview(name = "FileGeneration • Desktop Dark", device = Devices.DESKTOP)
@Composable
private fun FileGenerationScreenContentPreview5() = PreviewDarkMaterialTheme {
    val pagerState = rememberPagerState(initialPage = 1, pageCount = { FileGenerationStage.entries.size })

    FileGenerationScreenContent(
        pagerState = pagerState,
        onIntent = {},
        // SELECT_FILES
        initialLoading = false,
        selectedFileTypes = emptySet(),
        requiresMonth = true,
        selectedMonth = "",
        showMonthPicker = false,
        canGoNextFromFiles = true,
        needsShops = true,
        // SELECT_SHOPS
        isRefreshing = false,
        shops = FakeBackend.shops,      // <-- реалізуй нижче під свої моделі
        cities = FakeBackend.cities,    // <-- реалізуй нижче під свої моделі
        selectedShopIds = setOf("2"),
        canGenerate = false,
        // GENERATED
        isGenerating = false,
        progress = 0f,
        generatedFiles = FakeBackend.generatedFiles,
    )
}

@Preview(name = "FileGeneration • Desktop Dark", device = Devices.DESKTOP)
@Composable
private fun FileGenerationScreenContentPreview6() = PreviewDarkMaterialTheme {
    val pagerState = rememberPagerState(initialPage = 2, pageCount = { FileGenerationStage.entries.size })

    FileGenerationScreenContent(
        pagerState = pagerState,
        onIntent = {},
        // SELECT_FILES
        initialLoading = false,
        selectedFileTypes = emptySet(),
        requiresMonth = true,
        selectedMonth = "",
        showMonthPicker = false,
        canGoNextFromFiles = true,
        needsShops = true,
        // SELECT_SHOPS
        isRefreshing = false,
        shops = FakeBackend.shops,      // <-- реалізуй нижче під свої моделі
        cities = FakeBackend.cities,    // <-- реалізуй нижче під свої моделі
        selectedShopIds = setOf("2"),
        canGenerate = false,
        // GENERATED
        isGenerating = false,
        progress = 0f,
        generatedFiles = FakeBackend.generatedFiles,
    )
}
