package com.example.airconapp.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.LocalTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.airconapp.data.ControlInfo
import com.example.airconapp.data.db.SchedulerProfile
import com.example.airconapp.presentation.MainViewModel
import com.example.airconapp.presentation.SchedulerViewModel
import com.example.airconapp.ui.components.AirconIcons
import com.example.airconapp.ui.theme.AirconColors
import com.example.airconapp.ui.theme.Inter

/** Daikin AirBase mode codes, in the order the design shows them. */
enum class AirconMode(val code: String, val label: String, val accent: Color, val icon: ImageVector, val verb: String) {
    AUTO("3", "Auto", AirconColors.Auto, AirconIcons.ModeAuto, "Auto · holding"),
    COOL("2", "Cool", AirconColors.Cool, AirconIcons.ModeCool, "Cooling to"),
    HEAT("1", "Heat", AirconColors.Heat, AirconIcons.ModeHeat, "Heating to"),
    FAN("0", "Fan", AirconColors.Fan, AirconIcons.ModeFan, "Fan only"),
    DRY("7", "Dry", AirconColors.Dry, AirconIcons.ModeDry, "Drying at");

    companion object {
        fun of(code: String?): AirconMode? = entries.firstOrNull { it.code == code }
    }
}

/** The mode colour while the unit runs, grey while it is off. */
fun accentFor(info: ControlInfo?): Color {
    val mode = AirconMode.of(info?.mode)
    return if (info?.pow == "1" && mode != null) mode.accent else AirconColors.Disabled
}

private enum class HomeTab { Controls, Zones, Schedules }

/**
 * The full (non-simple) app: Controls, Zones and Schedules tabs. Adding or editing a schedule
 * and Settings open their own screens.
 */
@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    schedulerViewModel: SchedulerViewModel,
    onAddSchedule: () -> Unit,
    onEditSchedule: (SchedulerProfile) -> Unit,
    onOpenSettings: () -> Unit
) {
    var tab by rememberSaveable { mutableStateOf(HomeTab.Controls) }
    BackHandler(enabled = tab != HomeTab.Controls) { tab = HomeTab.Controls }
    val schedules by schedulerViewModel.allProfiles.collectAsState()

    val controlInfo by viewModel.controlInfo.collectAsState()
    val accent by animateColorAsState(accentFor(controlInfo), tween(200), label = "accent")
    val isOn = controlInfo?.pow == "1"
    // The grey "off" accent is too dark for text; labels use a lighter grey instead
    val navAccent = if (isOn) accent else AirconColors.TextSecondary

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(viewModel) {
        viewModel.messages.collect { snackbarHostState.showSnackbar(it) }
    }

    // The design's text: Inter, no extra tracking, line height from the font size (Material's
    // body style would give every label a 24sp line and 0.5sp letter spacing)
    CompositionLocalProvider(LocalTextStyle provides TextStyle(fontFamily = Inter, color = AirconColors.TextPrimary)) {
    Scaffold(
        containerColor = AirconColors.Background,
        contentColor = AirconColors.TextPrimary,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            BottomNav(
                selected = tab,
                accent = navAccent,
                onSelect = { tab = it },
                onOpenSettings = onOpenSettings
            )
        }
    ) { padding ->
        val modifier = Modifier.padding(padding)
        when (tab) {
            HomeTab.Controls -> ControlsScreen(viewModel, accent, onOpenZones = { tab = HomeTab.Zones }, onOpenSettings = onOpenSettings, modifier = modifier)
            HomeTab.Zones -> ZonesScreen(viewModel, accent, onOpenSchedules = { tab = HomeTab.Schedules }, onOpenSettings = onOpenSettings, modifier = modifier)
            HomeTab.Schedules -> SchedulesScreen(
                schedules = schedules,
                onAdd = onAddSchedule,
                onEdit = onEditSchedule,
                onSetActive = { schedule, active -> schedulerViewModel.update(schedule.copy(isActive = active)) },
                onDelete = { schedulerViewModel.delete(it) },
                modifier = modifier
            )
        }
    }
    }
}

@Composable
private fun BottomNav(
    selected: HomeTab,
    accent: Color,
    onSelect: (HomeTab) -> Unit,
    onOpenSettings: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .drawBehind {
                drawLine(AirconColors.Divider, Offset(0f, 0f), Offset(size.width, 0f), 1.dp.toPx())
            }
            .padding(start = 8.dp, end = 8.dp, top = 4.dp, bottom = 4.dp)
            .selectableGroup()
    ) {
        val item = Modifier.weight(1f)
        NavItem("Controls", AirconIcons.NavControls, selected == HomeTab.Controls, accent, item) { onSelect(HomeTab.Controls) }
        NavItem("Zones", AirconIcons.NavZones, selected == HomeTab.Zones, accent, item) { onSelect(HomeTab.Zones) }
        NavItem("Schedules", AirconIcons.Calendar, selected == HomeTab.Schedules, accent, item) { onSelect(HomeTab.Schedules) }
        NavItem("Settings", AirconIcons.Settings, false, accent, item, onOpenSettings)
    }
}

@Composable
private fun NavItem(
    label: String,
    icon: ImageVector,
    isSelected: Boolean,
    accent: Color,
    modifier: Modifier,
    onClick: () -> Unit
) {
    val color = if (isSelected) accent else AirconColors.TextMuted
    Column(
        modifier = modifier
            .selectable(selected = isSelected, onClick = onClick, role = Role.Tab)
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(22.dp))
        Text(
            label,
            color = color,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium
        )
    }
}
