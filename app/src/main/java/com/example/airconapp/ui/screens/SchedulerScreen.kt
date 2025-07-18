package com.example.airconapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.airconapp.data.db.SchedulerProfile
import com.example.airconapp.presentation.SchedulerViewModel
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import com.example.airconapp.data.model.Zone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SchedulerScreen(
    viewModel: SchedulerViewModel,
    availableZones: List<Zone>,
    onNavigateToAddEditSchedule: () -> Unit,
    onNavigateToEditSchedule: (SchedulerProfile) -> Unit,
    onNavigateBack: () -> Unit
) {
    val allProfiles by viewModel.allProfiles.collectAsState(initial = emptyList())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Schedules") },
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
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onNavigateToAddEditSchedule) {
                Icon(Icons.Filled.Add, "Add new schedule")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            if (allProfiles.isEmpty()) {
                Text(text = "No schedules yet. Click + to add one.", style = MaterialTheme.typography.bodyLarge)
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(allProfiles) { profile ->
                        ScheduleItem(
                            profile = profile,
                            availableZones = availableZones,
                            onEdit = { onNavigateToEditSchedule(profile) },
                            onDelete = { viewModel.delete(profile) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ScheduleItem(
    profile: SchedulerProfile,
    availableZones: List<Zone>,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val modeNames = mapOf(
        "0" to "Fan",
        "1" to "Heat",
        "2" to "Cool",
        "3" to "Auto",
        "7" to "Dry"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${profile.startTime} ${profile.endTime?.let { "- $it" } ?: ""}",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Row {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit Schedule")
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete Schedule")
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text("Mode: ${modeNames[profile.controlInfo.mode] ?: profile.controlInfo.mode}", fontSize = 16.sp)
            Text("Temp: ${profile.controlInfo.stemp}°C", fontSize = 16.sp)
            Text("Fan: ${profile.controlInfo.f_rate}", fontSize = 16.sp)
            val activeZoneNames = profile.zones
                .filter { it.isOn }
                .mapNotNull { savedZone -> 
                    availableZones.find { it.name == savedZone.name }?.name ?: savedZone.name
                }
                .joinToString(", ")
            Text("Zones: $activeZoneNames", fontSize = 16.sp)
        }
    }
}
