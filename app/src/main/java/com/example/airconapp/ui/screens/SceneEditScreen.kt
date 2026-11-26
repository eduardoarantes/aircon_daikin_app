package com.example.airconapp.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.airconapp.data.ControlInfo
import com.example.airconapp.data.Zone
import com.example.airconapp.domain.FanSpeed
import com.example.airconapp.domain.Scene
import com.example.airconapp.domain.SceneLogic
import com.example.airconapp.ui.components.AirconIcons
import com.example.airconapp.ui.components.FanSegments
import com.example.airconapp.ui.components.ModeTiles
import com.example.airconapp.ui.theme.AirconColors
import com.example.airconapp.ui.theme.Inter
import java.util.UUID

/** Launchers cut shortcut labels at around this length. */
private const val NAME_HINT_LENGTH = 25

/**
 * New / edit scene. [scene] is null when creating; a new scene starts from the unit's [current]
 * settings and open zones, so "set it up, then save it as a scene" works.
 */
@Composable
fun SceneEditScreen(
    scene: Scene?,
    current: ControlInfo?,
    deviceZones: List<Zone>,
    onSave: (Scene) -> Unit,
    onDelete: (Scene) -> Unit,
    onClose: () -> Unit
) {
    var name by remember { mutableStateOf(scene?.name ?: "") }
    var mode by remember { mutableStateOf(AirconMode.of(scene?.mode ?: current?.mode) ?: AirconMode.COOL) }
    var temperature by remember {
        mutableStateOf(
            (scene?.temperature ?: current?.stemp?.toFloatOrNull()?.toInt() ?: 21).coerceIn(SceneLogic.TEMPERATURE_RANGE)
        )
    }
    var fan by remember { mutableStateOf(scene?.fan ?: current?.let { FanSpeed.ofSchedule(it) } ?: FanSpeed.AUTO) }
    var selected by remember {
        mutableStateOf(scene?.zones?.toSet() ?: deviceZones.filter { it.isOn }.map { it.name }.toSet())
    }
    var confirmDelete by remember { mutableStateOf(false) }

    val zoneNames = (deviceZones.map { it.name } + scene?.zones.orEmpty()).distinct()
    val accent by animateColorAsState(mode.accent, tween(200), label = "sceneAccent")
    val canSave = name.isNotBlank() && selected.isNotEmpty()

    fun save() {
        onSave(
            Scene(
                id = scene?.id ?: UUID.randomUUID().toString(),
                name = name.trim(),
                mode = mode.code,
                temperature = temperature,
                fan = fan,
                // Keep the unit's zone order
                zones = zoneNames.filter { it in selected }
            )
        )
    }

    CompositionLocalProvider(LocalTextStyle provides TextStyle(fontFamily = Inter, color = AirconColors.TextPrimary)) {
        Column(modifier = Modifier.fillMaxSize().background(AirconColors.Background)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, top = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RoundIconButton(AirconIcons.Close, "Close", AirconColors.TextPrimary, onClose)
                Text(
                    if (scene == null) "New scene" else "Edit scene",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )
                if (scene != null) {
                    RoundIconButton(AirconIcons.Trash, "Delete scene", AirconColors.PowerOff) { confirmDelete = true }
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
                FormCard {
                    Text("Name", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it.take(40) },
                        placeholder = { Text("e.g. Bedroom cool", color = AirconColors.TextMuted) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                        supportingText = if (name.length > NAME_HINT_LENGTH) {
                            { Text("Long names may be cut short in the shortcut menu", color = AirconColors.TextMuted) }
                        } else null,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = AirconColors.TextPrimary,
                            unfocusedTextColor = AirconColors.TextPrimary,
                            focusedBorderColor = accent,
                            unfocusedBorderColor = AirconColors.Outline,
                            cursorColor = accent
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
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
                    accent = accent,
                    enabled = mode.hasSetpoint,
                    onChange = { temperature = it.coerceIn(SceneLogic.TEMPERATURE_RANGE) }
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
                    Text("The other zones are closed.", fontSize = 12.sp, color = AirconColors.TextMuted)
                    if (zoneNames.isEmpty()) {
                        Text("No zones yet: the aircon hasn't reported them.", fontSize = 13.sp, color = AirconColors.TextMuted)
                    } else {
                        ZoneChips(zoneNames, selected, accent) { zone ->
                            selected = if (zone in selected) selected - zone else selected + zone
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                PillButton("Cancel", AirconColors.Surface, AirconColors.TextPrimary, AirconColors.Outline, enabled = true, onClick = onClose)
                PillButton("Save scene", accent, AirconColors.OnAccent, accent, enabled = canSave, onClick = ::save)
            }
        }
    }

    if (confirmDelete && scene != null) {
        DeleteSceneDialog(
            name = scene.name,
            onConfirm = { confirmDelete = false; onDelete(scene) },
            onDismiss = { confirmDelete = false }
        )
    }
}

/** Asks before a scene is deleted (used by the list and the editor). */
@Composable
fun DeleteSceneDialog(name: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AirconColors.Surface,
        title = { Text("Delete scene?", color = AirconColors.TextPrimary) },
        text = { Text("\"$name\" will be removed, along with its shortcut.", color = AirconColors.TextSecondary) },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text("Delete", color = AirconColors.PowerOff, fontWeight = FontWeight.SemiBold) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = AirconColors.TextPrimary) }
        }
    )
}
