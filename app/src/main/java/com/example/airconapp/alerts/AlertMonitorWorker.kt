package com.example.airconapp.alerts

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.airconapp.di.NetworkModule
import com.example.airconapp.domain.AlertMonitor
import kotlinx.coroutines.CancellationException
import java.util.concurrent.TimeUnit

/**
 * Reads the unit's state in the background and posts any alerts the user switched on. Runs every
 * 15 minutes (Android's shortest periodic interval), so alerts can arrive up to that late and a
 * change undone within one interval may go unnoticed.
 */
class AlertMonitorWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        NetworkModule.initialize(applicationContext)
        val settings = NetworkModule.getAlertSettings()
        // Mock mode has no real unit to watch, and its state resets with each process
        if (!settings.any || NetworkModule.getUseMockApi()) return Result.success()

        val info = try {
            NetworkModule.airconApiService.getControlInfo()
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            // Unreachable this time; try again at the next interval
            return Result.success()
        }
        val alerts = NetworkModule.alertStateStore.update {
            AlertMonitor.observe(it, info.pow, info.mode, System.currentTimeMillis(), settings)
        }
        alerts.forEach { AlertNotifier.show(applicationContext, it) }
        return Result.success()
    }

    companion object {
        private const val WORK_NAME = "aircon_alert_monitor"
        private const val INTERVAL_MINUTES = 15L

        /** Starts the periodic check when any alert is on and stops it when all are off. */
        fun sync(context: Context, enabled: Boolean) {
            val workManager = WorkManager.getInstance(context)
            if (!enabled) {
                workManager.cancelUniqueWork(WORK_NAME)
                return
            }
            val request = PeriodicWorkRequestBuilder<AlertMonitorWorker>(INTERVAL_MINUTES, TimeUnit.MINUTES)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .build()
            workManager.enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
        }
    }
}
