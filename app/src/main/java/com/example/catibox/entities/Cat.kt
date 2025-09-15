package com.example.catibox.entities

import android.graphics.Bitmap
import android.graphics.Canvas

class Cat(
    var x: Float,
    var y: Float,
    val width: Int,
    val height: Int,
    val bitmap: Bitmap,
    val onHitGround: () -> Unit // callback para sonido enojado
) {
    private var baseSpeed = (5..15).random().toFloat()
    private var sliding = false
    private var slideDirection = 0f
    private val slideSpeed = (5..8).random().toFloat()
    var canBeCaught = true
    var gone = false
    private var hitGroundPlayed = false // para reproducir sonido solo una vez

    fun update(difficultyMultiplier: Float = 1f, screenWidth: Int, screenHeight: Int, player: Player) {
        val groundOffset = 180f // mismo offset que subiste al jugador

        val previousY = y // guardamos posición anterior antes de mover

        if (!sliding) {
            // Caída vertical
            y += baseSpeed * difficultyMultiplier

            // Altura donde empieza el deslizamiento: solo se ve la cabeza del gato
            val visiblePart = 20f
            val slideStartY = screenHeight - groundOffset - visiblePart

            if (y + height >= slideStartY) {
                sliding = true
                canBeCaught = false

                // reproducir sonido enojado solo una vez
                if (!hitGroundPlayed) {
                    onHitGround()
                    hitGroundPlayed = true
                }

                // Dirección opuesta al jugador
                slideDirection = if (x + width / 2f < player.x + player.width / 2f) -slideSpeed else slideSpeed

                // Ajustamos la posición para que solo sobresalga la cabeza
                y = slideStartY - height
            }
        } else {
            // Deslizamiento lateral
            x += slideDirection

            // Salir por los bordes de la pantalla
            if (x + width < 0 || x > screenWidth) {
                gone = true
            }
        }
    }

    fun draw(canvas: Canvas) {
        canvas.drawBitmap(bitmap, x, y, null)
    }

    fun isCaught(player: Player, previousY: Float): Boolean {
        if (!canBeCaught) return false

        // Solo la "caja" superior del jugador
        val boxTop = player.y + player.height * 0.2f
        val boxBottom = player.y + player.height * 0.5f
        val boxLeft = player.x
        val boxRight = player.x + player.width

        val isOverlappingHorizontally = x + width > boxLeft && x < boxRight
        val isOverlappingVertically = y + height > boxTop && y < boxBottom
        val isFallingOntoPlayer = previousY + height <= boxTop // viene de arriba

        return isOverlappingHorizontally && isOverlappingVertically && isFallingOntoPlayer
    }

    fun isOffScreen(screenWidth: Int): Boolean {
        return gone
    }
}

