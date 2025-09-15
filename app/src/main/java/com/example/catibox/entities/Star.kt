package com.example.catibox

import android.graphics.Bitmap
import android.graphics.Canvas
import kotlin.random.Random

class Star(
    var x: Float,
    var y: Float,
    val width: Int,
    val height: Int,
    val bitmap: Bitmap,
    private val screenWidth: Int,
    private val screenHeight: Int,
    private val speedY: Float = 8f
) {
    private var speedX: Float = Random.nextFloat() * 6f - 3f

    fun update(screenWidth: Int, screenHeight: Int) {
        x += speedX
        y += speedY

        // Rebotar en los bordes laterales
        if (x < 0f) { x = 0f; speedX = -speedX }
        if (x + width > screenWidth) { x = (screenWidth - width).toFloat(); speedX = -speedX }

        // Limitar que no salga por arriba o por abajo
        if (y + height > screenHeight) y = (screenHeight - height).toFloat()
        if (y < 0f) y = 0f
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
