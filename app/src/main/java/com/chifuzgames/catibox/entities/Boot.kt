package com.chifuzgames.catibox.entities

import android.graphics.Bitmap
import android.graphics.Canvas
import kotlin.math.hypot

class Boot(
    var x: Float,
    var y: Float,
    val width: Int,
    val height: Int,
    val bitmap: Bitmap,
    targetX: Float,
    targetY: Float,
    speed: Float
) {
    private var vx: Float
    private var vy: Float

    init {
        val dx = targetX - x
        val dy = targetY - y
        val distance = hypot(dx.toDouble(), dy.toDouble()).toFloat()
        vx = dx / distance * speed *20
        vy = dy / distance * speed *20
    }

    fun update(deltaTime: Float) {
        x += vx * deltaTime
        y += vy * deltaTime

    }

    fun draw(canvas: Canvas) {
        canvas.drawBitmap(bitmap, x, y, null)
    }

    fun isOffScreen(screenWidth: Int, screenHeight: Int): Boolean {
        return x + width < 0 || x > screenWidth || y + height < 0 || y > screenHeight
    }

    fun hasHitPlayer(player: Player): Boolean {
        val paddingX = player.width * 0.25f
        val paddingY = player.height * 0.25f

        val playerCenterRectLeft = player.x + paddingX
        val playerCenterRectTop = player.y + paddingY
        val playerCenterRectRight = player.x + player.width - paddingX
        val playerCenterRectBottom = player.y + player.height - paddingY

        return x < playerCenterRectRight &&
                x + width > playerCenterRectLeft &&
                y < playerCenterRectBottom &&
                y + height > playerCenterRectTop
    }
}
