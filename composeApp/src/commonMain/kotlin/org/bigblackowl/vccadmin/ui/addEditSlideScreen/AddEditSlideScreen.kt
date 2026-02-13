@file:Suppress("AssignedValueIsNeverRead")

package org.bigblackowl.vccadmin.ui.addEditSlideScreen

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.window.core.layout.WindowSizeClass.Companion.WIDTH_DP_MEDIUM_LOWER_BOUND
import coil3.compose.AsyncImage
import io.github.vinceglb.filekit.coil.securelyAccessFile
import org.bigblackowl.vccadmin.data.entity.Shop
import org.bigblackowl.vccadmin.data.repository.FakeBackend
import org.bigblackowl.vccadmin.data.state.UIEvents
import org.bigblackowl.vccadmin.data.utils.getGroupedShops
import org.bigblackowl.vccadmin.navigation.NavigationViewModel
import org.bigblackowl.vccadmin.resourses.DefaultValues
import org.bigblackowl.vccadmin.theme.PreviewDarkMaterialTheme
import org.bigblackowl.vccadmin.uiComponent.LoadingComponent
import org.bigblackowl.vccadmin.uiComponent.buttons.BackButton
import org.bigblackowl.vccadmin.uiComponent.buttons.DeleteButton
import org.bigblackowl.vccadmin.uiComponent.buttons.SaveButton
import org.bigblackowl.vccadmin.uiComponent.container.ButtonRowContainer
import org.bigblackowl.vccadmin.uiComponent.dialog.FullscreenZoomableImageViewer
import org.bigblackowl.vccadmin.uiComponent.dialog.UnsavedChangesDialog
import org.bigblackowl.vccadmin.uiComponent.icons.DefaultIcon
import org.bigblackowl.vccadmin.uiComponent.listItems.DefaultScrollbar
import org.bigblackowl.vccadmin.uiComponent.listItems.StickyCityHeader
import org.bigblackowl.vccadmin.uiComponent.text.BodyText
import org.bigblackowl.vccadmin.uiComponent.text.SmallText
import org.bigblackowl.vccadmin.utils.isWideScreen
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import vccadministrator.composeapp.generated.resources.Res
import vccadministrator.composeapp.generated.resources.active_slide
import vccadministrator.composeapp.generated.resources.code
import vccadministrator.composeapp.generated.resources.delete_slide_confirmation
import vccadministrator.composeapp.generated.resources.deselect_all_shops
import vccadministrator.composeapp.generated.resources.deselect_all_tablet
import vccadministrator.composeapp.generated.resources.deselect_all_tv
import vccadministrator.composeapp.generated.resources.device_for_slides
import vccadministrator.composeapp.generated.resources.file_name_en
import vccadministrator.composeapp.generated.resources.main_logo
import vccadministrator.composeapp.generated.resources.select_all_shops
import vccadministrator.composeapp.generated.resources.select_all_tablet
import vccadministrator.composeapp.generated.resources.select_all_tv
import vccadministrator.composeapp.generated.resources.select_image
import vccadministrator.composeapp.generated.resources.select_shops
import vccadministrator.composeapp.generated.resources.shop_address
import vccadministrator.composeapp.generated.resources.shops_not_found
import vccadministrator.composeapp.generated.resources.slides_preview

@Composable
fun AddEditSlideScreen(
    slideId: String? = null,
    snackbarHostState: SnackbarHostState,
    navigationViewModel: NavigationViewModel,
    viewModel: AddEditSlideViewModel = koinInject(),
) {
    val state by viewModel.uiState.collectAsState()

    DisposableEffect(slideId) {
        viewModel.onIntent(AddEditSlideIntent.LoadSlide(slideId))
        onDispose { viewModel.onIntent(AddEditSlideIntent.ClearData) }
    }

    val uiEvent by viewModel.uiEvent.collectAsStateWithLifecycle(null)
    var showUnsavedDialog by remember { mutableStateOf(false) }

    // Обробка одноразових подій (наприклад, помилок у Snackbar)
    LaunchedEffect(uiEvent) {
        uiEvent?.let { event ->
            when (event) {
                is UIEvents.ShowMessage -> {
                    val snackbarResult = snackbarHostState.showSnackbar(event.message, actionLabel = if (event.message.contains("Файл збережено", true)) "Open" else null)
                    when (snackbarResult) {
                        SnackbarResult.ActionPerformed -> viewModel.onIntent(AddEditSlideIntent.OpenFile)
                        else -> {}
                    }
                }

                is UIEvents.NotificationAndNavigate -> {
                    snackbarHostState.showSnackbar(event.message)
                    navigationViewModel.popBackStack()
                }

                is UIEvents.ShowUnsavedChangesDialog -> showUnsavedDialog = true
                is UIEvents.NavigateBack -> navigationViewModel.popBackStack()
            }
        }
    }

    AddSlideScreenContent(
        state = state,
        onIntent = viewModel::onIntent
    )

    UnsavedChangesDialog(show = showUnsavedDialog, onSave = {
        showUnsavedDialog = false
        viewModel.onIntent(AddEditSlideIntent.OnSave)
    }, onDiscard = {
        showUnsavedDialog = false
        viewModel.onIntent(AddEditSlideIntent.DiscardChanges)
    }, onCancel = { showUnsavedDialog = false }, onDismissRequest = { showUnsavedDialog = false })
}

