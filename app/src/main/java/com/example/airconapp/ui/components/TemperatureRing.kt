package com.example.airconapp.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitTouchSlopOrCancellation
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.airconapp.ui.theme.AirconColors
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.math.sin

// Angles below are compass angles: 0° at the top, clockwise. The temperature arc runs from the
// bottom-left (225°) over the top to the bottom-right (135°); the bottom quarter holds − and +.
private const val ARC_START = 225f
private const val ARC_SWEEP = 270f

/** Size of the design's dial; every other measure scales from it. */
private val DESIGN_SIZE = 300.dp

private val SegmentFill = Color(0xFF31363E)
private val SegmentPressed = Color(0xFF3E444D)

private enum class Segment { Minus, Plus }

/** Slider semantics for TalkBack: the set temperature and how to change it. */
class DialSlider(val value: Int, val range: IntRange, val onSet: (Int) -> Unit)

/**
 * The temperature dial: a 270° arc filled to [fraction] in [accent], a drag handle at its end,
 * and − / + segments in the bottom quarter of the same ring.
 *
 * - Dragging from the arc moves the handle; [onDrag] gets the fraction under the finger, then
 *   [onDragEnd] on release or [onDragCancel] if the gesture is taken away. Touches on the inner
 *   disc are left alone so the page can still scroll.
 * - Tapping a bottom segment calls [onDecrease] / [onIncrease] when [stepsEnabled].
 */
@Composable
fun TemperatureRing(
    accent: Color,
    fraction: Float,
    glow: Boolean,
    showHandle: Boolean,
    stepsEnabled: Boolean,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
    modifier: Modifier = Modifier,
    ringSize: Dp = DESIGN_SIZE,
    slider: DialSlider? = null,
    onDrag: ((Float) -> Unit)? = null,
    onDragEnd: () -> Unit = {},
    onDragCancel: () -> Unit = {},
    content: @Composable ColumnScope.() -> Unit
) {
    val k = ringSize / DESIGN_SIZE
    var pressed by remember { mutableStateOf<Segment?>(null) }

    val currentOnDrag by rememberUpdatedState(onDrag)
    val currentOnDragEnd by rememberUpdatedState(onDragEnd)
    val currentOnDragCancel by rememberUpdatedState(onDragCancel)
    val currentOnDecrease by rememberUpdatedState(onDecrease)
    val currentOnIncrease by rememberUpdatedState(onIncrease)
    val currentStepsEnabled by rememberUpdatedState(stepsEnabled)

    val gestures = Modifier.pointerInput(ringSize) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val dragBand = (110.dp * k).toPx()..(175.dp * k).toPx()
        val segmentBand = (100.dp * k).toPx()..(178.dp * k).toPx()
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            val distance = hypot(down.position.x - center.x, down.position.y - center.y)
            val angle = compassAngle(down.position, center)
            if (angle in 135f..225f) {
                // − / + segment: highlight while pressed, act on release
                if (distance !in segmentBand || !currentStepsEnabled) return@awaitEachGesture
                val segment = if (angle >= 180f) Segment.Minus else Segment.Plus
                down.consume()
                pressed = segment
                val up = waitForUpOrCancellation()
                pressed = null
                if (up != null) {
                    up.consume()
                    if (segment == Segment.Minus) currentOnDecrease() else currentOnIncrease()
                }
            } else {
                if (currentOnDrag == null || distance !in dragBand) return@awaitEachGesture
                val start = awaitTouchSlopOrCancellation(down.id) { change, _ -> change.consume() }
                    ?: return@awaitEachGesture
                currentOnDrag?.invoke(arcFraction(start.position, center))
                val released = drag(start.id) { change ->
                    change.consume()
                    currentOnDrag?.invoke(arcFraction(change.position, center))
                }
                if (released) currentOnDragEnd() else currentOnDragCancel()
            }
        }
    }

    Box(modifier = modifier.size(ringSize).then(gestures), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(ringSize)) {
            val band = 34.dp.toPx() * k
            val radius = size.minDimension / 2f
            if (glow) drawGlow(accent, radius, 44.dp.toPx() * k)
            val topLeft = Offset(band / 2f, band / 2f)
            val arcSize = Size(size.width - band, size.height - band)
            fun arc(color: Color, compassStart: Float, sweep: Float) =
                drawArc(color, compassStart - 90f, sweep, false, topLeft, arcSize, style = Stroke(band))
            arc(AirconColors.RingTrack, ARC_START, ARC_SWEEP)
            if (fraction > 0f) arc(accent, ARC_START, ARC_SWEEP * fraction.coerceIn(0f, 1f))
            arc(if (pressed == Segment.Plus) SegmentPressed else SegmentFill, 135f, 45f)
            arc(if (pressed == Segment.Minus) SegmentPressed else SegmentFill, 180f, 45f)
            // Thin cuts so the bottom reads as two buttons
            for (angle in listOf(135f, 180f, 225f)) {
                drawLine(
                    AirconColors.Background,
                    pointAt(center, radius - band - 2.dp.toPx(), angle),
                    pointAt(center, radius + 1f, angle),
                    strokeWidth = 2.dp.toPx() * k
                )
            }
        }

        // Slider semantics for the whole dial (TalkBack can step it up or down)
        if (slider != null) {
            Box(
                Modifier
                    .size(ringSize)
                    .semantics {
                        contentDescription = "Temperature"
                        stateDescription = "${slider.value}°C"
                        progressBarRangeInfo = ProgressBarRangeInfo(
                            slider.value.toFloat(),
                            slider.range.first.toFloat()..slider.range.last.toFloat(),
                            steps = slider.range.last - slider.range.first - 1
                        )
                        setProgress { target ->
                            slider.onSet(target.roundToInt().coerceIn(slider.range))
                            true
                        }
                    }
            )
        }

        Column(
            modifier = Modifier
                .size(ringSize - 68.dp * k)
                .shadow(elevation = 16.dp, shape = CircleShape)
                .drawBehind {
                    drawCircle(
                        Brush.radialGradient(
                            listOf(AirconColors.DiscTop, AirconColors.DiscBottom),
                            center = Offset(size.width / 2f, size.height * 0.35f),
                            radius = size.width * 0.75f
                        )
                    )
                }
                .padding(top = 6.dp * k),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            content = content
        )

        SegmentButton(AirconIcons.Minus, "Lower temperature", 202.5f, k, stepsEnabled, onDecrease)
        SegmentButton(AirconIcons.Plus, "Raise temperature", 157.5f, k, stepsEnabled, onIncrease)

        if (showHandle) {
            Canvas(modifier = Modifier.size(ringSize)) {
                val c = Offset(size.width / 2f, size.height / 2f)
                val position = pointAt(c, 133.dp.toPx() * k, ARC_START + ARC_SWEEP * fraction.coerceIn(0f, 1f))
                val outer = 18.dp.toPx() * k
                drawCircle(Color.Black.copy(alpha = 0.35f), outer + 6.dp.toPx() * k, position + Offset(0f, 4.dp.toPx() * k))
                drawCircle(AirconColors.Background.copy(alpha = 0.55f), outer + 4.dp.toPx() * k, position)
                drawCircle(accent, outer, position)
                drawCircle(AirconColors.TextPrimary, outer - 6.dp.toPx() * k, position)
            }
        }
    }
}

