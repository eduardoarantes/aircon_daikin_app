package com.example.airconapp.ui.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

/** One full set of design tokens; the app has a dark and a light one. */
data class AirconPalette(
    val isDark: Boolean,
    val background: Color,
    val surface: Color,
    val surfaceAlt: Color,
    val outline: Color,
    val divider: Color,
    val ringTrack: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    /** Text and icons drawn on an accent fill. */
    val onAccent: Color,
    val connected: Color,
    val powerOff: Color,
    /** Border of the power button while the aircon is off. */
    val powerOffBorder: Color,
    val disabled: Color,
    val activeBorder: Color,
    val switchKnobOff: Color,
    val offText: Color,
    val discTop: Color,
    val discBottom: Color,
    /** The dial's −/+ segments, and while pressed. */
    val segment: Color,
    val segmentPressed: Color,
    /** Shadow under the dial's drag handle, and the dot in its centre. */
    val handleShadow: Color,
    val handleKnob: Color,
    val cool: Color,
    val heat: Color,
    val fan: Color,
    val dry: Color,
    val auto: Color
)

val DarkPalette = AirconPalette(
    isDark = true,
    background = Color(0xFF111316),
    surface = Color(0xFF1A1D21),
    surfaceAlt = Color(0xFF23272D),
    outline = Color(0xFF2C3138),
    divider = Color(0xFF1F2328),
    ringTrack = Color(0xFF262A30),
    textPrimary = Color(0xFFF2F4F7),
    textSecondary = Color(0xFFC5CCD4),
    textMuted = Color(0xFF9AA3AE),
    onAccent = Color(0xFF0E1013),
    connected = Color(0xFF3DDC84),
    powerOff = Color(0xFFFF6B6B),
    powerOffBorder = Color(0xFF3A2A2C),
    disabled = Color(0xFF5B636D),
    activeBorder = Color(0xFF2F3A44),
    switchKnobOff = Color(0xFF8A939E),
    offText = Color(0xFF6B737D),
    discTop = Color(0xFF1E2227),
    discBottom = Color(0xFF14171A),
    segment = Color(0xFF31363E),
    segmentPressed = Color(0xFF3E444D),
    handleShadow = Color.Black.copy(alpha = 0.35f),
    handleKnob = Color(0xFFF2F4F7),
    cool = Color(0xFF5CC8FF),
    heat = Color(0xFFFF9A5C),
    fan = Color(0xFF6FE3C9),
    dry = Color(0xFFB59CFF),
    auto = Color(0xFFC7E86A)
)

/**
 * Light counterpart. The mode accents are deeper than the dark ones: they are also used as text
 * on the background, and the dark theme's pastels would be unreadable on white.
 */
val LightPalette = AirconPalette(
    isDark = false,
    background = Color(0xFFF3F5F8),
    surface = Color(0xFFFFFFFF),
    surfaceAlt = Color(0xFFE9EDF1),
    outline = Color(0xFFD3D9E0),
    divider = Color(0xFFE1E6EB),
    ringTrack = Color(0xFFE1E6EB),
    textPrimary = Color(0xFF111316),
    textSecondary = Color(0xFF39414B),
    textMuted = Color(0xFF5D6672),
    onAccent = Color(0xFFFFFFFF),
    connected = Color(0xFF15803D),
    powerOff = Color(0xFFD92D2D),
    powerOffBorder = Color(0xFFF1C9C9),
    disabled = Color(0xFF9AA3AE),
    activeBorder = Color(0xFFB9CCDA),
    switchKnobOff = Color(0xFF8A939E),
    offText = Color(0xFF8A939E),
    discTop = Color(0xFFFFFFFF),
    discBottom = Color(0xFFEDF0F4),
    segment = Color(0xFFE4E8ED),
    segmentPressed = Color(0xFFD5DBE2),
    handleShadow = Color.Black.copy(alpha = 0.18f),
    handleKnob = Color(0xFFFFFFFF),
    cool = Color(0xFF0277BD),
    heat = Color(0xFFD9520B),
    fan = Color(0xFF0F7F71),
    dry = Color(0xFF6D42D9),
    auto = Color(0xFF4D7C0F)
)

/**
 * Design tokens for the aircon UI. [AirconAppTheme] picks the palette from the device's dark
 * theme setting. Reads are snapshot state, so everything drawn with these colours (including
 * outside composition, e.g. in draw lambdas) updates when the setting changes.
 */
object AirconColors {
    var palette by mutableStateOf(DarkPalette)

    val isDark get() = palette.isDark
    val Background get() = palette.background
    val Surface get() = palette.surface
    val SurfaceAlt get() = palette.surfaceAlt
    val Outline get() = palette.outline
    val Divider get() = palette.divider
    val RingTrack get() = palette.ringTrack
    val TextPrimary get() = palette.textPrimary
    val TextSecondary get() = palette.textSecondary
    val TextMuted get() = palette.textMuted
    val OnAccent get() = palette.onAccent
    val Connected get() = palette.connected
    val PowerOff get() = palette.powerOff
    val PowerOffBorder get() = palette.powerOffBorder
    val Disabled get() = palette.disabled

    /** Border of a card whose zone is on. */
    val ActiveBorder get() = palette.activeBorder
    val SwitchKnobOff get() = palette.switchKnobOff
    val OffText get() = palette.offText
    val DiscTop get() = palette.discTop
    val DiscBottom get() = palette.discBottom
    val Segment get() = palette.segment
    val SegmentPressed get() = palette.segmentPressed
    val HandleShadow get() = palette.handleShadow
    val HandleKnob get() = palette.handleKnob

    val Cool get() = palette.cool
    val Heat get() = palette.heat
    val Fan get() = palette.fan
    val Dry get() = palette.dry
    val Auto get() = palette.auto
}
