package com.chifuzgames.catibox

import android.media.MediaPlayer
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    private lateinit var mediaPlayer: MediaPlayer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // inicializamos la música primero (archivo en res/raw/background_music.wav o .m4a)
        mediaPlayer = MediaPlayer.create(this, R.raw.background_music)
        mediaPlayer.isLooping = true
        mediaPlayer.setVolume(1f, 1f)
        mediaPlayer.start()

        // creamos GameView y le pasamos la referencia
        val gameView = GameView(this)
        gameView.setBackgroundPlayer(mediaPlayer)

        setContentView(gameView)
    }

    override fun onPause() {
        super.onPause()
        mediaPlayer.pause()
    }

    override fun onResume() {
        super.onResume()
        mediaPlayer.start()
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer.release()
    }
}
