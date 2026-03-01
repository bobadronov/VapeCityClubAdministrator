// File: commonMain/org/bigblackowl/vccadmin/ui/workSchedule/view/WorkScheduleViewScreen.kt
@file:Suppress("AssignedValueIsNeverRead")

package org.bigblackowl.vccadmin.ui.workSchedule.view

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toPersistentList
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.bigblackowl.vccadmin.data.events.UIEvents
import org.bigblackowl.vccadmin.navigation.NavigationViewModel
import org.bigblackowl.vccadmin.theme.DefaultValues
import org.bigblackowl.vccadmin.ui.workSchedule.COL_SHOP
import org.bigblackowl.vccadmin.ui.workSchedule.WorkRow
import org.bigblackowl.vccadmin.ui.workSchedule.colDay
import org.bigblackowl.vccadmin.ui.workSchedule.scaled
import org.bigblackowl.vccadmin.uiComponent.container.PlatformPullToRefreshBox
import org.bigblackowl.vccadmin.uiComponent.indicators.LoadingComponent
import org.bigblackowl.vccadmin.uiComponent.text.BodyText
import org.bigblackowl.vccadmin.uiComponent.text.HelperText
import org.bigblackowl.vccadmin.uiComponent.text.ScaledText
import org.bigblackowl.vccadmin.utils.isWideScreen
import org.koin.compose.koinInject
import ua.wwind.table.ExperimentalTableApi
import ua.wwind.table.Table
import ua.wwind.table.config.PinnedSide
import ua.wwind.table.config.RowHeightMode
import ua.wwind.table.config.SelectionMode
import ua.wwind.table.config.TableDefaults
import ua.wwind.table.config.TableSettings
import ua.wwind.table.state.rememberTableState
import ua.wwind.table.tableColumns
import kotlin.math.roundToInt
import kotlin.time.Clock

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkScheduleViewScreen(
    snackbarHostState: SnackbarHostState,
    navigationViewModel: NavigationViewModel,
    viewModel: WorkScheduleViewScreenViewModel = koinInject(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { viewModel.onIntent(WorkScheduleViewIntent.Load) }

    LaunchedEffect(Unit) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                is UIEvents.ShowMessage -> snackbarHostState.showSnackbar(event.message)
                else -> Unit
            }
        }
    }

    WorkScheduleViewContent(
        uiState = uiState,
        onIntent = viewModel::onIntent,
        modifier = Modifier.fillMaxSize(),
    )
}

