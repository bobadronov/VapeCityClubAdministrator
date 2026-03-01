package org.bigblackowl.vccadmin.ui.workSchedule.create

import kotlinx.datetime.LocalDate

sealed interface WorkScheduleCreateIntent {
    data object Load : WorkScheduleCreateIntent
    data object Save : WorkScheduleCreateIntent
    data object NextWeek : WorkScheduleCreateIntent
    data object PrevWeek : WorkScheduleCreateIntent
    data class SetAssignment(val day: LocalDate, val shopId: String, val userId: String?) : WorkScheduleCreateIntent
    data class MoveUser(
        val fromDay: LocalDate,
        val fromShopId: String,
        val toDay: LocalDate,
        val toShopId: String,
    ) : WorkScheduleCreateIntent

    data class SetUserColor(val userId: String, val argb: Long) : WorkScheduleCreateIntent
    data object ZoomIn : WorkScheduleCreateIntent
    data object ZoomOut : WorkScheduleCreateIntent
    data object ZoomReset : WorkScheduleCreateIntent
    data class ZoomSet(val scale: Float) : WorkScheduleCreateIntent
    data object NavigateToPreview : WorkScheduleCreateIntent
}