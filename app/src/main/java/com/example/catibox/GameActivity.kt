package com.example.catibox

import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class GameActivity : AppCompatActivity() {

    private lateinit var gameView: GameView
    private lateinit var backgroundMusic: MediaPlayer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Creamos GameView
        gameView = GameView(this)

        // Configuramos música de fondo
        backgroundMusic = MediaPlayer.create(this, R.raw.background_music) // reemplaza con tu música
        backgroundMusic.isLooping = true
        backgroundMusic.start()

        // Pasamos la música al GameView
        gameView.setBackgroundPlayer(backgroundMusic)

        setContentView(gameView)

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

    override fun onPause() {
        super.onPause()
        gameView.pauseThread()
        backgroundMusic.pause()
    }

    override fun onResume() {
        super.onResume()
        backgroundMusic.start()
        gameView.resumeThread()
    }

    override fun onDestroy() {
        super.onDestroy()
        backgroundMusic.release()
    }

    // --- Función para guardar score máximo y racha máxima ---
    private fun saveHighScores(score: Int, maxStreak: Int) {
        val prefs = getSharedPreferences("CATIBOX_PREFS", Context.MODE_PRIVATE)
        val editor = prefs.edit()

        val highestScore = prefs.getInt("HIGH_SCORE", 0)
        if (score > highestScore) editor.putInt("HIGH_SCORE", score)

        val highestStreak = prefs.getInt("HIGH_STREAK", 0)
        if (maxStreak > highestStreak) editor.putInt("HIGH_STREAK", maxStreak)

        editor.apply()
    }
}
