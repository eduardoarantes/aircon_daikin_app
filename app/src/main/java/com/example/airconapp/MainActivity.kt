package com.example.airconapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.airconapp.presentation.MainViewModel
import com.example.airconapp.ui.components.AirconControlScreenContent
import com.example.airconapp.ui.theme.AirconAppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AirconAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val viewModel: MainViewModel = viewModel()
                    val controlInfo by viewModel.controlInfo.collectAsState()
                    val zoneStatus by viewModel.zoneStatus.collectAsState()
                    val isLoading by viewModel.isLoading.collectAsState()
                    val error by viewModel.error.collectAsState()

                    AirconControlScreenContent(
                        controlInfo = controlInfo,
                        zoneStatus = zoneStatus,
                        isLoading = isLoading,
                        error = error,
                        onPowerToggle = { power -> viewModel.setPower(power) },
                        onSetTemperature = { temp -> viewModel.setTemperature(temp) },
                        onSetMode = { mode -> viewModel.setMode(mode) },
                        onSetFanRate = { fanRate -> viewModel.setFanRate(fanRate) }
                    )
                }
            }
        }
    }
}
