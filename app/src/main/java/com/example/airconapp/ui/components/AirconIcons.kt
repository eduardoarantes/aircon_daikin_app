package com.example.airconapp.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/** Outline icons from the design (24×24 viewport, round caps). Tint them with Icon's `tint`. */
object AirconIcons {

    val Power = icon("Power", 2.2f, "M12 3v8", "M6.3 6.8a8 8 0 1 0 11.4 0")

    val ModeAuto = icon(
        "ModeAuto", 1.8f,
        "M4.5 12a7.5 7.5 0 0 1 13-5.1", "M19.5 12a7.5 7.5 0 0 1-13 5.1", "M18 3.5v3.6h-3.6",
        "M6 20.5v-3.6h3.6", "M9.6 14.6L12 8.6l2.4 6", "M10.4 12.8h3.2"
    )

    val ModeCool = icon(
        "ModeCool", 1.8f,
        "M12 2.5v19", "M3.8 7.25l16.4 9.5", "M20.2 7.25l-16.4 9.5", "M9.3 4.3L12 7l2.7-2.7", "M9.3 19.7L12 17l2.7 2.7"
    )

    val ModeHeat = icon(
        "ModeHeat", 1.8f,
        circle(12f, 12f, 4f),
        "M12 2.5v2M12 19.5v2M5.3 5.3l1.4 1.4M17.3 17.3l1.4 1.4M2.5 12h2M19.5 12h2M5.3 18.7l1.4-1.4M17.3 6.7l1.4-1.4"
    )

    val ModeFan = icon(
        "ModeFan", 1.8f,
        circle(12f, 12f, 1.6f),
        "M12 10.4c-.6-3.2.2-6.9 2.8-6.9 2.4 0 2.6 3.6-1.3 6.9",
        "M13.6 12c3.2-.6 6.9.2 6.9 2.8 0 2.4-3.6 2.6-6.9-1.3",
        "M12 13.6c.6 3.2-.2 6.9-2.8 6.9-2.4 0-2.6-3.6 1.3-6.9",
        "M10.4 12c-3.2.6-6.9-.2-6.9-2.8 0-2.4 3.6-2.6 6.9 1.3"
    )

    val ModeDry = icon("ModeDry", 1.8f, "M12 3c3.6 4.3 6 7.7 6 11a6 6 0 0 1-12 0c0-3.3 2.4-6.7 6-11z", "M9.2 14.5a2.8 2.8 0 0 0 2.8 2.8")

    val House = icon("House", 1.8f, "M3 11l9-7 9 7v9a1 1 0 0 1-1 1h-5v-6h-6v6H4a1 1 0 0 1-1-1z")

    val Minus = icon("Minus", 2.2f, "M5 12h14")

    val Plus = icon("Plus", 2.2f, "M5 12h14M12 5v14")

    val ChevronRight = icon("ChevronRight", 2f, "M9 6l6 6-6 6")

    val Trash = icon("Trash", 1.8f, "M4 7h16M10 11v6M14 11v6M6 7l1 12a2 2 0 0 0 2 2h6a2 2 0 0 0 2-2l1-12M9 7V4h6v3")

    val Close = icon("Close", 2f, "M6 6l12 12M18 6L6 18")

    val Edit = icon("Edit", 1.8f, "M4 20h4L19 9l-4-4L4 16z", "M13.5 6.5l4 4")

    val ArrowUp = icon("ArrowUp", 2f, "M12 19V5M6 11l6-6 6 6")

    /** A wand: a scene sets several things at once. */
    val Scene = icon("Scene", 1.8f, "M4 20L15 9", "M18 3v4M16 5h4M19.5 10.5v3M18 12h3M10 2.5v3M8.5 4h3")

    val Check = icon("Check", 2.6f, "M5 12.5l4.5 4.5L19 7.5")

    val NavControls = icon(
        "NavControls", 1.8f,
        "M4 7h10M18 7h2M4 17h2M10 17h10", circle(16f, 7f, 2f), circle(8f, 17f, 2f)
    )

    val NavZones = icon(
        "NavZones", 1.8f,
        roundedRect(3f, 3f, 7.5f, 7.5f, 2f), roundedRect(13.5f, 3f, 7.5f, 7.5f, 2f),
        roundedRect(3f, 13.5f, 7.5f, 7.5f, 2f), roundedRect(13.5f, 13.5f, 7.5f, 7.5f, 2f)
    )

    val Calendar = icon("Calendar", 1.8f, roundedRect(3f, 5f, 18f, 16f, 2f), "M3 10h18M8 3v4M16 3v4")

    val Settings = icon(
        "Settings", 1.8f,
        circle(12f, 12f, 3f),
        "M12 2.5v2.2M12 19.3v2.2M4.6 4.6l1.6 1.6M17.8 17.8l1.6 1.6M2.5 12h2.2M19.3 12h2.2M4.6 19.4l1.6-1.6M17.8 6.2l1.6-1.6",
        circle(12f, 12f, 7f)
    )

    private fun icon(name: String, strokeWidth: Float, vararg paths: String): ImageVector =
        ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f).apply {
            paths.forEach {
                addPath(
                    pathData = addPathNodes(it),
                    stroke = SolidColor(Color.Black),
                    strokeLineWidth = strokeWidth,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round
                )
            }
        }.build()

    private fun circle(cx: Float, cy: Float, r: Float) =
        "M${cx - r} ${cy}a$r $r 0 1 0 ${2 * r} 0a$r $r 0 1 0 ${-2 * r} 0z"

    private fun roundedRect(x: Float, y: Float, w: Float, h: Float, r: Float) =
        "M${x + r} ${y}h${w - 2 * r}a$r $r 0 0 1 $r ${r}v${h - 2 * r}a$r $r 0 0 1 ${-r} ${r}" +
            "h${-(w - 2 * r)}a$r $r 0 0 1 ${-r} ${-r}v${-(h - 2 * r)}a$r $r 0 0 1 $r ${-r}z"
}
