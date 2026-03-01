// File: commonMain/org/bigblackowl/vccadmin/ui/workSchedule/create/WorkScheduleCreateScreen.kt
@file:Suppress("AssignedValueIsNeverRead")

package org.bigblackowl.vccadmin.ui.workSchedule.create

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Preview
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.skydoves.colorpicker.compose.AlphaSlider
import com.github.skydoves.colorpicker.compose.BrightnessSlider
import com.github.skydoves.colorpicker.compose.HsvColorPicker
import com.github.skydoves.colorpicker.compose.rememberColorPickerController
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.bigblackowl.vccadmin.data.entity.User
import org.bigblackowl.vccadmin.data.events.UIEvents
import org.bigblackowl.vccadmin.navigation.NavigationViewModel
import org.bigblackowl.vccadmin.navigation.Route
import org.bigblackowl.vccadmin.theme.DefaultValues
import org.bigblackowl.vccadmin.ui.workSchedule.COL_SHOP
import org.bigblackowl.vccadmin.ui.workSchedule.WorkRow
import org.bigblackowl.vccadmin.ui.workSchedule.colDay
import org.bigblackowl.vccadmin.ui.workSchedule.scaled
import org.bigblackowl.vccadmin.uiComponent.buttons.SaveButton
import org.bigblackowl.vccadmin.uiComponent.buttons.SettingsButton
import org.bigblackowl.vccadmin.uiComponent.icons.DefaultIcon
import org.bigblackowl.vccadmin.uiComponent.indicators.LoadingComponent
import org.bigblackowl.vccadmin.uiComponent.listItems.DefaultVerticalScrollbar
import org.bigblackowl.vccadmin.uiComponent.text.BodyText
import org.bigblackowl.vccadmin.uiComponent.text.HelperText
import org.bigblackowl.vccadmin.uiComponent.text.ScaledText
import org.bigblackowl.vccadmin.uiComponent.text.TitleText
import org.bigblackowl.vccadmin.utils.PlatformFunctionProvider.getPlainText
import org.bigblackowl.vccadmin.utils.PlatformFunctionProvider.setPlainText
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
fun WorkScheduleCreateScreen(
    snackbarHostState: androidx.compose.material3.SnackbarHostState,
    navigationViewModel: NavigationViewModel,
    viewModel: WorkScheduleCreateScreenViewModel = koinInject(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { viewModel.onIntent(WorkScheduleCreateIntent.Load) }

    LaunchedEffect(Unit) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                is UIEvents.ShowMessage -> snackbarHostState.showSnackbar(event.message)
                is UIEvents.Navigate -> navigationViewModel.navigateTo(Route.WorkScheduleView)
                else -> Unit
            }
        }
    }

    WorkScheduleCreateContent(
        uiState = uiState,
        onIntent = viewModel::onIntent,
    )
}

