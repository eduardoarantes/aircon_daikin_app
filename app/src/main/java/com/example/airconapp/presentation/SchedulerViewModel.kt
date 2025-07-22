package com.example.airconapp.presentation

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.airconapp.data.db.SchedulerProfile
import com.example.airconapp.data.repo.SchedulerRepository
import com.example.airconapp.di.NetworkModule
import com.example.airconapp.scheduler.ScheduleManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SchedulerViewModel(application: Application, private val repository: SchedulerRepository) : AndroidViewModel(application) {

    private val scheduleManager = ScheduleManager.getInstance(application)

    val allProfiles: StateFlow<List<SchedulerProfile>> = repository.allSchedulerProfiles
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _profileToEdit = MutableStateFlow<SchedulerProfile?>(null)
    val profileToEdit: StateFlow<SchedulerProfile?> = _profileToEdit

    fun loadProfile(id: Int) {
        viewModelScope.launch {
            _profileToEdit.value = repository.getProfileById(id)
        }
    }

    fun clearProfileToEdit() {
        _profileToEdit.value = null
    }

    fun insert(profile: SchedulerProfile) = viewModelScope.launch {
        val newId = repository.insert(profile).toInt()
        val newProfile = profile.copy(id = newId)
        if (newProfile.isActive) {
            scheduleManager.schedule(newProfile)
        }
    }

    fun update(profile: SchedulerProfile) = viewModelScope.launch {
        repository.update(profile)
        if (profile.isActive) {
            scheduleManager.schedule(profile)
        } else {
            scheduleManager.cancel(profile.id)
        }
    }

    fun delete(profile: SchedulerProfile) = viewModelScope.launch {
        repository.delete(profile)
        scheduleManager.cancel(profile.id)
    }

    companion object {
        fun factory(application: Application): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return SchedulerViewModel(
                    application,
                    NetworkModule.schedulerRepository
                ) as T
            }
        }
    }
}