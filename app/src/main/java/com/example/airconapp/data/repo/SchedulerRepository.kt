package com.example.airconapp.data.repo

import com.example.airconapp.data.db.SchedulerProfile
import com.example.airconapp.data.db.SchedulerProfileDao
import kotlinx.coroutines.flow.Flow

class SchedulerRepository(private val schedulerProfileDao: SchedulerProfileDao) {

    val allSchedulerProfiles: Flow<List<SchedulerProfile>> = schedulerProfileDao.getAll()

    suspend fun getProfileById(id: Int): SchedulerProfile? {
        return schedulerProfileDao.getById(id)
    }

    suspend fun insert(profile: SchedulerProfile): Long {
        return schedulerProfileDao.insert(profile)
    }

    suspend fun update(profile: SchedulerProfile) {
        schedulerProfileDao.update(profile)
    }

    suspend fun delete(profile: SchedulerProfile) {
        schedulerProfileDao.delete(profile)
    }
    
    suspend fun getAllSchedulesSync(): List<SchedulerProfile> {
        return schedulerProfileDao.getAllSync()
    }
}
