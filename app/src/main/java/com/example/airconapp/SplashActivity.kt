package com.example.airconapp

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.example.airconapp.wallpanel.WallPanel

class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        // Keep the splash screen on-screen until the UI is ready.
        // For this example, we'll just navigate immediately.
        // On a wall panel the app is already running as the home screen: reuse it
        // rather than starting a second copy (each copy polls the aircon).
        if (WallPanel.isDefaultHomeApp(this)) {
            WallPanel.goHome(this)
        } else {
            startActivity(Intent(this, MainActivity::class.java))
        }
        finish()
    }
}