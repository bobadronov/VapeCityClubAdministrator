package org.bigblackowl.vccadmin.ui.editSlidesSettings

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.window.core.layout.WindowSizeClass.Companion.WIDTH_DP_MEDIUM_LOWER_BOUND
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import org.bigblackowl.vccadmin.data.entity.TransitionEffect
import org.bigblackowl.vccadmin.data.repository.FakeBackend
import org.bigblackowl.vccadmin.data.events.UIEvents
import org.bigblackowl.vccadmin.navigation.NavigationViewModel
import org.bigblackowl.vccadmin.resourses.DefaultValues
import org.bigblackowl.vccadmin.theme.PreviewDarkMaterialTheme
import org.bigblackowl.vccadmin.theme.PreviewLightMaterialTheme
import org.bigblackowl.vccadmin.uiComponent.buttons.BackButton
import org.bigblackowl.vccadmin.uiComponent.buttons.SaveButton
import org.bigblackowl.vccadmin.uiComponent.container.ButtonRowContainer
import org.bigblackowl.vccadmin.uiComponent.dialog.UnsavedChangesDialog
import org.bigblackowl.vccadmin.uiComponent.icons.OnlineIcon
import org.bigblackowl.vccadmin.uiComponent.listItems.DefaultScrollbar
import org.bigblackowl.vccadmin.uiComponent.loading.LoadingComponent
import org.bigblackowl.vccadmin.uiComponent.text.BodyText
import org.bigblackowl.vccadmin.utils.isWideScreen
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import vccadministrator.composeapp.generated.resources.Res
import vccadministrator.composeapp.generated.resources.animation_speed
import vccadministrator.composeapp.generated.resources.effect
import vccadministrator.composeapp.generated.resources.horizontal
import vccadministrator.composeapp.generated.resources.hours_minutes
import vccadministrator.composeapp.generated.resources.hours_only
import vccadministrator.composeapp.generated.resources.last_modified
import vccadministrator.composeapp.generated.resources.last_modified_by_user
import vccadministrator.composeapp.generated.resources.minutes_only
import vccadministrator.composeapp.generated.resources.slide_switch_speed
import vccadministrator.composeapp.generated.resources.slides_list_empty
import vccadministrator.composeapp.generated.resources.slides_preview
import vccadministrator.composeapp.generated.resources.slides_refresh_interval
import vccadministrator.composeapp.generated.resources.transition_effect
import vccadministrator.composeapp.generated.resources.unknown
import vccadministrator.composeapp.generated.resources.vertical
import kotlin.time.Duration.Companion.seconds

// Компонента екрану редагування налаштувань слайдів
@Suppress("AssignedValueIsNeverRead")
@Composable
fun EditSlidesSettingsScreen(
    snackbarHostState: SnackbarHostState,
    navigationViewModel: NavigationViewModel,
    viewModel: EditSlidesSettingsScreenViewModel = koinInject(),
) {
    val uiEvent by viewModel.uiEvent.collectAsStateWithLifecycle(null)
    val settingsTabUiState by viewModel.state.collectAsStateWithLifecycle()
    var showUnsavedDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.onIntent(SlidesSettingsIntent.Load)
    }

    // Обробка одноразових подій (наприклад, помилок у Snackbar)
    LaunchedEffect(uiEvent) {
        uiEvent?.let { event ->
            when (event) {
                is UIEvents.ShowMessage -> snackbarHostState.showSnackbar(event.message)
                is UIEvents.ShowUnsavedChangesDialog -> showUnsavedDialog = true
                is UIEvents.NavigateBack -> navigationViewModel.popBackStack()
                else -> {}
            }
        }
    }

    EditSlidesSettingsScreenContent(
        state = settingsTabUiState,
        onIntent = viewModel::onIntent,
        onBack = {
            viewModel.onIntent(SlidesSettingsIntent.GoBack)
        },
    )

    UnsavedChangesDialog(show = showUnsavedDialog, onSave = {
        viewModel.onIntent(SlidesSettingsIntent.SaveSettings)
        showUnsavedDialog = false
    }, onDiscard = {
        viewModel.onIntent(SlidesSettingsIntent.DiscardChanges)
        showUnsavedDialog = false
    }, onCancel = { showUnsavedDialog = false }, onDismissRequest = { showUnsavedDialog = false })
}

