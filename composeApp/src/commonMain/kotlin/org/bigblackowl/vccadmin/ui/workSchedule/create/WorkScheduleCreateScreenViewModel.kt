// File: commonMain/org/bigblackowl/vccadmin/ui/workSchedule/create/WorkScheduleCreateScreenViewModel.kt
package org.bigblackowl.vccadmin.ui.workSchedule.create

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.aakira.napier.Napier
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import org.bigblackowl.vccadmin.data.entity.City
import org.bigblackowl.vccadmin.data.entity.Shop
import org.bigblackowl.vccadmin.data.entity.ShopGroup
import org.bigblackowl.vccadmin.data.entity.toUiShops
import org.bigblackowl.vccadmin.data.events.UIEvents
import org.bigblackowl.vccadmin.data.utils.NetworkMonitorProvider
import org.bigblackowl.vccadmin.domain.repository.CityRepository
import org.bigblackowl.vccadmin.domain.repository.LocalRepository
import org.bigblackowl.vccadmin.domain.repository.ShopRepository
import org.bigblackowl.vccadmin.domain.repository.UserRepository
import org.bigblackowl.vccadmin.domain.repository.WorkScheduleRepository
import org.bigblackowl.vccadmin.theme.DefaultValues
import org.bigblackowl.vccadmin.ui.workSchedule.WorkSchedule
import org.bigblackowl.vccadmin.ui.workSchedule.WorkScheduleHelper
import org.bigblackowl.vccadmin.utils.AppStringProvider
import org.bigblackowl.vccadmin.utils.AppStringProvider.toStringRes
import org.jetbrains.compose.resources.getString
import vccadministrator.composeapp.generated.resources.Res
import vccadministrator.composeapp.generated.resources.no_internet
import kotlin.time.Clock

