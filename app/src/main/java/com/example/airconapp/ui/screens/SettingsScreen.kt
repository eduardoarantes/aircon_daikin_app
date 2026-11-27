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
import android.os.SystemClock
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Router
import androidx.compose.material.icons.outlined.TabletAndroid
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material3.ListItem
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import com.example.airconapp.data.AdapterInfo
import com.example.airconapp.di.NetworkModule
import kotlinx.coroutines.delay
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/** The themed pages under Settings. [route] is appended to `settings/`. */
enum class SettingsPage(val route: String, val title: String) {
    APPEARANCE("appearance", "Appearance"),
    CONNECTION("connection", "Aircon connection"),
    REMOTE("remote", "Remote access"),
    SIMPLE_MODE("simple", "Simple mode & wall panel"),
    ADAPTER("adapter", "Adapter");

    companion object {
        fun fromRoute(route: String?): SettingsPage? = entries.firstOrNull { it.route == route }
    }
}

/**
 * Settings home: one row per theme, each opening its own page. Edits on the pages stay a draft
 * (shared through [viewModel]) until saved; leaving Settings with unsaved edits asks first.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateBack: () -> Unit,
    onOpenPage: (SettingsPage) -> Unit,
    onSettingsSaved: () -> Unit
) {
    val themeMode by viewModel.themeMode.collectAsState()
    val temperatureUnit by viewModel.temperatureUnit.collectAsState()
    val apiIp by viewModel.apiIp.collectAsState()
    val useMockMode by viewModel.useMockMode.collectAsState()
    val remoteEnrolled by viewModel.remoteEnrolled.collectAsState()
    val simpleModeEnabled by viewModel.simpleModeEnabled.collectAsState()
    val wallPanelEnabled by viewModel.wallPanelEnabled.collectAsState()

    var showUnsavedChanges by rememberSaveable { mutableStateOf(false) }
    val requestBack = {
        if (viewModel.hasUnsavedChanges) showUnsavedChanges = true else onNavigateBack()
    }
    BackHandler(onBack = requestBack)

    if (showUnsavedChanges) {
        UnsavedChangesDialog(
            viewModel = viewModel,
            onDismiss = { showUnsavedChanges = false },
            onSaved = { showUnsavedChanges = false; onSettingsSaved() },
            onDiscarded = { showUnsavedChanges = false; onNavigateBack() }
        )
    }

    SettingsScaffold(title = "Settings", onBack = requestBack) {
        Column(modifier = Modifier.fillMaxWidth()) {
            SettingsNavRow(
                icon = Icons.Outlined.Palette,
                title = SettingsPage.APPEARANCE.title,
                summary = "${themeMode.label} theme · ${label(temperatureUnit)}",
                onClick = { onOpenPage(SettingsPage.APPEARANCE) }
            )
            SettingsNavRow(
                icon = Icons.Outlined.Wifi,
                title = SettingsPage.CONNECTION.title,
                summary = if (useMockMode) "Mock mode" else hostOf(apiIp),
                onClick = { onOpenPage(SettingsPage.CONNECTION) }
            )
            SettingsNavRow(
                icon = Icons.Outlined.Cloud,
                title = SettingsPage.REMOTE.title,
                summary = if (remoteEnrolled) "On" else "Off",
                onClick = { onOpenPage(SettingsPage.REMOTE) }
            )
            SettingsNavRow(
                icon = Icons.Outlined.TabletAndroid,
                title = SettingsPage.SIMPLE_MODE.title,
                summary = when {
                    !simpleModeEnabled -> "Off"
                    wallPanelEnabled -> "On · wall panel"
                    else -> "On"
                },
                onClick = { onOpenPage(SettingsPage.SIMPLE_MODE) }
            )
            SettingsNavRow(
                icon = Icons.Outlined.Router,
                title = SettingsPage.ADAPTER.title,
                summary = "Time, firmware and connection test",
                onClick = { onOpenPage(SettingsPage.ADAPTER) }
            )
        }
    }
}

/**
 * One themed settings page. Pages with draft settings end with Save; Back returns to the
 * Settings home and keeps the draft (the home asks before leaving with unsaved changes).
 */
@Composable
fun SettingsPageScreen(
    viewModel: SettingsViewModel,
    page: SettingsPage,
    onBack: () -> Unit,
    onSettingsSaved: () -> Unit
) {
    BackHandler(onBack = onBack)
    SettingsScaffold(title = page.title, onBack = onBack) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            when (page) {
                SettingsPage.APPEARANCE -> AppearanceContent(viewModel)
                SettingsPage.CONNECTION -> ConnectionContent(viewModel)
                SettingsPage.REMOTE -> RemoteAccessContent(viewModel)
                SettingsPage.SIMPLE_MODE -> SimpleModeContent(viewModel)
                SettingsPage.ADAPTER -> AdapterContent(viewModel)
            }
            if (page.hasDraftSettings) {
                Spacer(modifier = Modifier.height(8.dp))
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
}

/** Remote access and the adapter page act immediately or only read; the rest are drafts until Save. */
private val SettingsPage.hasDraftSettings: Boolean
    get() = this == SettingsPage.APPEARANCE || this == SettingsPage.CONNECTION || this == SettingsPage.SIMPLE_MODE

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsScaffold(title: String, onBack: () -> Unit, content: @Composable () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            content()
        }
    }
}

@Composable
private fun SettingsNavRow(icon: ImageVector, title: String, summary: String, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(summary, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        leadingContent = { Icon(icon, contentDescription = null) },
        trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null) },
        modifier = Modifier.clickable(onClick = onClick)
    )
    HorizontalDivider()
}

