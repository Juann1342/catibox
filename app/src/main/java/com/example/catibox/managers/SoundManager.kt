package com.example.catibox.managers

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import com.example.catibox.R

object SoundManager {
    var isMuted: Boolean = false          // Estado global de mute
        private set

    private var soundPool: SoundPool? = null
    private val sounds = mutableMapOf<String, Int>()

    // Inicializar SoundPool y cargar sonidos
    fun init(context: Context) {
        if (soundPool == null) {
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()

            soundPool = SoundPool.Builder()
                .setMaxStreams(10)
                .setAudioAttributes(audioAttributes)
                .build()

            // Cargar sonidos
            sounds["gameOver"] = soundPool!!.load(context, R.raw.game_over, 1)
            sounds["catHappy"] = soundPool!!.load(context, R.raw.cat_happy, 1)
            sounds["catAngry"] = soundPool!!.load(context, R.raw.cat_angry, 1)
            sounds["bootCrash"] = soundPool!!.load(context, R.raw.boot_crash, 1)
            sounds["fruitCatch"] = soundPool!!.load(context, R.raw.fruit_catch, 1)
            sounds["starCatch"] = soundPool!!.load(context, R.raw.star_catch, 1)


        }
    }

    // Reproducir un sonido por su nombre
    fun playSound(name: String) {
        if (!isMuted) {
            sounds[name]?.let { id ->
                soundPool?.play(id, 1f, 1f, 1, 0, 1f)
            }
        }
    }

    // Cambiar el estado de mute y pausar/reanudar sonidos
    fun toggleMute() {
        isMuted = !isMuted
        soundPool?.let { sp ->
            if (isMuted) sp.autoPause()
            else sp.autoResume()
        }
    }

    // Liberar recursos al cerrar la app
    fun release() {
        soundPool?.release()
        soundPool = null
        sounds.clear()
    }
}