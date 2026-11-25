package com.example.airconapp.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.airconapp.data.ControlInfo
import com.example.airconapp.data.Zone
import com.example.airconapp.domain.FanSpeed
import com.example.airconapp.presentation.MainViewModel
import com.example.airconapp.ui.components.AirconIcons
import com.example.airconapp.ui.components.DialSlider
import com.example.airconapp.ui.components.FanSegments
import com.example.airconapp.ui.components.ModeTiles
import com.example.airconapp.ui.components.TemperatureRing
import com.example.airconapp.ui.theme.AirconColors
import com.example.airconapp.ui.theme.SpaceGrotesk
import kotlin.math.roundToInt

/** The unit's own limits (`cool_l`/`cool_h`, `heat_l`/`heat_h` in get_model_info). */
private const val MIN_TEMP = 16
private const val MAX_TEMP = 32

/** Height of everything on the Controls screen except the ring, used to fit the ring on short screens. */
private val CONTENT_WITHOUT_RING = 416.dp
private val RING_MAX = 300.dp
private val RING_MIN = 236.dp

@Composable
fun ControlsScreen(
    viewModel: MainViewModel,
    accent: Color,
    onOpenZones: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val controlInfo by viewModel.controlInfo.collectAsState()
    val zones by viewModel.zoneStatus.collectAsState()
    val sensorInfo by viewModel.sensorInfo.collectAsState()
    val isConnected by viewModel.isConnected.collectAsState()
    val mostUsed by viewModel.mostUsedZones.collectAsState()

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
    // Full size on a tall phone; on a short one the ring gives up some size before anything scrolls
    val ringSize = (maxHeight - CONTENT_WITHOUT_RING).coerceIn(RING_MIN, RING_MAX)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Aircon", fontSize = 24.sp, fontWeight = FontWeight.SemiBold, color = AirconColors.TextPrimary)
                ConnectionStatus(isConnected)
            }
            val info = controlInfo
            if (info != null) {
                PowerButton(isOn = info.pow == "1", enabled = isConnected == true) { viewModel.setPower(info.pow != "1") }
            }
        }

        val info = controlInfo
        if (info == null) {
            LoadingOrUnreachable(isConnected, onOpenSettings)
        } else {
        val enabled = isConnected == true
        Column(modifier = Modifier.alpha(if (enabled) 1f else 0.45f)) {
            ModeRow(info, accent, enabled, onSelect = { viewModel.setMode(it.code) })
            TemperatureSection(
                info = info,
                accent = accent,
                indoorTemp = sensorInfo?.indoorTemp,
                enabled = enabled,
                ringSize = ringSize,
                onSetTemperature = viewModel::setTemperature
            )
            FanSpeedCard(info, accent, enabled, onSelect = viewModel::setFanSpeed)
            val zoneList = zones
            if (!zoneList.isNullOrEmpty() && mostUsed.isNotEmpty()) {
                MostUsedZones(
                    zones = zoneList,
                    indices = mostUsed,
                    accent = accent,
                    textAccent = textAccent(info.pow == "1", accent),
                    enabled = enabled,
                    onToggle = viewModel::toggleZone,
                    onOpenZones = onOpenZones
                )
            }
            Spacer(Modifier.height(8.dp))
        }
        }
    }
    }
}

/**
 * Shown before the first status arrives: a spinner while searching for the aircon, then, once
 * it couldn't be reached after a few tries, a note with a way to Settings (searching goes on).
 */
