// File: commonMain/org/bigblackowl/vccadmin/ui/workSchedule/view/WorkScheduleViewScreenViewModel.kt
package org.bigblackowl.vccadmin.ui.workSchedule.view

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
import kotlinx.datetime.plus
import org.bigblackowl.vccadmin.data.entity.City
import org.bigblackowl.vccadmin.data.entity.Shop
import org.bigblackowl.vccadmin.data.entity.SupabaseShop
import org.bigblackowl.vccadmin.data.entity.User
import org.bigblackowl.vccadmin.data.entity.toUiShops
import org.bigblackowl.vccadmin.data.events.UIEvents
import org.bigblackowl.vccadmin.data.utils.NetworkMonitorProvider
import org.bigblackowl.vccadmin.domain.repository.CityRepository
import org.bigblackowl.vccadmin.domain.repository.LocalRepository
import org.bigblackowl.vccadmin.domain.repository.ShopRepository
import org.bigblackowl.vccadmin.domain.repository.UserRepository
import org.bigblackowl.vccadmin.domain.repository.WorkScheduleRepository
import org.bigblackowl.vccadmin.ui.workSchedule.WorkScheduleHelper
import org.bigblackowl.vccadmin.utils.AppStringProvider
import org.bigblackowl.vccadmin.utils.AppStringProvider.toStringRes
import org.jetbrains.compose.resources.getString
import vccadministrator.composeapp.generated.resources.Res
import vccadministrator.composeapp.generated.resources.error_load_data

class WorkScheduleViewScreenViewModel(
    private val shopRepository: ShopRepository,
    private val userRepository: UserRepository,
    private val cityRepository: CityRepository,
    private val localRepository: LocalRepository,
    private val workScheduleRepository: WorkScheduleRepository,
    private val networkMonitorProvider: NetworkMonitorProvider,
) : ViewModel() {

    private companion object {
        private const val ZOOM_STEP = 0.10f
    }

    private val helper = WorkScheduleHelper(
        localRepository = localRepository,
        workScheduleRepository = workScheduleRepository,
        dayNameProvider = { d -> getString(d.dayOfWeek.toStringRes()) },
    )

    private val _uiState = MutableStateFlow(WorkScheduleViewUiState())
    val uiState: StateFlow<WorkScheduleViewUiState> = _uiState.asStateFlow()

    private val _uiEvent = MutableSharedFlow<UIEvents>(replay = 0)
    val uiEvent: SharedFlow<UIEvents> = _uiEvent.asSharedFlow()

    init {
        viewModelScope.launch {
            networkMonitorProvider.isConnected.collect { connected ->
                if (connected) load(initial = true)
            }
        }
    }

    fun onIntent(intent: WorkScheduleViewIntent) {
        when (intent) {
            WorkScheduleViewIntent.Load -> load(initial = true)
            WorkScheduleViewIntent.Refresh -> load(initial = false, refreshing = true)

            is WorkScheduleViewIntent.SelectUser ->
                _uiState.update { it.copy(selectedUserId = intent.userId) }

            WorkScheduleViewIntent.PrevWeek -> shiftWeek(-7)
            WorkScheduleViewIntent.NextWeek -> shiftWeek(+7)

            WorkScheduleViewIntent.ZoomIn -> zoomApply(_uiState.value.zoomScale + ZOOM_STEP)
            WorkScheduleViewIntent.ZoomOut -> zoomApply(_uiState.value.zoomScale - ZOOM_STEP)
            WorkScheduleViewIntent.ZoomReset -> zoomApply(1f)
            is WorkScheduleViewIntent.ZoomSet -> zoomApply(intent.scale)
        }
    }

    private fun zoomApply(newScale: Float) = viewModelScope.launch {
        val clamped = helper.clampZoom(newScale)
        _uiState.update { it.copy(zoomScale = clamped) }
        helper.saveZoomState(clamped)
    }

    private fun shiftWeek(deltaDays: Int) {
        _uiState.update { st ->
            st.copy(weekStart = st.weekStart.plus(deltaDays, DateTimeUnit.DAY))
        }
        load(initial = false, refreshing = true)
    }

    private fun load(initial: Boolean, refreshing: Boolean = false) = viewModelScope.launch {
        if (initial) _uiState.update { it.copy(isInitialLoading = true, errorText = null) }
        else _uiState.update { it.copy(isRefreshing = refreshing, errorText = null) }

        runCatching {
            val zoom = helper.loadZoomState()

            val currentUser: User? = userRepository.getCurrentUser()
            val users: List<User> = userRepository.getUsers()
            val cities: List<City> = cityRepository.getCities()
            val shopsRaw: List<SupabaseShop> = shopRepository.getStores()

            val usersById = users.associateBy { it.id }
            val userColors = users.associate { it.id to it.scheduleColor }

            // show ALL shops across ALL cities
            val shopsUiAll: List<Shop> = shopsRaw.toUiShops(cities)

            // ordering: city -> street (UA collator)
            val sortedShops = shopsUiAll.sortedWith { a, b ->
                val byCity = AppStringProvider.ukrainianCollator.compare(a.cityName, b.cityName)
                if (byCity != 0) byCity
                else AppStringProvider.ukrainianCollator.compare(a.street, b.street)
            }
            val shopsById = sortedShops.associateBy { it.id }

            val focusWeekStart = _uiState.value.weekStart
            val days = helper.buildWindowDays(focusWeekStart)
            val headerByDay = helper.buildHeaderByDay(days)
            val periodLabel = helper.buildPeriodLabel(days.firstOrNull(), days.lastOrNull())

            // order — from focus week if exists; else from sortedShops
            val scheduleOfFocusWeek = workScheduleRepository.loadWorkSchedule(focusWeekStart)
            val scheduleOrder = scheduleOfFocusWeek?.shopOrder.orEmpty()
            val shopOrder = when {
                scheduleOrder.isNotEmpty() -> scheduleOrder.filter { it in shopsById.keys }
                else -> sortedShops.map { it.id }
            }

            val merged = helper.loadMergedAssignmentsForDays(days, shopsById.keys)
            val selectedUserId = _uiState.value.selectedUserId ?: currentUser?.id

            _uiState.update {
                it.copy(
                    isInitialLoading = false,
                    isRefreshing = false,
                    errorText = null,
                    currentUser = currentUser,
                    selectedUserId = selectedUserId,
                    weekStart = focusWeekStart,
                    days = days,
                    headerByDay = headerByDay,
                    periodLabel = periodLabel,
                    shopsById = shopsById,
                    shopOrder = shopOrder,
                    users = users,
                    usersById = usersById,
                    userColors = userColors,
                    assignments = merged,
                    zoomScale = zoom,
                )
            }
        }.onFailure { e ->
            val msg = e.message ?: getString(Res.string.error_load_data)
            _uiState.update {
                it.copy(
                    isInitialLoading = false,
                    isRefreshing = false,
                    errorText = msg
                )
            }
            showMessage(msg)
        }
    }

    private fun showMessage(message: String) = viewModelScope.launch {
        _uiEvent.emit(UIEvents.ShowMessage(message))
    }
}