package org.bigblackowl.vccadmin.ui.workSchedule.view

import kotlinx.datetime.LocalDate
import org.bigblackowl.vccadmin.data.entity.Shop
import org.bigblackowl.vccadmin.data.entity.User
import org.bigblackowl.vccadmin.theme.DefaultValues

data class WorkScheduleViewUiState(
    val isInitialLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val errorText: String? = null,
    val currentUser: User? = null,
    val selectedUserId: String? = null,
    val weekStart: LocalDate = LocalDate.parse(DefaultValues.Time.date.replace('_', '-')),
    val days: List<LocalDate> = emptyList(),
    val headerByDay: Map<LocalDate, String> = emptyMap(),
    val periodLabel: String = "",
    val shopsById: Map<String, Shop> = emptyMap(),
    val shopOrder: List<String> = emptyList(),
    val users: List<User> = emptyList(),
    val usersById: Map<String, User> = emptyMap(),
    val userColors: Map<String, Long> = emptyMap(),
    val assignments: Map<LocalDate, Map<String, String?>> = emptyMap(),
    val zoomScale: Float = 1f,
)