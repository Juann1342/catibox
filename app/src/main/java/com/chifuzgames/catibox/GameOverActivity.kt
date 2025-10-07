package com.chifuzgames.catibox
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import com.chifuzgames.catibox.ads.AdManager
import com.google.android.gms.ads.AdRequest

class GameOverActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game_over)

        // Obtener el puntaje y la racha desde el Intent
        val score = intent.getIntExtra("SCORE", 0)
        val maxStreak = intent.getIntExtra("MAX_STREAK", 0)
        val level = intent.getIntExtra("LEVEL", 0)


        // Obtener valores históricos desde SharedPreferences
        val prefs = getSharedPreferences("CATIBOX_PREFS", MODE_PRIVATE)
        val highScore = prefs.getInt("MAX_SCORE_HIST", 0)
        val highStreak = prefs.getInt("MAX_STREAK_HIST", 0)

        // Mostrar puntaje actual
        findViewById<TextView>(R.id.levelTextView).text = getString(R.string.score_text, level)

        findViewById<TextView>(R.id.scoreTextView).text = getString(R.string.score_text, score)
        // Mostrar racha actual
        findViewById<TextView>(R.id.streakTextView).text = getString(R.string.max_streak_text, maxStreak)
        // Mostrar récord histórico
        findViewById<TextView>(R.id.highScoreTextView).text = getString(R.string.high_score_text, highScore)
        findViewById<TextView>(R.id.highStreakTextView).text = getString(R.string.high_streak_text, highStreak)

        val extraMessageScore = findViewById<TextView>(R.id.extraMessageScoreTextView)
        val extraMessageStreak = findViewById<TextView>(R.id.extraMessageStreakTextView)
//recibo las flags de si son nuevos records o no
        val newHighScoreFlag = intent.getBooleanExtra("NEW_HIGH_SCORE", false)
        val newHighStreakFlag = intent.getBooleanExtra("NEW_HIGH_STREAK", false)

        // Chequear récords
        if (newHighScoreFlag) {
            extraMessageScore.text = getString(R.string.new_high_score)
            extraMessageScore.visibility = View.VISIBLE
        }

        if (newHighStreakFlag) {
            extraMessageStreak.text = getString(R.string.new_high_streak)
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
                // Estos flags limpian todas las actividades por encima de MainMenuActivity
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                startActivity(intent)
                finish()
            }
        })

        showBannerAd()
    }

    private fun showBannerAd() {
        val container = findViewById<LinearLayout>(R.id.bannerGameOverContainer)
        AdManager.bannerView?.let { banner ->
            // Si el banner ya estaba agregado en otra vista, lo removemos
            if (banner.parent != null) (banner.parent as ViewGroup).removeView(banner)
            // Lo agregamos al contenedor
            container.addView(banner)
            //  Cargar anuncio si no lo hiciste antes
            banner.loadAd(AdRequest.Builder().build())
        }
    }



}