@Composable
fun LoadingOrUnreachable(isConnected: Boolean?, onOpenSettings: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 120.dp, start = 32.dp, end = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (isConnected == false) {
            Text("Can't reach the aircon", color = AirconColors.TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Text(
                "Still trying. Check the Wi-Fi, or find the aircon in Settings.",
                color = AirconColors.TextMuted,
                fontSize = 13.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Box(
                modifier = Modifier
                    .height(44.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(AirconColors.Surface)
                    .border(1.dp, AirconColors.Outline, RoundedCornerShape(22.dp))
                    .clickable(role = Role.Button, onClick = onOpenSettings)
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Open Settings", color = AirconColors.TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
        } else {
            CircularProgressIndicator(color = AirconColors.TextMuted)
            Text("Searching for the aircon…", color = AirconColors.TextMuted, fontSize = 13.sp)
        }
    }
}

@Composable
fun ConnectionStatus(isConnected: Boolean?) {
    val (dot, label) = when (isConnected) {
        true -> AirconColors.Connected to "Connected"
        false -> AirconColors.PowerOff to "Disconnected"
        null -> AirconColors.TextMuted to "Connecting…"
    }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(Modifier.size(8.dp).background(dot, CircleShape))
        Text("Daikin · $label", fontSize = 13.sp, color = AirconColors.TextMuted)
    }
}

@Composable
private fun PowerButton(isOn: Boolean, enabled: Boolean, onClick: () -> Unit) {
    val background = if (isOn) AirconColors.Connected.copy(alpha = 0.14f) else AirconColors.Surface
    val border = if (isOn) AirconColors.Connected.copy(alpha = 0.5f) else Color(0xFF3A2A2C)
    val tint = if (isOn) AirconColors.Connected else AirconColors.PowerOff
    Box(
        modifier = Modifier
            .size(52.dp)
            .alpha(if (enabled) 1f else 0.45f)
            .clip(CircleShape)
            .background(background)
            .border(1.dp, border, CircleShape)
            .toggleable(value = isOn, enabled = enabled, role = Role.Switch, onValueChange = { onClick() })
            .semantics { contentDescription = "Power" },
        contentAlignment = Alignment.Center
    ) {
        Icon(AirconIcons.Power, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
    }
}

@Composable
private fun ModeRow(info: ControlInfo, accent: Color, enabled: Boolean, onSelect: (AirconMode) -> Unit) {
    ModeTiles(
        selected = AirconMode.of(info.mode),
        accent = accent,
        labelAccent = textAccent(info.pow == "1", accent),
        enabled = enabled,
        onSelect = onSelect,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp)
    )
}

@Composable
private fun TemperatureSection(
    info: ControlInfo,
    accent: Color,
    indoorTemp: String?,
    enabled: Boolean,
    ringSize: Dp,
    onSetTemperature: (Int) -> Unit
) {
    val k = ringSize / RING_MAX
    val isOn = info.pow == "1"
    val mode = AirconMode.of(info.mode)
    val isFan = mode == AirconMode.FAN
    val setTemp = info.stemp.toFloatOrNull()?.roundToInt()?.coerceIn(MIN_TEMP, MAX_TEMP)
    // The temperature can be set with the unit off too (it applies when it's turned on);
    // only Fan mode has none
    val canChange = enabled && !isFan && setTemp != null
    val step = { delta: Int -> setTemp?.let { onSetTemperature((it + delta).coerceIn(MIN_TEMP, MAX_TEMP)) } }

    // While dragging, the ring follows the finger; the command goes out on release
    var dragTemp by remember { mutableStateOf<Int?>(null) }
    val shownTemp = dragTemp ?: setTemp

    val target = when {
        isFan -> if (isOn) 1f else 0f
        shownTemp == null -> 0f
        else -> (shownTemp - MIN_TEMP).toFloat() / (MAX_TEMP - MIN_TEMP)
    }
    val fraction by animateFloatAsState(
        target,
        if (dragTemp != null) snap() else tween(300),
        label = "ringFill"
    )

    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().padding(top = 20.dp)) {
        TemperatureRing(
            accent = accent,
            fraction = fraction,
            glow = isOn,
            showHandle = !isFan,
            stepsEnabled = canChange,
            onDecrease = { step(-1) },
            onIncrease = { step(1) },
            ringSize = ringSize,
            slider = if (canChange) DialSlider(shownTemp!!, MIN_TEMP..MAX_TEMP) { onSetTemperature(it) } else null,
            onDrag = if (canChange) { f -> dragTemp = (MIN_TEMP + f * (MAX_TEMP - MIN_TEMP)).roundToInt() } else null,
            onDragEnd = {
                dragTemp?.let { if (it != setTemp) onSetTemperature(it) }
                dragTemp = null
            },
            onDragCancel = { dragTemp = null }
        ) {
            when {
                !isOn && isFan -> {
                    Text("Off", fontFamily = SpaceGrotesk, fontWeight = FontWeight.Medium, fontSize = 56.sp * k, color = AirconColors.OffText)
                    Text("Tap power to start", fontSize = 13.sp, color = AirconColors.TextMuted)
                }
                isFan -> {
                    CapsLabel("Fan only", accent)
                    Text(
                        FanSpeed.current(info)?.label ?: "—",
                        fontFamily = SpaceGrotesk,
                        fontWeight = FontWeight.Medium,
                        fontSize = 44.sp * k,
                        color = AirconColors.TextPrimary
                    )
                }
                else -> {
                    // Off: the temperature it will use when turned on, in grey
                    CapsLabel(if (isOn) mode?.verb ?: "Set to" else "Off · set to", textAccent(isOn, accent))
                    Row(verticalAlignment = Alignment.Top) {
                        Text(
                            shownTemp?.toString() ?: info.stemp,
                            style = TextStyle(
                                fontFamily = SpaceGrotesk,
                                fontWeight = FontWeight.Medium,
                                fontSize = 88.sp * k,
                                lineHeight = 88.sp * k,
                                letterSpacing = (-3.5).sp * k,
                                color = if (isOn) AirconColors.TextPrimary else AirconColors.TextSecondary
                            )
                        )
                        Text(
                            "°C",
                            fontFamily = SpaceGrotesk,
                            fontWeight = FontWeight.Medium,
                            fontSize = 26.sp * k,
                            color = AirconColors.TextSecondary,
                            modifier = Modifier.padding(top = 12.dp * k, start = 2.dp)
                        )
                    }
                }
            }
            // Only the unit's real reading; nothing when the sensor reports none
            if (indoorTemp != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Icon(AirconIcons.House, contentDescription = null, tint = AirconColors.TextMuted, modifier = Modifier.size(14.dp))
                    Text("Indoor $indoorTemp°C", fontSize = 13.sp, color = AirconColors.TextMuted)
                }
            }
        }
    }
}

