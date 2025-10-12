package com.chifuzgames.catibox

import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import com.chifuzgames.catibox.ads.AdManager
import androidx.core.content.edit

class GameOverActivity : AppCompatActivity() {
    var activityReady = false


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
          requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        setContentView(R.layout.activity_game_over)
        findViewById<Button>(R.id.reviveCurrentLevelButton).isEnabled = false
        findViewById<Button>(R.id.reviveNextLevelButton).isEnabled = false


        val score = intent.getIntExtra("SCORE", 0)
        val maxStreak = intent.getIntExtra("MAX_STREAK", 0)
        val level = intent.getIntExtra("LEVEL", 0)

        val prefs = getSharedPreferences("CATIBOX_PREFS", MODE_PRIVATE)
        val highScore = prefs.getInt("MAX_SCORE_HIST", 0)
        val highStreak = prefs.getInt("MAX_STREAK_HIST", 0)

        findViewById<TextView>(R.id.levelTextView).text = getString(R.string.score_text, level)
        findViewById<TextView>(R.id.scoreTextView).text = getString(R.string.score_text, score)
        findViewById<TextView>(R.id.streakTextView).text = getString(R.string.max_streak_text, maxStreak)
        findViewById<TextView>(R.id.highScoreTextView).text = getString(R.string.high_score_text, highScore)
        findViewById<TextView>(R.id.highStreakTextView).text = getString(R.string.high_streak_text, highStreak)

        val extraMessageScore = findViewById<TextView>(R.id.extraMessageScoreTextView)
        val extraMessageStreak = findViewById<TextView>(R.id.extraMessageStreakTextView)

        val newHighScoreFlag = intent.getBooleanExtra("NEW_HIGH_SCORE", false)
        val newHighStreakFlag = intent.getBooleanExtra("NEW_HIGH_STREAK", false)

        if (newHighScoreFlag) {
            extraMessageScore.text = getString(R.string.new_high_score)
            extraMessageScore.visibility = View.VISIBLE
        }

        if (newHighStreakFlag) {
            extraMessageStreak.text = getString(R.string.new_high_streak)
            extraMessageStreak.visibility = View.VISIBLE
        }

        val intentGame = Intent(this, GameActivity::class.java)

        findViewById<Button>(R.id.playAgainButton).setOnClickListener {
            intentGame.putExtra("INITIAL_LEVEL", 1)
            intentGame.putExtra("INITIAL_LIVES", 5)
            intentGame.putExtra("INITIAL_SCORE", 0)
            startActivity(intentGame)
            finish()
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val intentMain = Intent(this@GameOverActivity, MainMenuActivity::class.java)
                intentMain.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                startActivity(intentMain)
                finish()
            }
        })

        findViewById<Button>(R.id.reviveCurrentLevelButton).setOnClickListener {
            AdManager.showInterstitial(this) {
                val intent = Intent(this, GameActivity::class.java)
                intent.putExtra("INITIAL_LEVEL", level)
                intent.putExtra("INITIAL_LIVES", 1)
                intent.putExtra("INITIAL_SCORE", score)
                startActivity(intent)
                finish()
            }
        }

        findViewById<Button>(R.id.reviveNextLevelButton).setOnClickListener {
            var initialLives: Int
            var nextLevel = 0

            if (level != 9 && level != 19) {
                nextLevel = level + 1
                initialLives = 3

                AdManager.showRewarded(this) {
                    val intent = Intent(this, GameActivity::class.java)
                    intent.putExtra("INITIAL_LEVEL", nextLevel)
                    intent.putExtra("INITIAL_LIVES", initialLives)
                    intent.putExtra("INITIAL_SCORE", score)
                    startActivity(intent)
                    finish()
                }

            } else {
                nextLevel = level
                initialLives = 10

                val dialog = androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("Último nivel")
                    .setMessage("Has llegado al último nivel de este mundo! Disfruta 10 vidas extra como recompensa.")
                    .setPositiveButton("OK") { _, _ ->
                        AdManager.showRewarded(this) {
                            val intent = Intent(this, GameActivity::class.java)
                            intent.putExtra("INITIAL_LEVEL", nextLevel)
                            intent.putExtra("INITIAL_LIVES", initialLives)
                            intent.putExtra("INITIAL_SCORE", score)
                            startActivity(intent)
                            finish()
                        }
                    }
                    .setCancelable(false)
                    .create()
                dialog.show()
            }
        }

        // Mostrar banner
     /*   val container = findViewById<LinearLayout>(R.id.bannerGameOverContainer)
        AdManager.showBanner(this, container)*/
        val container = findViewById<LinearLayout>(R.id.bannerGameOverContainer)
        container.postDelayed({
            AdManager.showBanner(this, container)
        }, 200) // 200ms, deja que la UI se estabilice



    }

    override fun onResume() {
        super.onResume()
        activityReady = true
        enableAdButtonsIfReady()
    }

    private fun enableAdButtonsIfReady() {
        if (activityReady) {
            findViewById<Button>(R.id.reviveCurrentLevelButton).isEnabled = true
            findViewById<Button>(R.id.reviveNextLevelButton).isEnabled = true
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        AdManager.destroyBanner() // ahora no necesita recibir bannerView
    }
}
