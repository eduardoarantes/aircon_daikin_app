package com.example.airconapp.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40

    /* Other default colors to override
    background = Color(0xFFFFFBFE),
    surface = Color(0xFFFFFBFE),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF1C1B1F),
    */
)

/**
 * Set by screens that want black system bars (the simple mode screen). The theme owns the
 * bar colours, so it must decide here; otherwise its recompositions repaint them purple.
 */
object SystemBarsOverride {
    var black by mutableStateOf(false)
}

/** Replaces the light scheme's near-white surfaces with greys; accents are kept. */
private fun ColorScheme.withGreySurfaces() = copy(
    background = GreyBackground,
    surface = GreyBackground,
    surfaceBright = GreyBackground,
    surfaceDim = GreyContainer,
    surfaceVariant = GreyContainer,
    surfaceContainerLowest = GreyBackground,
    surfaceContainerLow = GreyBackground,
    surfaceContainer = GreyContainer,
    surfaceContainerHigh = GreyContainer,
    surfaceContainerHighest = GreyContainerHigh
)

@Composable
fun AirconAppTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val colorScheme = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context).withGreySurfaces()
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme.withGreySurfaces()
    }
    val view = LocalView.current
    val blackBars = SystemBarsOverride.black
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = if (blackBars) Color.Black.toArgb() else colorScheme.primary.toArgb()
            window.navigationBarColor = Color.Black.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = darkTheme && !blackBars
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
