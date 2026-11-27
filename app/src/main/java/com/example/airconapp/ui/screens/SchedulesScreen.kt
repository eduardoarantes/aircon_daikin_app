package com.example.airconapp.ui.screens

import com.example.airconapp.domain.TemperatureUnit
import com.example.airconapp.domain.displayTemp
import com.example.airconapp.domain.label
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.airconapp.data.db.SchedulerProfile
import com.example.airconapp.domain.FanSpeed
import com.example.airconapp.ui.components.AccentSwitch
import com.example.airconapp.ui.components.AirconIcons
import com.example.airconapp.ui.theme.AirconColors
import com.example.airconapp.ui.theme.SpaceGrotesk

/** The schedules tab: one card per saved schedule, an add button, and an empty state. */
@Composable
fun SchedulesScreen(
    schedules: List<SchedulerProfile>,
    onAdd: () -> Unit,
    onEdit: (SchedulerProfile) -> Unit,
    onSetActive: (SchedulerProfile, Boolean) -> Unit,
    onDelete: (SchedulerProfile) -> Unit,
    temperatureUnit: TemperatureUnit,
    modifier: Modifier = Modifier
) {
    var pendingDelete by remember { mutableStateOf<SchedulerProfile?>(null) }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            // Room at the bottom so the last card can scroll clear of the add button
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Column(
                    modifier = Modifier.padding(start = 8.dp, top = 24.dp, bottom = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text("Schedules", fontSize = 24.sp, fontWeight = FontWeight.SemiBold, color = AirconColors.TextPrimary)
                    Text(summary(schedules), fontSize = 13.sp, color = AirconColors.TextMuted)
                }
            }
            if (schedules.isEmpty()) {
                item { EmptyState() }
            }
            items(schedules, key = { it.id }) { schedule ->
                ScheduleCard(
                    schedule = schedule,
                    temperatureUnit = temperatureUnit,
                    onEdit = { onEdit(schedule) },
                    onSetActive = { onSetActive(schedule, it) },
                    onDelete = { pendingDelete = schedule }
                )
            }
        }

        AddButton(onAdd, Modifier.align(Alignment.BottomEnd).padding(end = 20.dp, bottom = 20.dp))
    }

    pendingDelete?.let { schedule ->
        DeleteScheduleDialog(
            startTime = schedule.startTime,
            onConfirm = {
                onDelete(schedule)
                pendingDelete = null
            },
            onDismiss = { pendingDelete = null }
        )
    }
}

private fun summary(schedules: List<SchedulerProfile>): String {
    if (schedules.isEmpty()) return "Nothing scheduled"
    val active = schedules.count { it.isActive }
    return "$active active · ${schedules.size - active} paused"
}

