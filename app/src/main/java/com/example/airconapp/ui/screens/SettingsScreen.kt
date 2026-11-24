package com.example.airconapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
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

            // Only offered once the app was made device owner (full kiosk) via adb
            var isDeviceOwner by remember { mutableStateOf(WallPanel.isDeviceOwner(context)) }
            if (isDeviceOwner) {
                OutlinedButton(
                    onClick = {
                        (context as? Activity)?.let { WallPanel.releaseDeviceOwner(it) }
                        isDeviceOwner = WallPanel.isDeviceOwner(context)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Release kiosk (device owner)", color = MaterialTheme.colorScheme.error)
                }
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