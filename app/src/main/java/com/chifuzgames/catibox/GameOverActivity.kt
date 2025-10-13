package com.chifuzgames.catibox

import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.chifuzgames.catibox.ads.AdManager

class GameOverActivity : AppCompatActivity() {

    private lateinit var reviveCurrentButton: Button
    private lateinit var reviveNextButton: Button
    private lateinit var playAgainButton: Button
    private lateinit var progressBar: ProgressBar

    private var score = 0
    private var maxStreak = 0
    private var level = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        setContentView(R.layout.activity_game_over)

        // Inicializar views
        reviveCurrentButton = findViewById(R.id.reviveCurrentLevelButton)
        reviveNextButton = findViewById(R.id.reviveNextLevelButton)
        playAgainButton = findViewById(R.id.playAgainButton)
        progressBar = findViewById(R.id.progressBar) // Agregar en tu layout
        progressBar.visibility = View.GONE

        // Inicialmente deshabilitados
        reviveCurrentButton.isEnabled = false
        reviveNextButton.isEnabled = false

        // Obtener datos
        score = intent.getIntExtra("SCORE", 0)
        maxStreak = intent.getIntExtra("MAX_STREAK", 0)
        level = intent.getIntExtra("LEVEL", 0)

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
        if (intent.getBooleanExtra("NEW_HIGH_SCORE", false)) {
            extraMessageScore.text = getString(R.string.new_high_score)
            extraMessageScore.visibility = View.VISIBLE
        }
        if (intent.getBooleanExtra("NEW_HIGH_STREAK", false)) {
            extraMessageStreak.text = getString(R.string.new_high_streak)
            extraMessageStreak.visibility = View.VISIBLE
        }

        // Botón "Jugar de nuevo"
        playAgainButton.setOnClickListener {
            val intentGame = Intent(this, GameActivity::class.java)
            intentGame.putExtra("INITIAL_LEVEL", 1)
            intentGame.putExtra("INITIAL_LIVES", 5)
            intentGame.putExtra("INITIAL_SCORE", 0)
            startActivity(intentGame)
            finish()
        }

        // Manejo del botón "revive current level"

        reviveCurrentButton.setOnClickListener {
            disableButtonsAndShowProgress()
            AdManager.showInterstitial(this,
                onAdClosed = {
                    val intent = Intent(this, GameActivity::class.java)
                    intent.putExtra("INITIAL_LEVEL", level)
                    intent.putExtra("INITIAL_LIVES", 1)
                    intent.putExtra("INITIAL_SCORE", score)
                    startActivity(intent)
                    finish()                },
                onAdUnavailable = {
                    enableButtonsAndShowProgress()
                    showAdUnavailableDialog()
                }
            )
        }

        reviveNextButton.setOnClickListener {
            disableButtonsAndShowProgress()

            val nextLevel: Int
            val initialLives: Int

            if (level != 9 && level != 19) {
                nextLevel = level + 1
                initialLives = 3

                AdManager.showRewarded(this,
                    onRewardEarned = { startNextLevel(nextLevel, initialLives) },
                    onAdUnavailable = {
                        enableButtonsAndShowProgress()
                        showAdUnavailableDialog()
                    }
                )

            } else {
                nextLevel = level
                initialLives = 10

                AlertDialog.Builder(this)
                    .setTitle("Último nivel")
                    .setMessage("Has llegado al último nivel de este mundo! Disfruta 10 vidas extra como recompensa.")
                    .setPositiveButton("OK") { _, _ ->
                        AdManager.showRewarded(this,
                            onRewardEarned = { startNextLevel(nextLevel, initialLives) },
                            onAdUnavailable = {
                                enableButtonsAndShowProgress()
                                showAdUnavailableDialog()
                            }
                        )
                    }
                    .setCancelable(false)
                    .show()
            }
        }


        // Mostrar banner
        val container = findViewById<LinearLayout>(R.id.bannerGameOverContainer)
        container.postDelayed({
            AdManager.showBanner(this, container)
            // Habilitar botones tras mostrar banner (opcional)
            reviveCurrentButton.isEnabled = true
            reviveNextButton.isEnabled = true
        }, 200)

        // Permitir volver al menú
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val intentMain = Intent(this@GameOverActivity, MainMenuActivity::class.java)
                intentMain.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                startActivity(intentMain)
                finish()
            }
        })
    }
    private fun showAdUnavailableDialog() {
        AlertDialog.Builder(this)
            .setTitle("Anuncio no disponible")
            .setMessage("No hay anuncios disponibles en este momento. Por favor, intenta más tarde.")
            .setPositiveButton("OK", null)
            .show()
    }

    private fun disableButtonsAndShowProgress() {
        reviveCurrentButton.isEnabled = false
        reviveNextButton.isEnabled = false
        playAgainButton.isEnabled = false
        progressBar.visibility = View.VISIBLE
    }

    private fun enableButtonsAndShowProgress() {
        reviveCurrentButton.isEnabled = true
        reviveNextButton.isEnabled = true
        playAgainButton.isEnabled = true
        progressBar.visibility = View.GONE
    }


    private fun startNextLevel(level: Int, lives: Int) {
        val intent = Intent(this, GameActivity::class.java)
        intent.putExtra("INITIAL_LEVEL", level)
        intent.putExtra("INITIAL_LIVES", lives)
        intent.putExtra("INITIAL_SCORE", score)
        startActivity(intent)
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        AdManager.destroyBanner()
    }
}
