package com.example.airconapp.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.airconapp.presentation.MainViewModel
import com.example.airconapp.ui.components.AirconControlScreenContent
import com.example.airconapp.ui.components.ConnectionErrorDialog

@Composable
fun AirconControlScreen(
    mainViewModel: MainViewModel = viewModel(),
    onNavigateToScheduler: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val controlInfo by mainViewModel.controlInfo.collectAsState()
    val zoneStatus by mainViewModel.zoneStatus.collectAsState()
    val isLoading by mainViewModel.isLoading.collectAsState()
    val error by mainViewModel.error.collectAsState()
    val showConnectionErrorDialog by mainViewModel.showConnectionErrorDialog.collectAsState()

    AirconControlScreenContent(
        controlInfo = controlInfo,
        zoneStatus = zoneStatus,
        isLoading = isLoading,
        error = error,
        onPowerToggle = { power -> mainViewModel.setPower(power) },
        onSetTemperature = { temp -> mainViewModel.setTemperature(temp) },
        onSetMode = { mode -> mainViewModel.setMode(mode) },
        onSetFanRate = { fanRate -> mainViewModel.setFanRate(fanRate) },
        onSetZonePower = { zone, power -> mainViewModel.setZonePower(zone, power) },
        onNavigateToScheduler = onNavigateToScheduler,
        onNavigateToSettings = onNavigateToSettings,
        showConnectionErrorDialog = showConnectionErrorDialog,
        onConfirmConnectionError = { mainViewModel.hideConnectionErrorDialog() },
        onSwitchToMock = { mainViewModel.switchToMockMode() }
    )
}