package com.chifuzgames.catibox.entities

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
    private val speed = 4f
    var hasDroppedItem = false

    fun update(deltaTime: Float) {
        if (fromLeft) x += speed * deltaTime*20 else x -= speed * deltaTime *20
    }

    fun draw(canvas: Canvas) {
        canvas.drawBitmap(bitmap, x, y, null)
    }

    fun isOffScreen(): Boolean {
        return if (fromLeft) x > screenWidth else x + width < 0
    }
}
