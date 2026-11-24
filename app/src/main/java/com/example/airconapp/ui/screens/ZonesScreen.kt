package com.example.airconapp.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.airconapp.data.Zone
import com.example.airconapp.presentation.MainViewModel
import com.example.airconapp.ui.components.AirconIcons
import com.example.airconapp.ui.theme.AirconColors

@Composable
fun ZonesScreen(
    viewModel: MainViewModel,
    accent: Color,
    onOpenSchedules: () -> Unit,
    modifier: Modifier = Modifier
) {
    val zones by viewModel.zoneStatus.collectAsState()
    val controlInfo by viewModel.controlInfo.collectAsState()
    val isConnected by viewModel.isConnected.collectAsState()
    val enabled = isConnected == true
    val textAccent = textAccent(controlInfo?.pow == "1", accent)

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        val list = zones
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 24.dp, end = 16.dp, top = 24.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Zones", fontSize = 24.sp, fontWeight = FontWeight.SemiBold, color = AirconColors.TextPrimary)
                if (list != null) {
                    Text("${list.count { it.isOn }} of ${list.size} zones on", fontSize = 13.sp, color = AirconColors.TextMuted)
                } else {
                    ConnectionStatus(isConnected)
                }
            }
            val canSetAll = enabled && !list.isNullOrEmpty()
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PillButton("All on", canSetAll) { viewModel.setAllZones(true) }
                PillButton("All off", canSetAll) { viewModel.setAllZones(false) }
            }
        }

        if (list == null) {
            LoadingOrUnreachable(isConnected)
        } else {
        Column(
            modifier = Modifier
                .padding(start = 16.dp, end = 16.dp, top = 20.dp)
                .alpha(if (enabled) 1f else 0.45f),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            list.forEachIndexed { index, zone ->
                ZoneCard(zone, accent, textAccent, enabled) { viewModel.toggleZone(index) }
            }
        }

        ManageSchedulesCard(onOpenSchedules)
        Spacer(Modifier.height(16.dp))
        }
    }
}

/** Accent for text: while the unit is off the grey accent is too dark to read, so use a lighter one. */
fun textAccent(unitOn: Boolean, accent: Color) = if (unitOn) accent else AirconColors.SwitchKnobOff

@Composable
private fun PillButton(label: String, enabled: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(20.dp)
    Box(
        modifier = Modifier
            .height(44.dp)
            .alpha(if (enabled) 1f else 0.45f)
            .clip(shape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        // 40dp pill inside a 44dp touch target
        Box(
            modifier = Modifier
                .height(40.dp)
                .background(AirconColors.Surface, shape)
                .border(1.dp, AirconColors.Outline, shape)
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(label, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = AirconColors.TextPrimary)
        }
    }
}

@Composable
private fun ZoneCard(zone: Zone, accent: Color, textAccent: Color, enabled: Boolean, onToggle: () -> Unit) {
    val shape = RoundedCornerShape(20.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(AirconColors.Surface)
            .border(1.dp, if (zone.isOn) AirconColors.ActiveBorder else AirconColors.SurfaceAlt, shape)
            .toggleable(value = zone.isOn, enabled = enabled, role = Role.Switch, onValueChange = { onToggle() })
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        ZoneIconTile(zone.isOn, accent, size = 44, corner = 14, iconSize = 20)
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(zone.name, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = AirconColors.TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            ZoneStatusText(zone.isOn, textAccent, fontSize = 13)
        }
        ZoneSwitch(zone.isOn, accent)
    }
}

/** 56×34dp track with a 28dp knob. The whole card is the touch target. */
@Composable
private fun ZoneSwitch(isOn: Boolean, accent: Color) {
    val track by animateColorAsState(if (isOn) accent else AirconColors.Outline, tween(200), label = "track")
    val knob by animateColorAsState(if (isOn) AirconColors.OnAccent else AirconColors.SwitchKnobOff, tween(200), label = "knob")
    val knobOffset by animateDpAsState(if (isOn) 22.dp else 0.dp, tween(200), label = "knobOffset")
    Box(
        modifier = Modifier
            .size(width = 56.dp, height = 34.dp)
            .background(track, RoundedCornerShape(17.dp))
            .padding(3.dp)
    ) {
        Box(
            modifier = Modifier
                .offset(x = knobOffset)
                .size(28.dp)
                .shadow(2.dp, CircleShape)
                .background(knob, CircleShape)
        )
    }
}

@Composable
private fun ManageSchedulesCard(onClick: () -> Unit) {
    val shape = RoundedCornerShape(20.dp)
    Row(
        modifier = Modifier
            .padding(start = 16.dp, end = 16.dp, top = 20.dp)
            .fillMaxWidth()
            .clip(shape)
            .background(AirconColors.Surface)
            .border(1.dp, AirconColors.SurfaceAlt, shape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier.size(40.dp).background(AirconColors.SurfaceAlt, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(AirconIcons.Calendar, contentDescription = null, tint = AirconColors.TextSecondary, modifier = Modifier.size(20.dp))
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text("Manage schedules", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = AirconColors.TextPrimary)
            Text("Set when zones and modes switch on", fontSize = 13.sp, color = AirconColors.TextMuted)
        }
        Icon(AirconIcons.ChevronRight, contentDescription = null, tint = AirconColors.TextMuted, modifier = Modifier.size(18.dp))
    }
}
