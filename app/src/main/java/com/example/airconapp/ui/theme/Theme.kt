package com.example.airconapp.ui.theme

import android.app.Activity
import android.app.UiModeManager
import android.content.Context
import android.os.Build
import com.example.airconapp.di.NetworkModule
import com.example.airconapp.domain.ThemeMode
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.remember
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/** Material colours for the older screens (Settings, dialogs), built from the app's palette. */
private fun colorScheme(p: AirconPalette): ColorScheme {
    val base = if (p.isDark) darkColorScheme() else lightColorScheme()
    return base.copy(
        primary = p.cool,
        onPrimary = p.onAccent,
        primaryContainer = p.surfaceAlt,
        onPrimaryContainer = p.textPrimary,
        secondary = p.cool,
        onSecondary = p.onAccent,
        secondaryContainer = p.surfaceAlt,
        onSecondaryContainer = p.textPrimary,
        tertiary = p.fan,
        onTertiary = p.onAccent,
        background = p.background,
        onBackground = p.textPrimary,
        surface = p.background,
        onSurface = p.textPrimary,
        surfaceVariant = p.surface,
        onSurfaceVariant = p.textMuted,
        surfaceContainerLowest = p.background,
        surfaceContainerLow = p.surface,
        surfaceContainer = p.surface,
        surfaceContainerHigh = p.surfaceAlt,
        surfaceContainerHighest = p.surfaceAlt,
        surfaceBright = p.surfaceAlt,
        surfaceDim = p.background,
        outline = p.outline,
        outlineVariant = p.divider,
        error = p.powerOff,
        onError = p.onAccent,
        inverseSurface = p.textPrimary,
        inverseOnSurface = p.background
    )
}

/**
 * Set by screens that want black system bars (the simple mode screen). The theme owns the
 * bar colours, so it must decide here; otherwise its recompositions repaint them.
 */
object SystemBarsOverride {
    var black by mutableStateOf(false)
}

/**
 * The theme chosen in Settings. Snapshot state, so changing it re-themes the open screens at
 * once. Loaded from preferences with [load] when the app starts.
 */
object ThemeSetting {
    var mode by mutableStateOf(ThemeMode.SYSTEM)
        private set

    fun load(context: Context) = apply(context, NetworkModule.getThemeMode(), save = false)

    fun set(context: Context, mode: ThemeMode) = apply(context, mode, save = true)

    private fun apply(context: Context, newMode: ThemeMode, save: Boolean) {
        mode = newMode
        if (save) NetworkModule.setThemeMode(newMode)
        // Android 12+: tell the system too, so the splash screen at the next launch matches
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(UiModeManager::class.java)?.setApplicationNightMode(
                when (newMode) {
                    ThemeMode.SYSTEM -> UiModeManager.MODE_NIGHT_AUTO
                    ThemeMode.LIGHT -> UiModeManager.MODE_NIGHT_NO
                    ThemeMode.DARK -> UiModeManager.MODE_NIGHT_YES
                }
            )
        }
    }
}

/** Light or dark as chosen in Settings; "System" follows the device and switches live. */
@Composable
fun AirconAppTheme(
    darkTheme: Boolean = when (ThemeSetting.mode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    },
    content: @Composable () -> Unit
) {
    val palette = if (darkTheme) DarkPalette else LightPalette
    // Set before the content reads it, so the first frame already has the right colours
    AirconColors.palette = palette
    val scheme = remember(palette) { colorScheme(palette) }
    val view = LocalView.current
    val blackBars = SystemBarsOverride.black
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            val barColor = if (blackBars) Color.Black else palette.background
            window.statusBarColor = barColor.toArgb()
            window.navigationBarColor = barColor.toArgb()
            // Dark bar icons on the light theme's light bars
            val lightBars = !blackBars && !palette.isDark
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = lightBars
                isAppearanceLightNavigationBars = lightBars
            }
        }
    }

    MaterialTheme(
        colorScheme = scheme,
        typography = Typography,
        content = content
    )
}
