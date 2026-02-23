package org.bigblackowl.vccadmin.ui.fileGenerator

import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime
import org.bigblackowl.vccadmin.data.entity.City
import org.bigblackowl.vccadmin.data.entity.Shop
import org.bigblackowl.vccadmin.domain.repository.FileType
import kotlin.time.Clock

data class FileGenerationUiState(
    val initialLoading: Boolean = true,

    // data
    val shops: List<Shop> = emptyList(),
    val cities: List<City> = emptyList(),

    // navigation/stage
    val stage: FileGenerationStage = FileGenerationStage.SELECT_FILES,

    // selections
    val selectedFileTypes: Set<FileType> = emptySet(),
    val selectedShopIds: Set<String> = emptySet(),
    val selectedMonth: String = defaultMonthNext(),

    // UI flags
    val showMonthPicker: Boolean = false,
    val isRefreshing: Boolean = false, // ✅

    // derived/validation
    val requiresMonth: Boolean = false,
    val needsShops: Boolean = false,
    val canGoNextFromFiles: Boolean = false,
    val canGenerate: Boolean = false,

    // generation state
    val isGenerating: Boolean = false,
    val progress: Float = 0f,

    // results
    val generatedFiles: List<GeneratedFile> = emptyList(),
)

private fun defaultMonthNext(): String {
    val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
    val next = if (now.month.number == 12) 1 else now.month.number + 1
    return next.toString().padStart(2, '0')
}