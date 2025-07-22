package com.example.airconapp.scheduler

import android.content.Context
import androidx.work.*
import com.example.airconapp.data.db.SchedulerProfile
import com.example.airconapp.worker.ScheduleWorker
import java.time.Duration
import java.time.LocalTime
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit

class ScheduleManager(private val context: Context) {

    private val workManager = WorkManager.getInstance(context)

    fun schedule(profile: SchedulerProfile) {
        val now = ZonedDateTime.now()
        var nextExecutionTime = now
            .with(LocalTime.parse(profile.startTime))

        if (nextExecutionTime.isBefore(now)) {
            nextExecutionTime = nextExecutionTime.plusDays(1)
        }

        val delay = Duration.between(now, nextExecutionTime).toMillis()

        val data = workDataOf("SCHEDULE_ID" to profile.id)

        val workRequest = OneTimeWorkRequestBuilder<ScheduleWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .setInputData(data)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .addTag("schedule_${profile.id}")
            .build()

        workManager.enqueueUniqueWork(
            "schedule_${profile.id}",
            ExistingWorkPolicy.REPLACE,
            workRequest
        )
    }

    fun cancel(profileId: Int) {
        workManager.cancelUniqueWork("schedule_$profileId")
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
