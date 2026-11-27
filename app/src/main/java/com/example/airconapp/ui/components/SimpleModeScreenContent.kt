package com.example.airconapp.ui.components

import com.example.airconapp.domain.TemperatureUnit
import com.example.airconapp.domain.displayTemp
import com.example.airconapp.domain.label
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.unit.sp
import com.example.airconapp.data.ControlInfo
import com.example.airconapp.data.SensorInfo
import com.example.airconapp.data.Zone
import com.example.airconapp.domain.SimpleModeLogic
import com.example.airconapp.ui.theme.AirconAppTheme
import com.example.airconapp.ui.theme.SystemBarsOverride
import android.app.Activity
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import kotlinx.coroutines.delay
import java.text.DateFormat
import java.util.Date

private val OnColor = Color(0xFF2E7D32)
private val OffColor = Color(0xFF757575)

@Composable
fun SimpleModeScreenContent(
    controlInfo: ControlInfo?,
    zoneStatus: List<Zone>?,
    sensorInfo: SensorInfo?,
    zoneName: String?,
    startTemperature: Int,
    error: String?,
    onToggle: () -> Unit,
    onNavigateToSettings: () -> Unit,
    isDisconnected: Boolean,
    temperatureUnit: TemperatureUnit = TemperatureUnit.CELSIUS,
    modifier: Modifier = Modifier
) {
    // No "switch to mock" dialog here: nobody at a wall panel should answer it. The periodic
    // refresh keeps retrying (e.g. Wi-Fi still connecting after boot) and this clears itself.
    // Always dark: black background, outlined text, outlined power button
    HiddenStatusBar()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // No visible settings button in this mode: tapping the clock 3 times opens Settings
        Spacer(modifier = Modifier.height(32.dp))
        Clock(onTripleTap = onNavigateToSettings)

        Spacer(modifier = Modifier.weight(1f))

        when {
            zoneName == null -> Text(
                "No zone selected.\nChoose one in Settings.",
                style = darkStyle(MaterialTheme.typography.titleLarge),
                textAlign = TextAlign.Center
            )
            controlInfo == null || zoneStatus == null -> CircularProgressIndicator()
            zoneStatus.none { it.name == zoneName } -> Text(
                "Zone \"$zoneName\" not found on the aircon.\nChoose another in Settings.",
                style = darkStyle(MaterialTheme.typography.titleLarge),
                textAlign = TextAlign.Center
            )
            else -> {
                val isPoweredOn = controlInfo.pow == "1"
                val isOn = SimpleModeLogic.isOn(isPoweredOn, zoneStatus, zoneName)
                ZonePowerButton(zoneName = zoneName, isOn = isOn, onToggle = onToggle)
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Aircon off: show the temperature the button will start it at, not the unit's stale one
            val setTemperature = if (controlInfo?.pow == "0") startTemperature.toString() else controlInfo?.stemp
            TemperatureCard(label = "Set", value = setTemperature, temperatureUnit = temperatureUnit, modifier = Modifier.weight(1f))
            TemperatureCard(label = "Room", value = sensorInfo?.indoorTemp, temperatureUnit = temperatureUnit, modifier = Modifier.weight(1f))
        }

        if (isDisconnected) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "Connecting to aircon…", style = darkStyle(MaterialTheme.typography.bodyMedium))
        } else if (error != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = error, color = MaterialTheme.colorScheme.error)
        }
    }
}

/** Below this size an outline is too thin to read, so small text is drawn solid. */
private val MIN_OUTLINE_FONT_SIZE = 24.sp

/**
 * Large text is outline only in [color]; small text, or any text with [solidAlpha] set,
 * is solid in [color] at that alpha (0.7 by default).
 */
@Composable
private fun darkStyle(style: TextStyle, color: Color = Color.White, solidAlpha: Float? = null): TextStyle {
    val fontSize = if (style.fontSize.isSpecified) style.fontSize else 16.sp
    if (solidAlpha != null || fontSize < MIN_OUTLINE_FONT_SIZE) {
        return style.copy(color = color.copy(alpha = solidAlpha ?: 0.7f))
    }
    // Scale the outline with the font so it stays crisp at every size
    val strokeWidth = with(LocalDensity.current) { maxOf(fontSize.toPx() * 0.025f, 0.6.dp.toPx()) }
    return style.copy(color = color, drawStyle = Stroke(width = strokeWidth))
}

