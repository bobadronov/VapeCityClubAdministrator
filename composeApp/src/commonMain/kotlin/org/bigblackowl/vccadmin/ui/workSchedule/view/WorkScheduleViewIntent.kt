package org.bigblackowl.vccadmin.ui.workSchedule.view

sealed interface WorkScheduleViewIntent {
    data object Load : WorkScheduleViewIntent
    data object Refresh : WorkScheduleViewIntent
    data class SelectUser(val userId: String?) : WorkScheduleViewIntent
    data object PrevWeek : WorkScheduleViewIntent
    data object NextWeek : WorkScheduleViewIntent
    data object ZoomIn : WorkScheduleViewIntent
    data object ZoomOut : WorkScheduleViewIntent
    data object ZoomReset : WorkScheduleViewIntent
    data class ZoomSet(val scale: Float) : WorkScheduleViewIntent
}