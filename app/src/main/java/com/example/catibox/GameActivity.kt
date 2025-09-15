package com.example.catibox

import android.annotation.SuppressLint
import android.content.Intent
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import android.content.pm.ActivityInfo
import com.example.catibox.managers.SoundManager
import androidx.core.content.edit


class GameActivity : AppCompatActivity() {

    private lateinit var gameView: GameView
    private lateinit var backgroundMusic: MediaPlayer

    @SuppressLint("SourceLockedOrientationActivity")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT


        // Creamos GameView
        gameView = GameView(this)

        // Configuramos música de fondo
        backgroundMusic = MediaPlayer.create(this, R.raw.background_music)
        backgroundMusic.isLooping = true
        backgroundMusic.start()

        // Pasamos la música al GameView
        gameView.setBackgroundPlayer(backgroundMusic)

        setContentView(gameView)

        // Activar fullscreen
        enableFullscreen()

        // Observamos si termina el juego
        gameView.onGameOverListener = {
            saveHighScores(gameView.score, gameView.maxStreak)

            val intent = Intent(this, GameOverActivity::class.java)
            intent.putExtra("SCORE", gameView.score)
            intent.putExtra("MAX_STREAK", gameView.maxStreak)
            startActivity(intent)
            finish()
        }
    }

    @Suppress("DEPRECATION")
    private fun enableFullscreen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.let { controller ->
                controller.hide(
                    android.view.WindowInsets.Type.statusBars() or
                            android.view.WindowInsets.Type.navigationBars()
                )
                controller.systemBarsBehavior =
                    android.view.WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            window.decorView.systemUiVisibility =
                View.SYSTEM_UI_FLAG_FULLSCREEN or
                        View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                        View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        }
    }

    override fun onPause() {
        super.onPause()
        gameView.pauseThread()
        backgroundMusic.pause()
    }

    override fun onResume() {
        super.onResume()
        backgroundMusic.start()
        gameView.resumeThread()
        enableFullscreen() // <- por si se pierde fullscreen al volver
    }

    override fun onDestroy() {
        super.onDestroy()
        backgroundMusic.release()
        SoundManager.release()

    }

    // --- Función para guardar score máximo y racha máxima ---
    private fun saveHighScores(score: Int, maxStreak: Int) {
        val prefs = getSharedPreferences("CATIBOX_PREFS",MODE_PRIVATE)
        prefs.edit {

            val highestScore = prefs.getInt("HIGH_SCORE", 0)
            if (score > highestScore) putInt("HIGH_SCORE", score)

            val highestStreak = prefs.getInt("HIGH_STREAK", 0)
            if (maxStreak > highestStreak) putInt("HIGH_STREAK", maxStreak)

        }
    }
}
