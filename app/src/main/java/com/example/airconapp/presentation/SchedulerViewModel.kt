package com.example.airconapp.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.airconapp.data.db.SchedulerProfile
import com.example.airconapp.data.repo.SchedulerRepository
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import androidx.lifecycle.ViewModelProvider
import com.example.airconapp.di.NetworkModule
import kotlinx.coroutines.flow.MutableStateFlow

class SchedulerViewModel(private val repository: SchedulerRepository) : ViewModel() {

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
        repository.insert(profile)
    }

    fun update(profile: SchedulerProfile) = viewModelScope.launch {
        repository.update(profile)
    }

    fun delete(profile: SchedulerProfile) = viewModelScope.launch {
        repository.delete(profile)
    }

    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return SchedulerViewModel(
                    NetworkModule.schedulerRepository
                ) as T
            }
        }
    }
}