@Composable
private fun CapsLabel(text: String, accent: Color) {
    Text(
        text.uppercase(),
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 1.4.sp,
        color = accent
    )
}

@Composable
private fun Card(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val shape = RoundedCornerShape(20.dp)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(AirconColors.Surface, shape)
            .border(1.dp, AirconColors.SurfaceAlt, shape)
            .padding(16.dp)
    ) { content() }
}

@Composable
private fun FanSpeedCard(info: ControlInfo, accent: Color, enabled: Boolean, onSelect: (FanSpeed) -> Unit) {
    val current = FanSpeed.current(info)
    Card(modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Fan speed", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = AirconColors.TextPrimary, modifier = Modifier.weight(1f))
                Text(current?.label ?: "—", fontSize = 13.sp, color = AirconColors.TextMuted)
            }
            FanSegments(FanSpeed.available(info), current, accent, enabled, onSelect)
        }
    }
}

@Composable
private fun MostUsedZones(
    zones: List<Zone>,
    indices: List<Int>,
    accent: Color,
    textAccent: Color,
    enabled: Boolean,
    onToggle: (Int) -> Unit,
    onOpenZones: () -> Unit
) {
    Column(
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 4.dp)) {
            Text("Most used zones", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = AirconColors.TextPrimary, modifier = Modifier.weight(1f))
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(role = Role.Button, onClick = onOpenZones)
                    .padding(horizontal = 4.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("All zones", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = AirconColors.TextMuted)
                Icon(AirconIcons.ChevronRight, contentDescription = null, tint = AirconColors.TextMuted, modifier = Modifier.size(16.dp))
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            // Ranked by use, shown in the unit's order so a tap never swaps the tiles under the finger
            indices.take(2).sorted().forEach { index ->
                ZoneTile(zones[index], accent, textAccent, enabled, Modifier.weight(1f)) { onToggle(index) }
            }
            if (indices.size == 1) Spacer(Modifier.weight(1f))
        }
    }
}

@Composable
private fun ZoneTile(zone: Zone, accent: Color, textAccent: Color, enabled: Boolean, modifier: Modifier, onToggle: () -> Unit) {
    val shape = RoundedCornerShape(18.dp)
    Row(
        modifier = modifier
            .clip(shape)
            .background(AirconColors.Surface)
            .border(1.dp, if (zone.isOn) AirconColors.ActiveBorder else AirconColors.SurfaceAlt, shape)
            .toggleable(value = zone.isOn, enabled = enabled, role = Role.Switch, onValueChange = { onToggle() })
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ZoneIconTile(zone.isOn, accent, size = 36, corner = 12, iconSize = 18)
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(zone.name, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = AirconColors.TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            ZoneStatusText(zone.isOn, textAccent, fontSize = 12)
        }
    }
}

@Composable
fun ZoneIconTile(isOn: Boolean, accent: Color, size: Int, corner: Int, iconSize: Int) {
    Box(
        modifier = Modifier
            .size(size.dp)
            .background(if (isOn) accent else AirconColors.SurfaceAlt, RoundedCornerShape(corner.dp)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            AirconIcons.House,
            contentDescription = null,
            tint = if (isOn) AirconColors.OnAccent else AirconColors.TextSecondary,
            modifier = Modifier.size(iconSize.dp)
        )
    }
}

/** "On" in [textAccent], "Off" muted. */
@Composable
fun ZoneStatusText(isOn: Boolean, textAccent: Color, fontSize: Int) {
    Text(
        if (isOn) "On" else "Off",
        fontSize = fontSize.sp,
        fontWeight = FontWeight.Medium,
        color = if (isOn) textAccent else AirconColors.TextMuted
    )
}
