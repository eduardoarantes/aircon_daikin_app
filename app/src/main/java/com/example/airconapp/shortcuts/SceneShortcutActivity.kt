package com.example.airconapp.shortcuts

import android.app.Activity
import android.os.Bundle
import android.widget.Toast
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.example.airconapp.di.NetworkModule
import com.example.airconapp.worker.SceneWorker

/**
 * Opened by a scene shortcut or the "Turn off" shortcut. It has no screen: it hands the scene to [SceneWorker] (so the
 * requests finish even after this closes) and leaves the user where they were.
 */
class SceneShortcutActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val authorized = ShortcutAuthorization(
            getSharedPreferences(ShortcutAuthorization.PREFS_NAME, MODE_PRIVATE)
        ).accepts(intent?.getStringExtra(ShortcutAuthorization.EXTRA_TOKEN))
        if (!authorized || intent?.action !in setOf(SceneShortcuts.ACTION_TURN_OFF, SceneShortcuts.ACTION_APPLY_SCENE)) {
            finish()
            return
        }
        NetworkModule.initialize(applicationContext)
        if (intent?.action == SceneShortcuts.ACTION_TURN_OFF) {
            WorkManager.getInstance(applicationContext).enqueue(
                OneTimeWorkRequestBuilder<SceneWorker>()
                    .addTag(SceneShortcuts.WORK_TAG)
                    .setInputData(workDataOf(SceneWorker.KEY_TURN_OFF to true))
                    .build()
            )
            Toast.makeText(this, "Turning the aircon off…", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        val sceneId = intent?.getStringExtra(SceneShortcuts.EXTRA_SCENE_ID)
        val scene = sceneId?.let { NetworkModule.sceneRepository.get(it) }
        if (scene == null) {
            Toast.makeText(this, "This scene no longer exists", Toast.LENGTH_SHORT).show()
        } else {
            WorkManager.getInstance(applicationContext).enqueue(
                OneTimeWorkRequestBuilder<SceneWorker>()
                    .addTag(SceneShortcuts.WORK_TAG)
                    .setInputData(workDataOf(SceneWorker.KEY_SCENE_ID to scene.id))
                    .build()
            )
            Toast.makeText(this, "Starting ${scene.name}…", Toast.LENGTH_SHORT).show()
        }
        finish()
    }
}
