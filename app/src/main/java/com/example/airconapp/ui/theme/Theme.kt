package com.example.airconapp.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
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

/** Dark only for now. Material components on the older screens pick these up. */
private val AirconColorScheme = darkColorScheme(
    primary = AirconColors.Cool,
    onPrimary = AirconColors.OnAccent,
    primaryContainer = AirconColors.SurfaceAlt,
    onPrimaryContainer = AirconColors.TextPrimary,
    secondary = AirconColors.Cool,
    onSecondary = AirconColors.OnAccent,
    secondaryContainer = AirconColors.SurfaceAlt,
    onSecondaryContainer = AirconColors.TextPrimary,
    tertiary = AirconColors.Fan,
    onTertiary = AirconColors.OnAccent,
    background = AirconColors.Background,
    onBackground = AirconColors.TextPrimary,
    surface = AirconColors.Background,
    onSurface = AirconColors.TextPrimary,
    surfaceVariant = AirconColors.Surface,
    onSurfaceVariant = AirconColors.TextMuted,
    surfaceContainerLowest = AirconColors.Background,
    surfaceContainerLow = AirconColors.Surface,
    surfaceContainer = AirconColors.Surface,
    surfaceContainerHigh = AirconColors.SurfaceAlt,
    surfaceContainerHighest = AirconColors.SurfaceAlt,
    surfaceBright = AirconColors.SurfaceAlt,
    surfaceDim = AirconColors.Background,
    outline = AirconColors.Outline,
    outlineVariant = AirconColors.Divider,
    error = AirconColors.PowerOff,
    onError = AirconColors.OnAccent,
    inverseSurface = AirconColors.TextPrimary,
    inverseOnSurface = AirconColors.Background
)

/**
 * Set by screens that want black system bars (the simple mode screen). The theme owns the
 * bar colours, so it must decide here; otherwise its recompositions repaint them.
 */
object SystemBarsOverride {
    var black by mutableStateOf(false)
}

@Composable
fun AirconAppTheme(content: @Composable () -> Unit) {
    val view = LocalView.current
    val blackBars = SystemBarsOverride.black
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            val barColor = if (blackBars) Color.Black else AirconColors.Background
            window.statusBarColor = barColor.toArgb()
            window.navigationBarColor = barColor.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = false
                isAppearanceLightNavigationBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = AirconColorScheme,
        typography = Typography,
        content = content
    )
}
