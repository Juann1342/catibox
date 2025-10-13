package com.chifuzgames.catibox.entities

import android.graphics.Bitmap
import android.graphics.Canvas

enum class CatState {
    NORMAL, SPACE
}

class Cat(
    var x: Float,
    var y: Float,
    val width: Int,
    val height: Int,
    val normalBitmap: Bitmap,
    val spaceBitmap: Bitmap,
    val onHitGround: () -> Unit
) {
    var state: CatState = CatState.NORMAL

    private var baseFallSpeed = (200..400).random().toFloat() // píxeles por segundo
    private var sliding = false
    private var slideDirection = 0f
    private val slideSpeed = (80..150).random().toFloat() // píxeles por segundo
    var canBeCaught = true
    var gone = false
    private var hitGroundPlayed = false

    fun update(deltaTime: Float, difficultyMultiplier: Float = 1f, screenWidth: Int, screenHeight: Int, player: Player) {
        val groundOffset = 180f

        if (!sliding) {
            // caída
            y += baseFallSpeed * difficultyMultiplier * deltaTime

            val visiblePart = 20f
            val slideStartY = screenHeight - groundOffset - visiblePart + 50

            if (y + height >= slideStartY) {
                sliding = true
                canBeCaught = false

                if (!hitGroundPlayed) {
                    onHitGround()
                    hitGroundPlayed = true
                }

                // dirección del slide (izquierda o derecha)
                slideDirection = if (x + width / 2f < player.x + player.width / 2f) -slideSpeed * difficultyMultiplier
                else slideSpeed * difficultyMultiplier
            }
        } else {
            // deslizar
            x += slideDirection * deltaTime

            // fuera de pantalla
            if (x + width < 0 || x > screenWidth) {
                gone = true
            }
        }
    }

    fun draw(canvas: Canvas) {
        val bmpToDraw = when (state) {
            CatState.SPACE -> spaceBitmap
            else -> normalBitmap
        }
        canvas.drawBitmap(bmpToDraw, x, y, null)
    }


    fun isCaught(player: Player, previousY: Float): Boolean {
        if (!canBeCaught) return false

        val boxTop = player.y + player.height * 0.2f
        val boxBottom = player.y + player.height * 0.5f
        val boxLeft = player.x
        val boxRight = player.x + player.width

        val isOverlappingHorizontally = x + width > boxLeft && x < boxRight
        val isOverlappingVertically = y + height > boxTop && y < boxBottom
        val isFallingOntoPlayer = previousY + height <= boxTop

        return isOverlappingHorizontally && isOverlappingVertically && isFallingOntoPlayer
    }

    fun isOffScreen(): Boolean {
        return gone
    }
}
