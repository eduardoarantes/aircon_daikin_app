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
import com.example.airconapp.di.NetworkModule
import com.example.airconapp.presentation.MainViewModel
import com.example.airconapp.presentation.SchedulerViewModel
import com.example.airconapp.presentation.SettingsViewModel
import com.example.airconapp.ui.components.ConnectionErrorDialog
import com.example.airconapp.ui.components.SimpleModeScreenContent
import com.example.airconapp.ui.screens.HomeScreen
import com.example.airconapp.ui.screens.ScheduleEditScreen
import com.example.airconapp.ui.screens.SceneEditScreen
import com.example.airconapp.ui.screens.SettingsScreen
import com.example.airconapp.ui.screens.SettingsPage
import com.example.airconapp.ui.screens.SettingsPageScreen
import androidx.compose.runtime.remember
import com.example.airconapp.ui.theme.AirconAppTheme
import com.example.airconapp.ui.theme.ThemeSetting
import com.example.airconapp.wallpanel.WallPanel
import android.view.WindowManager
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

    /** The wall panel stays silent: the volume buttons do nothing while it is active. */
    private fun blocksVolumeKey(keyCode: Int): Boolean = NetworkModule.isWallPanelActive() &&
        (keyCode == android.view.KeyEvent.KEYCODE_VOLUME_UP ||
            keyCode == android.view.KeyEvent.KEYCODE_VOLUME_DOWN ||
            keyCode == android.view.KeyEvent.KEYCODE_VOLUME_MUTE)

    override fun onKeyDown(keyCode: Int, event: android.view.KeyEvent): Boolean =
        blocksVolumeKey(keyCode) || super.onKeyDown(keyCode, event)

    override fun onKeyUp(keyCode: Int, event: android.view.KeyEvent): Boolean =
        blocksVolumeKey(keyCode) || super.onKeyUp(keyCode, event)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        NetworkModule.initialize(applicationContext)
        ThemeSetting.load(applicationContext)
        WallPanel.setHomeAppEnabled(this, NetworkModule.isWallPanelActive())
        // Keep the app icon's long-press menu in step with the saved scenes
        NetworkModule.sceneRepository.refreshShortcuts()

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
                    // One view model (and one status poll) shared by every screen
                    val mainViewModel: MainViewModel = viewModel()
                    val schedulerViewModel: SchedulerViewModel = viewModel(factory = SchedulerViewModel.factory(application))
                    // Poll the aircon only while the app is on screen
                    val lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current
                    DisposableEffect(lifecycleOwner) {
                        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
                            when (event) {
                                androidx.lifecycle.Lifecycle.Event.ON_START -> mainViewModel.onAppVisible()
                                androidx.lifecycle.Lifecycle.Event.ON_STOP -> mainViewModel.onAppHidden()
                                else -> Unit
                            }
                        }
                        lifecycleOwner.lifecycle.addObserver(observer)
                        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
                    }
                    // Home button on a wall panel always returns to the control screen
                    val homePressCount by homePresses.collectAsState()
                    androidx.compose.runtime.LaunchedEffect(homePressCount) {
                        if (homePressCount > 0) navController.popBackStack("main_control", inclusive = false)
                    }
                    val temperatureUnit by mainViewModel.temperatureUnit.collectAsState()
                    NavHost(navController = navController, startDestination = "main_control") {
                        composable("main_control") {
                            val controlInfo by mainViewModel.controlInfo.collectAsState()
                            val zoneStatus by mainViewModel.zoneStatus.collectAsState()
                            val error by mainViewModel.error.collectAsState()
                            val showConnectionErrorDialog by mainViewModel.showConnectionErrorDialog.collectAsState()
                            val isConnected by mainViewModel.isConnected.collectAsState()
                            val sensorInfo by mainViewModel.sensorInfo.collectAsState()
                            val simpleModeEnabled by mainViewModel.simpleModeEnabled.collectAsState()
                            val wallPanelEnabled by mainViewModel.wallPanelEnabled.collectAsState()
                            val wallPanelActive = simpleModeEnabled && wallPanelEnabled
                            val simpleModeZone by mainViewModel.simpleModeZone.collectAsState()
                            val simpleModeStartTemperature by mainViewModel.simpleModeStartTemperature.collectAsState()
                            val scenes by NetworkModule.sceneRepository.scenes.collectAsState()

                            // As a wall panel the app keeps the user inside it; otherwise it runs as a normal app
                            androidx.compose.runtime.LaunchedEffect(wallPanelActive) {
                                if (wallPanelActive) WallPanel.startKiosk(this@MainActivity)
                                else WallPanel.stopKiosk(this@MainActivity)
                                if (wallPanelActive) {
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
                                if (wallPanelActive) DisposableEffect(Unit) {
                                    WallPanel.setShowOnLockScreen(this@MainActivity, true)
                                    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                                    onDispose {
                                        WallPanel.setShowOnLockScreen(this@MainActivity, false)
                                        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                                    }
                                }
                                // The wall panel keeps its original look: Material's default type, not Inter
                                MaterialTheme(typography = androidx.compose.material3.Typography()) {
                                SimpleModeScreenContent(
                                    controlInfo = controlInfo,
                                    zoneStatus = zoneStatus,
                                    sensorInfo = sensorInfo,
                                    zoneName = simpleModeZone,
                                    startTemperature = simpleModeStartTemperature,
                                    temperatureUnit = temperatureUnit,
                                    error = error,
                                    onToggle = { mainViewModel.toggleSimpleMode() },
                                    // Shown over the lock screen, so Settings needs an unlock first
                                    onNavigateToSettings = {
                                        WallPanel.runAfterUnlock(this@MainActivity) { navController.navigate("settings") }
                                    },
                                    isDisconnected = isConnected != true
                                )
                                }
                            } else {
                                HomeScreen(
                                    viewModel = mainViewModel,
                                    schedulerViewModel = schedulerViewModel,
                                    onAddSchedule = { navController.navigate("schedule_edit/-1") },
                                    onEditSchedule = { navController.navigate("schedule_edit/${it.id}") },
                                    scenes = scenes,
                                    onAddScene = { navController.navigate("scene_edit/new") },
                                    onEditScene = { navController.navigate("scene_edit/${it.id}") },
                                    onMoveSceneUp = { NetworkModule.sceneRepository.moveUp(it) },
                                    onOpenSettings = { navController.navigate("settings") }
                                )
                                if (showConnectionErrorDialog) {
                                    ConnectionErrorDialog(
                                        onDismiss = { mainViewModel.hideConnectionErrorDialog() },
                                        onOpenSettings = {
                                            mainViewModel.hideConnectionErrorDialog()
                                            navController.navigate("settings")
                                        }
                                    )
                                }
                            }
                        }
                        // New schedule (id -1) or edit an existing one
                        composable(
                            "schedule_edit/{id}",
                            arguments = listOf(navArgument("id") { type = NavType.IntType })
                        ) { entry ->
                            val id = entry.arguments?.getInt("id") ?: -1
                            val currentZones by mainViewModel.zoneStatus.collectAsState()
                            val currentControl by mainViewModel.controlInfo.collectAsState()
                            val loaded by schedulerViewModel.profileToEdit.collectAsState()
                            androidx.compose.runtime.LaunchedEffect(id) {
                                if (id >= 0) schedulerViewModel.loadProfile(id) else schedulerViewModel.clearProfileToEdit()
                            }
                            // Wait for the saved schedule before building the form from it
                            if (id < 0 || loaded?.id == id) {
                                ScheduleEditScreen(
                                    temperatureUnit = temperatureUnit,
                                    schedule = if (id < 0) null else loaded,
                                    current = currentControl,
                                    deviceZones = currentZones.orEmpty(),
                                    onSave = { profile ->
                                        if (id < 0) schedulerViewModel.insert(profile) else schedulerViewModel.update(profile)
                                        navController.popBackStack()
                                    },
                                    onDelete = { profile ->
                                        schedulerViewModel.delete(profile)
                                        navController.popBackStack()
                                    },
                                    onClose = { navController.popBackStack() }
                                )
                            }
                        }
                        // New scene ("new") or edit an existing one
                        composable("scene_edit/{id}") { entry ->
                            val id = entry.arguments?.getString("id")
                            val scene = id?.let { NetworkModule.sceneRepository.get(it) }
                            val currentZones by mainViewModel.zoneStatus.collectAsState()
                            val currentControl by mainViewModel.controlInfo.collectAsState()
                            SceneEditScreen(
                                temperatureUnit = temperatureUnit,
                                scene = scene,
                                current = currentControl,
                                deviceZones = currentZones.orEmpty(),
                                onSave = {
                                    NetworkModule.sceneRepository.save(it)
                                    navController.popBackStack()
                                },
                                onDelete = {
                                    NetworkModule.sceneRepository.delete(it)
                                    navController.popBackStack()
                                },
                                onClose = { navController.popBackStack() }
                            )
                        }
                        val onSettingsSaved = {
                            // Pick up simple mode / wall panel changes now, not at the next poll
                            mainViewModel.fetchAirconStatus()
                            navController.navigate("main_control") { popUpTo("main_control") { inclusive = true } }
                        }
                        composable("settings") {
                            val settingsViewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.factory(application))
                            SettingsScreen(
                                viewModel = settingsViewModel,
                                onNavigateBack = { navController.popBackStack() },
                                onOpenPage = { navController.navigate("settings/${it.route}") },
                                onSettingsSaved = onSettingsSaved
                            )
                        }
                        // A themed settings page; shares the Settings home's view model so drafts carry across pages
                        composable("settings/{page}") { entry ->
                            val page = SettingsPage.fromRoute(entry.arguments?.getString("page"))
                            val settingsEntry = remember(entry) { navController.getBackStackEntry("settings") }
                            val settingsViewModel: SettingsViewModel =
                                viewModel(viewModelStoreOwner = settingsEntry, factory = SettingsViewModel.factory(application))
                            if (page != null) {
                                SettingsPageScreen(
                                    viewModel = settingsViewModel,
                                    page = page,
                                    onBack = { navController.popBackStack() },
                                    onSettingsSaved = onSettingsSaved
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
