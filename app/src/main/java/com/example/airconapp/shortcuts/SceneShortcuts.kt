package com.example.airconapp.shortcuts

import android.content.Context
import android.content.Intent
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.example.airconapp.R
import com.example.airconapp.domain.Scene

/** The scenes offered when the app icon is long-pressed in the launcher. */
object SceneShortcuts {

    const val ACTION_APPLY_SCENE = "com.example.airconapp.action.APPLY_SCENE"
    const val EXTRA_SCENE_ID = "scene_id"

    /**
     * Launchers show only the first few (usually four); the order matches the Scenes tab.
     * Pinned shortcuts (dragged onto the home screen) are updated too.
     */
    fun publish(context: Context, scenes: List<Scene>) {
        val max = ShortcutManagerCompat.getMaxShortcutCountPerActivity(context).coerceAtLeast(1)
        val shortcuts = scenes.take(max).mapIndexed { rank, scene -> shortcut(context, scene, rank) }
        runCatching {
            ShortcutManagerCompat.setDynamicShortcuts(context, shortcuts)
            // A pinned shortcut whose scene dropped out of the top list still gets its new name
            ShortcutManagerCompat.updateShortcuts(context, scenes.drop(max).map { shortcut(context, it, 0) })
        }
    }

    /** A deleted scene: its pinned shortcut stays on the home screen but greyed out. */
    fun disable(context: Context, sceneId: String) {
        runCatching { ShortcutManagerCompat.disableShortcuts(context, listOf(sceneId), "This scene was deleted") }
    }

    private fun shortcut(context: Context, scene: Scene, rank: Int): ShortcutInfoCompat {
        val intent = Intent(context, SceneShortcutActivity::class.java)
            .setAction(ACTION_APPLY_SCENE)
            .putExtra(EXTRA_SCENE_ID, scene.id)
        return ShortcutInfoCompat.Builder(context, scene.id)
            .setShortLabel(scene.name.take(25))
            .setLongLabel(scene.name)
            .setIcon(IconCompat.createWithResource(context, iconFor(scene.mode)))
            .setIntent(intent)
            .setRank(rank)
            .build()
    }

    private fun iconFor(mode: String): Int = when (mode) {
        "1" -> R.drawable.ic_shortcut_heat
        "0" -> R.drawable.ic_shortcut_fan
        "7" -> R.drawable.ic_shortcut_dry
        "3" -> R.drawable.ic_shortcut_auto
        else -> R.drawable.ic_shortcut_cool
    }
}
