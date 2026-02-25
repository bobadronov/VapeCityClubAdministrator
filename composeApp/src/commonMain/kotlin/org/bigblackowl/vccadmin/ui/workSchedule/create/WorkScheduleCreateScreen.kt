// File: commonMain/org/bigblackowl/vccadmin/ui/workSchedule/create/WorkScheduleCreateScreen.kt
@file:Suppress("AssignedValueIsNeverRead")

package org.bigblackowl.vccadmin.ui.workSchedule.create

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.DisableSelection
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isCtrlPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.number
import org.bigblackowl.vccadmin.data.entity.User
import org.bigblackowl.vccadmin.data.events.UIEvents
import org.bigblackowl.vccadmin.navigation.NavigationViewModel
import org.bigblackowl.vccadmin.resourses.DefaultValues
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkScheduleCreateScreen(
    snackbarHostState: SnackbarHostState,
    navigationViewModel: NavigationViewModel,
    viewModel: WorkScheduleCreateScreenViewModel = koinInject(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { viewModel.onIntent(WorkScheduleCreateIntent.Load) }

    LaunchedEffect(Unit) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                is UIEvents.ShowMessage -> snackbarHostState.showSnackbar(event.message)
                else -> Unit
            }
        }
    }
    WorkScheduleCreateContent(
        uiState = uiState,
        onIntent = viewModel::onIntent,
    )
}

