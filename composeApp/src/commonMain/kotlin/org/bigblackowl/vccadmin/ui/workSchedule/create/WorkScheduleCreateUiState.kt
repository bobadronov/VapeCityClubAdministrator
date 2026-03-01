package org.bigblackowl.vccadmin.ui.workSchedule.create

import kotlinx.datetime.LocalDate
import org.bigblackowl.vccadmin.data.entity.Shop
import org.bigblackowl.vccadmin.data.entity.ShopGroup
import org.bigblackowl.vccadmin.data.entity.User
import org.bigblackowl.vccadmin.theme.DefaultValues

data class WorkScheduleCreateUiState(
    val isInitialLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val weekStart: LocalDate = LocalDate.parse(DefaultValues.Time.date.replace('_', '-')),
    val days: List<LocalDate> = emptyList(),
    val visibleStart: LocalDate? = null,
    val visibleEnd: LocalDate? = null,
    val headerByDay: Map<LocalDate, String> = emptyMap(),
    val periodLabel: String = "",
    val shopsById: Map<String, Shop> = emptyMap(),
    val shopOrder: List<String> = emptyList(),
    val shopGroups: List<ShopGroup> = emptyList(),
    val users: List<User> = emptyList(),
    val assignments: Map<LocalDate, Map<String, String?>> = emptyMap(),
    val conflicts: Set<ConflictCell> = emptySet(),
    val hasUnsavedChanges: Boolean = false,
    val userColors: Map<String, Long> = emptyMap(), // userId -> 0xAARRGGBB
    val zoomScale: Float = 1f,
)