@Composable
private fun AddSlideScreenContent(
    state: AddSlideState,
    onIntent: (intent: AddEditSlideIntent) -> Unit,
) {

    val groupedShops = remember(state.allShopList, state.cities) {
        getGroupedShops(state.allShopList, state.cities)
    }

    val listState = rememberLazyListState()

    if (state.isLoading) {
        LoadingComponent()
        return
    }

    Column(
        modifier = Modifier.padding(DefaultValues.Padding.mainBoxPadding).fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(DefaultValues.Padding.verticalListItemPadding),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            Modifier.weight(1f).sizeIn(maxWidth = WIDTH_DP_MEDIUM_LOWER_BOUND.dp)
        ) {

            LazyColumn(
                modifier = Modifier.weight(1f),
                state = listState,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {

                        ImagePreview(state = state, onIntent = onIntent)

                        Spacer(Modifier.height(13.dp))

                        Button(onClick = { onIntent(AddEditSlideIntent.SelectFile) }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                            BodyText(stringResource(Res.string.select_image))
                        }
                    }
                }

                item {
                    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                        OutlinedTextField(
                            value = state.fileName,
                            onValueChange = { onIntent(AddEditSlideIntent.OnFileNameChanged(it)) },
                            label = { Text(stringResource(Res.string.file_name_en)) },
                            singleLine = true,
                            isError = state.fileNameError != null,
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Помилка валідації
                        state.fileNameError?.let { error ->
                            SmallText(
                                text = error,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(top = 4.dp),
                                textAlign = TextAlign.Center
                            )
                        }

                        // Підказка при виборі файлу з "поганим" ім’ям
                        state.fileNameHint?.let { hint ->
                            SmallText(
                                text = hint,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(top = 4.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                        OutlinedCard {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp, alignment = Alignment.CenterHorizontally),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Switch(checked = state.isActive, onCheckedChange = { onIntent(AddEditSlideIntent.OnActiveChanged(it)) })
                                BodyText(stringResource(Res.string.active_slide))
                            }
                        }
                    }
                }

                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                        BodyText(text = stringResource(Res.string.select_shops))
                    }
                }

                // Якщо немає магазинів
                if (groupedShops.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center
                        ) {
                            BodyText(text = stringResource(Res.string.shops_not_found))
                        }
                    }
                } else {
                    item {
                        Column {
                            OutlinedButton(
                                onClick = { onIntent(AddEditSlideIntent.OnToggleAllShops) },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                DefaultIcon(if (state.isAllSelected) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank)
                                Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                                BodyText(
                                    text = stringResource(
                                        if (state.isAllSelected) Res.string.deselect_all_shops else Res.string.select_all_shops
                                    )
                                )
                            }

                            OutlinedButton(
                                onClick = { onIntent(AddEditSlideIntent.OnToggleAllShopsWithTablet) },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                DefaultIcon(if (state.isAllTabletSelected) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank)
                                Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                                BodyText(
                                    text = stringResource(
                                        if (state.isAllTabletSelected) Res.string.deselect_all_tablet else Res.string.select_all_tablet
                                    )
                                )
                            }

                            OutlinedButton(
                                onClick = { onIntent(AddEditSlideIntent.OnToggleAllShopsWithTv) },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                DefaultIcon(if (state.isAllTvSelected) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank)
                                Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                                BodyText(
                                    text = stringResource(
                                        if (state.isAllTvSelected) Res.string.deselect_all_tv else Res.string.select_all_tv
                                    )
                                )
                            }
                        }
                    }

                    groupedShops.forEach { shopGroup ->
                        stickyHeader(
                            key = "header_${shopGroup.city.id}" // важливо для коректного sticky
                        ) {
                            StickyCityHeader(shopGroup.city)
                        }

                        items(items = shopGroup.shops, key = { it.id } // або it.code, якщо унікальний
                        ) { shop ->
                            ShopCardSlideListItem(shop, state.selectedShopCodes, onIntent = onIntent)
                        }
                    }
                }
            }

            DefaultScrollbar(scrollState = listState)

        }

        ButtonRowContainer {
            if (isWideScreen()) {
                BackButton(modifier = Modifier.weight(.5f)) { onIntent(AddEditSlideIntent.GoBack) }
            }

            if (state.slideId != null) {
                DeleteButton(
                    message = stringResource(Res.string.delete_slide_confirmation),
                    modifier = Modifier.weight(.5f),
                    onDeleteConfirmed = { onIntent(AddEditSlideIntent.DeleteSlide(state.slideId)) })
            }

            SaveButton(
                onSave = { onIntent(AddEditSlideIntent.OnSave) }, modifier = Modifier.weight(.5f), enabled = !state.isLoading
            )
        }
    }
}

