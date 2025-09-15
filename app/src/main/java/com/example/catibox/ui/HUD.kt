package com.example.catibox.ui

import android.content.Context
import android.graphics.*
import android.graphics.drawable.Drawable
import android.view.MotionEvent
import androidx.core.content.ContextCompat
import com.example.catibox.R
import com.example.catibox.managers.SoundManager

class HUD(private val context: Context) {

    var score = 0
    var lives = 5
    var streak = 0
    var maxStreak = 0
    var gameOver = false
    var level = 1
    var levelTransition = false
    var levelTransitionTimer = 0

    private val paint = Paint().apply { color = Color.WHITE; textSize = 60f; isAntiAlias = true }
    private val livesPaint = Paint().apply { color = Color.RED; textSize = 60f; isAntiAlias = true }
    private val streakPaint = Paint().apply { color = Color.CYAN; textSize = 50f; isAntiAlias = true }
    private val levelPaint = Paint().apply {
        color = Color.YELLOW
        textSize = 120f
        isFakeBoldText = true
        textAlign = Paint.Align.CENTER
        setShadowLayer(8f, 0f, 0f, Color.BLACK)
    }

    private val muteButtonRect = RectF()
    private val muteButtonSize = 100f
    private lateinit var muteIcon: Bitmap
    private lateinit var unmuteIcon: Bitmap

    fun initIcons() {
        muteIcon = drawableToBitmap(ContextCompat.getDrawable(context, R.drawable.ic_mute)!!, muteButtonSize.toInt(), muteButtonSize.toInt())
        unmuteIcon = drawableToBitmap(ContextCompat.getDrawable(context, R.drawable.ic_unmute)!!, muteButtonSize.toInt(), muteButtonSize.toInt())
    }

    private fun drawableToBitmap(drawable: Drawable, width: Int, height: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }

    fun draw(canvas: Canvas, screenWidth: Int) {
        // Score
        canvas.drawText("Score: $score", 30f, 150f, paint)
        canvas.drawText("Streak: $streak  (Max: $maxStreak)", 30f, 220f, streakPaint)

        // Lives
        val livesText = "Lives: $lives"
        val livesTextWidth = livesPaint.measureText(livesText)
        val livesX = screenWidth - livesTextWidth - 30f
        val livesY = 150f
        canvas.drawText(livesText, livesX, livesY, livesPaint)

        // Mute Icon
        val icon = if (SoundManager.isMuted) muteIcon else unmuteIcon
        val muteX = livesX + livesTextWidth / 2 - muteButtonSize / 2
        val muteY = livesY + 30f
        canvas.drawBitmap(icon, muteX, muteY, null)
        muteButtonRect.set(muteX, muteY, muteX + muteButtonSize, muteY + muteButtonSize)

        // Game Over overlay
        if (gameOver) {
            val overlay = Paint().apply { color = Color.argb(180, 0, 0, 0) }
            canvas.drawRect(0f, 0f, screenWidth.toFloat(), canvas.height.toFloat(), overlay)
        }

        // Level transition message
        if (levelTransition) {
            val total = 90f
            val elapsed = (total - levelTransitionTimer.toFloat()).coerceAtLeast(0f)
            val progress = (elapsed / total).coerceIn(0f, 1f)

            val alpha = when {
                progress < 0.3f -> (progress / 0.3f * 255).toInt()
                progress > 0.7f -> ((1f - progress) / 0.3f * 255).toInt()
                else -> 255
            }.coerceIn(0, 255)
            levelPaint.alpha = alpha

            val easeOut = 1f - (1f - progress) * (1f - progress)
            val baseScale = 0.85f
            val scaleRange = 0.25f
            val scale = baseScale + scaleRange * easeOut

            val cx = screenWidth / 2f
            val cy = canvas.height / 2f

            canvas.save()
            canvas.translate(cx, cy)
            canvas.scale(scale, scale)
            canvas.drawText("LEVEL $level", 0f, 0f, levelPaint)
            canvas.restore()
        }
    }

    fun handleTouch(x: Float, y: Float): Boolean {
        if (muteButtonRect.contains(x, y)) {
            toggleMute()
            return true
        }
        return false
    }

    private fun toggleMute() {
        SoundManager.toggleMute()
    }
}