@Composable
private fun ScheduleCard(
    temperatureUnit: TemperatureUnit,
    schedule: SchedulerProfile,
    onEdit: () -> Unit,
    onSetActive: (Boolean) -> Unit,
    onDelete: () -> Unit
) {
    val info = schedule.controlInfo
    val mode = AirconMode.of(info.mode)
    val active = schedule.isActive
    val shape = RoundedCornerShape(20.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(AirconColors.Surface, shape)
            .border(1.dp, AirconColors.SurfaceAlt, shape)
            .padding(16.dp)
            .height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .alpha(if (active) 1f else 0.55f)
                .clip(RoundedCornerShape(12.dp))
                .clickable(role = Role.Button, onClick = onEdit)
                .semantics(mergeDescendants = true) { contentDescription = "Edit schedule at ${schedule.startTime}" },
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(schedule.startTime, fontFamily = SpaceGrotesk, fontWeight = FontWeight.Medium, fontSize = 30.sp, letterSpacing = (-0.6).sp, color = AirconColors.TextPrimary)
                schedule.endTime?.let {
                    Text("– $it", fontFamily = SpaceGrotesk, fontWeight = FontWeight.Medium, fontSize = 15.sp, color = AirconColors.TextMuted, modifier = Modifier.padding(bottom = 3.dp))
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                val modeColor = if (active && mode != null) mode.accent else AirconColors.TextMuted
                Chip(
                    background = if (active && mode != null) mode.accent.copy(alpha = 0.14f) else AirconColors.SurfaceAlt,
                    color = modeColor
                ) {
                    Box(Modifier.size(7.dp).background(modeColor, CircleShape))
                    Text(mode?.label ?: "Mode ${info.mode}", color = modeColor, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
                Chip {
                    Text(if (mode != null && !mode.hasSetpoint) "—" else displayTemp(info.stemp, temperatureUnit)?.let { "$it${label(temperatureUnit)}" } ?: "—", color = AirconColors.TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
                Chip {
                    Text("Fan ${FanSpeed.ofSchedule(info).shortLabel}", color = AirconColors.TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(AirconIcons.House, contentDescription = null, tint = AirconColors.TextMuted, modifier = Modifier.size(14.dp))
                Text(
                    schedule.zones.filter { it.isOn }.joinToString(", ") { it.name }.ifEmpty { "No zones" },
                    fontSize = 13.sp,
                    color = AirconColors.TextMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxHeight()) {
            AccentSwitch(
                isOn = active,
                accent = AirconColors.Cool,
                width = 52.dp,
                height = 32.dp,
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .toggleable(value = active, role = Role.Switch, onValueChange = onSetActive)
                    .semantics { contentDescription = "Schedule at ${schedule.startTime} ${if (active) "active" else "paused"}" }
            )
            Box(
                modifier = Modifier
                    .offset(x = 8.dp, y = 8.dp)
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(role = Role.Button, onClick = onDelete)
                    .semantics { contentDescription = "Delete schedule at ${schedule.startTime}" },
                contentAlignment = Alignment.Center
            ) {
                Icon(AirconIcons.Trash, contentDescription = null, tint = AirconColors.TextMuted, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun Chip(
    background: Color = AirconColors.SurfaceAlt,
    color: Color = AirconColors.TextSecondary,
    content: @Composable () -> Unit
) {
    Row(
        modifier = Modifier
            .height(26.dp)
            .background(background, RoundedCornerShape(13.dp))
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) { content() }
}

@Composable
private fun EmptyState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .drawBehind {
                drawRoundRect(
                    color = AirconColors.Outline,
                    cornerRadius = CornerRadius(20.dp.toPx()),
                    style = Stroke(width = 1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 4.dp.toPx())))
                )
            }
            .padding(horizontal = 24.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("No schedules yet", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = AirconColors.TextPrimary)
        Text(
            "Tap + to switch the aircon on or off at set times.",
            fontSize = 13.sp,
            color = AirconColors.TextMuted,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun AddButton(onClick: () -> Unit, modifier: Modifier) {
    val shape = RoundedCornerShape(20.dp)
    Box(
        modifier = modifier
            .size(60.dp)
            .shadow(16.dp, shape, ambientColor = AirconColors.Cool, spotColor = AirconColors.Cool)
            .background(AirconColors.Cool, shape)
            .clip(shape)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = "Add schedule" },
        contentAlignment = Alignment.Center
    ) {
        Icon(AirconIcons.Plus, contentDescription = null, tint = AirconColors.OnAccent, modifier = Modifier.size(26.dp))
    }
}

/** Asks before a schedule is deleted (used by the list and the editor). */
@Composable
fun DeleteScheduleDialog(startTime: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AirconColors.Surface,
        title = { Text("Delete schedule?", color = AirconColors.TextPrimary) },
        text = { Text("The schedule at $startTime will be removed.", color = AirconColors.TextSecondary) },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text("Delete", color = AirconColors.PowerOff, fontWeight = FontWeight.SemiBold) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = AirconColors.TextPrimary) }
        }
    )
}
