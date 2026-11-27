package com.example.airconapp

import android.app.Application
import com.example.airconapp.di.NetworkModule

/** Workers can start the process without opening an activity. */
class AirconApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        NetworkModule.initialize(this)
    }
}
