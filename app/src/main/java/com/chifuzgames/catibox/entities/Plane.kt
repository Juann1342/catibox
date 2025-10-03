package com.chifuzgames.catibox.entities

import android.graphics.Bitmap
import android.graphics.Canvas
import kotlin.random.Random

class Plane(
    var x: Float,
    var y: Float,
    val width: Int,
    val height: Int,
    private val bitmap: Bitmap
)
 {
    private val speed = 4f
    var hasDroppedItem = false

    fun update(deltaTime: Float) {
        x -= speed * deltaTime * 35
    }

    fun draw(canvas: Canvas) {
        canvas.drawBitmap(bitmap, x, y, null)
    }

    fun isOffScreen(): Boolean {
        return x + width < 0
    }

    companion object {
        fun randomY(excludeRange: ClosedFloatingPointRange<Float>? = null): Float {
            var y: Float
            do {
                y = Random.nextInt(60, 500).toFloat()
            } while (excludeRange != null && y in excludeRange)
            return y
        }
    }
}
