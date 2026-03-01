package org.bigblackowl.vccadmin.ui.workSchedule

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import kotlinx.datetime.number
import kotlinx.datetime.plus
import org.bigblackowl.vccadmin.domain.repository.LocalRepository
import org.bigblackowl.vccadmin.domain.repository.WorkScheduleRepository

/**
 * Спільна логіка для Create/View:
 * - 28-денне "вікно" (1 тиждень назад + фокусний + 2 тижні вперед)
 * - headerByDay, periodLabel
 * - merge assignments з репо (по тижнях)
 * - zoom clamp/load/save
 *
 * dayNameProvider: suspend (LocalDate) -> "Пн/Вт/..." або повна назва — як тобі треба.
 */
class WorkScheduleHelper(
    private val localRepository: LocalRepository,
    private val workScheduleRepository: WorkScheduleRepository,
    private val dayNameProvider: suspend (LocalDate) -> String,
) {
    private val weeksBack: Int = 1
    private val weeksForward: Int = 2
    private val zoomMin: Float = 0.60f
    private val zoomMax: Float = 1.30f
    private val daysInWeek = 7
    private val windowDays = (weeksBack + 1 + weeksForward) * daysInWeek // 28

    /* ------------------------------ Zoom ------------------------------ */

    fun clampZoom(v: Float): Float = when {
        v < zoomMin -> zoomMin
        v > zoomMax -> zoomMax
        else -> v
    }

    fun loadZoomState(): Float = clampZoom(localRepository.getZoomState())
    fun saveZoomState(scale: Float) = localRepository.saveZoomState(clampZoom(scale))

    /* ------------------------------ Window ------------------------------ */

    fun weekStartOf(date: LocalDate): LocalDate {
        val dow = date.dayOfWeek.isoDayNumber // Mon=1..Sun=7
        return date.minus(dow - 1, DateTimeUnit.DAY)
    }

    fun weekStartForDay(day: LocalDate): LocalDate =
        day.minus(day.dayOfWeek.isoDayNumber - 1, DateTimeUnit.DAY)

    fun buildWindowDays(focusWeekStart: LocalDate): List<LocalDate> {
        val start = focusWeekStart.minus(weeksBack * daysInWeek, DateTimeUnit.DAY)

        return (0 until windowDays).map { start.plus(it, DateTimeUnit.DAY) }
    }

    fun buildPeriodLabel(start: LocalDate?, end: LocalDate?): String {
        if (start == null || end == null) return ""
        return "${ddMm(start)} — ${ddMm(end)}"
    }

    suspend fun buildHeaderByDay(days: List<LocalDate>): Map<LocalDate, String> {
        val out = LinkedHashMap<LocalDate, String>(days.size)
        for (d in days) {
            val dayName = dayNameProvider(d)
            out[d] = "${ddMm(d)} $dayName"
        }
        return out
    }

    /**
     * Витягує розклад по тижнях для всіх днів вікна, а потім “розплющує” в Map<day, Map<shopId, userId?>>
     * shopIds — які колонки ти хочеш мати (на Create може бути відсортований список магазинів, але тут треба Set).
     */
    suspend fun loadMergedAssignmentsForDays(
        days: List<LocalDate>,
        shopIds: Set<String>,
    ): Map<LocalDate, Map<String, String?>> {
        val weekStarts = days.map { weekStartForDay(it) }.distinct()

        // load all weeks (drafts)
        val draftsByWeek: Map<LocalDate, Map<String, Map<String, String?>>> =
            weekStarts.associateWith { ws ->
                workScheduleRepository.loadWorkSchedule(ws)?.assignments.orEmpty()
            }

        // flatten by day
        val out = mutableMapOf<LocalDate, Map<String, String?>>()
        for (day in days) {
            val ws = weekStartForDay(day)
            val rawWeek = draftsByWeek[ws].orEmpty()

            val dayIso = day.toString()
            val rawDayMap = rawWeek[dayIso].orEmpty()

            out[day] = buildMap {
                for (shopId in shopIds) put(shopId, rawDayMap[shopId])
            }
        }
        return out
    }

    /** dd.MM */
    private fun ddMm(day: LocalDate): String {
        val dd = day.day.toString().padStart(2, '0')
        val mm = day.month.number.toString().padStart(2, '0')
        return "$dd.$mm"
    }
}