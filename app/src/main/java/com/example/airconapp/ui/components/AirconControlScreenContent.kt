package com.example.airconapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.airconapp.data.ControlInfo
import com.example.airconapp.data.ZoneStatus
import com.example.airconapp.ui.theme.AirconAppTheme

@Composable
fun AirconControlScreenContent(
    controlInfo: ControlInfo?,
    zoneStatus: ZoneStatus?,
    isLoading: Boolean,
    error: String?,
    onPowerToggle: (String) -> Unit,
    onSetTemperature: (String) -> Unit,
    onSetMode: (String) -> Unit,
    onSetFanRate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (isLoading) {
            Text(text = "Loading aircon status...")
        } else if (error != null) {
            Text(text = "Error: $error", color = MaterialTheme.colorScheme.error)
            Button(onClick = { /* TODO: Implement retry logic */ }) {
                Text("Retry")
            }
        } else if (controlInfo != null && zoneStatus != null) {
            val isPoweredOn = controlInfo.pow == "1"
            FullAirconControlUI(
                controlInfo = controlInfo,
                zoneStatus = zoneStatus,
                isPoweredOn = isPoweredOn,
                onPowerToggle = onPowerToggle,
                onSetTemperature = onSetTemperature,
                onSetMode = onSetMode,
                onSetFanRate = onSetFanRate
            )
        } else {
            Text(text = "No aircon data available. Please check connection.")
        }
    }
}

@Composable
fun FullAirconControlUI(
    controlInfo: ControlInfo,
    zoneStatus: ZoneStatus,
    isPoweredOn: Boolean,
    onPowerToggle: (String) -> Unit,
    onSetTemperature: (String) -> Unit,
    onSetMode: (String) -> Unit,
    onSetFanRate: (String) -> Unit
) {
    val alpha = if (isPoweredOn) 1f else 0.5f // Dim controls when off

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxSize().alpha(alpha)
    ) {
        Spacer(modifier = Modifier.height(32.dp))

        // Temperature Display and Controls
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Default.KeyboardArrowUp,
                contentDescription = "Increase Temperature",
                modifier = Modifier
                    .size(48.dp)
                    .clickable {
                        val currentTemp = controlInfo.stemp.toIntOrNull() ?: 20
                        onSetTemperature((currentTemp + 1).toString())
                    }
            )
            Text(
                text = "${controlInfo.stemp}°C",
                fontSize = 96.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = "Decrease Temperature",
                modifier = Modifier
                    .size(48.dp)
                    .clickable {
                        val currentTemp = controlInfo.stemp.toIntOrNull() ?: 20
                        onSetTemperature((currentTemp - 1).toString())
                    }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Mode and Fan Speed Controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ModeSelector(currentMode = controlInfo.mode, onSetMode = onSetMode)
            FanRateSelector(currentFanRate = controlInfo.f_rate, onSetFanRate = onSetFanRate)
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Zones Display
        Text("Zones", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.align(Alignment.Start))
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
            val zoneNames = zoneStatus.zone_name.split(";")
            val zoneOnOff = zoneStatus.zone_onoff.split(";")
            items(zoneNames.indices.toList()) {
                val name = zoneNames[it]
                val status = zoneOnOff[it]
                ZoneItem(name = name, status = status)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Power Button (bottom center)
        PowerButton(isPoweredOn = isPoweredOn, onPowerToggle = onPowerToggle)
    }
}

@Composable
fun PowerButton(isPoweredOn: Boolean, onPowerToggle: (String) -> Unit) {
    val buttonColor = if (isPoweredOn) Color.Red else Color.Green
    val contentColor = Color.White

    Button(
        onClick = { onPowerToggle(if (isPoweredOn) "0" else "1") },
        modifier = Modifier
            .size(80.dp) // Make it a prominent circle
            .clip(CircleShape)
            .background(buttonColor),
        colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = buttonColor, contentColor = contentColor)
    ) {
        Icon(
            imageVector = Icons.Default.PowerSettingsNew,
            contentDescription = if (isPoweredOn) "Turn Off" else "Turn On",
            modifier = Modifier.size(48.dp)
        )
    }
}

@Composable
fun ModeSelector(currentMode: String, onSetMode: (String) -> Unit) {
    val modes = mapOf(
        "0" to "Auto",
        "1" to "Dry",
        "2" to "Cool",
        "3" to "Heat",
        "4" to "Fan"
    )
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Mode", style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            modes.forEach { (key, value) ->
                Text(
                    text = value,
                    color = if (currentMode == key) MaterialTheme.colorScheme.primary else Color.Gray,
                    fontWeight = if (currentMode == key) FontWeight.Bold else FontWeight.Normal,
                    modifier = Modifier.clickable { onSetMode(key) }
                )
            }
        }
    }
}

@Composable
fun FanRateSelector(currentFanRate: String, onSetFanRate: (String) -> Unit) {
    val fanRates = listOf("A", "1", "2", "3", "4", "5") // A for Auto
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Fan", style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            fanRates.forEach { rate ->
                Text(
                    text = rate,
                    color = if (currentFanRate == rate) MaterialTheme.colorScheme.primary else Color.Gray,
                    fontWeight = if (currentFanRate == rate) FontWeight.Bold else FontWeight.Normal,
                    modifier = Modifier.clickable { onSetFanRate(rate) }
                )
            }
        }
    }
}

@Composable
fun ZoneItem(name: String, status: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(4.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = name, fontSize = 18.sp, fontWeight = FontWeight.Medium)
        Text(
            text = if (status == "1") "ON" else "OFF",
            color = if (status == "1") Color.Green else Color.Red,
            fontWeight = FontWeight.Bold
        )
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 640)
@Composable
fun PreviewFullAirconControlUI() {
    AirconAppTheme {
        FullAirconControlUI(
            controlInfo = ControlInfo(pow = "1", mode = "2", stemp = "24", f_rate = "5", f_dir = "0"),
            zoneStatus = ZoneStatus(zone_onoff = "1;0;1;0;0;0;0;0", zone_name = "Living Room;Bedroom;Kitchen;Office;Hall;Dining;Guest;Master"),
            isPoweredOn = true,
            onPowerToggle = {},
            onSetTemperature = {},
            onSetMode = {},
            onSetFanRate = {}
        )
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 640)
@Composable
fun PreviewFullAirconControlUIOff() {
    AirconAppTheme {
        FullAirconControlUI(
            controlInfo = ControlInfo(pow = "0", mode = "2", stemp = "24", f_rate = "5", f_dir = "0"),
            zoneStatus = ZoneStatus(zone_onoff = "0;0;0;0;0;0;0;0", zone_name = "Living Room;Bedroom;Kitchen;Office;Hall;Dining;Guest;Master"),
            isPoweredOn = false,
            onPowerToggle = {},
            onSetTemperature = {},
            onSetMode = {},
            onSetFanRate = {}
        )
    }
}