@Composable
private fun UnsavedChangesDialog(
    viewModel: SettingsViewModel,
    onDismiss: () -> Unit,
    onSaved: () -> Unit,
    onDiscarded: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
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
                    onSaved()
                }
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = {
                viewModel.discardChanges()
                onDiscarded()
            }) { Text("Discard") }
        }
    )
}

/** `http://192.168.1.10` → `192.168.1.10`. */
private fun hostOf(url: String): String =
    url.trim().removePrefix("http://").removePrefix("https://").trimEnd('/').ifBlank { "Not set" }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppearanceContent(viewModel: SettingsViewModel) {
    val themeMode by viewModel.themeMode.collectAsState()
    val temperatureUnit by viewModel.temperatureUnit.collectAsState()
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
}

@Composable
private fun ConnectionContent(viewModel: SettingsViewModel) {
    val apiIp by viewModel.apiIp.collectAsState()
    val useMockMode by viewModel.useMockMode.collectAsState()
    val discoveryStatus by viewModel.discoveryStatus.collectAsState()

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
}

/** Remote access: enrol while on the home network, then control from anywhere. Acts immediately. */
@Composable
private fun RemoteAccessContent(viewModel: SettingsViewModel) {
    val remoteEnrolled by viewModel.remoteEnrolled.collectAsState()
    val remoteStatus by viewModel.remoteStatus.collectAsState()
    val remoteBusy by viewModel.remoteBusy.collectAsState()
    val manualUsername by viewModel.manualUsername.collectAsState()
    val manualPassword by viewModel.manualPassword.collectAsState()

    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
}

@Composable
private fun SimpleModeContent(viewModel: SettingsViewModel) {
    val temperatureUnit by viewModel.temperatureUnit.collectAsState()
    val simpleModeEnabled by viewModel.simpleModeEnabled.collectAsState()
    val wallPanelEnabled by viewModel.wallPanelEnabled.collectAsState()
    val simpleModeZone by viewModel.simpleModeZone.collectAsState()
    val availableZones by viewModel.availableZones.collectAsState()
    val zonesLoading by viewModel.zonesLoading.collectAsState()
    val startTemperature by viewModel.simpleModeStartTemperature.collectAsState()
    val context = LocalContext.current

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

        HorizontalDivider()

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
}

/**
 * The adapter's own settings, read-only: what the official Daikin app shows under
 * "Adapter settings". Changing them still needs the Daikin app.
 */
@Composable
private fun AdapterContent(viewModel: SettingsViewModel) {
    val state by viewModel.adapterState.collectAsState()
    val connectionTest by viewModel.connectionTest.collectAsState()
    val connectionTesting by viewModel.connectionTesting.collectAsState()
    LaunchedEffect(Unit) { viewModel.loadAdapterInfo() }

    when (val current = state) {
        SettingsViewModel.AdapterState.Loading -> Text("Reading the adapter…", modifier = Modifier.fillMaxWidth())
        SettingsViewModel.AdapterState.Unreachable -> Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                "Couldn't reach the adapter. Its details can only be read on your home Wi-Fi.",
                color = MaterialTheme.colorScheme.error
            )
            OutlinedButton(onClick = { viewModel.loadAdapterInfo() }, modifier = Modifier.fillMaxWidth()) {
                Text("Try again")
            }
        }
        is SettingsViewModel.AdapterState.Loaded -> AdapterDetails(current.info)
    }

    AdapterSection("Connection test") {
        Text(
            "Checks the aircon can be reached on your home Wi-Fi and through Daikin's cloud. Nothing on the unit changes.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        OutlinedButton(
            onClick = { viewModel.runConnectionTest() },
            enabled = !connectionTesting,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (connectionTesting) "Testing…" else "Run connection test")
        }
        connectionTest?.let { result ->
            AdapterValueRow("Home network", if (result.local) "Connected" else "Not reachable")
            AdapterValueRow(
                "Remote (Daikin cloud)",
                when (result.remote) {
                    null -> "Not set up"
                    true -> "Connected"
                    false -> "Not reachable"
                }
            )
        }
    }
}

@Composable
private fun AdapterDetails(info: AdapterInfo) {
    // The adapter reports its clock once; keep it ticking from when it was read
    val readAt = remember(info) { SystemClock.elapsedRealtime() }
    var now by remember { mutableLongStateOf(SystemClock.elapsedRealtime()) }
    LaunchedEffect(info) {
        while (true) {
            now = SystemClock.elapsedRealtime()
            delay(1_000)
        }
    }
    val clock = info.dateTime?.plusSeconds((now - readAt) / 1_000)
    val onOff = { value: Boolean? -> when (value) { true -> "On"; false -> "Off"; null -> "—" } }

    AdapterSection("Network") {
        AdapterValueRow("\"Out-of-Home\" (not at home)", onOff(info.outOfHome))
        Text(
            "Lets the adapter use Daikin's cloud, which remote access needs. Change it in the Daikin app.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
    AdapterSection("Time") {
        AdapterValueRow(
            "Adapter date and time",
            clock?.format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)) ?: "—"
        )
        AdapterValueRow("Time zone", info.timeZone ?: "—")
        AdapterValueRow("Automatic DST", onOff(info.automaticDst))
        AdapterValueRow("Disable time sync with internet", onOff(info.timeSyncDisabled))
    }
    AdapterSection("Information") {
        AdapterValueRow("Firmware version", info.firmware ?: "—")
        AdapterValueRow("IP address", hostOf(NetworkModule.getBaseUrl()))
        AdapterValueRow("MAC address", info.macAddress ?: "—")
    }
}

@Composable
private fun AdapterSection(title: String, content: @Composable () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        content()
        HorizontalDivider()
    }
}

@Composable
private fun AdapterValueRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
