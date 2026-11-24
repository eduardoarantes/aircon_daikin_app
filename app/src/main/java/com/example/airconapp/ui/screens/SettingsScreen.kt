package com.example.airconapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import android.app.Activity
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.material3.OutlinedButton
import androidx.compose.ui.platform.LocalContext
import com.example.airconapp.domain.SimpleModeLogic
import com.example.airconapp.presentation.SettingsViewModel
import com.example.airconapp.wallpanel.WallPanel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: SettingsViewModel, onNavigateBack: () -> Unit, onSettingsSaved: () -> Unit) {
    val apiIp by viewModel.apiIp.collectAsState()
    val useMockMode by viewModel.useMockMode.collectAsState()
    val simpleModeEnabled by viewModel.simpleModeEnabled.collectAsState()
    val wallPanelEnabled by viewModel.wallPanelEnabled.collectAsState()
    val simpleModeZone by viewModel.simpleModeZone.collectAsState()
    val availableZones by viewModel.availableZones.collectAsState()
    val zonesLoading by viewModel.zonesLoading.collectAsState()
    val startTemperature by viewModel.simpleModeStartTemperature.collectAsState()
    val context = LocalContext.current
    val discoveryStatus by viewModel.discoveryStatus.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = apiIp,
                    onValueChange = { viewModel.setApiIp(it) },
                    label = { Text("Aircon API IP (e.g., http://192.168.1.6)") },
                    modifier = Modifier.weight(1f)
                )
                OutlinedButton(onClick = { viewModel.findAircon() }) { Text("Find") }
            }
            discoveryStatus?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, modifier = Modifier.fillMaxWidth())
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Use Mock Mode", style = MaterialTheme.typography.titleMedium)
                Switch(
                    checked = useMockMode,
                    onCheckedChange = { viewModel.setUseMockMode(it) }
                )
            }

            HorizontalDivider()

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Simple On/Off Mode", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Main screen becomes a single on/off button for one zone",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = simpleModeEnabled,
                    onCheckedChange = { viewModel.setSimpleModeEnabled(it) }
                )
            }

            if (simpleModeEnabled) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Start temperature", style = MaterialTheme.typography.titleSmall)
                        Text(
                            "Used when the button turns the aircon on",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    OutlinedButton(
                        onClick = { viewModel.setSimpleModeStartTemperature(startTemperature - 1) },
                        enabled = startTemperature > SimpleModeLogic.START_TEMPERATURE_RANGE.first,
                        contentPadding = PaddingValues(0.dp),
                        modifier = Modifier.size(48.dp)
                    ) { Text("−", style = MaterialTheme.typography.titleLarge) }
                    Text(
                        "$startTemperature°C",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                    OutlinedButton(
                        onClick = { viewModel.setSimpleModeStartTemperature(startTemperature + 1) },
                        enabled = startTemperature < SimpleModeLogic.START_TEMPERATURE_RANGE.last,
                        contentPadding = PaddingValues(0.dp),
                        modifier = Modifier.size(48.dp)
                    ) { Text("+", style = MaterialTheme.typography.titleLarge) }
                }

                Text("Zone", style = MaterialTheme.typography.titleSmall, modifier = Modifier.fillMaxWidth())
                val zones = availableZones
                when {
                    zonesLoading -> Text("Loading zones…")
                    zones == null -> Text("Could not load zones from the aircon.", color = MaterialTheme.colorScheme.error)
                    else -> Column(modifier = Modifier.fillMaxWidth()) {
                        zones.forEach { zone ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .selectable(
                                        selected = zone == simpleModeZone,
                                        onClick = { viewModel.setSimpleModeZone(zone) },
                                        role = Role.RadioButton
                                    )
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(selected = zone == simpleModeZone, onClick = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(zone)
                            }
                        }
                    }
                }
            }

            if (simpleModeEnabled) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Wall panel features", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Locks the phone in this app, shows it over the lock screen, keeps the screen " +
                                "on and silences the phone. Turn off to use the app normally.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = wallPanelEnabled,
                        onCheckedChange = { viewModel.setWallPanelEnabled(it) }
                    )
                }
            }

            if (simpleModeEnabled && wallPanelEnabled) {
                // Saves first: the app is only offered as a home app once simple mode is saved
                OutlinedButton(
                    onClick = {
                        viewModel.saveSettings()
                        WallPanel.openHomeAppSettings(context)
                    },
                    enabled = simpleModeZone != null,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Set as Home App (wall panel)")
                }

                // Without Do Not Disturb access the panel mutes sounds but can't block everything
                if (!WallPanel.hasSilencePermission(context)) {
                    OutlinedButton(
                        onClick = { WallPanel.openSilencePermissionSettings(context) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Allow silencing the phone")
                    }
                }
            }

            // Full kiosk: how to turn it on (via adb) or, once on, how to release it
            if ((simpleModeEnabled && wallPanelEnabled) || WallPanel.isDeviceOwner(context)) {
                KioskSection()
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                enabled = !simpleModeEnabled || simpleModeZone != null,
                onClick = {
                    viewModel.saveSettings()
                    onSettingsSaved()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save Settings")
            }
        }
    }
}
/** adb command that makes this app the device owner, which turns on the full kiosk. */
private const val DEVICE_OWNER_COMMAND =
    "adb shell dpm set-device-owner com.example.airconapp/.wallpanel.WallPanelAdminReceiver"

/**
 * Explains the full kiosk (device owner). It can only be switched on from a computer with adb,
 * so this shows the steps; once it is on, it offers the only way back out short of a factory reset.
 */
@Composable
private fun KioskSection() {
    val context = LocalContext.current
    var isDeviceOwner by remember { mutableStateOf(WallPanel.isDeviceOwner(context)) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Full kiosk", style = MaterialTheme.typography.titleMedium)
            if (isDeviceOwner) {
                Text(
                    "On. The phone stays locked in this app after every reboot, with no " +
                        "\"Screen is pinned\" prompt. Home, Recents and the notification shade are blocked.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    "While this is on, the app can't be uninstalled. Release it here before " +
                        "uninstalling or giving the phone another use; otherwise only a factory reset removes it.",
                    style = MaterialTheme.typography.bodyMedium
                )
                OutlinedButton(
                    onClick = {
                        (context as? Activity)?.let { WallPanel.releaseDeviceOwner(it) }
                        isDeviceOwner = WallPanel.isDeviceOwner(context)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Release kiosk (device owner)", color = MaterialTheme.colorScheme.error)
                }
            } else {
                Text(
                    "Off. Simple mode uses screen pinning, which asks \"Screen is pinned\" after each " +
                        "reboot and can be exited by holding Back and Recents. The full kiosk removes both.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text("To turn it on:", style = MaterialTheme.typography.bodyMedium)
                Text(
                    "1. Remove every account from the phone (Settings > Accounts). Android refuses otherwise.\n" +
                        "2. Turn on USB debugging (Settings > System > Developer options).\n" +
                        "3. Connect the phone to a computer with adb and run:",
                    style = MaterialTheme.typography.bodyMedium
                )
                SelectionContainer {
                    Text(
                        DEVICE_OWNER_COMMAND,
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace)
                    )
                }
                Text(
                    "4. Restart the phone. It comes back locked in this app, and this section then offers the release button.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}
