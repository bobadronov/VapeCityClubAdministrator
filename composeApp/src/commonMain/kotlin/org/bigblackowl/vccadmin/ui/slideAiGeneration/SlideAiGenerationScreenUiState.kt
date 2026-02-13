package org.bigblackowl.vccadmin.ui.slideAiGeneration

/**
 * ======================
 *   MVI: State/Intent
 * ======================
 */
data class SlideAiGenerationScreenUiState(
    val prompt: String = "",
    val templates: List<TemplateSpec> = emptyList(),
    val selectedTemplateId: String? = null,
    val userPhoto: LocalImage? = null,

    val mode: GenerationModeUi = GenerationModeUi.Generate,
    val settings: ImageGenSettings = ImageGenSettings(),
    val availableModels: List<String> = emptyList(),

    val isLoading: Boolean = false,
    val results: List<GeneratedImage> = emptyList(),
    val selectedIndex: Int = 0,
    val error: String? = null,
)