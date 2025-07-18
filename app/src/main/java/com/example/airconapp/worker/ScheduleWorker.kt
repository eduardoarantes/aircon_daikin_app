package com.example.airconapp.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.airconapp.di.NetworkModule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import android.util.Log

class ScheduleWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val schedulerRepository = NetworkModule.schedulerRepository
    private val airconApiService = NetworkModule.airconApiService

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            Log.d("ScheduleWorker", "Checking for schedules to execute...")
            
            val currentTime = LocalTime.now()
            val currentTimeString = currentTime.format(DateTimeFormatter.ofPattern("HH:mm"))
            
            val allProfiles = schedulerRepository.getAllSchedulesSync()
            
            val schedulesToExecute = allProfiles.filter { profile ->
                profile.startTime == currentTimeString
            }
            
            Log.d("ScheduleWorker", "Found ${schedulesToExecute.size} schedules to execute at $currentTimeString")
            
            for (schedule in schedulesToExecute) {
                try {
                    Log.d("ScheduleWorker", "Executing schedule ID ${schedule.id}")
                    
                    airconApiService.setControlInfo(schedule.controlInfo)
                    
                    airconApiService.setZoneSetting(schedule.zones)
                    
                    Log.d("ScheduleWorker", "Successfully executed schedule ID ${schedule.id}")
                } catch (e: Exception) {
                    Log.e("ScheduleWorker", "Failed to execute schedule ID ${schedule.id}: ${e.message}")
                }
            }
            
            Result.success()
        } catch (e: Exception) {
            Log.e("ScheduleWorker", "ScheduleWorker failed: ${e.message}")
            Result.retry()
        }
    }
}