class WorkScheduleCreateScreenViewModel(
    private val shopRepository: ShopRepository,
    private val userRepository: UserRepository,
    private val cityRepository: CityRepository,
    private val localRepository: LocalRepository,
    private val workScheduleRepository: WorkScheduleRepository,
    private val networkMonitorProvider: NetworkMonitorProvider,
) : ViewModel() {

    private companion object {
        private const val ZOOM_STEP = 0.10f

        private const val AUTO_SAVE_DELAY_MS = 60_000L // 1 хв
        private const val AUTO_SAVE_TOAST_THROTTLE_MS = 15_000L // щоб не спамити "Автозбережено"
    }

    private val helper = WorkScheduleHelper(
        localRepository = localRepository,
        workScheduleRepository = workScheduleRepository,
        dayNameProvider = { d -> getString(d.dayOfWeek.toStringRes()) },
    )

    private val _uiState = MutableStateFlow(
        WorkScheduleCreateUiState(
            isInitialLoading = true,
            weekStart = parseDefaultDate(DefaultValues.Time.date),
        )
    )
    val uiState: StateFlow<WorkScheduleCreateUiState> = _uiState.asStateFlow()

    private val _uiEvent = MutableSharedFlow<UIEvents>(replay = 0)
    val uiEvent: SharedFlow<UIEvents> = _uiEvent.asSharedFlow()

    /* ----------------------------- Auto save ----------------------------- */

    private var autoSaveJob: Job? = null
    private var lastAutoSaveToastAtMs: Long = 0L

    private fun scheduleAutoSave() {
        autoSaveJob?.cancel()
        autoSaveJob = viewModelScope.launch {
            delay(AUTO_SAVE_DELAY_MS)

            val s = _uiState.value
            if (s.isInitialLoading) return@launch
            if (!s.hasUnsavedChanges) return@launch

            runCatching { saveSchedule(s) }
                .onSuccess {
                    _uiState.update { it.copy(hasUnsavedChanges = false) }

                    val now = kotlin.time.Clock.System.now().toEpochMilliseconds()
                    if (now - lastAutoSaveToastAtMs >= AUTO_SAVE_TOAST_THROTTLE_MS) {
                        lastAutoSaveToastAtMs = now
                        _uiEvent.emit(UIEvents.ShowMessage("Автозбережено"))
                    }
                    Napier.d { "Автозбережено" }
                }
                .onFailure { e ->
                    Napier.e(e) { "AutoSave failed" }
                }
        }
    }

    override fun onCleared() {
        autoSaveJob?.cancel()
        super.onCleared()
    }

    /* -------------------------------- Intents -------------------------------- */

    fun onIntent(intent: WorkScheduleCreateIntent) {
        when (intent) {
            WorkScheduleCreateIntent.Load -> load(initial = true)
            WorkScheduleCreateIntent.Save -> save()
            WorkScheduleCreateIntent.NextWeek -> shiftWeek(+7)
            WorkScheduleCreateIntent.PrevWeek -> shiftWeek(-7)

            is WorkScheduleCreateIntent.SetAssignment -> setAssignment(intent.day, intent.shopId, intent.userId)
            is WorkScheduleCreateIntent.MoveUser -> moveUser(intent)
            is WorkScheduleCreateIntent.SetUserColor -> setUserColor(id = intent.userId, color = intent.argb)

            WorkScheduleCreateIntent.ZoomIn -> zoomApply(_uiState.value.zoomScale + ZOOM_STEP)
            WorkScheduleCreateIntent.ZoomOut -> zoomApply(_uiState.value.zoomScale - ZOOM_STEP)
            WorkScheduleCreateIntent.ZoomReset -> zoomApply(1f)
            is WorkScheduleCreateIntent.ZoomSet -> zoomApply(intent.scale)

            WorkScheduleCreateIntent.NavigateToPreview -> checkAndNavigate()
        }
    }

    private fun checkAndNavigate() = viewModelScope.launch {
        save()
        _uiEvent.emit(UIEvents.Navigate)
    }

    /* -------------------------------- Zoom -------------------------------- */

    private fun zoomApply(newScale: Float) = viewModelScope.launch {
        val clamped = helper.clampZoom(newScale)
        _uiState.update { it.copy(zoomScale = clamped) }
        helper.saveZoomState(clamped)
    }

    /* -------------------------------- Colors -------------------------------- */

    private fun setUserColor(id: String, color: Long) = viewModelScope.launch {
        _uiState.update { s ->
            if (s.userColors[id] == color) return@update s
            s.copy(
                userColors = s.userColors + (id to color),
                hasUnsavedChanges = true
            )
        }

        runCatching { userRepository.setUserColor(id, color) }
            .onFailure { e -> _uiEvent.emit(UIEvents.ShowMessage(e.message ?: "Не вдалося зберегти колір")) }

        scheduleAutoSave()
    }

    /* -------------------------------- Load/Shift -------------------------------- */

    private fun load(initial: Boolean, refreshing: Boolean = false) = viewModelScope.launch {
        if (initial) _uiState.update { it.copy(isInitialLoading = true, isRefreshing = false) }
        else _uiState.update { it.copy(isRefreshing = refreshing) }

        runCatching {
            val zoom = helper.loadZoomState()

            val shopsRaw = shopRepository.getStores()
            val users = userRepository.getUsers()
            val cities = cityRepository.getCities()

            val userColors = users.associate { it.id to it.scheduleColor }

            // Варіант A: як у тебе було — фокус завжди "сьогодні"
            val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
            val focusWeekStart = helper.weekStartOf(today)

            // Варіант B: шанувати state.weekStart (розкоментуй якщо треба)
            // val focusWeekStart = _uiState.value.weekStart

            val shopsUi = shopsRaw.toUiShops(cities)
            val groups = getGroupedShops(shopsUi, cities)
            val sortedShops = groups.flatMap { it.shops }
            val shopsById = sortedShops.associateBy { it.id }
            val shopOrder = sortedShops.map { it.id }

            val days = helper.buildWindowDays(focusWeekStart)
            val merged = helper.loadMergedAssignmentsForDays(days, shopsById.keys)
            val headerByDay = helper.buildHeaderByDay(days)

            val visibleStart = days.firstOrNull()
            val visibleEnd = days.lastOrNull()

            _uiState.value = WorkScheduleCreateUiState(
                isInitialLoading = false,
                isRefreshing = false,
                weekStart = focusWeekStart,
                days = days,
                visibleStart = visibleStart,
                visibleEnd = visibleEnd,
                headerByDay = headerByDay,
                periodLabel = helper.buildPeriodLabel(visibleStart, visibleEnd),
                shopsById = shopsById,
                shopOrder = shopOrder,
                shopGroups = groups,
                users = users,
                assignments = merged,
                conflicts = computeConflicts(days, shopsById.keys, merged),
                hasUnsavedChanges = false,
                userColors = userColors,
                zoomScale = zoom,
            )
        }.onFailure { e ->
            _uiState.update { it.copy(isInitialLoading = false, isRefreshing = false) }
            _uiEvent.emit(UIEvents.ShowMessage(e.message ?: "Помилка завантаження"))
        }
    }

    private fun shiftWeek(deltaDays: Int) = viewModelScope.launch {
        persistCurrentDraftIfDirty()

        val current = _uiState.value
        val newFocus = current.weekStart.plus(deltaDays, DateTimeUnit.DAY)

        _uiState.update { it.copy(isRefreshing = true) }

        val days = helper.buildWindowDays(newFocus)
        val merged = helper.loadMergedAssignmentsForDays(days, current.shopsById.keys)
        val headerByDay = helper.buildHeaderByDay(days)

        val visibleStart = days.firstOrNull()
        val visibleEnd = days.lastOrNull()

        _uiState.value = current.copy(
            isRefreshing = false,
            weekStart = newFocus,
            days = days,
            visibleStart = visibleStart,
            visibleEnd = visibleEnd,
            headerByDay = headerByDay,
            periodLabel = helper.buildPeriodLabel(visibleStart, visibleEnd),
            assignments = merged,
            conflicts = computeConflicts(days, current.shopsById.keys, merged),
            hasUnsavedChanges = false,
        )
    }

    /* -------------------------- Draft merge/save helpers ------------------------- */

    private fun buildAssignmentsByWeek(
        s: WorkScheduleCreateUiState
    ): Map<LocalDate, Map<String, Map<String, String?>>> {
        val byWeek = mutableMapOf<LocalDate, MutableMap<String, Map<String, String?>>>()

        for (day in s.days) {
            val ws = helper.weekStartForDay(day)
            val dayIso = day.toString()
            val dayData = s.assignments[day].orEmpty()
            byWeek.getOrPut(ws) { mutableMapOf() }[dayIso] = dayData
        }
        return byWeek
    }

    private suspend fun saveSchedule(s: WorkScheduleCreateUiState) {
        val byWeek = buildAssignmentsByWeek(s)
        for ((ws, weekAssignments) in byWeek) {
            workScheduleRepository.saveWorkSchedule(
                WorkSchedule(
                    weekStartIso = ws.toString(),
                    shopOrder = s.shopOrder,
                    assignments = weekAssignments
                )
            )
        }
    }

    private suspend fun persistCurrentDraftIfDirty() {
        val s = _uiState.value
        if (!s.hasUnsavedChanges) return
        runCatching { saveSchedule(s) }
            .onFailure { e -> Napier.e(e) { "Persist draft failed" } }
    }

    private fun save() = viewModelScope.launch {
        if (!networkMonitorProvider.isConnected.value) {
            _uiEvent.emit(UIEvents.ShowMessage(getString(Res.string.no_internet)))
            return@launch
        }

        _uiState.update { it.copy(isRefreshing = true) }
        runCatching { saveSchedule(_uiState.value) }
            .onSuccess {
                _uiState.update { it.copy(hasUnsavedChanges = false) }
                _uiEvent.emit(UIEvents.ShowMessage("Збережено"))
            }
            .onFailure { e ->
                _uiEvent.emit(UIEvents.ShowMessage(e.message ?: "Помилка збереження"))
            }
        _uiState.update { it.copy(isRefreshing = false) }
    }

    /* ------------------------------ Assign / move ------------------------------ */

    private fun moveUser(intent: WorkScheduleCreateIntent.MoveUser) {
        if (intent.fromDay == intent.toDay && intent.fromShopId == intent.toShopId) return

        var changed = false

        _uiState.update { s ->
            val newAssignments = s.assignments.toMutableMap()

            val fromDayMap = (newAssignments[intent.fromDay] ?: emptyMap()).toMutableMap()
            val toDayMap = (newAssignments[intent.toDay] ?: emptyMap()).toMutableMap()

            val movingUser = fromDayMap[intent.fromShopId]
            if (movingUser.isNullOrBlank()) return@update s

            val targetUser = toDayMap[intent.toShopId]

            // swap
            fromDayMap[intent.fromShopId] = targetUser
            toDayMap[intent.toShopId] = movingUser

            newAssignments[intent.fromDay] = fromDayMap
            newAssignments[intent.toDay] = toDayMap

            val affectedDays = if (intent.fromDay == intent.toDay) listOf(intent.fromDay)
            else listOf(intent.fromDay, intent.toDay)

            changed = true

            s.copy(
                assignments = newAssignments,
                conflicts = recomputeConflictsSelective(
                    shopIds = s.shopsById.keys,
                    assignments = newAssignments,
                    prevConflicts = s.conflicts,
                    affectedDays = affectedDays
                ),
                hasUnsavedChanges = true
            )
        }

        if (changed) scheduleAutoSave()
    }

    private fun setAssignment(day: LocalDate, shopId: String, userId: String?) {
        var changed = false

        _uiState.update { s ->
            val current = s.assignments[day]?.get(shopId)
            if (current == userId) return@update s

            val newAssignments = s.assignments.toMutableMap()
            val dayMap = (newAssignments[day] ?: emptyMap()).toMutableMap()
            dayMap[shopId] = userId
            newAssignments[day] = dayMap

            changed = true

            s.copy(
                assignments = newAssignments,
                conflicts = recomputeConflictsSelective(
                    shopIds = s.shopsById.keys,
                    assignments = newAssignments,
                    prevConflicts = s.conflicts,
                    affectedDays = listOf(day)
                ),
                hasUnsavedChanges = true
            )
        }

        if (changed) scheduleAutoSave()
    }

    private fun recomputeConflictsSelective(
        shopIds: Set<String>,
        assignments: Map<LocalDate, Map<String, String?>>,
        prevConflicts: Set<ConflictCell>,
        affectedDays: List<LocalDate>
    ): Set<ConflictCell> {
        val cleaned = prevConflicts.filterNot { it.day in affectedDays }.toMutableSet()

        for (day in affectedDays) {
            val byUser = mutableMapOf<String, MutableList<String>>()
            for (shopId in shopIds) {
                val u = assignments[day]?.get(shopId) ?: continue
                if (u.isBlank()) continue
                byUser.getOrPut(u) { mutableListOf() }.add(shopId)
            }
            byUser.values.filter { it.size >= 2 }.forEach { shops ->
                shops.forEach { sid -> cleaned.add(ConflictCell(day, sid)) }
            }
        }
        return cleaned
    }

    private fun computeConflicts(
        days: List<LocalDate>,
        shopIds: Set<String>,
        assignments: Map<LocalDate, Map<String, String?>>
    ): Set<ConflictCell> {
        val conflicts = mutableSetOf<ConflictCell>()
        for (day in days) {
            val byUser = mutableMapOf<String, MutableList<String>>()
            for (shopId in shopIds) {
                val u = assignments[day]?.get(shopId) ?: continue
                if (u.isBlank()) continue
                byUser.getOrPut(u) { mutableListOf() }.add(shopId)
            }
            byUser.values.filter { it.size >= 2 }.forEach { shops ->
                shops.forEach { sid -> conflicts.add(ConflictCell(day, sid)) }
            }
        }
        return conflicts
    }

    private fun getGroupedShops(shops: List<Shop>, cities: List<City>): List<ShopGroup> {
        val cityMap = cities.associateBy { it.id }

        return shops
            .groupBy { it.cityId }
            .mapNotNull { (cityId, shopsInCity) ->
                val city = cityMap[cityId] ?: return@mapNotNull null

                ShopGroup(
                    city = city,
                    shops = shopsInCity.sortedWith { a, b ->
                        AppStringProvider.ukrainianCollator.compare(a.street, b.street)
                    }
                )
            }
            .sortedWith { a, b ->
                AppStringProvider.ukrainianCollator.compare(a.city.name, b.city.name)
            }
    }

    private fun parseDefaultDate(raw: String): LocalDate {
        val normalized = raw.trim().replace('_', '-')
        return LocalDate.parse(normalized)
    }
}