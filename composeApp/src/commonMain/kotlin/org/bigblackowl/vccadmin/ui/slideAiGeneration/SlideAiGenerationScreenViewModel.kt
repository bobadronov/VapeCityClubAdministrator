package org.bigblackowl.vccadmin.ui.slideAiGeneration

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.aakira.napier.Napier
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.openFilePicker
import io.github.vinceglb.filekit.name
import io.github.vinceglb.filekit.readBytes
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.bigblackowl.vccadmin.data.errorManager.ErrorCode
import org.bigblackowl.vccadmin.data.errorManager.ErrorManager
import org.bigblackowl.vccadmin.data.repository.AiRepository
import org.bigblackowl.vccadmin.data.state.UIEvents

/**
 * =================
 *   ViewModel MVI
 * =================
 */
class SlideAiGenerationScreenViewModel(
    private val errorManager: ErrorManager,
    private val aiRepository: AiRepository,
) : ViewModel() {
    companion object { private const val TAG = "SlideAiGenerationScreenViewModel" }

    private val _uiState = MutableStateFlow(SlideAiGenerationScreenUiState())
    val uiState: StateFlow<SlideAiGenerationScreenUiState> = _uiState.asStateFlow()

    private val _uiEvent = MutableSharedFlow<UIEvents>(replay = 0)
    val uiEvent: SharedFlow<UIEvents> = _uiEvent.asSharedFlow()

    init {
        viewModelScope.launch {
            runCatching { aiRepository.getModels() }
                .onSuccess { models ->
                    val ids = models.map { it.id.id }
                    val imageIds = ids
                        .filter { it.startsWith("gpt-image-") || it.startsWith("dall-e-") }
                        .sorted()

                    _uiState.update {
                        it.copy(
                            availableModels = imageIds,
                            settings = it.settings.copy(
                                modelId = imageIds.firstOrNull { m -> m.startsWith("dall-e-") }
                                    ?: imageIds.firstOrNull()
                                    ?: it.settings.modelId
                            )
                        )
                    }
                }
                .onFailure { e ->
                    Napier.e(tag = TAG) { "getModels failed: ${e.message}" }
                    errorManager.report(message = e.message.orEmpty(), errorCode = ErrorCode.SlideAiGenerationScreenViewModel.LOAD_DATA)
                }
        }
    }

    fun onIntent(intent: SlideAiGenerationScreenIntent) {
        when (intent) {
            is SlideAiGenerationScreenIntent.SetPrompt ->
                _uiState.update { it.copy(prompt = intent.text, error = null) }

            is SlideAiGenerationScreenIntent.SelectTemplate ->
                _uiState.update { it.copy(selectedTemplateId = intent.id, error = null) }

            SlideAiGenerationScreenIntent.PickUserPhoto ->
                viewModelScope.launch { val picked: PlatformFile? = FileKit.openFilePicker(type = FileKitType.Image)
                    if (picked == null) {
                        _uiEvent.emit(UIEvents.ShowMessage("Файл не обрано"))
                        return@launch
                    }

                    val bytes = picked.readBytes()
                    val mime = guessMimeByName(picked.name) ?: "image/jpeg"
                    onIntent(
                        SlideAiGenerationScreenIntent.UserPhotoPicked(
                            LocalImage(bytes = bytes, mime = mime, fileName = picked.name)
                        )
                    ) }

            is SlideAiGenerationScreenIntent.UserPhotoPicked ->
                _uiState.update { it.copy(userPhoto = intent.image, error = null) }

            SlideAiGenerationScreenIntent.RemoveUserPhoto ->
                _uiState.update { it.copy(userPhoto = null, error = null) }

            is SlideAiGenerationScreenIntent.SetMode ->
                _uiState.update { it.copy(mode = intent.mode, error = null) }

            is SlideAiGenerationScreenIntent.SetModel ->
                _uiState.update { it.copy(settings = it.settings.copy(modelId = intent.modelId), error = null) }

            is SlideAiGenerationScreenIntent.SetSize ->
                _uiState.update { it.copy(settings = it.settings.copy(size = intent.size), error = null) }

            SlideAiGenerationScreenIntent.IncN ->
                _uiState.update { it.copy(settings = it.settings.copy(n = (it.settings.n + 1).coerceAtMost(4))) }

            SlideAiGenerationScreenIntent.DecN ->
                _uiState.update { it.copy(settings = it.settings.copy(n = (it.settings.n - 1).coerceAtLeast(1))) }

            is SlideAiGenerationScreenIntent.SelectResult ->
                _uiState.update { it.copy(selectedIndex = intent.index.coerceIn(0, (it.results.size - 1).coerceAtLeast(0))) }

            SlideAiGenerationScreenIntent.ClearResult ->
                _uiState.update { it.copy(results = emptyList(), selectedIndex = 0, error = null) }

            SlideAiGenerationScreenIntent.Generate,
            SlideAiGenerationScreenIntent.Retry ->
                generate()
        }
    }

    private fun generate() {
        val snap = _uiState.value
        val prompt = snap.prompt.trim()

        // Мінімальні валідації
        if (prompt.isEmpty() && snap.mode == GenerationModeUi.Generate) {
            viewModelScope.launch { _uiEvent.emit(UIEvents.ShowMessage("Введіть prompt")) }
            return
        }

        val needPhoto = snap.mode in setOf(
            GenerationModeUi.EditWithPhoto,
            GenerationModeUi.Variations,
            GenerationModeUi.TemplateWithPhoto
        )
        if (needPhoto && snap.userPhoto == null) {
            viewModelScope.launch { _uiEvent.emit(UIEvents.ShowMessage("Для цього режиму потрібно фото")) }
            return
        }

        val template = snap.templates.firstOrNull { it.id == snap.selectedTemplateId }
        if (snap.mode == GenerationModeUi.TemplateWithPhoto && template == null) {
            viewModelScope.launch { _uiEvent.emit(UIEvents.ShowMessage("Оберіть template")) }
            return
        }

        val mode: GenerateMode = when (snap.mode) {
            GenerationModeUi.Generate -> GenerateMode.TextOnly
            GenerationModeUi.EditWithPhoto -> GenerateMode.EditWithPhoto(snap.userPhoto!!)
            GenerationModeUi.Variations -> GenerateMode.Variations(snap.userPhoto!!)
            GenerationModeUi.TemplateWithPhoto -> GenerateMode.TemplateWithPhoto(template!!, snap.userPhoto!!)
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            runCatching {
                aiRepository.generate(prompt = prompt, mode = mode, settings = snap.settings)
            }.onSuccess { imgs ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        results = imgs,
                        selectedIndex = 0,
                        error = null
                    )
                }
            }.onFailure { e ->
                Napier.e(tag = TAG) { e.message.orEmpty() }
                _uiState.update { it.copy(isLoading = false, error = e.message ?: "Помилка генерації") }
                _uiEvent.emit(UIEvents.ShowMessage("Помилка: ${e.message ?: "unknown"}"))
                errorManager.report(message = e.message.orEmpty(), errorCode = ErrorCode.SlideAiGenerationScreenViewModel.GENERATE)
            }
        }
    }
}

/**
 * ==========
 *  Helpers
 * ==========
 */
private fun guessMimeByName(name: String): String? {
    val lower = name.lowercase()
    return when {
        lower.endsWith(".png") -> "image/png"
        lower.endsWith(".jpg") || lower.endsWith(".jpeg") -> "image/jpeg"
        lower.endsWith(".webp") -> "image/webp"
        else -> null
    }
}
