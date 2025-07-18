package com.example.airconapp.scheduler

import android.content.Context
import androidx.work.*
import com.example.airconapp.worker.ScheduleWorker
import java.util.concurrent.TimeUnit

class ScheduleManager(private val context: Context) {
    
    private val workManager = WorkManager.getInstance(context)
    
    fun startScheduleMonitoring() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        
        val scheduleCheckRequest = PeriodicWorkRequestBuilder<ScheduleWorker>(
            15, TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .build()
        
        workManager.enqueueUniquePeriodicWork(
            "schedule_monitoring",
            ExistingPeriodicWorkPolicy.KEEP,
            scheduleCheckRequest
        )
    }
    
    fun stopScheduleMonitoring() {
        workManager.cancelUniqueWork("schedule_monitoring")
    }
    
    companion object {
        @Volatile
        private var INSTANCE: ScheduleManager? = null
        
        fun getInstance(context: Context): ScheduleManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: ScheduleManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
