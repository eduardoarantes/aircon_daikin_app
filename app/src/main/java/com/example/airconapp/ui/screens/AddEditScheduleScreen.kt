package com.example.airconapp.ui.screens

import android.app.TimePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Icon
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.* // Import all runtime components
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.airconapp.data.ControlInfo
import com.example.airconapp.data.Zone
import com.example.airconapp.data.db.SchedulerProfile
import com.example.airconapp.presentation.SchedulerViewModel
import com.example.airconapp.ui.components.FanRateSelector
import com.example.airconapp.ui.components.ModeSelector
import com.example.airconapp.ui.components.ZoneItem
import java.util.Calendar

@Composable
fun AddEditScheduleScreen(viewModel: SchedulerViewModel, schedule: SchedulerProfile?, availableZones: List<Zone>, onNavigateBack: () -> Unit) {
    val context = LocalContext.current

    var startTime by remember { mutableStateOf("") }
    var endTime by remember { mutableStateOf("") }
    var selectedMode by remember { mutableStateOf("2") } // Default to Cool
    var selectedTemperature by remember { mutableStateOf("23") } // Default temp
    var selectedFanRate by remember { mutableStateOf("A") } // Default to Auto
    var selectedZones by remember { mutableStateOf(listOf<Zone>()) }
    var isActive by remember { mutableStateOf(true) } // New state for isActive

    var isStartTimeError by remember { mutableStateOf(false) }
    var isZoneSelectionError by remember { mutableStateOf(false) }

    // Populate fields if editing an existing schedule
    LaunchedEffect(schedule, availableZones) {
        schedule?.let {
            startTime = it.startTime
            endTime = it.endTime ?: ""
            selectedMode = it.controlInfo.mode
            selectedTemperature = it.controlInfo.stemp
            selectedFanRate = it.controlInfo.f_rate
            selectedZones = it.zones
            isActive = it.isActive // Initialize isActive from existing schedule
        } ?: run { // If schedule is null (new schedule), initialize zones from availableZones
            selectedZones = availableZones.map { it.copy(isOn = false) } // All off by default for new schedule
            isActive = true // Default to active for new schedules
        }
    }

    val timePickerDialog = TimePickerDialog(
        context,
        { _, hourOfDay, minute ->
            val selectedTime = String.format("%02d:%02d", hourOfDay, minute)
            startTime = selectedTime
            isStartTimeError = selectedTime.isEmpty()
        },
        Calendar.getInstance().get(Calendar.HOUR_OF_DAY),
        Calendar.getInstance().get(Calendar.MINUTE),
        true // 24 hour format
    )

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("Main Configuration", "Zone Selection")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(text = if (schedule == null) "Add New Schedule" else "Edit Schedule", style = MaterialTheme.typography.headlineMedium)

        TabRow(selectedTabIndex = selectedTabIndex) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTabIndex == index,
                    onClick = { selectedTabIndex = index },
                    text = { Text(title) }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Column(
            modifier = Modifier.weight(1f) // Allow content to take available space
        ) {
            when (selectedTabIndex) {
                0 -> MainConfigurationTab(
                    startTime = startTime,
                    onStartTimeChange = { newValue ->
                        startTime = newValue
                        isStartTimeError = newValue.isEmpty()
                    },
                    isStartTimeError = isStartTimeError,
                    timePickerDialog = timePickerDialog,
                    endTime = endTime,
                    onEndTimeChange = { newValue -> endTime = newValue },
                    selectedMode = selectedMode,
                    onSetMode = { newMode -> selectedMode = newMode },
                    selectedFanRate = selectedFanRate,
                    onSetFanRate = { newFanRate -> selectedFanRate = newFanRate },
                    selectedTemperature = selectedTemperature,
                    onSetTemperature = { newTemp -> selectedTemperature = newTemp },
                    isActive = isActive,
                    onIsActiveChange = { newIsActive -> isActive = newIsActive }
                )
                1 -> ZoneSelectionTab(
                    selectedZones = selectedZones,
                    onZoneToggle = { index, isOn ->
                        selectedZones = selectedZones.toMutableList().apply {
                            this[index] = this[index].copy(isOn = isOn)
                        }
                        isZoneSelectionError = selectedZones.none { it.isOn }
                    },
                    isZoneSelectionError = isZoneSelectionError
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            Button(onClick = onNavigateBack) {
                Text("Cancel")
            }
            Button(
                onClick = {
                    isStartTimeError = startTime.isEmpty()
                    isZoneSelectionError = selectedZones.none { it.isOn }

                    if (!isStartTimeError && !isZoneSelectionError) {
                        val profileToSave = schedule?.copy(
                            startTime = startTime,
                            endTime = endTime.ifEmpty { null },
                            controlInfo = ControlInfo(
                                pow = "1", // Assuming schedules always turn AC on
                                mode = selectedMode,
                                stemp = selectedTemperature,
                                f_rate = selectedFanRate,
                                f_dir = "0" // Default fan direction
                            ),
                            zones = selectedZones,
                            isActive = isActive
                        ) ?: SchedulerProfile(
                            startTime = startTime,
                            endTime = endTime.ifEmpty { null },
                            controlInfo = ControlInfo(
                                pow = "1", // Assuming schedules always turn AC on
                                mode = selectedMode,
                                stemp = selectedTemperature,
                                f_rate = selectedFanRate,
                                f_dir = "0" // Default fan direction
                            ),
                            zones = selectedZones,
                            isActive = isActive
                        )

                        if (schedule == null) {
                            viewModel.insert(profileToSave)
                        } else {
                            viewModel.update(profileToSave)
                        }
                        onNavigateBack()
                    }
                },
                enabled = !isStartTimeError && !isZoneSelectionError // Disable button if there's any error
            ) {
                Text(if (schedule == null) "Save Schedule" else "Update Schedule")
            }
        }
    }
}

@Composable
fun MainConfigurationTab(
    startTime: String,
    onStartTimeChange: (String) -> Unit,
    isStartTimeError: Boolean,
    timePickerDialog: TimePickerDialog,
    endTime: String,
    onEndTimeChange: (String) -> Unit,
    selectedMode: String,
    onSetMode: (String) -> Unit,
    selectedFanRate: String,
    onSetFanRate: (String) -> Unit,
    selectedTemperature: String,
    onSetTemperature: (String) -> Unit,
    isActive: Boolean,
    onIsActiveChange: (Boolean) -> Unit
) {
    val context = LocalContext.current
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        OutlinedTextField(
            value = startTime,
            onValueChange = onStartTimeChange,
            label = { Text("Start Time (HH:MM)*") },
            readOnly = true,
            modifier = Modifier.fillMaxWidth(),
            isError = isStartTimeError,
            supportingText = if (isStartTimeError) { { Text("Start time is required") } } else null,
            trailingIcon = {
                Icon(Icons.Default.Settings, contentDescription = "Pick Time",
                    modifier = Modifier.clickable { timePickerDialog.show() })
            }
        )

        OutlinedTextField(
            value = endTime,
            onValueChange = onEndTimeChange,
            label = { Text("End Time (Optional HH:MM)") },
            readOnly = true,
            modifier = Modifier.fillMaxWidth(),
            trailingIcon = {
                Icon(Icons.Default.Settings, contentDescription = "Pick Time",
                    modifier = Modifier.clickable {
                        val endTimePicker = TimePickerDialog(
                            context,
                            { _, hourOfDay, minute ->
                                onEndTimeChange(String.format("%02d:%02d", hourOfDay, minute))
                            },
                            Calendar.getInstance().get(Calendar.HOUR_OF_DAY),
                            Calendar.getInstance().get(Calendar.MINUTE),
                            true
                        )
                        endTimePicker.show()
                    })
            }
        )

        // Mode and Fan Speed Controls
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ModeSelector(currentMode = selectedMode, onSetMode = onSetMode)
            FanRateSelector(currentFanRate = selectedFanRate, onSetFanRate = onSetFanRate)
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Temperature Slider
        Text("Temperature: ${selectedTemperature}°C", style = MaterialTheme.typography.titleMedium)
        Slider(
            value = selectedTemperature.toFloatOrNull() ?: 20f,
            onValueChange = { newValue -> onSetTemperature(newValue.toInt().toString()) },
            valueRange = 16f..30f,
            steps = 13,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Active", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.weight(1f))
            Switch(checked = isActive, onCheckedChange = onIsActiveChange)
        }
    }
}

@Composable
fun ZoneSelectionTab(
    selectedZones: List<Zone>,
    onZoneToggle: (Int, Boolean) -> Unit,
    isZoneSelectionError: Boolean
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Select Zones", style = MaterialTheme.typography.headlineSmall)
        if (isZoneSelectionError) {
            Text("At least one zone must be selected", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f) // Take remaining space
                .background(Color.LightGray.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                .clip(RoundedCornerShape(8.dp))
                .border(1.dp, Color.Gray, RoundedCornerShape(8.dp)),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            contentPadding = PaddingValues(8.dp)
        ) {
            itemsIndexed(selectedZones) { index, zone ->
                ZoneItem(zone = zone, onToggle = { isOn -> onZoneToggle(index, isOn) })
            }
        }
    }
}