@Composable
private fun WorkScheduleCreateContent(
    uiState: WorkScheduleCreateUiState,
    onIntent: (WorkScheduleCreateIntent) -> Unit,
) {
    if (uiState.isInitialLoading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val hScroll = rememberScrollState()
    val scope = rememberCoroutineScope()

    // ✅ карта bounds всіх cells
    val cellBounds = remember { mutableStateMapOf<CellKey, Rect>() }

    // ✅ drag overlay state
    var dragging by remember { mutableStateOf<DragPayload?>(null) }
    var dragPos by remember { mutableStateOf(Offset.Zero) } // позиція пальця/миші в root

    val vScroll = rememberScrollState()

    var editingCell by remember { mutableStateOf<CellKey?>(null) }
    val left = uiState.days.firstOrNull()
    val right = uiState.days.lastOrNull()


    Box(Modifier.fillMaxSize()) {

        Column(Modifier.fillMaxSize().verticalScroll(vScroll)) {
            // -------- header / controls --------
            Row(
                Modifier.fillMaxWidth().padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                AssistChip(onClick = { onIntent(WorkScheduleCreateIntent.PrevWeek) }, label = { Text("← Тиждень") })

                Text(
                    text = "Період: ${left?.toHuman() ?: "—"} — ${right?.toHuman() ?: "—"}",
                    style = MaterialTheme.typography.titleMedium
                )

                AssistChip(onClick = { onIntent(WorkScheduleCreateIntent.NextWeek) }, label = { Text("Тиждень →") })

                Spacer(Modifier.weight(1f))

                Text(
                    text = if (uiState.hasUnsavedChanges) "Є незбережені зміни" else " ",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (uiState.hasUnsavedChanges) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedButton(onClick = { onIntent(WorkScheduleCreateIntent.Save) }) {
                    Icon(Icons.Default.Save, contentDescription = "Save")
                }
            }

            // -------- table header --------
            DisableSelection {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(hScroll)
                        .padding(horizontal = 12.dp)
                ) {
                    TableHeaderCell("Магазин", 250.dp)

                    uiState.days.forEach { day ->
                        TableHeaderCell("${day.dayOfWeekUk()} ${day.toHuman()}", uiState.dayCellWidth)
                    }
                }
            }

            Spacer(Modifier.height(8.dp))


            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp)
                    .horizontalScroll(hScroll)
                    .ctrlWheelHorizontalScroll(hScroll, scope),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                uiState.shopGroups.forEach { group ->
                    CityHeaderRow(
                        cityName = group.city.name,
                        days = uiState.days,
                        dayCellWidth = uiState.dayCellWidth,
                        hScroll = hScroll,
                    )

                    group.shops.forEach { shop ->
                        val shopId = shop.id
                        val shop = uiState.shopsById[shopId] ?: return@forEach
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surface, shape = MaterialTheme.shapes.medium)
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.medium)
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(Modifier.width(240.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(shop.fullAddress, style = MaterialTheme.typography.titleSmall)
                            }

                            uiState.days.forEach { day ->
                                val cellKey = CellKey(day, shopId)

                                val selectedUserId = uiState.assignments[day]?.get(shopId)
                                val isConflict = uiState.conflicts.contains(ConflictCell(day, shopId))
                                val usersById = remember(uiState.users) { uiState.users.associateBy { it.id } }
                                val selectedUser = usersById[selectedUserId]

                                Box(
                                    modifier = Modifier
                                        .width(uiState.dayCellWidth)
                                        .padding(end = 6.dp)
                                        .onGloballyPositioned { coords ->
                                            cellBounds[cellKey] = coords.boundsInRoot()
                                        }
                                        // ✅ double click/tap -> toggle edit
                                        .pointerInput(cellKey, selectedUserId) {
                                            detectTapGestures(
                                                onDoubleTap = {
                                                    editingCell = if (editingCell == cellKey) null else cellKey
                                                }
                                            )
                                        }
                                ) {
                                    val isEditing = (editingCell == cellKey)

                                    when {
                                        // ✅ режим редагування: завжди dropdown
                                        isEditing -> {
                                            UserDropdownCell(
                                                modifier = Modifier.fillMaxWidth(),
                                                users = uiState.users,
                                                selectedUserId = selectedUserId,
                                                isConflict = isConflict,
                                                onSelect = { newUserId ->
                                                    onIntent(WorkScheduleCreateIntent.SetAssignment(day, shopId, newUserId))
                                                    editingCell = null // закриваємо редагування після вибору
                                                },
                                                onClear = {
                                                    onIntent(WorkScheduleCreateIntent.SetAssignment(day, shopId, null))
                                                    editingCell = null
                                                }
                                            )
                                        }

                                        // ✅ не редагуємо: якщо юзер є -> draggable chip
                                        selectedUser != null -> {
                                            DraggableUserChip(
                                                userName = selectedUser.fullName,
                                                isConflict = isConflict,
                                                onDragStart = { posInRoot ->
                                                    dragging = DragPayload(
                                                        userId = selectedUser.id,
                                                        userName = selectedUser.fullName,
                                                        fromDay = day,
                                                        fromShopId = shopId
                                                    )
                                                    dragPos = posInRoot
                                                },
                                                onDrag = { posInRoot -> dragPos = posInRoot },
                                                onDragEnd = { endPosInRoot ->
                                                    val payload = dragging
                                                    dragging = null

                                                    if (payload != null) {
                                                        val target = cellBounds.entries
                                                            .firstOrNull { (_, rect) -> rect.contains(endPosInRoot) }
                                                            ?.key

                                                        if (target != null) {
                                                            onIntent(
                                                                WorkScheduleCreateIntent.MoveUser(
                                                                    fromDay = payload.fromDay,
                                                                    fromShopId = payload.fromShopId,
                                                                    toDay = target.day,
                                                                    toShopId = target.shopId
                                                                )
                                                            )
                                                        }
                                                    }
                                                }
                                            )
                                        }

                                        // ✅ пусто і не редагуємо: показати “порожню” комірку (double click -> edit)
                                        else -> {
                                            Surface(
                                                modifier = Modifier.fillMaxWidth(),
                                                shape = MaterialTheme.shapes.large,
                                                tonalElevation = 0.dp
                                            ) {
                                                Box(
                                                    Modifier
                                                        .fillMaxWidth()
                                                        .border(
                                                            1.dp,
                                                            if (isConflict) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outlineVariant,
                                                            MaterialTheme.shapes.large
                                                        )
                                                        .padding(horizontal = 10.dp, vertical = 8.dp),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text("—", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))
            }
        }

        // ✅ Drag overlay chip (плаваючий) — щоб було видно “блок”
        val payload = dragging
        if (payload != null) {
            Surface(
                modifier = Modifier
                    .zIndex(999f)
                    .graphicsLayer {
                        translationX = dragPos.x + 1f
                        translationY = dragPos.y + 1f
                    },
                shape = RoundedCornerShape(DefaultValues.Shape.defaultShape),
                tonalElevation = 6.dp
            ) {
                AssistChip(
                    onClick = {},
                    label = { Text(payload.userName) },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                )
            }
        }
    }
}

@Composable
private fun CityHeaderRow(
    cityName: String,
    days: List<LocalDate>,
    dayCellWidth: Dp,
    hScroll: ScrollState,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(hScroll)
            .background(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.shapes.small)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.small)
            .padding(vertical = 6.dp, horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.width(240.dp)) {
            Text(
                text = cityName,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        // пусті “клітинки”, щоб рядок секції займав ту ж ширину, що й таблиця
        days.forEach { _ ->
            Spacer(Modifier.width(dayCellWidth + 6.dp))
        }
    }
}

@Composable
private fun DraggableUserChip(
    userName: String,
    isConflict: Boolean,
    onDragStart: (Offset) -> Unit,
    onDrag: (Offset) -> Unit,
    onDragEnd: (Offset) -> Unit,
) {
    val bg = if (isConflict) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.secondaryContainer
    val border = if (isConflict) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outlineVariant

    var chipRect by remember { mutableStateOf<Rect?>(null) }
    var lastRootPos by remember { mutableStateOf(Offset.Zero) }

    val dragModifier =
        Modifier.pointerInput(Unit) {
            detectDragGesturesAfterLongPress(
                onDragStart = { local ->
                    val rect = chipRect
                    val root = if (rect != null) rect.topLeft + local else local
                    lastRootPos = root
                    onDragStart(root)
                },
                onDrag = { change, _ ->
                    change.consume()
                    val rect = chipRect
                    val root = if (rect != null) rect.topLeft + change.position else change.position
                    lastRootPos = root
                    onDrag(root)
                },
                onDragEnd = { onDragEnd(lastRootPos) },
                onDragCancel = {}
            )
        }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .onGloballyPositioned { chipRect = it.boundsInRoot() }
            .then(dragModifier)
            .border(1.dp, border, MaterialTheme.shapes.large)
            .background(bg, MaterialTheme.shapes.large)
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Text(userName, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun TableHeaderCell(
    text: String,
    width: Dp,
) {
    Surface(
        tonalElevation = 1.dp, shape = MaterialTheme.shapes.small, modifier = Modifier.width(width).padding(end = 6.dp)
    ) {
        Box(Modifier.padding(10.dp), contentAlignment = Alignment.CenterStart) {
            Text(text, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UserDropdownCell(
    modifier: Modifier = Modifier,
    users: List<User>,
    selectedUserId: String?,
    isConflict: Boolean,
    onSelect: (String?) -> Unit,
    onClear: () -> Unit,
) {
    val selected = users.firstOrNull { it.id == selectedUserId }
    var expanded by remember { mutableStateOf(false) }

    val bg = when {
        isConflict -> MaterialTheme.colorScheme.errorContainer
        else -> MaterialTheme.colorScheme.surface
    }

    val border = when {
        isConflict -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.outlineVariant
    }

    ExposedDropdownMenuBox(
        expanded = expanded, onExpandedChange = { expanded = it }, modifier = modifier.padding(end = 6.dp)
    ) {
        OutlinedTextField(
            value = selected?.fullName ?: "",
            onValueChange = {},
            readOnly = true,
            placeholder = { Text("—") },
            singleLine = true,
            modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, enabled = true).fillMaxWidth()
                .background(bg, MaterialTheme.shapes.small),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = border, focusedBorderColor = border
            ),
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) })

        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(text = { Text("Очистити") }, onClick = {
                expanded = false
                onClear()
            })
            HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)
            users.forEach { u ->
                DropdownMenuItem(text = { Text(u.fullName) }, onClick = {
                    expanded = false
                    onSelect(u.id)
                })
            }
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
private fun Modifier.ctrlWheelHorizontalScroll(
    hScroll: ScrollState,
    scope: CoroutineScope,
): Modifier = pointerInput(hScroll) {
    awaitPointerEventScope {
        while (true) {
            val event = awaitPointerEvent()
            if (event.type != PointerEventType.Scroll) continue
            if (!event.keyboardModifiers.isCtrlPressed) continue

            val deltaY = event.changes.firstOrNull()?.scrollDelta?.y ?: 0f
            if (deltaY == 0f) continue

            // wheel up/down -> scroll left/right
            scope.launch {
                hScroll.scrollBy(deltaY * 60f) // множник під себе підкрутиш
            }

            // щоб не було одночасно vertical scroll
            event.changes.forEach { it.consume() }
        }
    }
}

private data class CellKey(val day: LocalDate, val shopId: String)

private data class DragPayload(
    val userId: String,
    val userName: String,
    val fromDay: LocalDate,
    val fromShopId: String,
)

private fun LocalDate.dayOfWeekUk(): String = when (this.dayOfWeek) {
    DayOfWeek.MONDAY -> "Пн"
    DayOfWeek.TUESDAY -> "Вт"
    DayOfWeek.WEDNESDAY -> "Ср"
    DayOfWeek.THURSDAY -> "Чт"
    DayOfWeek.FRIDAY -> "Пт"
    DayOfWeek.SATURDAY -> "Сб"
    DayOfWeek.SUNDAY -> "Нд"
}

private fun LocalDate.toHuman(): String {
    // dd.MM
    val dd = day.toString().padStart(2, '0')
    val mm = month.number.toString().padStart(2, '0')
    return "$dd.$mm"
}
