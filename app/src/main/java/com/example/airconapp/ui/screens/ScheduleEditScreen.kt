package com.example.airconapp.ui.screens

import com.example.airconapp.domain.TemperatureUnit
import com.example.airconapp.domain.displayTemp
import com.example.airconapp.domain.label
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.airconapp.data.ControlInfo
import com.example.airconapp.data.Zone
import com.example.airconapp.data.db.SchedulerProfile
import com.example.airconapp.domain.FanSpeed
import com.example.airconapp.ui.components.AccentSwitch
import com.example.airconapp.ui.components.AirconIcons
import com.example.airconapp.ui.components.FanSegments
import com.example.airconapp.ui.components.ModeTiles
import com.example.airconapp.ui.theme.AirconColors
import com.example.airconapp.ui.theme.Inter
import com.example.airconapp.ui.theme.SpaceGrotesk
import java.time.LocalTime

/** Same limits as the Controls dial: the unit's own range. */
private val TEMP_RANGE = 16..32
private const val DEFAULT_TEMP = 23

/**
 * New / edit schedule. [schedule] is null when creating; a new schedule starts from the unit's
 * [current] settings (mode, temperature, fan, and the zones that are open now). [deviceZones]
 * are the unit's zones; zones a saved schedule names that the unit no longer reports are kept
 * so nothing is lost.
 */
