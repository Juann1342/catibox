package com.chifuzgames.catibox

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.chifuzgames.catibox.managers.PhraseManager

class GameCompletedActivity : AppCompatActivity() {
    private lateinit var tvPhraseWin: TextView

    val phraseManager = PhraseManager(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_game_completed)

        tvPhraseWin = findViewById(R.id.tvPhraseWin)
        updatePhrase()

        // Ajuste de insets
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val startButton: Button = findViewById(R.id.btnRestart)
        startButton.setOnClickListener {
            val intent = Intent(this, GameActivity::class.java)
            startActivity(intent)
            finish()
        }

        // Recibir datos del intent
        val score = intent.getIntExtra("SCORE", 0)
        val maxStreak = intent.getIntExtra("MAX_STREAK", 0)
        val level = intent.getIntExtra("LEVEL", 1)
        val newHighScore = intent.getBooleanExtra("NEW_HIGH_SCORE", false)
        val newHighStreak = intent.getBooleanExtra("NEW_HIGH_STREAK", false)

        // Mostrar en layout
        findViewById<TextView>(R.id.tvScore).text = getString(R.string.score_text, score)
        findViewById<TextView>(R.id.tvMaxStreak).text = getString(R.string.max_streak_text, maxStreak)

        if (newHighScore) {
            findViewById<TextView>(R.id.tvHighScore).visibility = View.VISIBLE
        }
        if (newHighStreak) {
            findViewById<TextView>(R.id.tvHighStreak).visibility = View.VISIBLE
        }
    }
    private fun updatePhrase(){
        tvPhraseWin.text = phraseManager.getStartPhrase()
    }
}