@OptIn(ExperimentalTableApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun WorkScheduleViewContent(
    uiState: WorkScheduleViewUiState,
    onIntent: (WorkScheduleViewIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val horizontalScrollState = rememberScrollState()

    if (uiState.isInitialLoading) {
        LoadingComponent()
        return
    }

    uiState.errorText?.let {
        OutlinedCard(modifier = Modifier.fillMaxSize().padding(12.dp)) {
            Column(Modifier.padding(12.dp)) {
                Text("Помилка", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.padding(top = 6.dp))
                Text(it)
                Spacer(Modifier.padding(top = 10.dp))
                Button(onClick = { onIntent(WorkScheduleViewIntent.Refresh) }) {
                    Text("Повторити")
                }
            }
        }
        return
    }

    val tableScale = uiState.zoomScale
    val scaleLabel = remember(tableScale) { "${(tableScale * 100).roundToInt()}%" }
    val rowH = 44.dp.scaled(tableScale)

    // rows = shops in order + city headers (структура залежить тільки від shopOrder/shopsById)
    val rows: List<WorkRow> = remember(uiState.shopOrder, uiState.shopsById) {
        val out = ArrayList<WorkRow>(uiState.shopOrder.size + 12)
        var lastCity: String? = null

        for (shopId in uiState.shopOrder) {
            val shop = uiState.shopsById[shopId] ?: continue
            val city = shop.cityName
            if (city != lastCity) {
                out += WorkRow.CityHeader(cityName = city)
                lastCity = city
            }
            out += WorkRow.Shop(shopId = shopId)
        }
        out
    }

    val cols: ImmutableList<String> = remember(uiState.days) {
        (listOf(COL_SHOP) + uiState.days.map { colDay(it) }).toPersistentList()
    }

    // -------- OPT: стабільні refs на мінливі дані (без rebuild columns) --------
    val assignmentsState = rememberUpdatedState(uiState.assignments)
    val shopsByIdState = rememberUpdatedState(uiState.shopsById)
    val usersByIdState = rememberUpdatedState(uiState.usersById)
    val headerByDayState = rememberUpdatedState(uiState.headerByDay)
    val selectedUserIdState = rememberUpdatedState(uiState.selectedUserId)
    val userColorsState = rememberUpdatedState(uiState.userColors)

    // ✅ columns залежать тільки від структури (days + scale + rowH)
    val columns = remember(uiState.days, tableScale, rowH) {
        tableColumns<WorkRow, String, WorkTableData> {

            column(COL_SHOP, valueOf = { row ->
                when (row) {
                    is WorkRow.CityHeader -> row.cityName
                    is WorkRow.Shop -> row.shopId
                }
            }) {
                header {
                    ScaledText(
                        text = "Магазин",
                        scale = tableScale,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                width(
                    min = DefaultValues.Size.scheduleShopCellWidth.scaled(tableScale),
                    pref = DefaultValues.Size.scheduleShopCellWidth.scaled(tableScale)
                )
                rowHeight(max = rowH)

                cell { row, _ ->
                    when (row) {
                        is WorkRow.CityHeader -> {
                            Box(
                                Modifier
                                    .fillMaxSize()
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .padding(10.dp.scaled(tableScale))
                            ) {
                                ScaledText(
                                    text = row.cityName.uppercase(),
                                    scale = tableScale,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }

                        is WorkRow.Shop -> {
                            val shop = shopsByIdState.value[row.shopId]
                            Box(
                                Modifier
                                    .fillMaxSize()
                                    .padding(10.dp.scaled(tableScale)),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                ScaledText(
                                    text = shop?.fullAddress.orEmpty(),
                                    scale = tableScale,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                            }
                        }
                    }
                }
            }

            uiState.days.forEach { day ->
                column(colDay(day), valueOf = { row ->
                    when (row) {
                        is WorkRow.CityHeader -> ""
                        is WorkRow.Shop -> assignmentsState.value[day]?.get(row.shopId).orEmpty()
                    }
                }) {
                    header {
                        ScaledText(
                            text = headerByDayState.value[day] ?: day.toString(),
                            scale = tableScale,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    width(
                        min = DefaultValues.Size.scheduleDayCellWidth.scaled(tableScale),
                        pref = DefaultValues.Size.scheduleDayCellWidth.scaled(tableScale)
                    )
                    rowHeight(max = rowH)

                    cell { row, _ ->
                        when (row) {
                            is WorkRow.CityHeader -> {
                                Box(
                                    Modifier
                                        .fillMaxSize()
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                        .padding(10.dp.scaled(tableScale))
                                )
                            }

                            is WorkRow.Shop -> {
                                val shopId = row.shopId
                                val userId = assignmentsState.value[day]?.get(shopId)
                                val selectedUserId = selectedUserIdState.value
                                val highlightColorArgb = selectedUserId?.let { userColorsState.value[it] }
                                val isHighlighted = selectedUserId != null && userId == selectedUserId
                                val selectedUserName = userId?.let { usersByIdState.value[it]?.fullName }

                                ReadOnlyUserCell(
                                    scale = tableScale,
                                    selectedUserName = selectedUserName,
                                    isHighlighted = isHighlighted,
                                    highlightColorArgb = highlightColorArgb,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    val isWide = isWideScreen()

    val state = rememberTableState(
        columns = cols,
        settings = TableSettings(
            isDragEnabled = false,
            enableDragToScroll = true,
            showVerticalDividers = true,
            showRowDividers = true,
            stripedRows = true,
            selectionMode = SelectionMode.None,
            pinnedColumnsCount = if (isWide) 1 else 0 ,
            pinnedColumnsSide = PinnedSide.Left,
            rowHeightMode = RowHeightMode.Dynamic
        ),
    )

    PlatformPullToRefreshBox(
        isRefreshing = uiState.isRefreshing,
        onRefresh = { onIntent(WorkScheduleViewIntent.Refresh) },
        modifier = modifier.fillMaxSize().padding(DefaultValues.Padding.mainBoxPadding)
    ) {
        Column {
            WorkScheduleViewHeader(
                uiState = uiState,
                onIntent = onIntent,
                periodLabel = uiState.periodLabel,
                onPrevWeekClicked = { onIntent(WorkScheduleViewIntent.PrevWeek) },
                onNextWeekClicked = { onIntent(WorkScheduleViewIntent.NextWeek) },
                scaleLabel = scaleLabel,
                onZoomOut = { onIntent(WorkScheduleViewIntent.ZoomOut) },
                onZoomIn = { onIntent(WorkScheduleViewIntent.ZoomIn) },
                onZoomReset = { onIntent(WorkScheduleViewIntent.ZoomReset) },
                modifier = Modifier.fillMaxWidth(),
            )

            Table(
                modifier = Modifier.fillMaxWidth().weight(1f),
                itemsCount = rows.size,
                itemAt = { rows.getOrNull(it) },
                state = state,
                columns = columns,
                tableData = WorkTableData,
                border = TableDefaults.NoBorder,
                shape = RectangleShape,
                horizontalState = horizontalScrollState,
            )
        }
    }

    // Auto-scroll to today
    // ✅ краще не "навіки": перевираховуємо при зміні weekStart
    val today: LocalDate = remember(uiState.weekStart) {
        Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
    }
    val dayIndex = remember(uiState.days, today) { uiState.days.indexOfFirst { it == today } }

    // ✅ одноразово для поточного вікна/тижня
    var didAutoScroll by rememberSaveable(uiState.weekStart.toString()) { mutableStateOf(false) }
    val density = LocalDensity.current

    LaunchedEffect(dayIndex, uiState.weekStart, uiState.days, didAutoScroll, tableScale) {
        if (didAutoScroll) return@LaunchedEffect
        if (dayIndex < 0) return@LaunchedEffect

        // ✅ FIX: не масштабуємо двічі
        val dayCellWidthPx = with(density) {
            DefaultValues.Size.scheduleDayCellWidth.scaled(tableScale).toPx()
        }
        val targetPx = (dayIndex * dayCellWidthPx).roundToInt()
        val paddingPx = with(density) { 24.dp.toPx() }.roundToInt()

        horizontalScrollState.animateScrollTo((targetPx - paddingPx).coerceAtLeast(0))
        didAutoScroll = true
    }
}

/* --------------------------------- Header --------------------------------- */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WorkScheduleViewHeader(
    uiState: WorkScheduleViewUiState,
    onIntent: (WorkScheduleViewIntent) -> Unit,
    periodLabel: String,
    onPrevWeekClicked: () -> Unit,
    onNextWeekClicked: () -> Unit,
    scaleLabel: String,
    onZoomOut: () -> Unit,
    onZoomIn: () -> Unit,
    onZoomReset: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }

    // Вибір користувача: якщо selectedUserId не заданий — показуємо currentUser
    val selectedUser = uiState.selectedUserId?.let { uiState.usersById[it] } ?: uiState.currentUser

    Surface(modifier = modifier.fillMaxWidth(), tonalElevation = 2.dp) {
        FlowRow(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded },
            ) {
                TextField(
                    modifier = Modifier
                        .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, enabled = true),
                    value = selectedUser?.fullName ?: "Юзер не вибраний",
                    onValueChange = {},
                    readOnly = true,
                    label = { BodyText("Юзер") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                )

                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    // (опційно) перший пункт: скинути підсвітку
                    DropdownMenuItem(
                        text = { BodyText("— Не підсвічувати —") },
                        onClick = {
                            expanded = false
                            onIntent(WorkScheduleViewIntent.SelectUser(null))
                        }
                    )

                    uiState.users.forEach { u ->
                        DropdownMenuItem(
                            text = { BodyText(u.fullName) },
                            onClick = {
                                expanded = false
                                onIntent(WorkScheduleViewIntent.SelectUser(u.id))
                            }
                        )
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                AssistChip(onClick = onPrevWeekClicked, label = { HelperText("< Тиждень") })
                BodyText(text = periodLabel)
                AssistChip(onClick = onNextWeekClicked, label = { HelperText("Тиждень >") })
            }

            Spacer(Modifier.weight(1f))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                AssistChip(onClick = onZoomOut, label = { HelperText("−") })
                AssistChip(onClick = onZoomReset, label = { BodyText(scaleLabel) })
                AssistChip(onClick = onZoomIn, label = { HelperText("+") })
            }
        }
    }
}

/* --------------------------------- Cell (read-only) --------------------------------- */

@Composable
private fun ReadOnlyUserCell(
    scale: Float,
    selectedUserName: String?,
    isHighlighted: Boolean,
    highlightColorArgb: Long?,
    modifier: Modifier = Modifier,
) {
    val bg = when {
        isHighlighted && highlightColorArgb != null -> Color(highlightColorArgb.toInt())
        isHighlighted -> MaterialTheme.colorScheme.secondaryContainer
        else -> MaterialTheme.colorScheme.surface
    }

    val stroke = MaterialTheme.colorScheme.outlineVariant
    val borderWidth = 0.5.dp.scaled(scale)

    Box(
        modifier = modifier
            .background(bg)
            .border(borderWidth, stroke)
            .padding(2.dp.scaled(scale)),
        contentAlignment = Alignment.CenterStart
    ) {
        ScaledText(
            text = selectedUserName ?: "—",
            scale = scale,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Start
        )
    }
}

/* --------------------------------- Private models --------------------------------- */

private object WorkTableData