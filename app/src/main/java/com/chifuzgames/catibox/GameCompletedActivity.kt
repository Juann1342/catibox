package com.chifuzgames.catibox

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class GameCompletedActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_game_completed)

        // Ajuste de insets
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Recibir datos del intent
        val score = intent.getIntExtra("SCORE", 0)
        val maxStreak = intent.getIntExtra("MAX_STREAK", 0)
        val level = intent.getIntExtra("LEVEL", 1)
        val newHighScore = intent.getBooleanExtra("NEW_HIGH_SCORE", false)
        val newHighStreak = intent.getBooleanExtra("NEW_HIGH_STREAK", false)

        // Mostrar en layout
        findViewById<TextView>(R.id.tvScore).text = "Score: $score"
        findViewById<TextView>(R.id.tvMaxStreak).text = "Max Streak: $maxStreak"

        if (newHighScore) {
            findViewById<TextView>(R.id.tvHighScore).visibility = View.VISIBLE
        }
        if (newHighStreak) {
            findViewById<TextView>(R.id.tvHighStreak).visibility = View.VISIBLE
        }
    }
}

