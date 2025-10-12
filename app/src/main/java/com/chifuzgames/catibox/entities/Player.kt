package com.chifuzgames.catibox.entities



import android.graphics.Bitmap
import android.graphics.Canvas

enum class PlayerState {
    NORMAL, DOUBLE_POINTS, SAD, HAPPY, OUCH, SPACE
}

class Player(
    var x: Float,
    var y: Float,
    val width: Int,
    val height: Int
) {
    var state: PlayerState = PlayerState.NORMAL
    var stateTimer: Float = 0f // ahora en segundos

    // --- VARIABLES PARA WALK ANIMATION ---
    var isSliding = false          // true cuando el jugador se desliza
    private var walkNormalBitmap: Bitmap? = null
    private var walkOuchBitmap: Bitmap? = null
    private var walkSadBitmap: Bitmap? = null
    private var walkHappyBitmap: Bitmap? = null
    private var walkBonusBitmap: Bitmap? = null
    private var walkSpaceBitmap: Bitmap? = null

    private var walkTimer = 0f     // contador para alternar
    private var showNormal = true  // alterna entre normal y walk

    fun update(deltaTime: Float) {
        // estado temporal (HAPPY, SAD, OUCH)
        if (stateTimer > 0f) {
            stateTimer -= deltaTime
            if (stateTimer <= 0f && state != PlayerState.DOUBLE_POINTS) {
                state = PlayerState.NORMAL
            }
        }

        // animación de caminar al deslizar
        if (isSliding && walkNormalBitmap != null) {
            walkTimer += deltaTime
            if (walkTimer >= 0.3f) { // 300 ms
                showNormal = !showNormal
                walkTimer = 0f
            }
        } else {
            showNormal = true
            walkTimer = 0f
        }
    }

    fun setWalkBitmap(bitmapNormalWalk: Bitmap, bitmapOuchWalk: Bitmap,bitmapSadWalk: Bitmap,bitmapHappyWalk: Bitmap,bitmapBonusWalk: Bitmap,bitmapSpaceWalk: Bitmap) {
        walkNormalBitmap = bitmapNormalWalk
        walkOuchBitmap = bitmapOuchWalk
        walkSadBitmap = bitmapSadWalk
        walkHappyBitmap = bitmapHappyWalk
        walkBonusBitmap = bitmapBonusWalk
        walkSpaceBitmap = bitmapSpaceWalk

    }


    fun draw(
        canvas: Canvas,
        normalBitmap: Bitmap,
        doublePointsBitmap: Bitmap? = null,
        sadBitmap: Bitmap? = null,
        happyBitmap: Bitmap? = null,
        ouchBitmap: Bitmap? = null,
        spaceBitmap:Bitmap?= null
    ) {
        val bitmapToDraw = when(state) {
            PlayerState.NORMAL -> if (isSliding) {
                if (showNormal) normalBitmap else walkNormalBitmap ?: normalBitmap
            } else normalBitmap

            PlayerState.DOUBLE_POINTS -> if (isSliding) {
                if (showNormal) doublePointsBitmap ?: normalBitmap else walkBonusBitmap ?: doublePointsBitmap ?: normalBitmap
            } else doublePointsBitmap ?: normalBitmap

            PlayerState.SAD -> if (isSliding) {
                if (showNormal) sadBitmap ?: normalBitmap else walkSadBitmap ?: sadBitmap ?: normalBitmap
            } else sadBitmap ?: normalBitmap

            PlayerState.HAPPY -> if (isSliding) {
                if (showNormal) happyBitmap ?: normalBitmap else walkHappyBitmap ?: happyBitmap ?: normalBitmap
            } else happyBitmap ?: normalBitmap

            PlayerState.OUCH -> if (isSliding) {
                if (showNormal) ouchBitmap ?: normalBitmap else walkOuchBitmap ?: ouchBitmap ?: normalBitmap
            } else ouchBitmap ?: normalBitmap

            PlayerState.SPACE -> if (isSliding) {
                if (showNormal) spaceBitmap ?: normalBitmap else walkSpaceBitmap ?: spaceBitmap ?: normalBitmap
            } else spaceBitmap ?: normalBitmap
        }
        canvas.drawBitmap(bitmapToDraw, x, y, null)



    }

}