// AddEditSlideScreen.kt (фрагмент)

@Composable
private fun LazyItemScope.ShopCardSlideListItem(
    shop: Shop,
    list: Set<String>,
    onIntent: (intent: AddEditSlideIntent) -> Unit
) {

    Card(
        onClick = { onIntent(AddEditSlideIntent.OnShopToggled(shop.code)) },
        modifier = Modifier
            .fillMaxWidth()
            .animateItem()
    ) {
        Column(
            modifier = Modifier
                .padding(DefaultValues.Padding.cardContentPadding)
                .fillMaxWidth(),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(
                    checked = shop.code in list,
                    onCheckedChange = { onIntent(AddEditSlideIntent.OnShopToggled(shop.code)) }
                )
                BodyText(
                    text = stringResource(Res.string.shop_address, shop.street, shop.houseNumber, shop.addressComment),
                )
            }

            SmallText(text = "${stringResource(Res.string.code)}: ${shop.code}")

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(15.dp)
            ) {
                SmallText(
                    text = "${stringResource(Res.string.device_for_slides)}: ${stringResource(shop.deviceType.title)}",
                )
                DefaultIcon(shop.deviceType.icon)
            }
        }
    }
}

@Composable
private fun ImagePreview(
    state: AddSlideState,
    onIntent: (intent: AddEditSlideIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val imageRequest = remember(state.selectedFile, state.currentImageUrl) {
        when {
            state.selectedFile != null -> state.selectedFile
            state.currentImageUrl != null -> state.currentImageUrl
            else -> null
        }
    }

    var showViewer by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        AnimatedContent(
            targetState = imageRequest,
            transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(300)) },
            label = "ImagePreviewTransition",
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
                .clip(RoundedCornerShape(12.dp))
        ) { request ->
            if (request != null) {
                AsyncImage(
                    model = request,
                    contentDescription = stringResource(Res.string.slides_preview),
                    modifier = Modifier.fillMaxSize(),
                    onState = { sta ->
                        sta.securelyAccessFile(state.selectedFile)
                    }
                )
            } else {
                DefaultIcon(painterResource(Res.drawable.main_logo))
            }
        }

        if (imageRequest != null) {
            Column(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilledIconButton(
                    onClick = { showViewer = true },
                ) {
                    DefaultIcon(Icons.Default.Fullscreen, tint = LocalContentColor.current)
                }

                when (state.isFileDownloaded) {
                    true -> {
                        FilledIconButton(
                            onClick = { onIntent(AddEditSlideIntent.OpenFile) },
                        ) {
                            DefaultIcon(Icons.Default.FileOpen, tint = LocalContentColor.current)
                        }
                    }

                    else -> {
                        FilledIconButton(
                            onClick = { onIntent(AddEditSlideIntent.DownloadIconFile) },
                        ) {
                            DefaultIcon(Icons.Default.Download, tint = LocalContentColor.current)
                        }
                    }
                }
            }
        }
    }

    if (showViewer && imageRequest != null) {
        FullscreenZoomableImageViewer(
            model = imageRequest,
            onClose = { showViewer = false },
        )
    }
}

@Preview
@Composable
private fun AddSlideScreenContentPreview1() = PreviewDarkMaterialTheme {
    AddSlideScreenContent(
        state = AddSlideState(
            isLoading = false,
            allShopList = FakeBackend.shops,
            slideId = "sad",
            cities = FakeBackend.cities,
            fileNameError = "akjlkasjflkjas",
            fileNameHint = "kjhsakfjhakjhsd",
        ),
        onIntent = {}
    )
}