package com.example.catibox

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.widget.Button
import android.widget.TextView

class MainMenuActivity : AppCompatActivity() {
    private lateinit var tvHighScore: TextView
    private lateinit var tvLongestStreak: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_menu)

        // Inicializamos los TextView
        tvHighScore = findViewById(R.id.tvYourHighScore)
        tvLongestStreak = findViewById(R.id.tvYourLongestStreak)

        // Botón para iniciar el juego
        val startButton: Button = findViewById(R.id.startButton)
        startButton.setOnClickListener {
            val intent = Intent(this, GameActivity::class.java)
            startActivity(intent)
        }

        // Mostrar scores al iniciar
        updateScores()
    }

    override fun onResume() {
        super.onResume()
        // Se llama cuando volvemos del GameActivity
        updateScores()
    }

    private fun updateScores() {
        val prefs = getSharedPreferences("CATIBOX_PREFS", MODE_PRIVATE)
        val highScore = prefs.getInt("MAX_SCORE_HIST", 0)
        val highStreak = prefs.getInt("MAX_STREAK_HIST", 0)
        tvHighScore.text = "\uD83D\uDE80  Your High Score $highScore"
        tvLongestStreak.text = "🔥 Your Longest Streak $highStreak"
    }
}
