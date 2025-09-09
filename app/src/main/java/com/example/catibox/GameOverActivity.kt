package com.example.catibox

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity

class GameOverActivity : AppCompatActivity() {
    private var gameOverSoundId = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game_over)

        // Obtener el puntaje y la racha desde el Intent
        val score = intent.getIntExtra("SCORE", 0)
        val maxStreak = intent.getIntExtra("MAX_STREAK", 0)

        // Obtener valores históricos desde SharedPreferences
        val prefs = getSharedPreferences("CATIBOX_PREFS", Context.MODE_PRIVATE)
        val highScore = prefs.getInt("MAX_SCORE_HIST", 0)
        val highStreak = prefs.getInt("MAX_STREAK_HIST", 0)

        // Mostrar puntaje actual
        findViewById<TextView>(R.id.scoreTextView).text = "Score: $score"

        // Mostrar racha actual
        findViewById<TextView>(R.id.streakTextView).text = "Max Streak: $maxStreak"

        // Mostrar récord histórico
        findViewById<TextView>(R.id.highScoreTextView).text = "High Score: $highScore"
        findViewById<TextView>(R.id.highStreakTextView).text = "Longest Streak: $highStreak"

        val extraMessageScore = findViewById<TextView>(R.id.extraMessageScoreTextView)
        val extraMessageStreak = findViewById<TextView>(R.id.extraMessageStreakTextView)
//recibo las flags de si son nuevos records o no
        val newHighScoreFlag = intent.getBooleanExtra("NEW_HIGH_SCORE", false)
        val newHighStreakFlag = intent.getBooleanExtra("NEW_HIGH_STREAK", false)

        // Chequear récords
        if (newHighScoreFlag) {
            extraMessageScore.text = "🎉 New High Score!"
            extraMessageScore.visibility = View.VISIBLE
        }

        if (newHighStreakFlag) {
            extraMessageStreak.text = "🔥 New Longest Streak!"
            extraMessageStreak.visibility = View.VISIBLE
        }

        // Botón volver a jugar
        findViewById<Button>(R.id.playAgainButton).setOnClickListener {
            val intent = Intent(this, GameActivity::class.java)
            startActivity(intent)
            finish()
        }

        // Bloquear botón "back"
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val intent = Intent(this@GameOverActivity, MainMenuActivity::class.java)
                startActivity(intent)
                finish()
            }
        })
    }

}
