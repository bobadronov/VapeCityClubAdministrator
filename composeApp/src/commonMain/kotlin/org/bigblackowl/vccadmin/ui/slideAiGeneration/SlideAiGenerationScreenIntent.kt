package org.bigblackowl.vccadmin.ui.slideAiGeneration

import com.aallam.openai.api.image.ImageSize

sealed interface SlideAiGenerationScreenIntent {
    data class SetPrompt(val text: String) : SlideAiGenerationScreenIntent
    data class SelectTemplate(val id: String?) : SlideAiGenerationScreenIntent

    data object PickUserPhoto : SlideAiGenerationScreenIntent
    data class UserPhotoPicked(val image: LocalImage) : SlideAiGenerationScreenIntent
    data object RemoveUserPhoto : SlideAiGenerationScreenIntent

    data class SetMode(val mode: GenerationModeUi) : SlideAiGenerationScreenIntent
    data class SetModel(val modelId: String) : SlideAiGenerationScreenIntent
    data class SetSize(val size: ImageSize) : SlideAiGenerationScreenIntent
    data object IncN : SlideAiGenerationScreenIntent
    data object DecN : SlideAiGenerationScreenIntent

    data class SelectResult(val index: Int) : SlideAiGenerationScreenIntent

    data object Generate : SlideAiGenerationScreenIntent
    data object Retry : SlideAiGenerationScreenIntent
    data object ClearResult : SlideAiGenerationScreenIntent
}