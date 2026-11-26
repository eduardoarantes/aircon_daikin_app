package com.example.airconapp.data.repo

import android.content.Context
import com.example.airconapp.domain.Scene
import com.example.airconapp.shortcuts.SceneShortcuts
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Saved scenes, kept as JSON in shared preferences (a handful of small records). Every change
 * also refreshes the launcher shortcuts so they always match the list.
 */
class SceneRepository(context: Context) {

    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()

    private val _scenes = MutableStateFlow(load())
    val scenes: StateFlow<List<Scene>> = _scenes

    fun get(id: String): Scene? = _scenes.value.firstOrNull { it.id == id }

    /** Adds [scene], or replaces the saved scene with the same id. */
    fun save(scene: Scene) {
        val list = _scenes.value
        val index = list.indexOfFirst { it.id == scene.id }
        store(if (index < 0) list + scene else list.toMutableList().apply { this[index] = scene })
    }

    fun delete(scene: Scene) {
        store(_scenes.value.filterNot { it.id == scene.id })
        SceneShortcuts.disable(appContext, scene.id)
    }

    /** Moves [scene] one place up the list; the top scenes are the ones the launcher shows. */
    fun moveUp(scene: Scene) {
        val list = _scenes.value.toMutableList()
        val index = list.indexOfFirst { it.id == scene.id }
        if (index <= 0) return
        list.add(index - 1, list.removeAt(index))
        store(list)
    }

    /** Re-publishes the shortcuts, e.g. on app start in case the launcher dropped them. */
    fun refreshShortcuts() = SceneShortcuts.publish(appContext, _scenes.value)

    private fun store(scenes: List<Scene>) {
        prefs.edit().putString(KEY_SCENES, gson.toJson(scenes)).apply()
        _scenes.value = scenes
        refreshShortcuts()
    }

    private fun load(): List<Scene> {
        val json = prefs.getString(KEY_SCENES, null) ?: return emptyList()
        val type = object : TypeToken<List<Scene>>() {}.type
        return runCatching { gson.fromJson<List<Scene>>(json, type) }.getOrNull().orEmpty()
            // Gson can leave fields null on records written by an older version: drop those
            .filter { it.id != null && it.name != null && it.mode != null && it.fan != null && it.zones != null }
    }

    private companion object {
        const val PREFS_NAME = "aircon_scenes"
        const val KEY_SCENES = "scenes"
    }
}
