package com.example.airconapp.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.airconapp.di.NetworkModule

class EndTimeWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    private val airconApiService = NetworkModule.airconApiService

    override suspend fun doWork(): Result {
        val scheduleId = inputData.getInt("SCHEDULE_ID", -1)
        if (scheduleId == -1) {
            Log.e("EndTimeWorker", "No SCHEDULE_ID provided.")
            return Result.failure()
        }

        Log.d("EndTimeWorker", "Executing end time for schedule ID $scheduleId")

        return try {
            // Turn off the aircon (assuming pow = "0" means off)
            airconApiService.setControlInfo(com.example.airconapp.data.ControlInfo(pow = "0", mode = "0", stemp = "25", f_rate = "A", f_dir = "0"))

            // Set schedule to inactive after end time execution
            val schedulerRepository = NetworkModule.schedulerRepository
            val schedule = schedulerRepository.getProfileById(scheduleId)
            if (schedule != null) {
                val updatedSchedule = schedule.copy(isActive = false)
                schedulerRepository.update(updatedSchedule)
                Log.d("EndTimeWorker", "Schedule ID $scheduleId set to inactive after end time execution.")
            }

            Log.d("EndTimeWorker", "Successfully turned off aircon for schedule ID $scheduleId.")
            Result.success()
        } catch (e: Exception) {
            Log.e("EndTimeWorker", "Failed to execute end time for schedule ID $scheduleId: ${e.message}")
            Result.retry()
        }
    }
}