/**
 * Hides the status bar (time, Wi-Fi, battery) while this screen shows; the screen has its own
 * clock. A swipe from the top edge reveals it briefly. Bars are black in case they're revealed.
 */
@Composable
private fun HiddenStatusBar() {
    val view = LocalView.current
    DisposableEffect(Unit) {
        SystemBarsOverride.black = true
        val window = (view.context as? Activity)?.window
        val controller = window?.let { WindowCompat.getInsetsController(it, view) }
        controller?.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller?.hide(WindowInsetsCompat.Type.statusBars())
        onDispose {
            controller?.show(WindowInsetsCompat.Type.statusBars())
            SystemBarsOverride.black = false
        }
    }
}

/** Max gap between taps for them to count towards the triple tap. */
private const val TRIPLE_TAP_WINDOW_MS = 500L

@Composable
private fun Clock(onTripleTap: () -> Unit) {
    var now by remember { mutableStateOf(Date()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = Date()
            delay(1000)
        }
    }
    var tapCount by remember { mutableStateOf(0) }
    var lastTapAt by remember { mutableStateOf(0L) }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.pointerInput(onTripleTap) {
            detectTapGestures {
                val t = System.currentTimeMillis()
                tapCount = if (t - lastTapAt <= TRIPLE_TAP_WINDOW_MS) tapCount + 1 else 1
                lastTapAt = t
                if (tapCount >= 3) {
                    tapCount = 0
                    onTripleTap()
                }
            }
        }
    ) {
        Text(
            text = DateFormat.getTimeInstance(DateFormat.SHORT).format(now),
            style = darkStyle(TextStyle(fontSize = 72.sp, fontWeight = FontWeight.Bold))
        )
        Text(
            text = DateFormat.getDateInstance(DateFormat.FULL).format(now),
            style = darkStyle(MaterialTheme.typography.titleMedium)
        )
    }
}

@Composable
private fun ZonePowerButton(zoneName: String, isOn: Boolean, onToggle: () -> Unit) {
    val stateColor = if (isOn) OnColor else OffColor
    Text(zoneName, style = darkStyle(MaterialTheme.typography.headlineMedium, solidAlpha = 0.7f))
    Spacer(modifier = Modifier.height(16.dp))
    Button(
        onClick = onToggle,
        modifier = Modifier.size(200.dp),
        shape = CircleShape,
        contentPadding = PaddingValues(0.dp),
        // Outline only, in the on/off colour
        border = BorderStroke(4.dp, stateColor),
        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, contentColor = stateColor)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Filled.PowerSettingsNew,
                contentDescription = if (isOn) "Turn off $zoneName" else "Turn on $zoneName",
                modifier = Modifier.size(96.dp)
            )
            Text(
                text = if (isOn) "ON" else "OFF",
                style = darkStyle(TextStyle(fontSize = 28.sp, fontWeight = FontWeight.Bold), stateColor, solidAlpha = 1f)
            )
        }
    }
}

@Composable
private fun TemperatureCard(label: String, value: String?, temperatureUnit: TemperatureUnit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(label, style = darkStyle(MaterialTheme.typography.titleMedium))
        Text(
            text = displayTemp(value, temperatureUnit)?.let { "$it${label(temperatureUnit)}" } ?: "--",
            style = darkStyle(TextStyle(fontSize = 40.sp, fontWeight = FontWeight.Bold))
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SimpleModeScreenPreview() {
    AirconAppTheme {
        SimpleModeScreenContent(
            controlInfo = ControlInfo(pow = "1", mode = "2", stemp = "22", f_rate = "1", f_dir = "0"),
            zoneStatus = listOf(Zone("Master", true), Zone("Mel", false)),
            sensorInfo = SensorInfo(indoorTemp = "26", outdoorTemp = null),
            zoneName = "Master",
            startTemperature = 21,
            error = null,
            onToggle = {},
            onNavigateToSettings = {},
            isDisconnected = false
        )
    }
}
