package org.bigblackowl.vccadmin.ui.workSchedule.view

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.bigblackowl.vccadmin.data.entity.User
import org.bigblackowl.vccadmin.data.events.UIEvents
import org.bigblackowl.vccadmin.navigation.NavigationViewModel
import org.bigblackowl.vccadmin.navigation.Route
import org.bigblackowl.vccadmin.ui.WorkScheduleView.WorkScheduleViewDto
import org.bigblackowl.vccadmin.ui.WorkScheduleView.WorkScheduleViewIntent
import org.bigblackowl.vccadmin.ui.WorkScheduleView.WorkScheduleViewScreenViewModel
import org.bigblackowl.vccadmin.ui.WorkScheduleView.WorkScheduleViewUiState
import org.bigblackowl.vccadmin.uiComponent.indicators.LoadingComponent
import org.bigblackowl.vccadmin.uiComponent.text.BodyText
import org.bigblackowl.vccadmin.uiComponent.text.TitleText
import org.koin.compose.koinInject

@Composable
fun WorkScheduleViewScreen(
    snackbarHostState: SnackbarHostState,
    navigationViewModel: NavigationViewModel,
    viewModel: WorkScheduleViewScreenViewModel = koinInject(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                is UIEvents.ShowMessage -> snackbarHostState.showSnackbar(event.message)
                else -> Unit
            }
        }
    }

    if (uiState.isInitialLoading) {
        LoadingComponent()
        return
    }

    WorkScheduleViewContent(
        uiState = uiState,
        onIntent = viewModel::onIntent,
        onCreateClicked = {navigationViewModel.navigateTo(Route.WorkScheduleCreate)}
    )
}

@Composable
private fun WorkScheduleViewContent(
    uiState: WorkScheduleViewUiState,
    onIntent: (WorkScheduleViewIntent) -> Unit,
    onCreateClicked: () -> Unit,
    ) {
    Scaffold(
        floatingActionButton = {
            AnimatedVisibility(!uiState.isInitialLoading) {
                Row {
                    FloatingActionButton(
                        onClick = onCreateClicked
                    ) {
                        Text("Створити", Modifier.padding(12.dp))
                    }
                FloatingActionButton(
                    onClick = { onIntent(WorkScheduleViewIntent.Load) }
                ) {
                    Text("Завантажити Excel", Modifier.padding(12.dp))
                }
                }
            }
        },
        modifier = Modifier.fillMaxSize().padding(12.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            BodyText("На тесті")
            uiState.schedule?.let { schedule ->
                WorkScheduleViewTable(schedule, currentUser = uiState.currentUser)
            }
        }
    }
}

@Composable
fun WorkScheduleViewTable(schedule: WorkScheduleViewDto, currentUser: User?) {
    val horizontal = rememberScrollState()
    val vertical = rememberScrollState()
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(vertical)
            .horizontalScroll(horizontal)
    ) {
        // Header
        Row {
            HeaderCell("Магазин")
            schedule.dates.forEach { HeaderCell(it) }
        }

        Spacer(Modifier.height(8.dp))

        // Rows
        schedule.shops.forEach { shop ->
            Row {
                BodyCell(shop.shop, false)
                shop.shifts.forEach { shift ->
                    val active = nameMatchesStrict(currentUser?.fullName, shift.employees)
                    BodyCell(
                        text = shift.employees.joinToString(", "),
                        active = active
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
        }
    }
}

private fun normalizeName(s: String): String =
    s.lowercase()
        .replace('’', '\'')
        .replace(Regex("[^a-zа-яіїєґ0-9\\s]"), " ")
        .replace(Regex("\\s+"), " ")
        .trim()

private fun nameMatchesStrict(userFullName: String?, employees: List<String>): Boolean {
    val u = userFullName?.let(::normalizeName).orEmpty()
    if (u.isBlank()) return false
    val userTokens = u.split(" ").filter { it.length >= 3 }.toSet()

    return employees.any { emp ->
        val e = normalizeName(emp)
        val empTokens = e.split(" ").filter { it.length >= 3 }.toSet()
        empTokens.any { it in userTokens } || userTokens.any { it in empTokens }
    }
}

@Composable
private fun HeaderCell(text: String) {
    Surface(tonalElevation = 2.dp, shape = RectangleShape, border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface)) {
        TitleText(
            text = text,
            modifier = Modifier.width(250.dp).padding(8.dp),
        )
    }
}

@Composable
private fun BodyCell(text: String, active: Boolean) {
    Surface(
        tonalElevation = 2.dp,
        shape = RectangleShape,
        border = BorderStroke(2.dp, color = if (active) Color.Red else MaterialTheme.colorScheme.onSurface)
    ) {
        BodyText(
            text = text,
            modifier = Modifier.width(250.dp).padding(8.dp),
        )
    }
}