@Composable
fun ScheduleEditScreen(
    schedule: SchedulerProfile?,
    current: ControlInfo?,
    deviceZones: List<Zone>,
    temperatureUnit: TemperatureUnit,
    onSave: (SchedulerProfile) -> Unit,
    onDelete: (SchedulerProfile) -> Unit,
    onClose: () -> Unit
) {
    // Editing: the saved settings. New: what the unit is set to right now
    val info = schedule?.controlInfo ?: current
    var startTime by remember { mutableStateOf(schedule?.startTime ?: defaultStartTime()) }
    var endTime by remember { mutableStateOf(schedule?.endTime) }
    var mode by remember { mutableStateOf(AirconMode.of(info?.mode) ?: AirconMode.COOL) }
    var temperature by remember {
        mutableStateOf(info?.stemp?.toFloatOrNull()?.toInt()?.coerceIn(TEMP_RANGE) ?: DEFAULT_TEMP)
    }
    var fan by remember { mutableStateOf(info?.let { FanSpeed.ofSchedule(it) } ?: FanSpeed.AUTO) }
    var selected by remember {
        mutableStateOf((schedule?.zones ?: deviceZones).filter { it.isOn }.map { it.name }.toSet())
    }
    var active by remember { mutableStateOf(schedule?.isActive ?: true) }

    var picking by remember { mutableStateOf<TimeField?>(null) }
    var confirmDelete by remember { mutableStateOf(false) }

    val zoneNames = (deviceZones.map { it.name } + schedule?.zones.orEmpty().map { it.name }).distinct()
    val accent by animateColorAsState(mode.accent, tween(200), label = "formAccent")
    val canSave = selected.isNotEmpty()

    fun save() {
        val base = info ?: ControlInfo(pow = "1", mode = mode.code, stemp = "$temperature", f_rate = "3", f_dir = "0")
        val control = FanSpeed.applyToSchedule(
            base.copy(pow = "1", mode = mode.code, stemp = "$temperature"),
            fan
        )
        val zones = zoneNames.map { Zone(it, it in selected) }
        val profile = schedule?.copy(startTime = startTime, endTime = endTime, controlInfo = control, zones = zones, isActive = active)
            ?: SchedulerProfile(startTime = startTime, endTime = endTime, controlInfo = control, zones = zones, isActive = active)
        onSave(profile)
    }

    CompositionLocalProvider(LocalTextStyle provides TextStyle(fontFamily = Inter, color = AirconColors.TextPrimary)) {
        Column(modifier = Modifier.fillMaxSize().background(AirconColors.Background)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, top = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RoundIconButton(AirconIcons.Close, "Close", AirconColors.TextPrimary, onClose)
                Text(
                    if (schedule == null) "New schedule" else "Edit schedule",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                if (schedule != null) {
                    RoundIconButton(AirconIcons.Trash, "Delete schedule", AirconColors.PowerOff) { confirmDelete = true }
                } else {
                    Spacer(Modifier.size(44.dp))
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    TimeTile("Start", startTime, accent.copy(alpha = 0.5f), Modifier.weight(1f)) { picking = TimeField.Start }
                    TimeTile("End · optional", endTime, AirconColors.SurfaceAlt, Modifier.weight(1f)) { picking = TimeField.End }
                }

                FormCard {
                    Text("Mode", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    ModeTiles(
                        selected = mode,
                        accent = accent,
                        labelAccent = accent,
                        enabled = true,
                        onSelect = { mode = it },
                        tileSize = 46.dp,
                        corner = 16.dp,
                        iconSize = 22.dp,
                        labelGap = 6.dp,
                        unselectedFill = AirconColors.SurfaceAlt
                    )
                }

                TemperatureCard(
                    temperature = temperature,
                    temperatureUnit = temperatureUnit,
                    accent = accent,
                    enabled = mode.hasSetpoint,
                    onChange = { temperature = it.coerceIn(TEMP_RANGE) }
                )

                FormCard {
                    Text("Fan speed", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    FanSegments(FanSpeed.entries, fan, accent, enabled = true, onSelect = { fan = it })
                }

                FormCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Zones", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                        Text(
                            if (selected.isEmpty()) "Pick at least one" else "${selected.size} selected",
                            fontSize = 12.sp,
                            color = AirconColors.TextMuted
                        )
                    }
                    if (zoneNames.isEmpty()) {
                        Text("No zones yet: the aircon hasn't reported them.", fontSize = 13.sp, color = AirconColors.TextMuted)
                    } else {
                        ZoneChips(zoneNames, selected, accent) { name ->
                            selected = if (name in selected) selected - name else selected + name
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .toggleable(value = active, role = Role.Switch, onValueChange = { active = it })
                        .padding(horizontal = 6.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text("Schedule active", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        Text("Pause without deleting it", fontSize = 12.sp, color = AirconColors.TextMuted)
                    }
                    AccentSwitch(active, accent, width = 52.dp, height = 32.dp)
                }
            }

            // Footer stays put; only the form above scrolls on short screens
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                PillButton("Cancel", AirconColors.Surface, AirconColors.TextPrimary, AirconColors.Outline, enabled = true, onClick = onClose)
                PillButton("Save schedule", accent, AirconColors.OnAccent, accent, enabled = canSave, onClick = ::save)
            }
        }
    }

    when (picking) {
        TimeField.Start -> TimePickerDialog(
            title = "Start time",
            initial = startTime,
            accent = accent,
            onConfirm = { startTime = it; picking = null },
            onRemove = null,
            onDismiss = { picking = null }
        )
        TimeField.End -> TimePickerDialog(
            title = "End time",
            initial = endTime ?: startTime,
            accent = accent,
            onConfirm = { endTime = it; picking = null },
            onRemove = endTime?.let { { endTime = null; picking = null } },
            onDismiss = { picking = null }
        )
        null -> Unit
    }

    if (confirmDelete && schedule != null) {
        DeleteScheduleDialog(
            startTime = schedule.startTime,
            onConfirm = { confirmDelete = false; onDelete(schedule) },
            onDismiss = { confirmDelete = false }
        )
    }
}

private enum class TimeField { Start, End }

/** Now, rounded up to the next 5 minutes. */
private fun defaultStartTime(): String {
    val now = LocalTime.now().withSecond(0).withNano(0)
    val rounded = now.plusMinutes(((5 - now.minute % 5) % 5).toLong())
    return "%02d:%02d".format(rounded.hour, rounded.minute)
}

@Composable
internal fun RoundIconButton(icon: ImageVector, description: String, tint: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
    }
}

