package com.example.catibox.entities

import android.graphics.Bitmap
import android.graphics.Canvas

class UFO(
    var x: Float,
    var y: Float,
    val width: Int,
    val height: Int,
    private val bitmap: Bitmap,
    val fromLeft: Boolean,
    private val screenWidth: Int
) {
    private val speed = 6f
    var hasDroppedItem = false // Para saber si ya lanzó la estrella

    fun update() {
        if (fromLeft) x += speed else x -= speed
    }

    fun draw(canvas: Canvas) {
        canvas.drawBitmap(bitmap, x, y, null)
    }

    fun isOffScreen(): Boolean {
        return if (fromLeft) x > screenWidth else x + width < 0
    }
}
