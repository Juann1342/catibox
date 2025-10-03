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
    var stateTimer: Float = 0f // ahora en segundos

    fun update(deltaTime: Float) {
        if (stateTimer > 0f) {
            stateTimer -= deltaTime
            if (stateTimer <= 0f && state != PlayerState.DOUBLE_POINTS) {
                state = PlayerState.NORMAL
            }
        }
    }

    fun draw(
        canvas: Canvas,
        normalBitmap: Bitmap,
        doublePointsBitmap: Bitmap? = null,
        sadBitmap: Bitmap? = null,
        happyBitmap: Bitmap? = null,
        ouchBitmap: Bitmap? = null
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