// Вміст табу налаштувань
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditSlidesSettingsScreenContent(
    state: SlidesSettingsState,
    onIntent: (SlidesSettingsIntent) -> Unit,
    onBack: () -> Unit,
) {
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
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(DefaultValues.Padding.verticalListItemPadding),
            ) {
                item {
                    OutlinedCard {
                        Column(
                            modifier = Modifier.padding(10.dp).fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            BodyText(stringResource(Res.string.slide_switch_speed, state.slideDuration))

                            Slider(
                                value = state.slideDuration.toFloat(),
                                onValueChange = { onIntent(SlidesSettingsIntent.ChangeSlideDuration(it.toInt())) },
                                valueRange = 1f..10f,
                                steps = 8,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                item {
                    OutlinedCard {
                        Column(
                            modifier = Modifier.padding(10.dp).fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            BodyText(stringResource(Res.string.animation_speed, state.transitionDuration))

                            Slider(
                                value = state.transitionDuration.toFloat(),
                                onValueChange = { onIntent(SlidesSettingsIntent.ChangeTransitionDuration(it.toInt())) },
                                valueRange = 500f..5000f,
                                steps = 44,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                item {
                    OutlinedCard {
                        Column(
                            modifier = Modifier.padding(10.dp).fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            BodyText(stringResource(Res.string.slides_refresh_interval, rememberAutoReloadTimeText(state.autoReloadTime)))
                            Slider(
                                value = state.autoReloadTime.toFloat(),
                                onValueChange = { onIntent(SlidesSettingsIntent.ChangeAutoReloadTime(it.toInt())) },
                                valueRange = 1f..180f,
                                steps = 179,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                item {
                    BodyText(stringResource(Res.string.transition_effect))

                    var expanded by remember { mutableStateOf(false) }

                    val effectLabels = TransitionEffect.entries.associateWith { stringResource(it.labelRes) }

                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = it },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        OutlinedTextField(
                            value = effectLabels[state.transitionEffect] ?: stringResource(Res.string.unknown),
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(stringResource(Res.string.effect)) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                            modifier = Modifier.menuAnchor(type = ExposedDropdownMenuAnchorType.PrimaryNotEditable, true).fillMaxWidth()
                        )

                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false },
                            modifier = Modifier.fillParentMaxWidth(),
                            shape = RoundedCornerShape(DefaultValues.Shape.defaultShape)
                        ) {
                            TransitionEffect.entries.forEachIndexed { index, effect ->
                                DropdownMenuItem(
                                    text = { BodyText(effectLabels[effect] ?: effect.name) },
                                    onClick = {
                                        onIntent(SlidesSettingsIntent.ChangeEffect(effect))
                                        expanded = false
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                if (index < TransitionEffect.entries.size - 1) {
                                    HorizontalDivider(modifier = Modifier.fillMaxWidth())
                                }
                            }
                        }
                    }
                }

                item {
                    SlidePreviewItem(state = state)
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally)
                    ) {
                        BodyText(
                            "${stringResource(Res.string.last_modified, state.lastModified)}\n${
                                stringResource(
                                    Res.string.last_modified_by_user, state.lastModifiedByUser
                                )
                            }",
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = .5f),
                        )
                    }
                }
            }

            DefaultScrollbar(scrollState = listState)

        }

        ButtonRowContainer {
            if (isWideScreen()) {
                BackButton(
                    modifier = Modifier.weight(1f), onBack = onBack
                )
            }

            SaveButton(
                modifier = Modifier.weight(1f), onSave = { onIntent(SlidesSettingsIntent.SaveSettings) })
        }
    }

}

@Composable
private fun rememberAutoReloadTimeText(minutesTotal: Int): String {
    return when {
        minutesTotal < 60 -> stringResource(Res.string.minutes_only, minutesTotal)

        minutesTotal % 60 == 0 -> stringResource(Res.string.hours_only, minutesTotal / 60)

        else -> stringResource(
            Res.string.hours_minutes, minutesTotal / 60, minutesTotal % 60
        )
    }
}

// Компонента прев'ю слайду з анімацією
@Composable
private fun SlidePreviewItem(
    state: SlidesSettingsState,
) {
    var currentPage by remember { mutableStateOf(0) }

    var isPortrait by remember { mutableStateOf(false) } // false = горизонтальний, true = вертикальний
    val animatedWidth by animateDpAsState(targetValue = if (isPortrait) 300.dp else 500.dp)
    val animatedHeight by animateDpAsState(targetValue = if (isPortrait) 500.dp else 400.dp)

    val tabTitles = listOf(stringResource(Res.string.horizontal), stringResource(Res.string.vertical))

    LaunchedEffect(state.slides, state.slideDuration, state.transitionEffect, state.transitionDuration) {
        delay(1.seconds)
        if (state.slides.isNotEmpty()) {
            while (isActive) {
                delay(state.slideDuration.seconds)
                currentPage = (currentPage + 1) % state.slides.size
            }
        }
    }

    val slide = state.slides.getOrNull(currentPage)

    OutlinedCard {
        Crossfade(slide) { sl ->
            Column(
                modifier = Modifier.padding(10.dp).fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                BodyText(stringResource(Res.string.slides_preview))
                if (sl == null) {
                    BodyText(
                        text = stringResource(Res.string.slides_list_empty), color = MaterialTheme.colorScheme.error
                    )
                } else {
                    SecondaryTabRow(
                        selectedTabIndex = if (isPortrait) 1 else 0, modifier = Modifier.fillMaxWidth()
                    ) {
                        tabTitles.forEachIndexed { index, title ->
                            Tab(selected = (index == if (isPortrait) 1 else 0), onClick = { isPortrait = (index == 1) }, text = { BodyText(title) })
                        }
                    }

                    AnimatedContent(
                        targetState = sl, transitionSpec = {
                            getTransitionSpec(state.transitionEffect, state.transitionDuration)
                        }, label = "SlideAnimation"
                    ) { animatedSlide ->
                        OnlineIcon(
                            model = animatedSlide.url,
                            modifier = Modifier
                                .rotate(if (isPortrait) -90f else 0f)
                                .width(animatedWidth)
                                .height(animatedHeight)
                                .aspectRatio(16f / 9f),
                        )
                    }
                }
            }
        }
    }
}

// Отримання специфікації переходу для анімації
private fun getTransitionSpec(
    effect: TransitionEffect,
    transitionDuration: Int,
): ContentTransform {
    return when (effect) {
        TransitionEffect.FADE -> {
            fadeIn(tween(transitionDuration, easing = LinearEasing)) togetherWith fadeOut(tween(transitionDuration, easing = LinearEasing))
        }

        TransitionEffect.SLIDE_HORIZONTAL -> {
            slideInHorizontally(tween(transitionDuration, easing = LinearEasing)) { it } togetherWith slideOutHorizontally(
                tween(
                    transitionDuration,
                    easing = LinearEasing
                )
            ) { -it }
        }

        TransitionEffect.SLIDE_VERTICAL -> {
            slideInVertically(tween(transitionDuration, easing = LinearEasing)) { it } togetherWith slideOutVertically(
                tween(
                    transitionDuration,
                    easing = LinearEasing
                )
            ) { -it }
        }

        TransitionEffect.ZOOM -> {
            (scaleIn(tween(transitionDuration)) + fadeIn()) togetherWith (scaleOut(tween(transitionDuration)) + fadeOut())
        }

        TransitionEffect.SLIDE_FADE -> {
            (slideInHorizontally(tween(transitionDuration, easing = LinearEasing)) { it / 2 } + fadeIn(
                tween(transitionDuration, easing = LinearEasing)
            )) togetherWith (slideOutHorizontally(tween(transitionDuration, easing = LinearEasing)) { -it / 2 } + fadeOut(
                tween(transitionDuration, easing = LinearEasing)
            ))
        }

        TransitionEffect.POP -> {
            (scaleIn(
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
            ) + fadeIn(tween(transitionDuration, easing = LinearEasing))) togetherWith (scaleOut(
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
            ) + fadeOut(tween(transitionDuration, easing = LinearEasing)))
        }
    }
}


@Preview(
    showSystemUi = true,
    device = Devices.DESKTOP,
)
@Composable
private fun EditSlidesSettingsContentPreview1() = PreviewDarkMaterialTheme {
    EditSlidesSettingsScreenContent(state = SlidesSettingsState(slides = List(3) {
        SlideOrderItem(
            id = FakeBackend.singleSlide.id,
            fileName = FakeBackend.singleSlide.fileName,
            position = FakeBackend.singleSlide.position,
            url = FakeBackend.singleSlide.publicUrl
        )
    }), onIntent = {}, onBack = {})
}

@Preview
@Composable
private fun EditSlidesSettingsContentPreview2() = PreviewLightMaterialTheme {
    EditSlidesSettingsScreenContent(state = SlidesSettingsState(), onIntent = {}, onBack = {})
}