@OptIn(ExperimentalTableApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun WorkScheduleCreateContent(
    uiState: WorkScheduleCreateUiState,
    onIntent: (WorkScheduleCreateIntent) -> Unit,
) {
    val horizontalScrollState = rememberScrollState()
    val haptic = LocalHapticFeedback.current

    if (uiState.isInitialLoading) {
        LoadingComponent()
        return
    }

    val tableScale = uiState.zoomScale
    val rowH = 44.dp.scaled(tableScale)
    val scaleLabel = remember(tableScale) { "${(tableScale * 100).roundToInt()}%" }

    // rows = shops in order
    val rows: List<WorkRow> = remember(uiState.shopOrder, uiState.shopsById) {
        val out = ArrayList<WorkRow>(uiState.shopOrder.size + 8)
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

    var settingWindowVisible by remember { mutableStateOf(false) }

    // cols = first pinned "Shop" + days
    val cols: ImmutableList<String> = remember(uiState.days) {
        (listOf(COL_SHOP) + uiState.days.map { colDay(it) }).toPersistentList()
    }

    // --------- OPT: rect cache is NOT state (no snapshot invalidations) ----------
    val cellRects = remember { HashMap<CellKey, Rect>(4096) }

    // ✅ очищаємо rect-івки коли міняється масштаб або сутність таблиці
    LaunchedEffect(tableScale, rows.size, uiState.days.size) {
        cellRects.clear()
    }

    // Drag state (payload + позиція в root)
    var dragging by remember { mutableStateOf<DragPayload?>(null) }
    var dragPos by remember { mutableStateOf(Offset.Zero) }

    fun findDropTarget(pos: Offset): CellKey? {
        // lastOrNull to prefer top-most (last written) in case of overlaps
        return cellRects.entries.lastOrNull { (_, rect) -> rect.contains(pos) }?.key
    }

    val clipboard = LocalClipboard.current
    var selectedCell by remember { mutableStateOf<CellKey?>(null) }

    // fallback (якщо системний clipboard недоступний/порожній) — web/wasm
    var localClipboard by remember { mutableStateOf<CopiedUser?>(null) }

    val scope = rememberCoroutineScope()

    fun copyFromCell(cell: CellKey) {
        scope.launch {
            val userId = uiState.assignments[cell.day]?.get(cell.shopId) ?: return@launch
            if (userId.isBlank()) return@launch
            val userName = uiState.users.firstOrNull { it.id == userId }?.fullName ?: return@launch
            val copied = CopiedUser(userId, userName)
            localClipboard = copied
            clipboard.setPlainText(encodeClipboard(copied))
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
    }

    fun pasteToCell(cell: CellKey) {
        scope.launch {
            val sysText = clipboard.getPlainText()
            val fromSys = decodeClipboard(sysText)
            val copied = fromSys ?: localClipboard ?: return@launch
            onIntent(WorkScheduleCreateIntent.SetAssignment(cell.day, cell.shopId, copied.userId))
            haptic.performHapticFeedback(HapticFeedbackType.Confirm)
        }
    }

    fun cutFromCell(cell: CellKey) {
        copyFromCell(cell)
        onIntent(WorkScheduleCreateIntent.SetAssignment(cell.day, cell.shopId, null))
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }

    fun clearCell(cell: CellKey) {
        onIntent(WorkScheduleCreateIntent.SetAssignment(cell.day, cell.shopId, null))
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }

    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(selectedCell) {
        if (selectedCell != null) focusRequester.requestFocus()
    }

    // --------- OPT: stable tableData (do NOT recreate on each dragPos tick) ----------
    val onIntentRef = rememberUpdatedState(onIntent)
    val findDropTargetRef = rememberUpdatedState<(Offset) -> CellKey?> { pos -> findDropTarget(pos) }

    val tableData = remember {
        WorkTableData(
            dragging = null,
            dragPos = Offset.Zero,
            onStartDrag = { _, _ -> },
            onDrag = { },
            onEndDrag = { }
        )
    }

    val stableTableData = remember(tableData) {
        tableData.copy(
            onStartDrag = { payload, startPos ->
                dragging = payload
                dragPos = startPos
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            },
            onDrag = { pos ->
                dragPos = pos
            },
            onEndDrag = { endPos ->
                val payload = dragging
                dragging = null
                if (payload == null) return@copy

                val target = findDropTargetRef.value(endPos)
                if (target == null) {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove) // soft "no"
                    return@copy
                }
                if (target.day == payload.fromDay && target.shopId == payload.fromShopId) return@copy

                onIntentRef.value(
                    WorkScheduleCreateIntent.MoveUser(
                        fromDay = payload.fromDay,
                        fromShopId = payload.fromShopId,
                        toDay = target.day,
                        toShopId = target.shopId
                    )
                )
                haptic.performHapticFeedback(HapticFeedbackType.SegmentFrequentTick)
                focusRequester.requestFocus(FocusDirection.Right)
            }
        )
    }

    // --------- OPT: stable "refs" so columns don't rebuild on every assignments/users/conflicts change ----------
    val assignmentsState = rememberUpdatedState(uiState.assignments)
    val usersState = rememberUpdatedState(uiState.users)
    val conflictsState = rememberUpdatedState(uiState.conflicts)
    val userColorsState = rememberUpdatedState(uiState.userColors)
    val shopsByIdState = rememberUpdatedState(uiState.shopsById)
    val headerByDayState = rememberUpdatedState(uiState.headerByDay)

    // ✅ columns depend ONLY on structure (days/scale/rowH)
    val columns = remember(uiState.days, tableScale, rowH) {
        tableColumns<WorkRow, String, WorkTableData> {

            // 1) Shop column
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

            // 2) Day columns
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

                    cell { row, td ->
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
                                val users = usersState.value
                                val selectedUser = users.firstOrNull { it.id == userId }
                                val isConflict = conflictsState.value.contains(ConflictCell(day, shopId))
                                val userColorArgb = userId?.let { userColorsState.value[it] }
                                val cellKey = CellKey(day, shopId)

                                Box(
                                    Modifier.onGloballyPositioned { coords ->
                                        cellRects[CellKey(day, shopId)] = coords.boundsInRoot()
                                    }
                                ) {
                                    UserCell(
                                        users = users,
                                        scale = tableScale,
                                        selectedUserId = userId,
                                        selectedUserName = selectedUser?.fullName,
                                        userColorArgb = userColorArgb,
                                        isConflict = isConflict,
                                        isDraggingSource = td.dragging?.let { it.fromDay == day && it.fromShopId == shopId } == true,
                                        isSelected = (selectedCell == cellKey),
                                        onRequestSelect = {
                                            selectedCell = cellKey
                                            focusRequester.requestFocus()
                                        },
                                        onSelect = { newUserId ->
                                            onIntent(WorkScheduleCreateIntent.SetAssignment(day, shopId, newUserId))
                                        },
                                        onClear = {
                                            onIntent(WorkScheduleCreateIntent.SetAssignment(day, shopId, null))
                                        },
                                        onStartDrag = { startPos ->
                                            val uid = userId ?: return@UserCell
                                            val uname = selectedUser?.fullName ?: return@UserCell
                                            td.onStartDrag(
                                                DragPayload(
                                                    userId = uid,
                                                    userName = uname,
                                                    fromDay = day,
                                                    fromShopId = shopId,
                                                    bgColor = selectedUser.scheduleColor
                                                ),
                                                startPos
                                            )
                                        },
                                        onDrag = td.onDrag,
                                        onDragEnd = td.onEndDrag,
                                    )
                                }
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(DefaultValues.Padding.mainBoxPadding)
            .focusRequester(focusRequester)
            .focusable()
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                val cell = selectedCell ?: return@onPreviewKeyEvent false
                val ctrl = event.isCtrlPressed || event.isMetaPressed
                return@onPreviewKeyEvent when (event.key) {
                    Key.Delete, Key.Backspace -> {
                        clearCell(cell); true
                    }

                    Key.C -> if (ctrl) {
                        copyFromCell(cell); true
                    } else false

                    Key.V -> if (ctrl) {
                        pasteToCell(cell); true
                    } else false

                    Key.X -> if (ctrl) {
                        cutFromCell(cell); true
                    } else false

                    else -> false
                }
            }
    ) {
        WorkScheduleHeader(
            periodLabel = uiState.periodLabel,
            hasUnsavedChanges = uiState.hasUnsavedChanges,
            onPrevWeekClicked = { onIntent(WorkScheduleCreateIntent.PrevWeek) },
            onNextWeekClicked = { onIntent(WorkScheduleCreateIntent.NextWeek) },
            onSaveClicked = { onIntent(WorkScheduleCreateIntent.Save) },
            onSettingsClicked = { settingWindowVisible = true },
            scaleLabel = scaleLabel,
            onZoomOut = { onIntent(WorkScheduleCreateIntent.ZoomOut) },
            onZoomIn = { onIntent(WorkScheduleCreateIntent.ZoomIn) },
            onZoomReset = { onIntent(WorkScheduleCreateIntent.ZoomReset) },
            onPreviewClicked = { onIntent(WorkScheduleCreateIntent.NavigateToPreview) },
            modifier = Modifier.fillMaxWidth()
        )

        Table(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            itemsCount = rows.size,
            itemAt = { rows.getOrNull(it) },
            state = state,
            columns = columns,
            tableData = stableTableData.copy(dragging = dragging, dragPos = dragPos),
            border = TableDefaults.NoBorder,
            shape = RectangleShape,
            horizontalState = horizontalScrollState,
        )
    }

    // today (recomputed when weekStart changes so it won't be stuck "forever")
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

        // ✅ FIX: do not scale twice
        val dayCellWidthPx = with(density) {
            DefaultValues.Size.scheduleDayCellWidth.scaled(tableScale).toPx()
        }
        val targetPx = (dayIndex * dayCellWidthPx).roundToInt()
        val paddingPx = with(density) { 24.dp.toPx() }.roundToInt()

        horizontalScrollState.animateScrollTo((targetPx - paddingPx).coerceAtLeast(0))
        didAutoScroll = true
    }

    DragOverlay(
        payload = dragging,
        dragPos = dragPos,
    )

    if (uiState.isRefreshing) {
        Box(Modifier.fillMaxSize().zIndex(9999f)) {
            LoadingComponent()
        }
    }

    if (settingWindowVisible) {
        SettingsDialog(
            users = uiState.users,
            userColors = uiState.userColors,
            onColorChanged = { userId, argb -> onIntent(WorkScheduleCreateIntent.SetUserColor(userId, argb)) },
            onDismiss = { settingWindowVisible = false }
        )
    }
}

/* --------------------------------- Settings Dialog --------------------------------- */

@Composable
private fun SettingsDialog(
    users: List<User>,
    userColors: Map<String, Long>,
    onColorChanged: (userId: String, argb: Long) -> Unit,
    onDismiss: () -> Unit,
) {
    val scrollState = rememberLazyListState()

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(DefaultValues.Padding.cardContentPadding),
            shape = RoundedCornerShape(DefaultValues.Shape.defaultShape),
        ) {
            Column(Modifier.padding(12.dp)) {
                TitleText("Кольори користувачів")
                Spacer(Modifier.height(8.dp))

                Row(Modifier.fillMaxWidth()) {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        state = scrollState,
                        verticalArrangement = Arrangement.spacedBy(DefaultValues.Padding.verticalListItemPadding)
                    ) {
                        items(items = users, key = { it.id }) { user ->
                            UserColorCard(
                                user = user,
                                currentColorArgb = userColors[user.id] ?: 0xFF0000FF,
                                onColorSelected = { argb -> onColorChanged(user.id, argb) }
                            )
                        }
                    }
                    DefaultVerticalScrollbar(scrollState)
                }

                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Закрити") }
                }
            }
        }
    }
}

