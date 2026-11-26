package com.example.airconapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.airconapp.domain.Scene
import com.example.airconapp.ui.components.AirconIcons
import com.example.airconapp.ui.theme.AirconColors

/** How many scenes launchers typically show when the app icon is long-pressed, after "Turn off". */
private const val SHORTCUTS_SHOWN = 3

/** The scenes tab: tap a scene to run it; the first few also appear on the app icon's long-press menu. */
@Composable
fun ScenesScreen(
    scenes: List<Scene>,
    onApply: (Scene) -> Unit,
    onAdd: () -> Unit,
    onEdit: (Scene) -> Unit,
    onMoveUp: (Scene) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Column(
                    modifier = Modifier.padding(start = 8.dp, top = 24.dp, bottom = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text("Scenes", fontSize = 24.sp, fontWeight = FontWeight.SemiBold, color = AirconColors.TextPrimary)
                    Text(
                        "Tap to run. Long-press the app icon for Turn off and the first $SHORTCUTS_SHOWN.",
                        fontSize = 13.sp,
                        color = AirconColors.TextMuted
                    )
                }
            }
            if (scenes.isEmpty()) item { EmptyScenes() }
            itemsIndexed(scenes, key = { _, scene -> scene.id }) { index, scene ->
                SceneCard(
                    scene = scene,
                    inShortcuts = index < SHORTCUTS_SHOWN,
                    canMoveUp = index > 0,
                    onApply = { onApply(scene) },
                    onEdit = { onEdit(scene) },
                    onMoveUp = { onMoveUp(scene) }
                )
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = 20.dp)
                .size(60.dp)
                .shadow(16.dp, RoundedCornerShape(20.dp), ambientColor = AirconColors.Cool, spotColor = AirconColors.Cool)
                .background(AirconColors.Cool, RoundedCornerShape(20.dp))
                .clip(RoundedCornerShape(20.dp))
                .clickable(role = Role.Button, onClick = onAdd)
                .semantics { contentDescription = "Add scene" },
            contentAlignment = Alignment.Center
        ) {
            Icon(AirconIcons.Plus, contentDescription = null, tint = AirconColors.OnAccent, modifier = Modifier.size(26.dp))
        }
    }
}

@Composable
private fun SceneCard(
    scene: Scene,
    inShortcuts: Boolean,
    canMoveUp: Boolean,
    onApply: () -> Unit,
    onEdit: () -> Unit,
    onMoveUp: () -> Unit
) {
    val mode = AirconMode.of(scene.mode)
    val accent = mode?.accent ?: AirconColors.TextMuted
    val shape = RoundedCornerShape(20.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(AirconColors.Surface, shape)
            .border(1.dp, AirconColors.SurfaceAlt, shape)
            .padding(start = 12.dp, end = 4.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(14.dp))
                .clickable(role = Role.Button, onClick = onApply)
                .semantics(mergeDescendants = true) { contentDescription = "Run scene ${scene.name}" }
                .padding(4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier.size(48.dp).background(accent, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(mode?.icon ?: AirconIcons.Scene, contentDescription = null, tint = AirconColors.OnAccent, modifier = Modifier.size(24.dp))
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(scene.name, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = AirconColors.TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                val settings = listOfNotNull(
                    mode?.label,
                    if (mode == AirconMode.FAN) null else "${scene.temperature}°C",
                    "Fan ${scene.fan.shortLabel}"
                ).joinToString(" · ")
                Text(settings, fontSize = 13.sp, color = accent, fontWeight = FontWeight.SemiBold)
                Text(
                    scene.zones.joinToString(", "),
                    fontSize = 13.sp,
                    color = AirconColors.TextMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (inShortcuts) {
                    Text("On the app icon menu", fontSize = 11.sp, color = AirconColors.TextMuted)
                }
            }
        }
        if (canMoveUp) {
            SmallIconButton(AirconIcons.ArrowUp, "Move ${scene.name} up", onMoveUp)
        }
        SmallIconButton(AirconIcons.Edit, "Edit ${scene.name}", onEdit)
    }
}

@Composable
private fun SmallIconButton(icon: ImageVector, description: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = AirconColors.TextMuted, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun EmptyScenes() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .drawBehind {
                drawRoundRect(
                    color = AirconColors.Outline,
                    cornerRadius = CornerRadius(20.dp.toPx()),
                    style = Stroke(width = 1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 4.dp.toPx())))
                )
            }
            .padding(horizontal = 24.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("No scenes yet", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = AirconColors.TextPrimary)
        Text(
            "Tap + to save zones, mode and temperature together, e.g. \"Bedroom · Cool 21°\". " +
                "Scenes also appear when you long-press the app icon.",
            fontSize = 13.sp,
            color = AirconColors.TextMuted,
            textAlign = TextAlign.Center
        )
    }
}