@Composable
private fun TimeTile(label: String, time: String?, border: Color, modifier: Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(20.dp)
    Column(
        modifier = modifier
            .height(76.dp)
            .clip(shape)
            .background(AirconColors.Surface)
            .border(1.dp, border, shape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically)
    ) {
        Text(label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = AirconColors.TextMuted)
        if (time != null) {
            Text(time, fontFamily = SpaceGrotesk, fontWeight = FontWeight.Medium, fontSize = 28.sp, lineHeight = 28.sp)
        } else {
            Row(
                modifier = Modifier.height(28.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(AirconIcons.Plus, contentDescription = null, tint = AirconColors.TextSecondary, modifier = Modifier.size(16.dp))
                Text("Add end time", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = AirconColors.TextSecondary)
            }
        }
    }
}

@Composable
internal fun FormCard(content: @Composable ColumnScope.() -> Unit) {
    val shape = RoundedCornerShape(20.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(AirconColors.Surface, shape)
            .border(1.dp, AirconColors.SurfaceAlt, shape)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        content = content
    )
}

@Composable
internal fun TemperatureCard(temperatureUnit: TemperatureUnit, temperature: Int, accent: Color, enabled: Boolean, onChange: (Int) -> Unit) {
    val shape = RoundedCornerShape(20.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(AirconColors.Surface, shape)
            .border(1.dp, AirconColors.SurfaceAlt, shape)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text("Temperature", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text(
                if (enabled) "${displayTemp(TEMP_RANGE.first, temperatureUnit)}–${displayTemp(TEMP_RANGE.last, temperatureUnit)}${label(temperatureUnit)}" else "Not used in this mode",
                fontSize = 12.sp,
                color = AirconColors.TextMuted
            )
        }
        Row(
            modifier = Modifier.alpha(if (enabled) 1f else 0.35f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            StepperButton(AirconIcons.Minus, "Lower temperature", enabled && temperature > TEMP_RANGE.first) { onChange(temperature - 1) }
            Text(
                "${displayTemp(temperature, temperatureUnit)}${label(temperatureUnit)}",
                fontFamily = SpaceGrotesk,
                fontWeight = FontWeight.Medium,
                fontSize = 26.sp,
                color = accent,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.width(76.dp)
            )
            StepperButton(AirconIcons.Plus, "Raise temperature", enabled && temperature < TEMP_RANGE.last) { onChange(temperature + 1) }
        }
    }
}

@Composable
private fun StepperButton(icon: ImageVector, description: String, enabled: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(AirconColors.SurfaceAlt)
            .border(1.dp, AirconColors.Outline, CircleShape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = AirconColors.TextPrimary, modifier = Modifier.size(18.dp))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ZoneChips(names: List<String>, selected: Set<String>, accent: Color, onToggle: (String) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        names.forEach { name ->
            val isOn = name in selected
            val shape = RoundedCornerShape(19.dp)
            Row(
                modifier = Modifier
                    .height(38.dp)
                    .clip(shape)
                    .background(if (isOn) accent.copy(alpha = 0.14f) else AirconColors.SurfaceAlt)
                    .border(1.dp, if (isOn) accent else AirconColors.Outline, shape)
                    .toggleable(value = isOn, role = Role.Checkbox, onValueChange = { onToggle(name) })
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (isOn) Icon(AirconIcons.Check, contentDescription = null, tint = accent, modifier = Modifier.size(14.dp))
                Text(name, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = if (isOn) accent else AirconColors.TextSecondary)
            }
        }
    }
}

@Composable
internal fun RowScope.PillButton(
    label: String,
    fill: Color,
    content: Color,
    border: Color,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(26.dp)
    Box(
        modifier = Modifier
            .weight(1f)
            .height(52.dp)
            .alpha(if (enabled) 1f else 0.4f)
            .clip(shape)
            .background(fill)
            .border(1.dp, border, shape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(label, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = content)
    }
}

/** Material time picker in a dialog. [onRemove] adds a "Remove" button (used to clear the end time). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePickerDialog(
    title: String,
    initial: String,
    accent: Color,
    onConfirm: (String) -> Unit,
    onRemove: (() -> Unit)?,
    onDismiss: () -> Unit
) {
    val parsed = runCatching { LocalTime.parse(initial) }.getOrElse { LocalTime.now() }
    val state = rememberTimePickerState(initialHour = parsed.hour, initialMinute = parsed.minute, is24Hour = true)
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(shape = RoundedCornerShape(28.dp), color = AirconColors.Surface, modifier = Modifier.padding(horizontal = 16.dp)) {
            Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = AirconColors.TextMuted,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp)
                )
                TimePicker(
                    state = state,
                    colors = TimePickerDefaults.colors(
                        clockDialColor = AirconColors.SurfaceAlt,
                        clockDialSelectedContentColor = AirconColors.OnAccent,
                        clockDialUnselectedContentColor = AirconColors.TextPrimary,
                        selectorColor = accent,
                        containerColor = AirconColors.Surface,
                        timeSelectorSelectedContainerColor = accent.copy(alpha = 0.2f),
                        timeSelectorSelectedContentColor = accent,
                        timeSelectorUnselectedContainerColor = AirconColors.SurfaceAlt,
                        timeSelectorUnselectedContentColor = AirconColors.TextPrimary
                    )
                )
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    if (onRemove != null) {
                        TextButton(onClick = onRemove) { Text("Remove", color = AirconColors.PowerOff, fontWeight = FontWeight.SemiBold) }
                    }
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = onDismiss) { Text("Cancel", color = AirconColors.TextPrimary) }
                    TextButton(onClick = { onConfirm("%02d:%02d".format(state.hour, state.minute)) }) {
                        Text("OK", color = accent, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}
