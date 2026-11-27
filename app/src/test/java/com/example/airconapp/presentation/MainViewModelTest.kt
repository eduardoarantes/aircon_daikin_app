package com.example.airconapp.presentation

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import com.example.airconapp.data.*
import com.example.airconapp.di.NetworkModule
import io.mockk.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val store = ViewModelStore()

    private class FakeApi : AirconApiService {
        val controls = mutableListOf<ControlInfo>()
        val zones = mutableListOf<List<Zone>>()
        var readStarts = 0
        var activeReads = 0
        var cancelledReads = 0
        var blockControls: CompletableDeferred<Unit>? = null
        var blockSensors: CompletableDeferred<Unit>? = null
        override suspend fun getControlInfo(): ControlInfo {
            readStarts++
            activeReads++
            try {
                blockControls?.await()
                return ControlInfo("OK", "1", "2", "22", "3", "0")
            } catch (e: CancellationException) {
                cancelledReads++
                throw e
            } finally { activeReads-- }
        }
        override suspend fun getZoneSetting() = ZoneStatusResponse(listOf(Zone("Room", true)))
        override suspend fun getSensorInfo(): SensorInfo {
            blockSensors?.await()
            return SensorInfo("24", null)
        }
        override suspend fun setControlInfo(controlInfo: ControlInfo): String { controls.add(controlInfo); return "ret=OK" }
        override suspend fun setZoneSetting(zones: List<Zone>): String { this.zones.add(zones); return "ret=OK" }
    }

    @Before fun setUp() {
        Dispatchers.setMain(dispatcher)
        val prefs = mockk<SharedPreferences>()
        val context = mockk<Context>()
        every { context.applicationContext } returns context
        every { context.getSharedPreferences(any(), any()) } returns prefs
        every { prefs.getString(any(), any()) } answers { secondArg() }
        every { prefs.getBoolean(any(), any()) } answers { secondArg() }
        every { prefs.getInt(any(), any()) } answers { secondArg() }
        every { prefs.contains(any()) } returns true
        NetworkModule.initialize(context)
    }

    @After fun tearDown() {
        store.clear()
        dispatcher.scheduler.runCurrent()
        Dispatchers.resetMain()
        unmockkAll()
    }

    private fun create(api: FakeApi): MainViewModel = ViewModelProvider(store, object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = MainViewModel { api } as T
    })[MainViewModel::class.java]

    @Test fun `closing unchanged session sends no commands`() = runTest(dispatcher) {
        val api = FakeApi()
        val vm = create(api)
        vm.fetchAirconStatus().join()
        store.clear()
        runCurrent()
        assertTrue(api.controls.isEmpty())
        assertTrue(api.zones.isEmpty())
    }

    @Test fun `closing flushes only actual pending changes once`() = runTest(dispatcher) {
        val api = FakeApi()
        val vm = create(api)
        vm.fetchAirconStatus().join()
        vm.setTemperature(23)
        store.clear()
        runCurrent()
        assertEquals(listOf("23"), api.controls.map { it.stemp })
        assertTrue(api.zones.isEmpty())
        advanceTimeBy(3000)
        runCurrent()
        assertEquals(1, api.controls.size)
    }

    @Test fun `reverted and already sent changes are not resent on close`() = runTest(dispatcher) {
        val api = FakeApi()
        val vm = create(api)
        vm.fetchAirconStatus().join()
        vm.setTemperature(23)
        vm.setTemperature(22)
        advanceTimeBy(2001)
        runCurrent()
        assertTrue(api.controls.isEmpty())
        vm.setTemperature(24)
        advanceTimeBy(2001)
        runCurrent()
        assertEquals(1, api.controls.size)
        store.clear()
        runCurrent()
        assertEquals(1, api.controls.size)
        assertTrue(api.zones.isEmpty())
    }

    @Test fun `slow polling is single flight and hidden app cancels the request`() = runTest(dispatcher) {
        val api = FakeApi().apply { blockControls = CompletableDeferred() }
        val vm = create(api)
        vm.onAppVisible()
        runCurrent()
        advanceTimeBy(16000)
        runCurrent()
        assertEquals(1, api.readStarts)
        val first = vm.fetchAirconStatus()
        assertSame(first, vm.fetchAirconStatus())
        vm.onAppHidden()
        runCurrent()
        assertEquals(0, api.activeReads)
        assertEquals(1, api.cancelledReads)
        api.blockControls!!.complete(Unit)
        runCurrent()
        assertNull(vm.controlInfo.value)
        assertFalse(vm.isLoading.value)
    }

    @Test fun `controls are usable before optional sensors finish and sensor cancellation propagates`() = runTest(dispatcher) {
        val api = FakeApi().apply { blockSensors = CompletableDeferred() }
        val vm = create(api)
        val fetch = vm.fetchAirconStatus()
        runCurrent()
        assertNotNull(vm.controlInfo.value)
        assertEquals(true, vm.isConnected.value)
        assertNotNull(vm.zoneStatus.value)
        assertNull(vm.sensorInfo.value)
        assertTrue(fetch.isActive)
        vm.onAppHidden()
        runCurrent()
        assertTrue(fetch.isCancelled)
        api.blockSensors!!.complete(Unit)
        runCurrent()
        assertNull(vm.sensorInfo.value)
    }

    @Test fun `returning to app restarts polling after cancellation`() = runTest(dispatcher) {
        val api = FakeApi().apply { blockControls = CompletableDeferred() }
        val vm = create(api)
        vm.onAppVisible()
        runCurrent()
        vm.onAppHidden()
        runCurrent()
        api.blockControls = null
        vm.onAppVisible()
        runCurrent()
        assertEquals(2, api.readStarts)
        assertNotNull(vm.controlInfo.value)
        vm.onAppHidden()
    }
}
