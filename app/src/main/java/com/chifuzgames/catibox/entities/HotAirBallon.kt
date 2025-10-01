package com.chifuzgames.catibox.entities

import android.graphics.Bitmap
import android.graphics.Canvas

class HotAirBalloon(
    var x: Float,
    var y: Float,
    val width: Int,
    val height: Int,
    var hasDroppedBoot: Boolean = false,
    private val bitmap: Bitmap,
    val fromLeft: Boolean
) {
    private val speed = 3f // velocidad del globo

    fun update() {
        if (fromLeft) x += speed else x -= speed
    }

    fun draw(canvas: Canvas) {
        canvas.drawBitmap(bitmap, x, y, null)
    }

    fun isOffScreen(screenWidth: Int): Boolean {
        return if (fromLeft) x > screenWidth else x + width < 0
    }


}
