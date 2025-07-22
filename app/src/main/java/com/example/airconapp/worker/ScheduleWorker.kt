package com.example.airconapp.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.airconapp.di.NetworkModule
import com.example.airconapp.scheduler.ScheduleManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ScheduleWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val schedulerRepository = NetworkModule.schedulerRepository
    private val airconApiService = NetworkModule.airconApiService
    private val scheduleManager = ScheduleManager.getInstance(context)

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val scheduleId = inputData.getInt("SCHEDULE_ID", -1)
        if (scheduleId == -1) {
            return@withContext Result.failure()
        }

        try {
            Log.d("ScheduleWorker", "Executing schedule ID $scheduleId")

            val schedule = schedulerRepository.getProfileById(scheduleId)

            if (schedule == null) {
                Log.e("ScheduleWorker", "Schedule with ID $scheduleId not found")
                return@withContext Result.failure()
            }

            if (!schedule.isActive) {
                Log.d("ScheduleWorker", "Schedule ID $scheduleId is not active. Skipping execution.")
                return@withContext Result.success()
            }

            airconApiService.setControlInfo(schedule.controlInfo)
            airconApiService.setZoneSetting(schedule.zones)

            // Update isActive based on endTime presence
            if (schedule.endTime == null) {
                val updatedSchedule = schedule.copy(isActive = false)
                schedulerRepository.update(updatedSchedule)
                Log.d("ScheduleWorker", "Successfully executed schedule ID $scheduleId and set to inactive (no end time).")
            } else {
                Log.d("ScheduleWorker", "Successfully executed schedule ID $scheduleId. Remaining active for end time.")
            }

            Result.success()
        } catch (e: Exception) {
            Log.e("ScheduleWorker", "Failed to execute schedule ID $scheduleId: ${e.message}")
            Result.retry()
        }
    }
}
