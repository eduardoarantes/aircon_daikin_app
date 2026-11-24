package com.example.airconapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.AutoMode
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material.icons.outlined.AcUnit
import androidx.compose.material.icons.outlined.AutoMode
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material.icons.outlined.Whatshot
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.airconapp.data.Zone

// Pickers used by the schedule editor, kept as they were before the main screen redesign.

@Composable
fun ModeSelector(currentMode: String, onSetMode: (String) -> Unit) {
    val modes = mapOf(
        "0" to Pair(Pair(Icons.Filled.Settings, Icons.Outlined.Settings), Color.Gray), // Fan (temporarily using Settings icon)
        "1" to Pair(Pair(Icons.Filled.Whatshot, Icons.Outlined.Whatshot), Color.Red), // Heat
        "2" to Pair(Pair(Icons.Filled.AcUnit, Icons.Outlined.AcUnit), Color.Blue), // Cool
        "3" to Pair(Pair(Icons.Filled.AutoMode, Icons.Outlined.AutoMode), Color.Green), // Auto
        "7" to Pair(Pair(Icons.Filled.WaterDrop, Icons.Outlined.WaterDrop), Color.Cyan) // Dry
    )
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Mode", style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            modes.forEach { (key, iconPairAndColor) ->
                val (iconPair, color) = iconPairAndColor
                val (filledIcon, outlinedIcon) = iconPair
                val icon = if (currentMode == key) filledIcon else outlinedIcon

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = icon,
                        contentDescription = "Mode ${key}",
                        tint = color,
                        modifier = Modifier
                            .size(32.dp)
                            .clickable { onSetMode(key) }
                    )
                    Text(text = when(key) {
                        "0" -> "Fan"
                        "1" -> "Heat"
                        "2" -> "Cool"
                        "3" -> "Auto"
                        "7" -> "Dry"
                        else -> "Unknown"
                    }, style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

@Composable
fun FanRateSelector(currentFanRate: String, onSetFanRate: (String) -> Unit) {
    val fanRates = mapOf(
        "A" to Pair(Pair(Icons.Filled.Speed, Icons.Outlined.Speed), Color.Gray), // Auto
        "1" to Pair(Pair(Icons.Filled.Speed, Icons.Outlined.Speed), Color.Blue.copy(alpha = 0.3f)), // Speed 1
        "2" to Pair(Pair(Icons.Filled.Speed, Icons.Outlined.Speed), Color.Blue.copy(alpha = 0.5f)), // Speed 2
        "3" to Pair(Pair(Icons.Filled.Speed, Icons.Outlined.Speed), Color.Blue.copy(alpha = 0.7f)), // Speed 3
        "4" to Pair(Pair(Icons.Filled.Speed, Icons.Outlined.Speed), Color.Blue.copy(alpha = 0.9f)), // Speed 4
        "5" to Pair(Pair(Icons.Filled.Speed, Icons.Outlined.Speed), Color.Blue) // Speed 5
    )
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Fan", style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            fanRates.forEach { (key, iconPairAndColor) ->
                val (iconPair, color) = iconPairAndColor
                val (filledIcon, outlinedIcon) = iconPair
                val icon = if (currentFanRate == key) filledIcon else outlinedIcon

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = icon,
                        contentDescription = "Fan Rate ${key}",
                        tint = color,
                        modifier = Modifier
                            .size(32.dp)
                            .clickable { onSetFanRate(key) }
                    )
                    Text(text = when(key) {
                        "A" -> "Auto"
                        else -> key
                    }, style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

@Composable
fun ZoneItem(zone: Zone, onToggle: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(4.dp))
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = zone.name, fontSize = 18.sp, fontWeight = FontWeight.Medium)
        Switch(
            checked = zone.isOn,
            onCheckedChange = onToggle
        )
    }
}
