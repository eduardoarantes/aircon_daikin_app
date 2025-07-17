package com.example.airconapp.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import com.example.airconapp.data.ControlInfo
import com.example.airconapp.data.Zone
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

@Entity(tableName = "scheduler_profiles")
data class SchedulerProfile(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val startTime: String,
    val endTime: String?,
    val controlInfo: ControlInfo,
    val zones: List<Zone>
)

data class ControlInfo(
    val ret: String? = null,
    val pow: String,
    val mode: String,
    val stemp: String,
    val f_rate: String,
    val f_dir: String
)

data class Zone(
    val name: String,
    val isOn: Boolean
)

class Converters {
    @TypeConverter
    fun fromControlInfo(controlInfo: ControlInfo): String {
        return Gson().toJson(controlInfo)
    }

    @TypeConverter
    fun toControlInfo(controlInfoString: String): ControlInfo {
        return Gson().fromJson(controlInfoString, ControlInfo::class.java)
    }

    @TypeConverter
    fun fromZoneList(zones: List<Zone>): String {
        return Gson().toJson(zones)
    }

    @TypeConverter
    fun toZoneList(zoneListString: String): List<Zone> {
        val listType = object : TypeToken<List<Zone>>() {}.type
        return Gson().fromJson(zoneListString, listType)
    }
}
