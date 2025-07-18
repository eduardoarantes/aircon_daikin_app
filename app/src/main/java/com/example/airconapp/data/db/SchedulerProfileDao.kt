package com.example.airconapp.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface SchedulerProfileDao {
    @Query("SELECT * FROM scheduler_profiles ORDER BY startTime ASC")
    fun getAll(): Flow<List<SchedulerProfile>>

    @Query("SELECT * FROM scheduler_profiles WHERE id = :id")
    suspend fun getById(id: Int): SchedulerProfile?

    @Query("SELECT * FROM scheduler_profiles ORDER BY startTime ASC")
    suspend fun getAllSync(): List<SchedulerProfile>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(profile: SchedulerProfile)

    @Update
    suspend fun update(profile: SchedulerProfile)

    @Delete
    suspend fun delete(profile: SchedulerProfile)
}
