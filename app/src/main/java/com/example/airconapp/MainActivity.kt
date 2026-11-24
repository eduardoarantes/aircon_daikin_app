package com.example.airconapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.airconapp.data.db.SchedulerProfile
import com.example.airconapp.di.NetworkModule
import com.example.airconapp.presentation.MainViewModel
import com.example.airconapp.presentation.SchedulerViewModel
import com.example.airconapp.presentation.SettingsViewModel
import com.example.airconapp.ui.components.AirconControlScreenContent
import com.example.airconapp.ui.components.SimpleModeScreenContent
import com.example.airconapp.ui.screens.AddEditScheduleScreen
import com.example.airconapp.ui.screens.SchedulerScreen
import com.example.airconapp.ui.screens.SettingsScreen
import com.example.airconapp.ui.theme.AirconAppTheme
import com.example.airconapp.wallpanel.WallPanel
import android.view.WindowManager
import com.google.gson.Gson
import com.example.airconapp.scheduler.ScheduleManager
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    /** Incremented on each Home press while this app is the home app. */
    private val homePresses = kotlinx.coroutines.flow.MutableStateFlow(0)

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        if (intent.hasCategory(android.content.Intent.CATEGORY_HOME)) homePresses.value++
    }

    /** The wall panel stays silent: the volume buttons do nothing while simple mode is on. */
    override fun dispatchKeyEvent(event: android.view.KeyEvent): Boolean {
        val isVolumeKey = event.keyCode == android.view.KeyEvent.KEYCODE_VOLUME_UP ||
            event.keyCode == android.view.KeyEvent.KEYCODE_VOLUME_DOWN ||
            event.keyCode == android.view.KeyEvent.KEYCODE_VOLUME_MUTE
        if (isVolumeKey && NetworkModule.getSimpleModeEnabled()) return true
        return super.dispatchKeyEvent(event)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        NetworkModule.initialize(applicationContext)
        WallPanel.setHomeAppEnabled(this, NetworkModule.getSimpleModeEnabled())

        // Schedule existing profiles
        lifecycleScope.launch {
            val schedulerRepository = NetworkModule.schedulerRepository
            val scheduleManager = ScheduleManager.getInstance(applicationContext)
            schedulerRepository.allSchedulerProfiles.first().forEach {
                scheduleManager.schedule(it)
            }
        }

        setContent {
            AirconAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    // Home button on a wall panel always returns to the control screen
                    val homePressCount by homePresses.collectAsState()
                    androidx.compose.runtime.LaunchedEffect(homePressCount) {
                        if (homePressCount > 0) navController.popBackStack("main_control", inclusive = false)
                    }
                    NavHost(navController = navController, startDestination = "main_control") {
                        composable("main_control") {
                            val mainViewModel: MainViewModel = viewModel()
                            val controlInfo by mainViewModel.controlInfo.collectAsState()
                            val zoneStatus by mainViewModel.zoneStatus.collectAsState()
                            val isLoading by mainViewModel.isLoading.collectAsState()
                            val error by mainViewModel.error.collectAsState()
                            val showConnectionErrorDialog by mainViewModel.showConnectionErrorDialog.collectAsState()
                            val sensorInfo by mainViewModel.sensorInfo.collectAsState()
                            val simpleModeEnabled by mainViewModel.simpleModeEnabled.collectAsState()
                            val simpleModeZone by mainViewModel.simpleModeZone.collectAsState()
                            val simpleModeStartTemperature by mainViewModel.simpleModeStartTemperature.collectAsState()

                            // Simple mode keeps the user in the app; turning it off releases the lock
                            androidx.compose.runtime.LaunchedEffect(simpleModeEnabled) {
                                if (simpleModeEnabled) WallPanel.startKiosk(this@MainActivity)
                                else WallPanel.stopKiosk(this@MainActivity)
                                if (simpleModeEnabled) {
                                    // Re-applied regularly in case something turned sound back on
                                    while (true) {
                                        WallPanel.enforceSilence(this@MainActivity)
                                        kotlinx.coroutines.delay(30_000)
                                    }
                                } else {
                                    WallPanel.releaseSilence(this@MainActivity)
                                }
                            }

                            if (simpleModeEnabled) {
                                // Wall-panel behaviour, only while this screen is showing
                                DisposableEffect(Unit) {
                                    WallPanel.setShowOnLockScreen(this@MainActivity, true)
                                    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                                    onDispose {
                                        WallPanel.setShowOnLockScreen(this@MainActivity, false)
                                        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                                    }
                                }
                                SimpleModeScreenContent(
                                    controlInfo = controlInfo,
                                    zoneStatus = zoneStatus,
                                    sensorInfo = sensorInfo,
                                    zoneName = simpleModeZone,
                                    startTemperature = simpleModeStartTemperature,
                                    error = error,
                                    onToggle = { mainViewModel.toggleSimpleMode() },
                                    // Shown over the lock screen, so Settings needs an unlock first
                                    onNavigateToSettings = {
                                        WallPanel.runAfterUnlock(this@MainActivity) { navController.navigate("settings") }
                                    },
                                    isDisconnected = showConnectionErrorDialog
                                )
                            } else AirconControlScreenContent(
                                controlInfo = controlInfo,
                                zoneStatus = zoneStatus,
                                isLoading = isLoading,
                                error = error,
                                onPowerToggle = { power -> mainViewModel.setPower(power) },
                                onSetTemperature = { temp -> mainViewModel.setTemperature(temp) },
                                onSetMode = { mode -> mainViewModel.setMode(mode) },
                                onSetFanRate = { fanRate -> mainViewModel.setFanRate(fanRate) },
                                onSetZonePower = { index, isOn -> mainViewModel.setZonePower(index, isOn) },
                                onNavigateToScheduler = { navController.navigate("scheduler_list") },
                                onNavigateToSettings = { navController.navigate("settings") },
                                showConnectionErrorDialog = showConnectionErrorDialog,
                                onConfirmConnectionError = { mainViewModel.hideConnectionErrorDialog() },
                                onSwitchToMock = { mainViewModel.switchToMockMode() }
                            )
                        }
                        composable("scheduler_list") {
                            val schedulerViewModel: SchedulerViewModel = viewModel(factory = SchedulerViewModel.factory(application))
                            val mainViewModel: MainViewModel = viewModel()
                            val currentZones by mainViewModel.zoneStatus.collectAsState()
                            SchedulerScreen(
                                viewModel = schedulerViewModel,
                                availableZones = currentZones ?: emptyList(),
                                onNavigateToAddEditSchedule = { navController.navigate("add_edit_schedule") },
                                onNavigateToEditSchedule = { schedule ->
                                    val scheduleJson = Gson().toJson(schedule)
                                    navController.navigate("add_edit_schedule_json/$scheduleJson")
                                },
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }
                        composable(
                            "add_edit_schedule_json/{scheduleJson}",
                            arguments = listOf(navArgument("scheduleJson") { type = NavType.StringType; nullable = true })
                        ) {
                            val schedulerViewModel: SchedulerViewModel = viewModel(factory = SchedulerViewModel.factory(application))
                            val scheduleJson = it.arguments?.getString("scheduleJson")
                            val schedule = if (scheduleJson != null) Gson().fromJson(scheduleJson, SchedulerProfile::class.java) else null
                            val mainViewModel: MainViewModel = viewModel() // Get MainViewModel to access zoneStatus
                            val currentZones by mainViewModel.zoneStatus.collectAsState()
                            AddEditScheduleScreen(
                                viewModel = schedulerViewModel,
                                schedule = schedule,
                                availableZones = currentZones ?: emptyList(), // Pass available zones
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }
                        composable("add_edit_schedule") {
                            val schedulerViewModel: SchedulerViewModel = viewModel(factory = SchedulerViewModel.factory(application))
                            val mainViewModel: MainViewModel = viewModel() // Get MainViewModel to access zoneStatus
                            val currentZones by mainViewModel.zoneStatus.collectAsState()
                            AddEditScheduleScreen(
                                viewModel = schedulerViewModel,
                                schedule = null,
                                availableZones = currentZones ?: emptyList(), // Pass available zones
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }
                        composable("settings") {
                            val settingsViewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.factory(application))
                            SettingsScreen(
                                viewModel = settingsViewModel,
                                onNavigateBack = { navController.popBackStack() },
                                onSettingsSaved = { navController.navigate("main_control") { popUpTo("main_control") { inclusive = true } } }
                            )
                        }
                    }
                }
            }
        }
    }
}
