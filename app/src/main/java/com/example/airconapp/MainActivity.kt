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
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.airconapp.data.db.SchedulerProfile
import com.example.airconapp.di.NetworkModule
import com.example.airconapp.presentation.MainViewModel
import com.example.airconapp.presentation.SchedulerViewModel
import com.example.airconapp.ui.components.AirconControlScreenContent
import com.example.airconapp.ui.screens.AddEditScheduleScreen
import com.example.airconapp.ui.screens.SchedulerScreen
import com.example.airconapp.ui.theme.AirconAppTheme
import com.google.gson.Gson
import com.example.airconapp.scheduler.ScheduleManager

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        NetworkModule.initialize(applicationContext)
        
        ScheduleManager.getInstance(this).startScheduleMonitoring()
        setContent {
            AirconAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    NavHost(navController = navController, startDestination = "main_control") {
                        composable("main_control") {
                            val mainViewModel: MainViewModel = viewModel()
                            val controlInfo by mainViewModel.controlInfo.collectAsState()
                            val zoneStatus by mainViewModel.zoneStatus.collectAsState()
                            val isLoading by mainViewModel.isLoading.collectAsState()
                            val error by mainViewModel.error.collectAsState()

                            AirconControlScreenContent(
                                controlInfo = controlInfo,
                                zoneStatus = zoneStatus,
                                isLoading = isLoading,
                                error = error,
                                onPowerToggle = { power -> mainViewModel.setPower(power) },
                                onSetTemperature = { temp -> mainViewModel.setTemperature(temp) },
                                onSetMode = { mode -> mainViewModel.setMode(mode) },
                                onSetFanRate = { fanRate -> mainViewModel.setFanRate(fanRate) },
                                onSetZonePower = { index, isOn -> mainViewModel.setZonePower(index, isOn) },
                                onNavigateToScheduler = { navController.navigate("scheduler_list") }
                            )
                        }
                        composable("scheduler_list") {
                            val schedulerViewModel: SchedulerViewModel = viewModel(factory = SchedulerViewModel.Factory)
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
                            val schedulerViewModel: SchedulerViewModel = viewModel(factory = SchedulerViewModel.Factory)
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
                            val schedulerViewModel: SchedulerViewModel = viewModel(factory = SchedulerViewModel.Factory)
                            val mainViewModel: MainViewModel = viewModel() // Get MainViewModel to access zoneStatus
                            val currentZones by mainViewModel.zoneStatus.collectAsState()
                            AddEditScheduleScreen(
                                viewModel = schedulerViewModel,
                                schedule = null,
                                availableZones = currentZones ?: emptyList(), // Pass available zones
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }
}
