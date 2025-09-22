package com.chifuzgames.catibox.entities



import android.graphics.Bitmap
import android.graphics.Canvas

enum class PlayerState {
    NORMAL, DOUBLE_POINTS, SAD, HAPPY, OUCH
}

class Player(
    var x: Float,
    var y: Float,
    val width: Int,
    val height: Int
) {
    var state: PlayerState = PlayerState.NORMAL
    var stateTimer: Int = 0 // duración del estado temporal en frames

    fun update() {
        // Si hay un estado temporal activo, reducir el timer
        if (stateTimer > 0) {
            stateTimer--
        } else if (state != PlayerState.DOUBLE_POINTS) {
            state = PlayerState.NORMAL
        }
    }

    fun draw(
        canvas: Canvas,
        normalBitmap: Bitmap,
        doublePointsBitmap: Bitmap?,
        sadBitmap: Bitmap?,
        happyBitmap: Bitmap?,
        ouchBitmap: Bitmap?
    ) {
        val bitmapToDraw = when(state) {
            PlayerState.NORMAL -> normalBitmap
            PlayerState.DOUBLE_POINTS -> doublePointsBitmap ?: normalBitmap
            PlayerState.SAD -> sadBitmap ?: normalBitmap
            PlayerState.HAPPY -> happyBitmap ?: normalBitmap
            PlayerState.OUCH -> ouchBitmap ?: normalBitmap
        }
        canvas.drawBitmap(bitmapToDraw, x, y, null)
    }
}

