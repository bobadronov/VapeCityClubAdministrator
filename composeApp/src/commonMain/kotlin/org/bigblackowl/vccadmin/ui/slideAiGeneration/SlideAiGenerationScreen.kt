package org.bigblackowl.vccadmin.ui.slideAiGeneration

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.aallam.openai.api.image.ImageSize
import org.bigblackowl.vccadmin.data.events.UIEvents
import org.bigblackowl.vccadmin.navigation.NavigationViewModel
import org.bigblackowl.vccadmin.theme.DefaultValues
import org.bigblackowl.vccadmin.theme.PreviewDarkMaterialTheme
import org.bigblackowl.vccadmin.theme.PreviewLightMaterialTheme
import org.bigblackowl.vccadmin.uiComponent.buttons.BackButton
import org.bigblackowl.vccadmin.uiComponent.buttons.RetryButton
import org.bigblackowl.vccadmin.uiComponent.container.ButtonRowContainer
import org.bigblackowl.vccadmin.uiComponent.icons.DefaultIcon
import org.bigblackowl.vccadmin.uiComponent.icons.OnlineIcon
import org.bigblackowl.vccadmin.uiComponent.listItems.DefaultVerticalScrollbar
import org.bigblackowl.vccadmin.uiComponent.text.HelperText
import org.bigblackowl.vccadmin.uiComponent.text.TitleText
import org.bigblackowl.vccadmin.utils.isWideScreen
import org.koin.compose.koinInject

/**
 * ==============
 *   UI (Route // https://platform.openai.com/settings/organization/billing/overview
 * ==============
 */
@Composable
fun SlideAiGenerationScreen(
    snackbarHostState: SnackbarHostState,
    navigationViewModel: NavigationViewModel,
    viewModel: SlideAiGenerationScreenViewModel = koinInject(),
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.uiEvent.collect { eff ->
            when (eff) {
                is UIEvents.ShowMessage -> {
                    snackbarHostState.showSnackbar(eff.message)
                }

                else -> {}
            }
        }
    }

    SlideAiGenerationScreenContent(
        state = state,
        onIntent = viewModel::onIntent,
        onBack = { navigationViewModel.requestBack() }
    )
}

