package com.chifuzgames.catibox

import android.app.Activity
import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.chifuzgames.catibox.managers.PhraseManager
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform

class MainMenuActivity : AppCompatActivity() {
    private lateinit var tvHighScore: TextView
    private lateinit var tvLongestStreak: TextView

    private lateinit var tvPhraseInit: TextView

    val phraseManager = PhraseManager(this)


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
          requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        setContentView(R.layout.activity_main_menu)


        // Inicializamos los TextView
        tvHighScore = findViewById(R.id.tvYourHighScore)
        tvLongestStreak = findViewById(R.id.tvYourLongestStreak)
        tvPhraseInit = findViewById(R.id.tvPhraseInit)

        val toolbar: androidx.appcompat.widget.Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)  // <-- esto habilita el menú
        // Botón para iniciar el juego
        val startButton: Button = findViewById(R.id.startButton)
        startButton.setOnClickListener {
            val intent = Intent(this, GameActivity::class.java)
            startActivity(intent)
        }

        // Mostrar scores al iniciar
        updateScores()
        updatePhrase()
        showPrivacyConsentIfNeeded()

    }
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_settings, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_privacy -> {
                showSettingsDialog() // aquí usamos la función de extensión
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
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
        tvHighScore.text = getString(R.string.your_high_score_k, highScore)
        tvLongestStreak.text = getString(R.string.your_longest_streak_k, highStreak)

    }

    private fun updatePhrase(){
        tvPhraseInit.text = phraseManager.getStartPhrase()
    }

    private fun showPrivacyConsentIfNeeded() {
        val prefs = getSharedPreferences("CATIBOX_PREFS", MODE_PRIVATE)
        val hasShown = prefs.getBoolean("hasShownConsentDialog", false)
        if (hasShown) return // ya se mostró antes

        val consentInformation = UserMessagingPlatform.getConsentInformation(this)
        val params = ConsentRequestParameters.Builder().build()

        consentInformation.requestConsentInfoUpdate(
            this,
            params,
            {
                if (consentInformation.isConsentFormAvailable) {
                    UserMessagingPlatform.loadConsentForm(
                        this,
                        { consentForm ->
                            consentForm.show(this) { formError ->
                                formError?.let {
                                    Log.e("UMP", "Error mostrando formulario: ${it.message}")
                                }
                                prefs.edit().putBoolean("hasShownConsentDialog", true).apply()
                            }
                        },
                        { loadError ->
                            Log.e("UMP", "Error cargando formulario: ${loadError.message}")
                            prefs.edit().putBoolean("hasShownConsentDialog", true).apply()
                        }
                    )
                }
            },
            { requestError ->
                Log.e("UMP", "Error actualizando info de consentimiento: ${requestError.message}")
            }
        )
    }

    fun Activity.showSettingsDialog() {
        val options = arrayOf(getString(R.string.privacy_settings)) // se pueden agregar más opciones

        AlertDialog.Builder(this)
            .setTitle(getString(R.string.privacy_settings_menu))
            .setItems(options) { _, which ->
                when (which) {
                    0 -> openPrivacyOptions()
                }
            }
            .setNegativeButton(getString(R.string.privacy_settings_cancel), null)
            .show()
    }

    fun Activity.openPrivacyOptions() {
        val consentInformation = UserMessagingPlatform.getConsentInformation(this)
        val params = ConsentRequestParameters.Builder().build()

        consentInformation.requestConsentInfoUpdate(
            this,
            params,
            {
                if (consentInformation.isConsentFormAvailable) {
                    UserMessagingPlatform.loadConsentForm(
                        this,
                        { consentForm ->
                            consentForm.show(this) { formError ->
                                formError?.let {
                                    Log.e("UMP", "Error mostrando formulario: ${it.message}")
                                }
                            }
                        },
                        { loadError ->
                            loadError.let {
                                Log.e("UMP", "Error cargando formulario: ${it.message}")
                            }
                        }
                    )
                }else {
                    // No hay formulario disponible → mensaje
                    AlertDialog.Builder(this)
                        .setTitle(getString(R.string.privacy_settings_privacy))
                        .setMessage(getString(R.string.privacy_settings_no_apply))
                        .setPositiveButton(getString(R.string.privacy_settings_no_ok), null)
                        .show()

                }
            },
            { requestError ->
                requestError.let {
                    Log.e("UMP", "Error actualizando info de consentimiento: ${it.message}")
                }
            }
        )
    }
}