/**
 * The − / + icon in its segment. Taps are handled by the dial's own hit test (the whole
 * segment is the target); this only draws the icon and gives TalkBack a button.
 */
@Composable
private fun SegmentButton(icon: ImageVector, description: String, angle: Float, k: Float, enabled: Boolean, onClick: () -> Unit) {
    val radius = 131.dp * k
    val radians = Math.toRadians(angle.toDouble())
    Box(
        modifier = Modifier
            .offset(x = radius * sin(radians).toFloat(), y = -radius * cos(radians).toFloat())
            .size(48.dp * k)
            .semantics {
                role = Role.Button
                contentDescription = description
                if (enabled) onClick { onClick(); true } else disabled()
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = AirconColors.TextPrimary,
            modifier = Modifier.size(22.dp * k).alpha(if (enabled) 1f else 0.35f)
        )
    }
}

private fun DrawScope.drawGlow(accent: Color, radius: Float, spread: Float) {
    // Soft halo (drawn past the bounds) standing in for a large blur
    val glowRadius = radius + spread
    val edge = radius / glowRadius
    drawCircle(
        brush = Brush.radialGradient(
            0f to accent.copy(alpha = 0.30f),
            edge to accent.copy(alpha = 0.26f),
            edge + (1f - edge) * 0.45f to accent.copy(alpha = 0.08f),
            1f to Color.Transparent,
            center = center,
            radius = glowRadius
        ),
        radius = glowRadius
    )
    // Keep the halo outside the ring
    drawCircle(AirconColors.Background, radius = radius)
}

private fun pointAt(center: Offset, radius: Float, compassDegrees: Float): Offset {
    val radians = Math.toRadians(compassDegrees.toDouble())
    return Offset(center.x + radius * sin(radians).toFloat(), center.y - radius * cos(radians).toFloat())
}

private fun compassAngle(position: Offset, center: Offset): Float {
    val degrees = Math.toDegrees(atan2((position.x - center.x).toDouble(), (center.y - position.y).toDouble())).toFloat()
    return (degrees + 360f) % 360f
}

/** Where [position] falls along the arc: 0 at the bottom-left start, 1 at the bottom-right end. */
private fun arcFraction(position: Offset, center: Offset): Float {
    val along = (compassAngle(position, center) - ARC_START + 360f) % 360f
    // In the bottom gap: snap to whichever end is closer
    if (along > ARC_SWEEP) return if (along < ARC_SWEEP + (360f - ARC_SWEEP) / 2f) 1f else 0f
    return along / ARC_SWEEP
}