@Composable
private fun UserColorCard(
    user: User,
    currentColorArgb: Long,
    onColorSelected: (Long) -> Unit,
) {
    var window by remember { mutableStateOf(false) }

    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(DefaultValues.Shape.defaultShape),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(DefaultValues.Padding.cardContentPadding),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(24.dp)
                    .background(Color(currentColorArgb.toInt()), RoundedCornerShape(6.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(6.dp))
            )

            Spacer(Modifier.width(10.dp))
            BodyText(user.fullName, Modifier.weight(1f))

            OutlinedIconButton(onClick = { window = true }) {
                DefaultIcon(Icons.Default.Edit)
            }
        }
    }

    if (window) {
        UserColorPickerDialog(
            title = user.fullName,
            initialArgb = currentColorArgb,
            onDismiss = { window = false },
            onApply = { argb ->
                onColorSelected(argb)
                window = false
            }
        )
    }
}

@Composable
private fun UserColorPickerDialog(
    title: String,
    initialArgb: Long,
    onDismiss: () -> Unit,
    onApply: (Long) -> Unit,
) {
    val controller = rememberColorPickerController()
    var tempArgb by remember(initialArgb) { mutableStateOf(initialArgb) }

    LaunchedEffect(initialArgb) {
        controller.selectByColor(color = Color(initialArgb), fromUser = true)
        val a = ((initialArgb ushr 24) and 0xFF).toFloat() / 255f
        controller.setAlpha(alpha = a, fromUser = true)
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(DefaultValues.Shape.defaultShape),
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {

                TitleText("Колір: $title")

                HsvColorPicker(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(360.dp),
                    controller = controller,
                    onColorChanged = { env -> tempArgb = env.color.toArgb().toLong() }
                )

                AlphaSlider(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(35.dp),
                    controller = controller,
                )

                BrightnessSlider(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(35.dp),
                    controller = controller,
                )

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Скасувати") }
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = { onApply(tempArgb) }) { Text("Застосувати") }
                }
            }
        }
    }
}

