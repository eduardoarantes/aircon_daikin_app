package com.example.airconapp.wallpanel

import android.app.Activity
import android.app.ActivityManager
import android.app.KeyguardManager
import android.app.NotificationManager
import android.app.admin.DevicePolicyManager
import android.app.role.RoleManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.media.AudioManager
import android.os.Build
import android.provider.Settings
import android.view.WindowManager

/**
 * Helpers for running simple mode on a docked wall phone: show over the lock screen,
 * act as the home app, and require an unlock before leaving to Settings.
 */
object WallPanel {

    /** activity-alias in AndroidManifest.xml that registers MainActivity as a home app. */
    private const val HOME_ALIAS = "com.example.airconapp.HomeAlias"

    /** Show this activity over the lock screen and wake the screen when it comes to front. */
    @Suppress("DEPRECATION")
    fun setShowOnLockScreen(activity: Activity, enabled: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            activity.setShowWhenLocked(enabled)
            activity.setTurnScreenOn(enabled)
        } else {
            val flags = WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            if (enabled) activity.window.addFlags(flags) else activity.window.clearFlags(flags)
        }
    }

    /**
     * Offer the app as a home app only while simple mode is on, so normal installs
     * never get the "choose home app" prompt.
     */
    fun setHomeAppEnabled(context: Context, enabled: Boolean) {
        val component = ComponentName(context, HOME_ALIAS)
        val newState = if (enabled) PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            else PackageManager.COMPONENT_ENABLED_STATE_DISABLED
        // Skip no-op changes: each change broadcasts a package update to the launcher
        if (context.packageManager.getComponentEnabledSetting(component) == newState) return
        context.packageManager.setComponentEnabledSetting(component, newState, PackageManager.DONT_KILL_APP)
    }

    private fun homeIntent() = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)

    /** True when the user has picked this app as the default home app. */
    fun isDefaultHomeApp(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // resolveActivity can't be trusted on API 30+: package visibility hides other
            // launchers, so it would resolve to this app even when it isn't the default.
            return context.getSystemService(RoleManager::class.java)?.isRoleHeld(RoleManager.ROLE_HOME) == true
        }
        return context.packageManager.resolveActivity(homeIntent(), PackageManager.MATCH_DEFAULT_ONLY)
            ?.activityInfo?.packageName == context.packageName
    }

    /** Brings the home-task copy of the app to front (instead of starting a second copy). */
    fun goHome(context: Context) {
        context.startActivity(homeIntent().addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    /** Opens the system screen where the user picks the default home app. */
    fun openHomeAppSettings(context: Context) {
        val intent = Intent(Settings.ACTION_HOME_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (intent.resolveActivity(context.packageManager) != null) {
            context.startActivity(intent)
        } else {
            context.startActivity(Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }

    /** Runs [onUnlocked] immediately if the phone is unlocked, otherwise after the user unlocks it. */
    fun runAfterUnlock(activity: Activity, onUnlocked: () -> Unit) {
        val keyguard = activity.getSystemService(KeyguardManager::class.java)
        if (keyguard == null || !keyguard.isKeyguardLocked) {
            onUnlocked()
            return
        }
        keyguard.requestDismissKeyguard(activity, object : KeyguardManager.KeyguardDismissCallback() {
            override fun onDismissSucceeded() = onUnlocked()
        })
    }

    // --- Kiosk (lock task) mode ---

    private fun adminComponent(context: Context) = ComponentName(context, WallPanelAdminReceiver::class.java)

    /** True once the app was made device owner with `adb shell dpm set-device-owner ...`. */
    fun isDeviceOwner(context: Context): Boolean =
        context.getSystemService(DevicePolicyManager::class.java)?.isDeviceOwnerApp(context.packageName) == true

    private fun isInLockTask(activity: Activity): Boolean =
        activity.getSystemService(ActivityManager::class.java)?.lockTaskModeState !=
            ActivityManager.LOCK_TASK_MODE_NONE

    /**
     * Keeps the user inside the app. As device owner this is a silent, full kiosk
     * (no Home, Recents or notification shade). Otherwise Android falls back to screen
     * pinning, which asks for confirmation and can be exited with Back + Recents.
     */
    fun startKiosk(activity: Activity) {
        if (isInLockTask(activity)) return
        if (isDeviceOwner(activity)) {
            val dpm = activity.getSystemService(DevicePolicyManager::class.java)
            val admin = adminComponent(activity)
            dpm.setLockTaskPackages(admin, arrayOf(activity.packageName))
            // Also make this the home app, since the system "choose home app" screen
            // can't be opened from inside the kiosk
            setHomeAppEnabled(activity, true)
            val homeFilter = IntentFilter(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
                addCategory(Intent.CATEGORY_DEFAULT)
            }
            dpm.addPersistentPreferredActivity(admin, homeFilter, ComponentName(activity, HOME_ALIAS))
        }
        runCatching { activity.startLockTask() }
    }

    /** Leaves kiosk mode (used when simple mode is turned off). */
    fun stopKiosk(activity: Activity) {
        if (!isInLockTask(activity)) return
        if (isDeviceOwner(activity)) {
            activity.getSystemService(DevicePolicyManager::class.java)
                .clearPackagePersistentPreferredActivities(adminComponent(activity), activity.packageName)
        }
        runCatching { activity.stopLockTask() }
    }

    /**
     * Gives up device owner so the phone can be used (and the app uninstalled) normally.
     * A device owner app can't be removed from outside without a factory reset.
     */
    @Suppress("DEPRECATION")
    fun releaseDeviceOwner(activity: Activity) {
        stopKiosk(activity)
        if (!isDeviceOwner(activity)) return
        val dpm = activity.getSystemService(DevicePolicyManager::class.java)
        dpm.clearPackagePersistentPreferredActivities(adminComponent(activity), activity.packageName)
        dpm.clearDeviceOwnerApp(activity.packageName)
    }

    // --- Silence ---

    private val MUTED_STREAMS = listOf(
        AudioManager.STREAM_MUSIC, AudioManager.STREAM_NOTIFICATION, AudioManager.STREAM_SYSTEM,
        AudioManager.STREAM_RING, AudioManager.STREAM_ALARM
    )

    /** True once the app may switch Do Not Disturb (granted in system settings or via adb). */
    fun hasSilencePermission(context: Context): Boolean =
        context.getSystemService(NotificationManager::class.java)?.isNotificationPolicyAccessGranted == true

    /** Opens the system screen where the user grants Do Not Disturb access. */
    fun openSilencePermissionSettings(context: Context) {
        context.startActivity(
            Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    /**
     * Keeps the wall panel silent: Do Not Disturb set to total silence (when permitted),
     * every sound stream muted, and no click sounds or haptics from this app's own views.
     * Safe to call repeatedly; it only changes what isn't already silent.
     */
    fun enforceSilence(activity: Activity) {
        activity.window.decorView.apply {
            isSoundEffectsEnabled = false
            isHapticFeedbackEnabled = false
        }
        val notifications = activity.getSystemService(NotificationManager::class.java)
        if (notifications?.isNotificationPolicyAccessGranted == true &&
            notifications.currentInterruptionFilter != NotificationManager.INTERRUPTION_FILTER_NONE
        ) {
            notifications.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_NONE)
        }
        val audio = activity.getSystemService(AudioManager::class.java) ?: return
        for (stream in MUTED_STREAMS) {
            // Muting the ring stream needs Do Not Disturb access; skip it rather than crash
            if (!audio.isStreamMute(stream)) {
                runCatching { audio.adjustStreamVolume(stream, AudioManager.ADJUST_MUTE, 0) }
            }
        }
    }

    /** Undoes [enforceSilence] when simple mode is turned off. */
    fun releaseSilence(activity: Activity) {
        activity.window.decorView.apply {
            isSoundEffectsEnabled = true
            isHapticFeedbackEnabled = true
        }
        val notifications = activity.getSystemService(NotificationManager::class.java)
        if (notifications?.isNotificationPolicyAccessGranted == true &&
            notifications.currentInterruptionFilter == NotificationManager.INTERRUPTION_FILTER_NONE
        ) {
            notifications.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL)
        }
        val audio = activity.getSystemService(AudioManager::class.java) ?: return
        for (stream in MUTED_STREAMS) {
            runCatching { audio.adjustStreamVolume(stream, AudioManager.ADJUST_UNMUTE, 0) }
        }
    }
}