/**
 * =================
 *   UI (Content)
 * =================
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SlideAiGenerationScreenContent(
    state: SlideAiGenerationScreenUiState,
    onIntent: (SlideAiGenerationScreenIntent) -> Unit,
    onBack: () -> Unit,
) {
    val listState = rememberLazyListState()

    Column(
        modifier = Modifier.padding(DefaultValues.Padding.mainBoxPadding).fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(DefaultValues.Padding.verticalListItemPadding)
    ) {

        TitleText("BETA TEST, NOT WORK YET!!!", color = Color.Red) /// todo: remove after test

        Row(
            modifier = Modifier.weight(1f),
        ) {
            LazyColumn(
                modifier = Modifier
                    .weight(1f),
                state = listState,
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                item {
                    // Header
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Генерація / редагування / варіації з превʼю та налаштуваннями.",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
                item {
                    // Prompt card
                    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedTextField(
                                value = state.prompt,
                                onValueChange = { onIntent(SlideAiGenerationScreenIntent.SetPrompt(it)) },
                                label = { Text("Prompt") },
                                modifier = Modifier.fillMaxWidth(),
                                minLines = 3,
                            )

                            // Templates (optional)
                            if (state.templates.isNotEmpty()) {
                                Row(
                                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    state.templates.forEach { t ->
                                        FilterChip(
                                            selected = state.selectedTemplateId == t.id,
                                            onClick = { onIntent(SlideAiGenerationScreenIntent.SelectTemplate(t.id)) },
                                            label = { Text(t.title) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                item {
                    // Settings card
                    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {

                            // Model picker
                            var modelExpanded by remember { mutableStateOf(false) }
                            ExposedDropdownMenuBox(
                                expanded = modelExpanded,
                                onExpandedChange = { modelExpanded = !modelExpanded },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                OutlinedTextField(
                                    value = state.settings.modelId,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Model") },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = modelExpanded) },
                                    modifier = Modifier
                                        .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                                        .fillMaxWidth(),
                                )
                                ExposedDropdownMenu(
                                    expanded = modelExpanded,
                                    onDismissRequest = { modelExpanded = false }
                                ) {
                                    val items = state.availableModels.ifEmpty { listOf("dall-e-3", "dall-e-2") }

                                    items.forEach { id ->
                                        DropdownMenuItem(
                                            text = { Text(id.uppercase()) },
                                            onClick = {
                                                modelExpanded = false
                                                onIntent(SlideAiGenerationScreenIntent.SetModel(id))
                                            }
                                        )
                                    }
                                }
                            }

                            // Mode selector
                            Text("Mode", style = MaterialTheme.typography.labelLarge)
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                GenerationModeUi.entries.forEach { m ->
                                    FilterChip(
                                        selected = state.mode == m,
                                        onClick = { onIntent(SlideAiGenerationScreenIntent.SetMode(m)) },
                                        leadingIcon = { if (state.mode == m) DefaultIcon(Icons.Default.Done) },
                                        label = { Text(m.name) }
                                    )
                                }
                            }

                            // Size selector (simple)
                            Text("Size", style = MaterialTheme.typography.labelLarge)
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf(
                                    ImageSize.is256x256,
                                    ImageSize.is512x512,
                                    ImageSize.is1024x1024
                                ).forEach { s ->
                                    FilterChip(
                                        selected = state.settings.size == s,
                                        onClick = { onIntent(SlideAiGenerationScreenIntent.SetSize(s)) },
                                        leadingIcon = { if (state.settings.size == s) DefaultIcon(Icons.Default.Done) },
                                        label = { Text(s.size.uppercase()) }
                                    )
                                }
                            }

                            // N selector
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text("Variants: ${state.settings.n}", style = MaterialTheme.typography.labelLarge)
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    OutlinedButton(
                                        onClick = { onIntent(SlideAiGenerationScreenIntent.DecN) },
                                        enabled = !state.isLoading && state.settings.n > 1
                                    ) { Text("-") }
                                    OutlinedButton(
                                        onClick = { onIntent(SlideAiGenerationScreenIntent.IncN) },
                                        enabled = !state.isLoading && state.settings.n < 4
                                    ) { Text("+") }
                                }
                            }

                            HorizontalDivider()
                            ButtonRowContainer {
                                Button(
                                    onClick = { onIntent(SlideAiGenerationScreenIntent.PickUserPhoto) },
                                    enabled = !state.isLoading,
                                    modifier = Modifier.weight(1f),
                                ) {
                                    Text(if (state.userPhoto == null) "Обрати фото" else "Змінити фото")
                                }
                                if (state.userPhoto != null) {
                                    OutlinedButton(
                                        onClick = { onIntent(SlideAiGenerationScreenIntent.RemoveUserPhoto) },
                                        enabled = !state.isLoading,
                                        modifier = Modifier.weight(1f),
                                    ) {
                                        Text("Прибрати фото")
                                    }
                                }

                                // Generate button
                                Button(
                                    onClick = { onIntent(SlideAiGenerationScreenIntent.Generate) },
                                    enabled = !state.isLoading,
                                    modifier = Modifier.weight(1f),
                                ) {
                                    if (state.isLoading) {
                                        CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                                        Spacer(Modifier.width(8.dp))
                                    }
                                    Text("Згенерувати")
                                }
                            }
                        }
                    }
                }

                item {
                    state.error?.let {
                        SelectionContainer(modifier = Modifier.weight(1f)) {
                            HelperText(text = it, color = MaterialTheme.colorScheme.error)
                        }
                    }
                }

                item {
                    // Results gallery + selected preview
                    if (state.results.isNotEmpty()) {
                        OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text("Results", style = MaterialTheme.typography.titleMedium)

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    state.results.forEachIndexed { index, img ->
                                        val selected = index == state.selectedIndex
                                        Box(
                                            modifier = Modifier
                                                .size(90.dp)
                                                .clip(MaterialTheme.shapes.medium)
                                                .background(
                                                    if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                                )
                                                .clickable { onIntent(SlideAiGenerationScreenIntent.SelectResult(index)) }
                                                .padding(6.dp)
                                        ) {
                                            OnlineIcon(
                                                model = img.bytes,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        }
                                    }
                                }

                                val selected = state.results.getOrNull(state.selectedIndex)
                                if (selected != null) {
                                    OnlineIcon(
                                        model = selected.bytes,
                                        modifier = Modifier.fillMaxWidth().height(340.dp),
                                    )

                                    ButtonRowContainer {

                                        OutlinedButton(
                                            onClick = { onIntent(SlideAiGenerationScreenIntent.ClearResult) },
                                            modifier = Modifier.weight(1f),
                                        ) { Text("Очистити") }


                                        RetryButton(
                                            onClick = { onIntent(SlideAiGenerationScreenIntent.Retry) },
                                            modifier = Modifier.weight(1f),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            DefaultVerticalScrollbar(scrollState = listState)
        }

        ButtonRowContainer {
            if (isWideScreen()) {
                BackButton(
//                    modifier = Modifier.weight(.5f),
                    onBack = onBack
                )
            }
        }
    }
}


@Preview
@Composable
fun SlideAiGenerationScreenContentPreview1() = PreviewDarkMaterialTheme {
    SlideAiGenerationScreenContent(
        state = SlideAiGenerationScreenUiState(),
        onIntent = {},
        onBack = {},
    )
}

@Preview
@Composable
fun SlideAiGenerationScreenContentPreview2() = PreviewLightMaterialTheme {
    SlideAiGenerationScreenContent(
        state = SlideAiGenerationScreenUiState(),
        onIntent = {},
        onBack = {},
    )
}