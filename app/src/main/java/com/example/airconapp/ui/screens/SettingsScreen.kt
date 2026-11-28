package com.example.airconapp.ui.screens

import com.example.airconapp.domain.TemperatureUnit
import com.example.airconapp.domain.displayTemp
import com.example.airconapp.domain.label
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
import androidx.compose.material3.TextButton
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.material3.OutlinedButton
import androidx.compose.ui.platform.LocalContext
import com.example.airconapp.domain.SimpleModeLogic
import com.example.airconapp.domain.ThemeMode
import androidx.activity.compose.BackHandler
import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import com.example.airconapp.presentation.SettingsViewModel
import com.example.airconapp.wallpanel.WallPanel
import com.example.airconapp.alerts.AlertNotifier
import com.example.airconapp.domain.AlertMonitor
import com.example.airconapp.domain.AlertSettings
import android.Manifest
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: SettingsViewModel, onNavigateBack: () -> Unit, onSettingsSaved: () -> Unit) {
    val themeMode by viewModel.themeMode.collectAsState()
    val temperatureUnit by viewModel.temperatureUnit.collectAsState()
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
    val remoteEnrolled by viewModel.remoteEnrolled.collectAsState()
    val remoteStatus by viewModel.remoteStatus.collectAsState()
    val remoteBusy by viewModel.remoteBusy.collectAsState()
    val manualUsername by viewModel.manualUsername.collectAsState()
    val manualPassword by viewModel.manualPassword.collectAsState()

    var showUnsavedChanges by rememberSaveable { mutableStateOf(false) }
    val requestBack = {
        if (viewModel.hasUnsavedChanges) showUnsavedChanges = true else onNavigateBack()
    }
    BackHandler(onBack = requestBack)

    if (showUnsavedChanges) {
        AlertDialog(
            onDismissRequest = { showUnsavedChanges = false },
            title = { Text("Save changes?") },
            text = {
                Text(if (viewModel.canSaveSettings) "You have unsaved settings changes. Save them before leaving?"
                    else "You have unsaved settings changes. Choose a zone before saving Simple mode, or discard your changes.")
            },
            confirmButton = {
                TextButton(
                    enabled = viewModel.canSaveSettings,
                    onClick = {
                        viewModel.saveSettings()
                        showUnsavedChanges = false
                        onSettingsSaved()
                    }
                ) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = {
                    viewModel.discardChanges()
                    showUnsavedChanges = false
                    onNavigateBack()
                }) { Text("Discard") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = requestBack) {
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
            ThemeChooser(themeMode, viewModel::setThemeMode)
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Temperature unit", style = MaterialTheme.typography.titleMedium)
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    TemperatureUnit.entries.forEachIndexed { index, unit ->
                        SegmentedButton(
                            selected = temperatureUnit == unit,
                            onClick = { viewModel.setTemperatureUnit(unit) },
                            shape = SegmentedButtonDefaults.itemShape(index, TemperatureUnit.entries.size)
                        ) { Text(label(unit)) }
                    }
                }
            }

            HorizontalDivider()

            val alertSettings by viewModel.alertSettings.collectAsState()
            NotificationsSection(
                settings = alertSettings,
                onOverrunChange = viewModel::setOverrunAlert,
                onPowerChange = viewModel::setPowerChangeAlert,
                onModeChange = viewModel::setModeChangeAlert
            )

            HorizontalDivider()

            // Remote access: enrol while on the home network, then control from anywhere.
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Remote access", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Control the aircon when you're away from home, through Daikin's cloud. " +
                        "Set this up while connected to your home Wi-Fi.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedButton(
                    onClick = { viewModel.enableRemoteAccess() },
                    enabled = !remoteBusy,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (remoteEnrolled) "Refresh remote access" else "Enable remote access")
                }
                if (remoteEnrolled) {
                    OutlinedButton(
                        onClick = { viewModel.clearRemoteAccess() },
                        enabled = !remoteBusy,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Turn off remote access", color = MaterialTheme.colorScheme.error)
                    }
                }
                remoteStatus?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, modifier = Modifier.fillMaxWidth())
                }

                // Advanced: enter a credential by hand (for setup without reaching the adapter).
                // Visibility lives in the ViewModel so it collapses itself on a successful save.
                val showManual by viewModel.manualEntryVisible.collectAsState()
                TextButton(
                    onClick = { viewModel.toggleManualEntry() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (showManual) "Hide manual entry" else "Enter credentials manually")
                }
                if (showManual) {
                    Text(
                        "Advanced: enter the username and password for your Daikin remote account to " +
                            "set up remote access without being on the home network.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = manualUsername,
                        onValueChange = { viewModel.setManualUsername(it) },
                        label = { Text("Username") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = manualPassword,
                        onValueChange = { viewModel.setManualPassword(it) },
                        label = { Text("Password") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedButton(
                        onClick = { viewModel.saveManualRemoteCredential() },
                        enabled = !remoteBusy && manualUsername.isNotBlank() && manualPassword.isNotBlank(),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Save and enable")
                    }
                }
            }

            HorizontalDivider()

            // Local network: the adapter's address, discovery and mock mode.
            // Address field full width, with Find underneath, so neither is squeezed on
            // narrow screens or with large font settings
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = apiIp,
                    onValueChange = { viewModel.setApiIp(it) },
                    label = { Text("Aircon address") },
                    placeholder = { Text("http://192.168.1.6") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedButton(onClick = { viewModel.findAircon() }, modifier = Modifier.fillMaxWidth()) {
                    Text("Find aircon on network")
                }
                discoveryStatus?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, modifier = Modifier.fillMaxWidth())
                }
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
                        "${displayTemp(startTemperature, temperatureUnit)}${label(temperatureUnit)}",
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
                enabled = viewModel.canSaveSettings,
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
/**
 * Background alerts, like the Daikin app's notification settings. Switching one on asks for
 * notification permission (Android 13+) if the app doesn't have it yet.
 */
@Composable
private fun NotificationsSection(
    settings: AlertSettings,
    onOverrunChange: (Boolean) -> Unit,
    onPowerChange: (Boolean) -> Unit,
    onModeChange: (Boolean) -> Unit
) {
    val context = LocalContext.current
    var canNotify by remember { mutableStateOf(AlertNotifier.canNotify(context)) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        canNotify = AlertNotifier.canNotify(context)
    }
    val enabling: ((Boolean) -> Unit) -> (Boolean) -> Unit = { set ->
        { enabled ->
            set(enabled)
            canNotify = AlertNotifier.canNotify(context)
            if (enabled && !canNotify && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Notifications", style = MaterialTheme.typography.titleMedium)
        AlertSwitchRow(
            title = "Overrun warning",
            description = "Get a notification if the aircon has been running for over " +
                "${AlertMonitor.OVERRUN_HOURS} hours.",
            checked = settings.overrun,
            onCheckedChange = enabling(onOverrunChange)
        )
        AlertSwitchRow(
            title = "On/off monitoring",
            description = "Get a notification if someone turns the aircon on or off.",
            checked = settings.powerChange,
            onCheckedChange = enabling(onPowerChange)
        )
        AlertSwitchRow(
            title = "Mode-change monitoring",
            description = "Get a notification if someone changes the operation mode.",
            checked = settings.modeChange,
            onCheckedChange = enabling(onModeChange)
        )
        Text(
            "The aircon is checked about every 15 minutes while the phone is online, so alerts can " +
                "arrive a little late. Changes made from this app don't trigger alerts.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (settings.any && !canNotify) {
            Text(
                "Notifications are blocked for this app, so alerts can't be shown.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
            OutlinedButton(
                onClick = {
                    context.startActivity(
                        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    )
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Allow notifications")
            }
        }
    }
}

@Composable
private fun AlertSwitchRow(title: String, description: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(
                description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

/** Theme is a draft until Save, consistent with the other settings. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ThemeChooser(selectedMode: ThemeMode, onSelect: (ThemeMode) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Theme", style = MaterialTheme.typography.titleMedium)
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            ThemeMode.entries.forEachIndexed { index, mode ->
                SegmentedButton(
                    selected = selectedMode == mode,
                    onClick = { onSelect(mode) },
                    shape = SegmentedButtonDefaults.itemShape(index, ThemeMode.entries.size),
                    icon = {}
                ) {
                    Text(mode.label, maxLines = 1)
                }
            }
        }
        Text(
            "System follows your phone's dark theme setting.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
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