/* --------------------------------- Cells --------------------------------- */

@Composable
private fun UserCell(
    users: List<User>,
    scale: Float,
    selectedUserId: String?,
    selectedUserName: String?,
    userColorArgb: Long?,
    isConflict: Boolean,
    isDraggingSource: Boolean,
    isSelected: Boolean,
    onRequestSelect: () -> Unit,
    onSelect: (String?) -> Unit,
    onClear: () -> Unit,
    onStartDrag: (Offset) -> Unit,
    onDrag: (Offset) -> Unit,
    onDragEnd: (Offset) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    var cellRect by remember { mutableStateOf<Rect?>(null) }
    var lastRootPos by remember { mutableStateOf(Offset.Zero) }

    val bg = when {
        isDraggingSource -> MaterialTheme.colorScheme.secondaryContainer
        isConflict -> MaterialTheme.colorScheme.errorContainer
        userColorArgb != null -> Color(userColorArgb.toInt())
        else -> MaterialTheme.colorScheme.surface
    }

    val stroke = if (isConflict) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outlineVariant
    val selectedStroke = if (isSelected) MaterialTheme.colorScheme.primary else stroke
    val borderWidth = (if (isSelected) 1.5.dp else 0.5.dp).scaled(scale)

    Box(
        modifier = modifier
            .onGloballyPositioned { cellRect = it.boundsInRoot() }
            .pointerInput(Unit) { detectTapGestures(onTap = { onRequestSelect() }) }
            .background(bg)
            .border(borderWidth, selectedStroke)
            .padding(2.dp.scaled(scale))
    ) {
        Row(
            Modifier
                .fillMaxSize()
                .pointerInput(selectedUserId) {
                    detectDragGesturesAfterLongPress(
                        onDragStart = { local ->
                            if (selectedUserId.isNullOrBlank()) return@detectDragGesturesAfterLongPress
                            val rect = cellRect
                            val root = if (rect != null) rect.topLeft + local else local
                            lastRootPos = root
                            onStartDrag(root)
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            val rect = cellRect
                            val root = if (rect != null) rect.topLeft + change.position else change.position
                            lastRootPos = root
                            onDrag(root)
                        },
                        onDragEnd = { onDragEnd(lastRootPos) },
                        onDragCancel = {}
                    )
                }
                .align(Alignment.Center),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ScaledText(
                text = selectedUserName ?: "—",
                scale = scale,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .weight(1f)
                    .alpha(if (selectedUserName == null) 0.7f else 1f)
                    .padding(2.dp.scaled(scale))
            )

            IconButton(onClick = { expanded = true }) {
                Icon(Icons.Filled.MoreVert, contentDescription = "Меню")
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = { Text("Очистити") },
                onClick = {
                    expanded = false
                    onClear()
                }
            )
            users.forEach { u ->
                DropdownMenuItem(
                    text = { Text(u.fullName) },
                    onClick = {
                        expanded = false
                        onSelect(u.id)
                    }
                )
            }
        }
    }
}

