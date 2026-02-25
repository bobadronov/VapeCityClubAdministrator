package org.bigblackowl.vccadmin.ui.workSchedule.create

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.Serializable
import org.bigblackowl.vccadmin.data.entity.City
import org.bigblackowl.vccadmin.data.entity.Shop
import org.bigblackowl.vccadmin.data.entity.ShopGroup
import org.bigblackowl.vccadmin.data.entity.User
import org.bigblackowl.vccadmin.data.entity.toUiShops
import org.bigblackowl.vccadmin.data.events.UIEvents
import org.bigblackowl.vccadmin.domain.repository.CityRepository
import org.bigblackowl.vccadmin.domain.repository.LocalRepository
import org.bigblackowl.vccadmin.domain.repository.ShopRepository
import org.bigblackowl.vccadmin.domain.repository.UserRepository
import kotlin.time.Clock

class WorkScheduleCreateScreenViewModel(
    private val shopRepository: ShopRepository,
    private val userRepository: UserRepository,
    private val cityRepository: CityRepository,
    private val storage: LocalRepository
) : ViewModel() {

    private val clock: Clock = Clock.System

    private val _uiState = MutableStateFlow(WorkScheduleCreateUiState(isInitialLoading = true))
    val uiState: StateFlow<WorkScheduleCreateUiState> = _uiState.asStateFlow()

    private val _uiEvent = MutableSharedFlow<UIEvents>(replay = 0)
    val uiEvent: SharedFlow<UIEvents> = _uiEvent.asSharedFlow()

    // ✅ anchor (правий тиждень)
    private var anchorWeekStart: LocalDate? = null

    fun onIntent(intent: WorkScheduleCreateIntent) {
        when (intent) {
            WorkScheduleCreateIntent.Load -> load()
            WorkScheduleCreateIntent.Save -> save()
            WorkScheduleCreateIntent.NextWeek -> shiftWeek(+7)
            WorkScheduleCreateIntent.PrevWeek -> shiftWeek(-7)
            is WorkScheduleCreateIntent.SetAssignment -> setAssignment(intent.day, intent.shopId, intent.userId)
            is WorkScheduleCreateIntent.MoveUser -> moveUser(intent)
        }
    }

    private fun load() = viewModelScope.launch {
        _uiState.update { it.copy(isInitialLoading = true) }

        val shopsRaw = shopRepository.getStores()
        val users = userRepository.getUsers()
        val cities = cityRepository.getCities()

        val week = currentWeekStart(clock)
        anchorWeekStart = week

        val shopsUi = shopsRaw.toUiShops(cities)
        val groups = getGroupedShops(shopsUi, cities)
        val sortedShops = groups.flatMap { it.shops }
        val shopsById = sortedShops.associateBy { it.id }
        val shopOrder = sortedShops.map { it.id }

        val days14 = buildDays14(anchorWeekStart!!)

        // ✅ load drafts for two weeks
        val merged = loadMergedAssignmentsFor14Days(
            prevWeekStart = anchorWeekStart!!.minus(7, DateTimeUnit.Companion.DAY),
            anchorWeekStart = anchorWeekStart!!,
            days14 = days14,
            shopIds = shopsById.keys
        )

        _uiState.value = WorkScheduleCreateUiState(
            isInitialLoading = false,
            weekStart = anchorWeekStart!!,
            days = days14,
            shopsById = shopsById,
            shopOrder = shopOrder,
            shopGroups = groups,
            users = users,
            assignments = merged,
            conflicts = computeConflicts(days14, shopsById.keys, merged),
            hasUnsavedChanges = false,
        )
    }

    private fun shiftWeek(deltaDays: Int) = viewModelScope.launch {
        persistCurrentDraftIfDirty() // ✅ автозбереження перед зсувом

        val current = _uiState.value
        val newAnchor = current.weekStart.plus(deltaDays, DateTimeUnit.Companion.DAY)
        anchorWeekStart = newAnchor

        _uiState.update { it.copy(isInitialLoading = true) }

        val days14 = buildDays14(newAnchor)

        val merged = loadMergedAssignmentsFor14Days(
            prevWeekStart = newAnchor.minus(7, DateTimeUnit.Companion.DAY),
            anchorWeekStart = newAnchor,
            days14 = days14,
            shopIds = current.shopsById.keys
        )

        // shop order зберігаємо поточний (reorder)
        _uiState.value = current.copy(
            isInitialLoading = false,
            weekStart = newAnchor,
            days = days14,
            assignments = merged,
            conflicts = computeConflicts(days14, current.shopsById.keys, merged),
            hasUnsavedChanges = false,
        )
    }

    private suspend fun loadMergedAssignmentsFor14Days(
        prevWeekStart: LocalDate,
        anchorWeekStart: LocalDate,
        days14: List<LocalDate>,
        shopIds: Set<String>,
    ): Map<LocalDate, Map<String, String?>> {
        val prevDraft = storage.loadWorkScheduleDraft(prevWeekStart)
        val anchorDraft = storage.loadWorkScheduleDraft(anchorWeekStart)

        // raw maps keyed by dayIso
        val rawPrev = prevDraft?.assignments ?: emptyMap()
        val rawAnchor = anchorDraft?.assignments ?: emptyMap()

        // merge: якщо день належить prev week => з rawPrev, інакше з rawAnchor
        val out = mutableMapOf<LocalDate, Map<String, String?>>()
        for (day in days14) {
            val dayIso = day.toString()
            val weekStartOfDay = day.minus(day.dayOfWeek.isoDayNumber - 1, DateTimeUnit.Companion.DAY)

            val rawDay = if (weekStartOfDay == prevWeekStart) rawPrev[dayIso] else rawAnchor[dayIso]
            val dayMap = rawDay.orEmpty()

            val normalized = buildMap<String, String?> {
                for (shopId in shopIds) put(shopId, dayMap[shopId])
            }
            out[day] = normalized
        }
        return out
    }

    private fun buildDays14(anchor: LocalDate): List<LocalDate> {
        val prev = anchor.minus(7, DateTimeUnit.Companion.DAY)
        return (0..13).map { prev.plus(it, DateTimeUnit.Companion.DAY) }
    }

    private fun moveUser(intent: WorkScheduleCreateIntent.MoveUser) {
        if (intent.fromDay == intent.toDay && intent.fromShopId == intent.toShopId) return

        _uiState.update { s ->
            val newAssignments = s.assignments.toMutableMap()

            val fromDayMap = (newAssignments[intent.fromDay] ?: emptyMap()).toMutableMap()
            val toDayMap = (newAssignments[intent.toDay] ?: emptyMap()).toMutableMap()

            val movingUser = fromDayMap[intent.fromShopId]
            if (movingUser.isNullOrBlank()) return@update s

            val targetUser = toDayMap[intent.toShopId]

            // ✅ swap
            fromDayMap[intent.fromShopId] = targetUser
            toDayMap[intent.toShopId] = movingUser

            newAssignments[intent.fromDay] = fromDayMap
            newAssignments[intent.toDay] = toDayMap

            val daysToRecheck = if (intent.fromDay == intent.toDay) listOf(intent.fromDay) else listOf(intent.fromDay, intent.toDay)
            val newConflicts = recomputeConflictsSelective(
                shopIds = s.shopsById.keys,
                assignments = newAssignments,
                prevConflicts = s.conflicts,
                affectedDays = daysToRecheck
            )

            s.copy(
                assignments = newAssignments,
                conflicts = newConflicts,
                hasUnsavedChanges = true
            )
        }
    }

    private fun setAssignment(day: LocalDate, shopId: String, userId: String?) {
        _uiState.update { s ->
            val newAssignments = s.assignments.toMutableMap()
            val dayMap = (newAssignments[day] ?: emptyMap()).toMutableMap()
            dayMap[shopId] = userId
            newAssignments[day] = dayMap

            val newConflicts = recomputeConflictsSelective(
                shopIds = s.shopsById.keys,
                assignments = newAssignments,
                prevConflicts = s.conflicts,
                affectedDays = listOf(day)
            )

            s.copy(
                assignments = newAssignments,
                conflicts = newConflicts,
                hasUnsavedChanges = true
            )
        }
    }

    private fun recomputeConflictsSelective(
        shopIds: Set<String>,
        assignments: Map<LocalDate, Map<String, String?>>,
        prevConflicts: Set<ConflictCell>,
        affectedDays: List<LocalDate>
    ): Set<ConflictCell> {
        // 1) прибрати старі конфлікти для affected days
        val cleaned = prevConflicts.filterNot { it.day in affectedDays }.toMutableSet()

        // 2) додати нові для affected days
        for (day in affectedDays) {
            val byUser = mutableMapOf<String, MutableList<String>>()
            for (shopId in shopIds) {
                val u = assignments[day]?.get(shopId) ?: continue
                if (u.isBlank()) continue
                byUser.getOrPut(u) { mutableListOf() }.add(shopId)
            }
            byUser.values.filter { it.size >= 2 }.forEach { shops ->
                shops.forEach { shopId ->
                    cleaned.add(ConflictCell(day, shopId))
                }
            }
        }
        return cleaned
    }

    private suspend fun persistCurrentDraftIfDirty() {
        val s = _uiState.value
        if (!s.hasUnsavedChanges) return
        saveTwoWeeksDrafts(s)
    }

    private fun save() = viewModelScope.launch {
        val s = _uiState.value
        saveTwoWeeksDrafts(s)
        _uiState.update { it.copy(hasUnsavedChanges = false) }
        _uiEvent.emit(UIEvents.ShowMessage("Збережено локально"))
    }

    private suspend fun saveTwoWeeksDrafts(s: WorkScheduleCreateUiState) {
        // у state.weekStart — anchorWeekStart (правий тиждень)
        val anchor = s.weekStart
        val prev = anchor.minus(7, DateTimeUnit.Companion.DAY)

        // split assignments by weekStartOfDay
        val prevMap = mutableMapOf<String, Map<String, String?>>()
        val anchorMap = mutableMapOf<String, Map<String, String?>>()

        for (day in s.days) {
            val weekStartOfDay = day.minus(day.dayOfWeek.isoDayNumber - 1, DateTimeUnit.Companion.DAY)
            val dayIso = day.toString()
            val dayData = s.assignments[day].orEmpty()

            if (weekStartOfDay == prev) prevMap[dayIso] = dayData
            if (weekStartOfDay == anchor) anchorMap[dayIso] = dayData
        }

        storage.saveWorkScheduleDraft(
            WorkScheduleDraft(
                weekStartIso = prev.toString(),
                shopOrder = s.shopOrder,
                assignments = prevMap
            )
        )
        storage.saveWorkScheduleDraft(
            WorkScheduleDraft(
                weekStartIso = anchor.toString(),
                shopOrder = s.shopOrder,
                assignments = anchorMap
            )
        )
    }

    // --- сортування як ти хотів ---
    private fun getGroupedShops(shops: List<Shop>, cities: List<City>): List<ShopGroup> {
        val cityMap = cities.associateBy { it.id }
        return shops
            .groupBy { it.cityId }
            .mapNotNull { (cityId, shopsInCity) ->
                val city = cityMap[cityId] ?: return@mapNotNull null
                ShopGroup(city = city, shops = shopsInCity.sortedBy { it.code })
            }
            .sortedBy { it.city.name }
    }
    // -------------------- Utils --------------------

    private fun currentWeekStart(clock: Clock): LocalDate {
        val today = clock.now().toLocalDateTime(TimeZone.Companion.currentSystemDefault()).date
        // зробимо “понеділок” стартом тижня
        val dow = today.dayOfWeek.isoDayNumber // Mon=1..Sun=7
        return today.minus(dow - 1, DateTimeUnit.Companion.DAY)
    }

    private fun computeConflicts(
        days: List<LocalDate>, shopIds: Set<String>, assignments: Map<LocalDate, Map<String, String?>>
    ): Set<ConflictCell> {
        val conflicts = mutableSetOf<ConflictCell>()
        for (day in days) {
            val byUser = mutableMapOf<String, MutableList<String>>() // userId -> shopIds
            for (shopId in shopIds) {
                val u = assignments[day]?.get(shopId) ?: continue
                if (u.isBlank()) continue
                byUser.getOrPut(u) { mutableListOf() }.add(shopId)
            }
            byUser.values.filter { it.size >= 2 }.forEach { shops ->
                shops.forEach { shopId ->
                    conflicts.add(ConflictCell(day, shopId))
                }
            }
        }
        return conflicts
    }
}

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
}

data class WorkScheduleCreateUiState(
    val isInitialLoading: Boolean = false,

    val weekStart: LocalDate = LocalDate(2026, 2, 23), // placeholder
    val days: List<LocalDate> = emptyList(),

    val shopsById: Map<String, Shop> = emptyMap(),
    val shopOrder: List<String> = emptyList(),
    val shopGroups: List<ShopGroup> = emptyList(), // ✅ додали

    val users: List<User> = emptyList(),

    // assignments[day][shopId] = userId?
    val assignments: Map<LocalDate, Map<String, String?>> = emptyMap(),

    val conflicts: Set<ConflictCell> = emptySet(),

    val hasUnsavedChanges: Boolean = false,

    val dayCellWidth: Dp = 220.dp,
)

data class ConflictCell(val day: LocalDate, val shopId: String)

@Serializable
data class WorkScheduleDraft(
    val weekStartIso: String,
    val shopOrder: List<String>,
    // Map<String(dayIso), Map<String(shopId), String?(userId)>>
    val assignments: Map<String, Map<String, String?>>,
)