package com.example.airconapp.presentation

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.airconapp.di.NetworkModule
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val _apiIp = MutableStateFlow(NetworkModule.getBaseUrl())
    val apiIp: StateFlow<String> = _apiIp.asStateFlow()

    private val _useMockMode = MutableStateFlow(NetworkModule.getUseMockApi())
    val useMockMode: StateFlow<Boolean> = _useMockMode.asStateFlow()

    fun setApiIp(ip: String) {
        _apiIp.value = ip
    }

    fun setUseMockMode(useMock: Boolean) {
        _useMockMode.value = useMock
    }

    fun saveSettings() {
        NetworkModule.setBaseUrl(_apiIp.value)
        NetworkModule.setUseMockApi(_useMockMode.value)
    }

    companion object {
        fun factory(application: Application): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return SettingsViewModel(application) as T
            }
        }
    }
}