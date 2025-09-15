package com.example.catibox.entities

import android.graphics.Bitmap
import android.graphics.Canvas
import kotlin.math.hypot

class Fruit(
    var x: Float,
    var y: Float,
    val width: Int,
    val height: Int,
    val bitmap: Bitmap,
    var targetX: Float,
    var targetY: Float,
    private val speed: Float
) {
    private var vx: Float
    private var vy: Float

    init {
        val dx = targetX - x
        val dy = targetY - y
        val distance = hypot(dx.toDouble(), dy.toDouble()).toFloat()
        vx = dx / distance * speed
        vy = dy / distance * speed
    }

    fun update(screenWidth: Int, screenHeight: Int) {
        x += vx
        y += vy

        // Rebotar solo en los bordes laterales
        if (x < 0) {
            x = 0f
            vx = -vx
        }
        if (x + width > screenWidth) {
            x = (screenWidth - width).toFloat()
            vx = -vx
        }
        // 👇 ya no rebotamos en suelo ni en techo
    }

    fun draw(canvas: Canvas) {
        canvas.drawBitmap(bitmap, x, y, null)
    }

    fun checkHitPlayer(player: Player): Boolean {
        val paddingX = player.width * 0.25f
        val paddingY = player.height * 0.25f
        val left = player.x + paddingX
        val top = player.y + paddingY
        val right = player.x + player.width - paddingX
        val bottom = player.y + player.height - paddingY
        return x < right && x + width > left && y < bottom && y + height > top
    }

    fun isOffScreen(screenWidth: Int, screenHeight: Int): Boolean {
        return y > screenHeight
    }
}
