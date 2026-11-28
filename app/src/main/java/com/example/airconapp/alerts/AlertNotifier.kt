package com.example.airconapp.alerts

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.airconapp.R
import com.example.airconapp.SplashActivity
import com.example.airconapp.domain.Alert
import com.example.airconapp.domain.AlertMonitor

/** Posts the alert notifications. Each kind replaces its own previous notification. */
object AlertNotifier {

    private const val CHANNEL_ID = "aircon_alerts"

    fun canNotify(context: Context): Boolean {
        val permitted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        return permitted && NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    fun show(context: Context, alert: Alert) {
        if (!canNotify(context)) return
        ensureChannel(context)
        val (id, title, text) = when (alert) {
            Alert.Overrun -> Triple(
                1001,
                "Aircon has been on for over ${AlertMonitor.OVERRUN_HOURS} hours",
                "It's still running. Tap to check it or turn it off."
            )
            is Alert.PowerChanged -> Triple(
                1002,
                if (alert.isOn) "Aircon turned on" else "Aircon turned off",
                "Someone turned the aircon ${if (alert.isOn) "on" else "off"}."
            )
            is Alert.ModeChanged -> Triple(
                1003,
                "Aircon mode changed",
                "Someone changed the mode from ${AlertMonitor.modeLabel(alert.fromMode)} " +
                    "to ${AlertMonitor.modeLabel(alert.toMode)}."
            )
        }
        val openApp = PendingIntent.getActivity(
            context,
            0,
            Intent(context, SplashActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setContentIntent(openApp)
            .setAutoCancel(true)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(id, notification)
        } catch (_: SecurityException) {
            // Permission withdrawn between the check and the post
        }
    }

    private fun ensureChannel(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        manager.createNotificationChannel(
            // High importance so alerts pop up on screen, like the Daikin app's
            NotificationChannel(CHANNEL_ID, "Aircon alerts", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Overrun warnings and on/off or mode changes made outside this app"
            }
        )
    }
}
