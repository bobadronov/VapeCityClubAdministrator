package org.bigblackowl.vccadmin.ui.fileGenerator

import org.bigblackowl.vccadmin.data.repository.FileType

sealed interface FileGenerationIntent {
    object Init : FileGenerationIntent
    object Refresh : FileGenerationIntent
    object Exit : FileGenerationIntent

    // stage
    data class NavigateTo(val stage: FileGenerationStage) : FileGenerationIntent
    object GoBack : FileGenerationIntent

    // selections
    data class ToggleFileType(val fileType: FileType, val selected: Boolean) : FileGenerationIntent
    data class ToggleShop(val shopId: String, val selected: Boolean) : FileGenerationIntent
    object SelectAllShops : FileGenerationIntent
    object DeselectAllShops : FileGenerationIntent

    // month picker
    object OpenMonthPicker : FileGenerationIntent
    object CloseMonthPicker : FileGenerationIntent
    data class SelectMonth(val month: String) : FileGenerationIntent

    // generation
    object Generate : FileGenerationIntent
    object ResetGeneratedFiles : FileGenerationIntent

    // actions
    data class OpenFile(val fileName: String) : FileGenerationIntent
    object ShareAllFiles : FileGenerationIntent
}