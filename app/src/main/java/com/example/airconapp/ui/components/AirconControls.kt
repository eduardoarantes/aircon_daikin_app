package com.example.airconapp.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.airconapp.domain.FanSpeed
import com.example.airconapp.ui.screens.AirconMode
import com.example.airconapp.ui.theme.AirconColors

/**
 * The five mode tiles (Auto, Cool, Heat, Fan, Dry). The selected tile is filled with [accent]
 * and labelled in [labelAccent]; the others use [unselectedFill] with an outline.
 */
@Composable
fun ModeTiles(
    selected: AirconMode?,
    accent: Color,
    labelAccent: Color,
    enabled: Boolean,
    onSelect: (AirconMode) -> Unit,
    modifier: Modifier = Modifier,
    tileSize: Dp = 52.dp,
    corner: Dp = 18.dp,
    iconSize: Dp = 24.dp,
    labelGap: Dp = 8.dp,
    unselectedFill: Color = AirconColors.Surface
) {
    Row(modifier = modifier.fillMaxWidth().selectableGroup(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        AirconMode.entries.forEach { mode ->
            val isSelected = mode == selected
            val shape = RoundedCornerShape(corner)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(shape)
                    .selectable(selected = isSelected, enabled = enabled, role = Role.Tab, onClick = { onSelect(mode) }),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(labelGap)
            ) {
                Box(
                    modifier = Modifier
                        .size(tileSize)
                        .background(if (isSelected) accent else unselectedFill, shape)
                        .border(1.dp, if (isSelected) accent else AirconColors.Outline, shape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        mode.icon,
                        contentDescription = null,
                        tint = if (isSelected) AirconColors.OnAccent else AirconColors.TextSecondary,
                        modifier = Modifier.size(iconSize)
                    )
                }
                Text(
                    mode.label,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (isSelected) labelAccent else AirconColors.TextMuted
                )
            }
        }
    }
}

/** Segmented fan speed control on a dark track; the selected option is filled with [accent]. */
@Composable
fun FanSegments(
    options: List<FanSpeed>,
    selected: FanSpeed?,
    accent: Color,
    enabled: Boolean,
    onSelect: (FanSpeed) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(AirconColors.Background, RoundedCornerShape(14.dp))
            .padding(4.dp)
            .selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        options.forEach { speed ->
            val isSelected = speed == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isSelected) accent else Color.Transparent)
                    .selectable(selected = isSelected, enabled = enabled, role = Role.Tab, onClick = { onSelect(speed) }),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    speed.shortLabel,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isSelected) AirconColors.OnAccent else AirconColors.TextSecondary,
                    maxLines = 1
                )
            }
        }
    }
}

/**
 * The design's switch: accent track with a dark knob when on, grey when off. Drawing only; the
 * caller makes the row or the switch itself toggleable.
 */
@Composable
fun AccentSwitch(isOn: Boolean, accent: Color, modifier: Modifier = Modifier, width: Dp = 56.dp, height: Dp = 34.dp) {
    val knobSize = height - 6.dp
    val track by animateColorAsState(if (isOn) accent else AirconColors.Outline, tween(200), label = "track")
    val knob by animateColorAsState(if (isOn) AirconColors.OnAccent else AirconColors.SwitchKnobOff, tween(200), label = "knob")
    val knobOffset by animateDpAsState(if (isOn) width - height else 0.dp, tween(200), label = "knobOffset")
    Box(
        modifier = modifier
            .size(width = width, height = height)
            .background(track, RoundedCornerShape(height / 2))
            .padding(3.dp)
    ) {
        Box(
            modifier = Modifier
                .offset(x = knobOffset)
                .size(knobSize)
                .shadow(2.dp, CircleShape)
                .background(knob, CircleShape)
        )
    }
}
