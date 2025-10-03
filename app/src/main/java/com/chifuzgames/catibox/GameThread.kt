package com.chifuzgames.catibox

import android.graphics.Canvas
import android.view.SurfaceHolder

class GameThread(private val surfaceHolder: SurfaceHolder, private val gameView: GameView) : Thread() {

    @Volatile
    var running = false
    private val targetFPS = 30 //define fps del juego
    private val targetTime = (1000 / targetFPS).toLong()

    fun stopThread(isRunning: Boolean) {
        running = isRunning
    }

    override fun run() {
        var canvas: Canvas?
        var lastTime = System.nanoTime()

        while (running) {
            val startTime = System.nanoTime()
            val deltaTime = (startTime - lastTime) / 1_000_000_000f
            lastTime = startTime

            canvas = null
            try {
                // <-- chequeo de validez
                if (!surfaceHolder.surface.isValid) continue

                canvas = surfaceHolder.lockCanvas()
                synchronized(surfaceHolder) {
                    gameView.update(deltaTime)
                    if (canvas != null) gameView.draw(canvas)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                if (canvas != null) {
                    try {
                        surfaceHolder.unlockCanvasAndPost(canvas)
                    } catch (_: Exception) {}
                }
            }

            val timeMillis = (System.nanoTime() - startTime) / 1_000_000
            val waitTime = targetTime - timeMillis
            if (waitTime > 0) sleep(waitTime)
        }
    }


}