/* --------------------------------- Overlay --------------------------------- */

@Composable
private fun DragOverlay(
    payload: DragPayload?,
    dragPos: Offset,
) {
    if (payload == null) return

    Surface(
        modifier = Modifier
            .zIndex(9999f)
            .graphicsLayer {
                translationX = dragPos.x
                translationY = dragPos.y
            },
        shape = RoundedCornerShape(DefaultValues.Shape.defaultShape),
        tonalElevation = 6.dp,
        color = Color(payload.bgColor.toInt())
    ) {
        BodyText(payload.userName, modifier = Modifier.padding(6.dp))
    }
}

/* --------------------------------- Header --------------------------------- */

@Composable
private fun WorkScheduleHeader(
    periodLabel: String,
    hasUnsavedChanges: Boolean,
    onPrevWeekClicked: () -> Unit,
    onNextWeekClicked: () -> Unit,
    onSaveClicked: () -> Unit,
    onSettingsClicked: () -> Unit,
    scaleLabel: String,
    onZoomOut: () -> Unit,
    onZoomIn: () -> Unit,
    onZoomReset: () -> Unit,
    onPreviewClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(modifier = modifier.fillMaxWidth(), tonalElevation = 2.dp) {
        FlowRow(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                AssistChip(onClick = onPrevWeekClicked, label = { HelperText("< Тиждень") })
                BodyText(text = periodLabel)
                AssistChip(onClick = onNextWeekClicked, label = { HelperText("Тиждень >") })
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (hasUnsavedChanges) {
                    BodyText(
                        text = "Є незбережені зміни",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                AssistChip(onClick = onZoomOut, label = { HelperText("−") })
                AssistChip(onClick = onZoomReset, label = { BodyText(scaleLabel) })
                AssistChip(onClick = onZoomIn, label = { HelperText("+") })
            }
            Spacer(Modifier.weight(1f))
            SettingsButton { onSettingsClicked() }
            SaveButton { onSaveClicked() }
            OutlinedIconButton(onClick = onPreviewClicked) {
                DefaultIcon(Icons.Default.Preview)
            }
        }
    }
}

/* --------------------------------- Clipboard --------------------------------- */

private data class CopiedUser(val userId: String, val userName: String)

private const val CLIP_PREFIX = "vcc_user:"
private fun encodeClipboard(u: CopiedUser): String = "$CLIP_PREFIX${u.userId}|${u.userName}"

private fun decodeClipboard(text: String?): CopiedUser? {
    val t = text?.trim().orEmpty()
    if (!t.startsWith(CLIP_PREFIX)) return null
    val payload = t.removePrefix(CLIP_PREFIX)
    val sep = payload.indexOf('|')
    if (sep <= 0) return null
    val id = payload.substring(0, sep).trim()
    val name = payload.substring(sep + 1).trim()
    if (id.isBlank()) return null
    return CopiedUser(id, name)
}

/* --------------------------------- Private models --------------------------------- */
private data class CellKey(val day: LocalDate, val shopId: String)

private data class DragPayload(
    val userId: String,
    val userName: String,
    val bgColor: Long,
    val fromDay: LocalDate,
    val fromShopId: String,
)

private data class WorkTableData(
    val dragging: DragPayload? = null,
    val dragPos: Offset = Offset.Zero,
    val onStartDrag: (payload: DragPayload, startPos: Offset) -> Unit = { _, _ -> },
    val onDrag: (pos: Offset) -> Unit = {},
    val onEndDrag: (endPos: Offset) -> Unit = {},
)