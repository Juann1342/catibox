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

    private val pauseButtonRect = RectF()
    private val pauseButtonSize = 100f
    private lateinit var pauseIcon: Bitmap
    private lateinit var resumeIcon: Bitmap
    var isPaused = false
    var isMuted = false


    // Callbacks para GameView
    var onPauseToggle: (() -> Unit)? = null
    var onMuteToggle: (() -> Unit)? = null

    fun initIcons() {
        muteIcon = drawableToBitmap(ContextCompat.getDrawable(context, R.drawable.ic_mute)!!, muteButtonSize.toInt(), muteButtonSize.toInt())
        unmuteIcon = drawableToBitmap(ContextCompat.getDrawable(context, R.drawable.ic_unmute)!!, muteButtonSize.toInt(), muteButtonSize.toInt())
        pauseIcon = drawableToBitmap(ContextCompat.getDrawable(context, R.drawable.ic_pause)!!, pauseButtonSize.toInt(), pauseButtonSize.toInt())
        resumeIcon = drawableToBitmap(ContextCompat.getDrawable(context, R.drawable.ic_play)!!, pauseButtonSize.toInt(), pauseButtonSize.toInt())
    }

    private fun drawableToBitmap(drawable: Drawable, width: Int, height: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }

    fun draw(canvas: Canvas, screenWidth: Int) {
        // Score y Streak
        canvas.drawText("Score: $score", 30f, 150f, paint)
        canvas.drawText("Streak: $streak  (Max: $maxStreak)", 30f, 220f, streakPaint)

        // Lives
        val livesText = "Lives: $lives"
        val livesTextWidth = livesPaint.measureText(livesText)
        val livesX = screenWidth - livesTextWidth - 30f
        val livesY = 150f
        canvas.drawText(livesText, livesX, livesY, livesPaint)

        // Mute y Pause
        val iconMute = if (SoundManager.isMuted) muteIcon else unmuteIcon
        val muteX = livesX
        val muteY = livesY + 40f
        canvas.drawBitmap(iconMute, muteX, muteY, null)
        muteButtonRect.set(muteX, muteY, muteX + muteButtonSize, muteY + muteButtonSize)

        val iconPause = if (isPaused) resumeIcon else pauseIcon
        val pauseX = muteButtonRect.left
        val pauseY = muteButtonRect.bottom + 20f
        canvas.drawBitmap(iconPause, pauseX, pauseY, null)
        pauseButtonRect.set(pauseX, pauseY, pauseX + pauseButtonSize, pauseY + pauseButtonSize)

        // Overlay Game Over
        if (gameOver) {
            val overlay = Paint().apply { color = Color.argb(200, 0, 0, 0) }
            canvas.drawRect(0f, 0f, screenWidth.toFloat(), canvas.height.toFloat(), overlay)
            val textPaint = Paint().apply {
                color = Color.RED
                textSize = 150f
                textAlign = Paint.Align.CENTER
                isFakeBoldText = true
            }
            canvas.drawText("GAME OVER", screenWidth / 2f, canvas.height / 2f, textPaint)
        }

        // Level transition
        if (levelTransition) {
            val total = 90f
            val elapsed = (total - levelTransitionTimer.toFloat()).coerceAtLeast(0f)
            val progress = (elapsed / total).coerceIn(0f, 1f)
            levelPaint.alpha = ((1f - progress) * 255).toInt().coerceIn(0, 255)
            val cx = screenWidth / 2f
            val cy = canvas.height / 2f
            canvas.drawText("LEVEL $level", cx, cy, levelPaint)
        }

        // Pausa visible
        if (isPaused && !gameOver) {
            val pausePaint = Paint().apply {
                color = Color.argb(180, 0, 0, 0)
                textSize = 100f
                textAlign = Paint.Align.CENTER
                isFakeBoldText = true
            }
            canvas.drawText("PAUSED", screenWidth / 2f, canvas.height / 2f, pausePaint)
        }
    }

    fun handleTouch(x: Float, y: Float): Boolean {
        if (muteButtonRect.contains(x, y)) {
            toggleMute()
            onMuteToggle?.invoke()  // avisa a GameView
            return true
        }
        if (pauseButtonRect.contains(x, y)) {
            togglePause()
            onPauseToggle?.invoke() // avisa a GameView
            return true
        }
        return false
    }

    private fun togglePause() {
        isPaused = !isPaused
    }

    private fun toggleMute() {
        SoundManager.toggleMute()
    }

    fun handleTouch(x: Float, y: Float, event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_UP -> {
                if (pauseButtonRect.contains(x, y)) {
                    isPaused = !isPaused
                    onPauseToggle?.invoke()
                    return true
                }
                if (muteButtonRect.contains(x, y)) {
                    isMuted = !isMuted
                    onMuteToggle?.invoke()
                    return true
                }
            }
        }
        return false
    }

    // Método auxiliar para evitar mover jugador sobre los botones
    fun isTouchingButton(x: Float, y: Float): Boolean {
        return pauseButtonRect.contains(x, y) || muteButtonRect.contains(x, y)
    }

}
