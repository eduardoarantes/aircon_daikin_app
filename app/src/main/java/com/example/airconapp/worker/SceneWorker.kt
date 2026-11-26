package com.example.airconapp.worker

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.airconapp.data.SceneRunner
import com.example.airconapp.di.NetworkModule

/** Applies a scene (or turns the aircon off) from a launcher shortcut, then says whether it worked. */
class SceneWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        NetworkModule.initialize(applicationContext)
        if (inputData.getBoolean(KEY_TURN_OFF, false)) {
            return try {
                SceneRunner.turnOff()
                toast("Aircon is off")
                Result.success()
            } catch (e: Exception) {
                Log.e("SceneWorker", "Failed to turn the aircon off: ${e.message}")
                toast("Couldn't reach the aircon to turn it off")
                Result.failure()
            }
        }
        val sceneId = inputData.getString(KEY_SCENE_ID) ?: return Result.failure()
        val scene = NetworkModule.sceneRepository.get(sceneId) ?: return Result.failure()
        return try {
            SceneRunner.apply(scene)
            toast("${scene.name} is on")
            Result.success()
        } catch (e: Exception) {
            Log.e("SceneWorker", "Failed to apply scene $sceneId: ${e.message}")
            // A shortcut is a "now" action: retrying minutes later would surprise the user
            toast("Couldn't reach the aircon for ${scene.name}")
            Result.failure()
        }
    }

    private fun toast(message: String) {
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(applicationContext, message, Toast.LENGTH_SHORT).show()
        }
    }

    companion object {
        const val KEY_SCENE_ID = "SCENE_ID"
        const val KEY_TURN_OFF = "TURN_OFF"
    }
}
