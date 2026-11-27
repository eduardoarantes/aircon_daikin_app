package com.example.airconapp

import android.content.Intent
import android.content.Context
import android.content.pm.ShortcutManager
import android.os.SystemClock
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.work.WorkManager
import androidx.work.ListenableWorker
import androidx.work.testing.TestListenableWorkerBuilder
import com.example.airconapp.worker.ScheduleWorker
import com.example.airconapp.worker.EndTimeWorker
import java.util.concurrent.TimeUnit
import com.example.airconapp.di.NetworkModule
import com.example.airconapp.shortcuts.SceneShortcutActivity
import com.example.airconapp.shortcuts.SceneShortcuts
import com.example.airconapp.shortcuts.ShortcutAuthorization
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StartupSecurityTest {
    @Test fun processInitializesDependenciesWithoutAnActivity() {
        val app = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext
        assertTrue(app is AirconApplication)
        assertNotNull(NetworkModule.getBaseUrl())
        assertNotNull(NetworkModule.airconApiService)
        assertNotNull(NetworkModule.schedulerRepository)
    }

    @Test fun scheduledWorkersCanStartWithoutOpeningTheApp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        // Missing IDs exit before any adapter requests; exercise construction and startup only.
        val start = TestListenableWorkerBuilder<ScheduleWorker>(context).build()
        val end = TestListenableWorkerBuilder<EndTimeWorker>(context).build()
        assertEquals(ListenableWorker.Result.failure(), start.startWork().get(5, TimeUnit.SECONDS))
        assertEquals(ListenableWorker.Result.failure(), end.startWork().get(5, TimeUnit.SECONDS))
    }

    @Test fun forgedShortcutIntentsCannotEnqueueCommands() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val workManager = WorkManager.getInstance(context)
        val before = workManager.getWorkInfosByTag(SceneShortcuts.WORK_TAG).get().size
        for (token in listOf(null, "forged")) {
            val intent = Intent(context, SceneShortcutActivity::class.java)
                .setAction(SceneShortcuts.ACTION_TURN_OFF)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            token?.let { intent.putExtra(ShortcutAuthorization.EXTRA_TOKEN, it) }
            instrumentation.startActivitySync(intent)
            instrumentation.waitForIdleSync()
            assertEquals(before, workManager.getWorkInfosByTag(SceneShortcuts.WORK_TAG).get().size)
        }
    }

    @Test fun publishedShortcutRemainsOneTapInMockMode() {
        // This positive-path test is only safe against the mock adapter.
        assumeTrue(NetworkModule.getUseMockApi())
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val workManager = WorkManager.getInstance(context)
        val before = workManager.getWorkInfosByTag(SceneShortcuts.WORK_TAG).get().map { it.id }.toSet()
        SceneShortcuts.publish(context, emptyList())
        val shortcut = context.getSystemService(ShortcutManager::class.java).dynamicShortcuts.single { it.id == "turn_off" }
        val intent = shortcut.intent!!
        val auth = ShortcutAuthorization(context.getSharedPreferences(ShortcutAuthorization.PREFS_NAME, Context.MODE_PRIVATE))
        assertTrue(auth.accepts(intent.getStringExtra(ShortcutAuthorization.EXTRA_TOKEN)))
        instrumentation.startActivitySync(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        val deadline = SystemClock.elapsedRealtime() + 5000
        do {
            val work = workManager.getWorkInfosByTag(SceneShortcuts.WORK_TAG).get().filter { it.id !in before }
            if (work.any { it.state == androidx.work.WorkInfo.State.SUCCEEDED }) return
            SystemClock.sleep(50)
        } while (SystemClock.elapsedRealtime() < deadline)
        fail("Published shortcut did not successfully execute its mock command")
    }
}
