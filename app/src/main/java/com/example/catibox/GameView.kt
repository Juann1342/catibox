package com.example.catibox

import android.content.Context
import android.graphics.*
import android.graphics.drawable.Drawable
import android.media.MediaPlayer
import android.os.Build
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.core.content.ContextCompat
import kotlin.random.Random

class GameView(context: Context, attrs: AttributeSet? = null) : SurfaceView(context, attrs), SurfaceHolder.Callback {

    companion object {
        const val PLAYER_WIDTH_RATIO = 5f
        const val PLAYER_HEIGHT_MULT = 1.8f
        const val CAT_BASE_RATIO = 8f
        const val BALLOON_WIDTH_RATIO = 4f
        const val BALLOON_HEIGHT_MULT = 1.65f
        const val PLANE_WIDTH_RATIO = 3.5f
        const val PLANE_HEIGHT_MULT = 0.55f
        const val UFO_WIDTH_RATIO = 3.5f
        const val UFO_HEIGHT_MULT = 0.55f

        var BALLOON_SCORE_INTERVAL = 100
        var PLANE_SCORE_INTERVAL = 190
        var UFO_SCORE_INTERVAL = 310
    }

    // Bitmaps
    private lateinit var playerBitmap: Bitmap
    private lateinit var playerBonusBitmap: Bitmap
    private lateinit var playerSadBitmap : Bitmap
    private lateinit var playerOuchBitmap : Bitmap
    private lateinit var playerHappyBitmap : Bitmap
    private lateinit var catBitmap: Bitmap

    // legacy single vars kept for compatibility (we'll use currentBackground/currentGrass for drawing)
    private lateinit var backgroundBitmap: Bitmap
    private lateinit var grassBitmap: Bitmap

    private lateinit var muteIcon: Bitmap
    private lateinit var unmuteIcon: Bitmap
    private lateinit var balloonBitmap: Bitmap
    private lateinit var planeBitmap: Bitmap
    private lateinit var ufoBitmap: Bitmap
    private lateinit var bootBitmap: Bitmap
    private lateinit var fruitBitmap: Bitmap
    private lateinit var starBitmap: Bitmap

    // Thread / player
    private var thread: GameThread? = null
    private var player: Player? = null

    // Pantalla
    private var screenWidth = 0
    private var screenHeight = 0

    // Juego
    private val cats = mutableListOf<Cat>()
    private var balloon: HotAirBalloon? = null
    private var plane: Plane? = null
    private var ufo: UFO? = null
    private val boots = mutableListOf<Boot>()
    private var activeFruit: Fruit? = null
    private var activeStar: Star? = null

    private var frameCount = 0
    var score = 0
    private val paint = Paint().apply { color = Color.WHITE; textSize = 60f; isAntiAlias = true }
    private var lives = 5
    private val livesPaint = Paint().apply { color = Color.RED; textSize = 60f; isAntiAlias = true }
    private var difficultyMultiplier = 1f
    private var spawnInterval = 60

    // Música de fondo
    private var backgroundPlayer: MediaPlayer? = null

    // Estado game over
    var gameOver = false
        private set

    // Mute button
    private val muteButtonRect = RectF()
    private val muteButtonSize = 100f

    // Callback Game Over
    var onGameOverListener: (() -> Unit)? = null

    // --- Variables globales de GameView ---
    private var doublePointsActive = false
    private var doublePointsTimer = 0 // en frames
    private val DOUBLE_POINTS_DURATION = 10 * 60 // 10 segundos * 60 fps

    // --- Niveles ---
    private var level = 1
    private var catsSpawned = 0
    private val catsPerLevel = listOf(10, 15, 20, 25, Int.MAX_VALUE) // nivel 5 sin límite
    private var levelTransition = false
    private var levelTransitionTimer = 0

    var planeSpawned = false
    var ufoSpawned = false

    // Paint para el texto de nivel (global para no recrear en cada draw)
    private val levelPaint = Paint().apply {
        color = Color.YELLOW
        textSize = 120f
        isFakeBoldText = true
        textAlign = Paint.Align.CENTER
        setShadowLayer(8f, 0f, 0f, Color.BLACK)
    }

    // --- Background / Grass per level (crossfade) ---
    // resource names per level (we'll resolve ids at runtime; fallback to default if missing)
    private val backgroundNames = listOf("background", "background2", "background3", "background4", "background5")
    private val grassNames = listOf("grass", "grass2", "grass3", "grass4", "grass5")

    private var currentBackground: Bitmap? = null
    private var nextBackground: Bitmap? = null
    private var backgroundTransition = false

    private var currentGrass: Bitmap? = null
    private var nextGrass: Bitmap? = null
    private var grassTransition = false


    // --- Racha (streak) ---
    private var streak = 0
     var maxStreak = 0
    private val streakPaint = Paint().apply {
        color = Color.CYAN
        textSize = 50f
        isAntiAlias = true
    }

    // --- Dificultad ajustable ---
    // --- Dificultad ajustable ---
    private var initialLives = 5
    // Vidas iniciales del jugador al comenzar la partida.

    private var initialSpawnInterval = 40
    // Intervalo base de aparición de gatos (en frames).
    // A menor número → más gatos aparecen más rápido.

    private var initialDifficultyMultiplier = 1f
    // Multiplicador inicial de dificultad. Puede usarse para escalar velocidades o frecuencia.

    private var difficultyIncreasePerScore = 0.1f
    // Incremento progresivo de dificultad cada vez que se supera un umbral de score.
    // Ejemplo: más velocidad o menor spawn interval.

    private var minSpawnInterval = 20
    // Intervalo mínimo permitido entre spawns.
    // Evita que el juego se vuelva imposible (demasiados gatos juntos).

    // --- Control de incrementos de dificultad ---
    private var nextScoreThreshold = 50
    // Próximo puntaje en el que se aplicará un incremento de dificultad.

    private val scoreThresholdStep = 50
    // Cada cuántos puntos aumentar la dificultad.
    // Ejemplo: 50, 100, 150, etc.

    // --- Control de llegadas simultáneas ---
    private val CAT_BASE_FALL_SPEED = 6f
    // Velocidad base de caída de los gatos (píxeles por frame).
    // Usar el mismo valor que en Cat.update() para mantener coherencia.

    private val minLandingGapFrames = 60
    // Número mínimo de frames que deben separar la llegada de dos gatos al suelo.
    // Evita que dos gatos aterricen al mismo tiempo y sea imposible atraparlos.

    private val levelDifficulty = listOf(
        Pair(1f, 50), // nivel 1: velocidad base x1, intervalo 50 frames
        Pair(1.1f, 45), // nivel 2: más rápido, menos intervalo
        Pair(1.2f, 40), // nivel 3
        Pair(1.3f, 35), // nivel 4
        Pair(1.4f, 30)  // nivel 5+
    )
    

    init {
        holder.addCallback(this)
        isFocusable = true
        enableFullscreen()
        SoundManager.init(context)
        initIcons()
    }

    private fun enableFullscreen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            windowInsetsController?.let { controller ->
                controller.hide(android.view.WindowInsets.Type.statusBars() or android.view.WindowInsets.Type.navigationBars())
                controller.systemBarsBehavior =
                    android.view.WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            systemUiVisibility = SYSTEM_UI_FLAG_FULLSCREEN or SYSTEM_UI_FLAG_HIDE_NAVIGATION or SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        }
    }

    private fun initIcons() {
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

    override fun surfaceCreated(holder: SurfaceHolder) {
        screenWidth = width
        screenHeight = height

        playerBitmap = BitmapFactory.decodeResource(resources, R.drawable.player)
        playerBonusBitmap = BitmapFactory.decodeResource(resources,R.drawable.player_bonus)
        playerSadBitmap= BitmapFactory.decodeResource(resources,R.drawable.player_sad)
        playerHappyBitmap = BitmapFactory.decodeResource(resources,R.drawable.player_happy)
        playerOuchBitmap = BitmapFactory.decodeResource(resources,R.drawable.player_ouch)
        catBitmap = BitmapFactory.decodeResource(resources, R.drawable.cat)

        // load defaults (kept for compatibility)
        backgroundBitmap = BitmapFactory.decodeResource(resources, R.drawable.background)
        grassBitmap = BitmapFactory.decodeResource(resources, R.drawable.grass)

        balloonBitmap = BitmapFactory.decodeResource(resources, R.drawable.hot_air_balloon)
        planeBitmap = BitmapFactory.decodeResource(resources, R.drawable.plane)
        ufoBitmap = BitmapFactory.decodeResource(resources, R.drawable.ufo)
        bootBitmap = BitmapFactory.decodeResource(resources, R.drawable.boot)
        fruitBitmap = BitmapFactory.decodeResource(resources, R.drawable.fruit)
        starBitmap = BitmapFactory.decodeResource(resources, R.drawable.star)

        val playerWidth = (screenWidth / PLAYER_WIDTH_RATIO).toInt()
        val playerHeight = (playerWidth * PLAYER_HEIGHT_MULT).toInt()
        val balloonWidth = (screenWidth / BALLOON_WIDTH_RATIO).toInt()
        val balloonHeight = (balloonWidth * BALLOON_HEIGHT_MULT).toInt()
        val planeWidth = (screenWidth / PLANE_WIDTH_RATIO).toInt()
        val planeHeight = (planeWidth * PLANE_HEIGHT_MULT).toInt()
        val ufoWidth = (screenWidth / UFO_WIDTH_RATIO).toInt()
        val ufoHeight = (ufoWidth * UFO_HEIGHT_MULT).toInt()

        playerBitmap = Bitmap.createScaledBitmap(playerBitmap, playerWidth, playerHeight, false)
        grassBitmap = Bitmap.createScaledBitmap(grassBitmap, screenWidth + 100, 400, false)
        balloonBitmap = Bitmap.createScaledBitmap(balloonBitmap, balloonWidth, balloonHeight, false)
        planeBitmap = Bitmap.createScaledBitmap(planeBitmap, planeWidth, planeHeight, false)
        ufoBitmap = Bitmap.createScaledBitmap(ufoBitmap, ufoWidth, ufoHeight, false)
        bootBitmap = Bitmap.createScaledBitmap(bootBitmap, balloonWidth / 3, balloonHeight / 3, false)
        fruitBitmap = Bitmap.createScaledBitmap(fruitBitmap, 60, 60, false)
        starBitmap = Bitmap.createScaledBitmap(starBitmap, 60, 60, false)
        playerBonusBitmap = Bitmap.createScaledBitmap(playerBonusBitmap, playerWidth, playerHeight, false)
        playerSadBitmap = Bitmap.createScaledBitmap(playerSadBitmap, playerWidth, playerHeight, false)
        playerHappyBitmap = Bitmap.createScaledBitmap(playerHappyBitmap, playerWidth, playerHeight, false)
        playerOuchBitmap = Bitmap.createScaledBitmap(playerOuchBitmap, playerWidth, playerHeight, false)

        // init player
        player = Player(
            x = screenWidth / 2f - playerWidth / 2f,
            y = screenHeight.toFloat() - playerHeight - 200f,
            width = playerWidth,
            height = playerHeight
        )
        // inicializar línea de partida
        lives = initialLives
        difficultyMultiplier = initialDifficultyMultiplier
        spawnInterval = initialSpawnInterval
        nextScoreThreshold = scoreThresholdStep


        // Initialize current background/grass from the default (keeps compatibility)
        currentBackground = Bitmap.createScaledBitmap(
            BitmapFactory.decodeResource(resources, getDrawableIdByName(backgroundNames[0])),
            screenWidth, screenHeight, false
        )

        currentGrass = Bitmap.createScaledBitmap(
            BitmapFactory.decodeResource(resources, getDrawableIdByName(grassNames[0])),
            screenWidth + 100, 400, false
        )

        thread = GameThread(holder, this)
        thread?.setRunning(true)
        thread?.start()
    }

    private fun getDrawableIdByName(name: String): Int {
        val id = resources.getIdentifier(name, "drawable", context.packageName)
        return if (id != 0) id else R.drawable.background // fallback to background if not found
    }

    private fun getGrassIdByName(name: String): Int {
        val id = resources.getIdentifier(name, "drawable", context.packageName)
        return if (id != 0) id else R.drawable.grass // fallback
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {}
    override fun surfaceDestroyed(holder: SurfaceHolder) {
        var retry = true
        thread?.setRunning(false)
        while (retry) {
            try {
                thread?.join()
                retry = false
            } catch (_: InterruptedException) {}
        }
    }

    // Load next level assets into nextBackground/nextGrass and enable transitions
    private fun loadLevelAssets(nextLevel: Int) {
        val bgName = backgroundNames.getOrNull(nextLevel - 1) ?: backgroundNames.last()
        val grassName = grassNames.getOrNull(nextLevel - 1) ?: grassNames.last()

        val bgId = getDrawableIdByName(bgName)
        val grassId = getGrassIdByName(grassName)

        // prepare nextBackground and nextGrass scaled to screen
        nextBackground = BitmapFactory.decodeResource(resources, bgId)
        nextBackground = Bitmap.createScaledBitmap(nextBackground!!, screenWidth, screenHeight, false)

        nextGrass = BitmapFactory.decodeResource(resources, grassId)
        nextGrass = Bitmap.createScaledBitmap(nextGrass!!, screenWidth + 100, 400, false)

        // enable transitions
        backgroundTransition = true
        grassTransition = true
    }

    fun update() {
        if (player == null || gameOver) return

        // --- Transición de nivel ---
        if (levelTransition) {
            levelTransitionTimer--
            if (levelTransitionTimer <= 0) {
                levelTransition = false
                catsSpawned = 0 // reset para el próximo nivel

                // finalize background/grass if not already swapped (defensive)
                if (backgroundTransition && nextBackground != null) {
                    currentBackground = nextBackground
                    nextBackground = null
                    backgroundTransition = false
                }
                if (grassTransition && nextGrass != null) {
                    currentGrass = nextGrass
                    nextGrass = null
                    grassTransition = false
                }
            }
            return
        }

        player?.update()
        frameCount++

        // --- Spawning de gatos controlado por nivel ---
        if (frameCount % spawnInterval == 0 && catsSpawned < catsPerLevel[level - 1]) {
            spawnCat()
            catsSpawned++
        }

        // --- Globo + Bota ---
        val balloonWidth = (screenWidth / BALLOON_WIDTH_RATIO).toInt()
        val balloonHeight = (balloonWidth * BALLOON_HEIGHT_MULT).toInt()
        if (score > 0 && score % BALLOON_SCORE_INTERVAL == 0 && balloon == null) {
            val fromLeft = Random.nextBoolean()
            val xPos = if (fromLeft) -balloonWidth.toFloat() else screenWidth.toFloat()
            val yPos = Random.nextInt(80, (screenHeight * 0.35).toInt()).toFloat()
            balloon = HotAirBalloon(
                x = xPos,
                y = yPos,
                width = balloonWidth,
                height = balloonHeight,
                bitmap = balloonBitmap,
                fromLeft = fromLeft
            )
        }
        balloon?.update()
        balloon?.let { b ->
            if (!b.hasDroppedBoot && player != null) {
                val reachedMiddle = (b.fromLeft && b.x + b.width / 2f >= screenWidth / 2f) ||
                        (!b.fromLeft && b.x + b.width / 2f <= screenWidth / 2f)
                if (reachedMiddle) {
                    boots.add(
                        Boot(
                            x = b.x + b.width / 2f - bootBitmap.width / 2f,
                            y = b.y + b.height,
                            width = bootBitmap.width,
                            height = bootBitmap.height,
                            bitmap = bootBitmap,
                            targetX = player!!.x + player!!.width / 2f,
                            targetY = player!!.y + player!!.height / 2f,
                            speed = 13f
                        )
                    )
                    b.hasDroppedBoot = true
                }
            }
        }
        if (balloon?.isOffScreen(screenWidth) == true) balloon = null

        // --- Avión + Fruta ---
        val planeWidth = (screenWidth / PLANE_WIDTH_RATIO).toInt()
        val planeHeight = (planeWidth * PLANE_HEIGHT_MULT).toInt()
        if (score > 0 && score % PLANE_SCORE_INTERVAL == 0 && plane == null) {

            if(!planeSpawned){
                PLANE_SCORE_INTERVAL = PLANE_SCORE_INTERVAL - 20
                planeSpawned = true
            }

            val excludeY = balloon?.y?.let { it..(it + balloon!!.height) }
            val yPlane = Plane.randomY(excludeY)
            plane = Plane(screenWidth.toFloat(), yPlane, planeWidth, planeHeight, planeBitmap)
        }
        plane?.let { p ->
            p.update()
            if (!p.hasDroppedItem) {
                val reachedMiddle = p.x + p.width / 2f <= screenWidth / 2f
                if (reachedMiddle) {
                    val targetX = Random.nextInt(0, screenWidth - 60).toFloat()
                    val targetY = Random.nextInt(100, screenHeight - 100).toFloat()
                    activeFruit = Fruit(
                        p.x + p.width / 2f - 30f,
                        p.y + p.height / 2f,
                        60, 60, fruitBitmap,
                        targetX, targetY, 8f
                    )
                    p.hasDroppedItem = true
                }
            }
            if (p.isOffScreen()) plane = null
        }

        // --- Fruit ---
        activeFruit?.let { f ->
            player?.let { p ->
                if (f.checkHitPlayer(p)) {
                    lives++
                    SoundManager.playSound("fruitCatch")
                    player?.state = PlayerState.HAPPY
                    player?.stateTimer = 60
                    activeFruit = null
                    return@let
                }
            }
            f.update(screenWidth, screenHeight)
            if (f.isOffScreen(screenWidth, screenHeight)) activeFruit = null
        }

        // --- OVNI + Estrella ---
        val ufoWidth = (screenWidth / UFO_WIDTH_RATIO).toInt()
        val ufoHeight = (ufoWidth * UFO_HEIGHT_MULT).toInt()
        if (score > 0 && score % UFO_SCORE_INTERVAL == 0 && ufo == null) {
            if(!ufoSpawned){
                UFO_SCORE_INTERVAL = UFO_SCORE_INTERVAL - 100
                ufoSpawned = true
            }
            val fromLeft = Random.nextBoolean()
            val yPos = Random.nextInt(100, (screenHeight * 0.6).toInt()).toFloat()
            val xPos = if (fromLeft) -ufoWidth.toFloat() else screenWidth.toFloat()
            ufo = UFO(xPos, yPos, ufoWidth, ufoHeight, ufoBitmap, fromLeft, screenWidth)
        }
        ufo?.let { u ->
            u.update()
            if (!u.hasDroppedItem) {
                val reachedMiddle = (u.fromLeft && u.x + u.width / 2f >= screenWidth / 2f) ||
                        (!u.fromLeft && u.x + u.width / 2f <= screenWidth / 2f)
                if (reachedMiddle) {
                    activeStar = Star(
                        u.x + u.width / 2f - 30f,
                        u.y + u.height / 2f,
                        60, 60, starBitmap,
                        screenWidth, screenHeight, 8f
                    )
                    u.hasDroppedItem = true
                }
            }
            if (u.isOffScreen()) ufo = null
        }

        // --- Star ---
        activeStar?.let { s ->
            player?.let { p ->
                if (s.checkHitPlayer(p)) {
                    doublePointsActive = true
                    doublePointsTimer = DOUBLE_POINTS_DURATION
                    SoundManager.playSound("starCatch")
                    player?.state = PlayerState.DOUBLE_POINTS
                    player?.stateTimer = DOUBLE_POINTS_DURATION
                    activeStar = null
                    return@let
                }
            }
            s.update(screenWidth, screenHeight)
            if (s.isOffScreen(screenWidth, screenHeight)) activeStar = null
        }

        // --- Cats ---
        val iterator = cats.iterator()
        while (iterator.hasNext()) {
            val cat = iterator.next()
            val previousY = cat.y
            cat.update(difficultyMultiplier, screenWidth, screenHeight, player!!)
            if (cat.isOffScreen(screenWidth)) {
                iterator.remove()
            } else if (cat.isCaught(player!!, previousY)) {
                iterator.remove()
                val points = if (doublePointsActive) 20 else 10
                score += points
                SoundManager.playSound("catHappy")
                    // aumentar racha
                    streak++
                    if (streak > maxStreak) {
                        maxStreak = streak
                    }

                // subir dificultad cada 50 pts


// aplicar incremento por score sólo si alcanzamos el siguiente umbral
                if (score >= nextScoreThreshold) {
                    // calcular cuántos umbrales hemos pasado (por si saltaste más de uno)
                    val steps = score / scoreThresholdStep
                    // aplicar incrementos en pasos hasta adecuar nextScoreThreshold
                    // (alternativa: aplicar sólo un paso y aumentar nextScoreThreshold += scoreThresholdStep)
                    difficultyMultiplier += difficultyIncreasePerScore * steps
                    spawnInterval = (spawnInterval - 5 * steps).coerceAtLeast(minSpawnInterval)
                    // fijar el siguiente umbral (simple)
                    nextScoreThreshold = (steps + 1) * scoreThresholdStep
                }


            }
        }

        // Si ya terminó el nivel: iniciar transición (pausa reducida)
        if (catsSpawned >= catsPerLevel[level - 1] && cats.isEmpty() && level < catsPerLevel.size) {
            level++
            // prepare assets for the next level and start transition
            loadLevelAssets(level)
            levelTransition = true
            levelTransitionTimer = 90 // <-- REDUCIDO: 90 frames ≈ 1.5s
            //dificultad al comenzar la transición
            if (level < levelDifficulty.size) {
                difficultyMultiplier = levelDifficulty[level - 1].first
                spawnInterval = levelDifficulty[level - 1].second
            }

        }

        // --- Botas ---
        val bootIterator = boots.iterator()
        while (bootIterator.hasNext()) {
            val boot = bootIterator.next()
            boot.update()
            if (boot.isOffScreen(screenWidth, screenHeight)) bootIterator.remove()
            else if (player != null && boot.hasHitPlayer(player!!)) {
                SoundManager.playSound("bootCrash")
                player?.state = PlayerState.OUCH
                player?.stateTimer = 60
                lives--
                bootIterator.remove()
            }
        }

        // --- Timer de bonus ---
        if (doublePointsActive) {
            doublePointsTimer--
            if (doublePointsTimer <= 0) doublePointsActive = false
        }

        // --- Game Over ---
        if (lives <= 0 && !gameOver) {
            gameOver = true
            SoundManager.playSound("gameOver")

// Obtener máximos históricos guardados
            val prefs = context.getSharedPreferences("CATIBOX_PREFS", Context.MODE_PRIVATE)

// Obtener máximos históricos guardados
            val maxScoreHist = prefs.getInt("MAX_SCORE_HIST", 0)
            val maxStreakHist = prefs.getInt("MAX_STREAK_HIST", 0)

            //comparo si son nuevos maximos
            val isNewHighScore = score > maxScoreHist
            val isNewHighStreak = maxStreak > maxStreakHist

// Comparar y actualizar explícitamente

            if (score > maxScoreHist) {
                prefs.edit().putInt("MAX_SCORE_HIST", score).apply()
            }

            if (maxStreak > maxStreakHist) {
                prefs.edit().putInt("MAX_STREAK_HIST", maxStreak).apply()
            }

            val intent = android.content.Intent(context, GameOverActivity::class.java)
            intent.putExtra("SCORE", score)
            intent.putExtra("MAX_STREAK", maxStreak)  //
            intent.putExtra("NEW_HIGH_SCORE", isNewHighScore)
            intent.putExtra("NEW_HIGH_STREAK", isNewHighStreak)
            context.startActivity(intent)
            pauseThread()
        }
    }

    override fun draw(canvas: Canvas) {
        super.draw(canvas)
        if (player == null) return

        // --- Background crossfade (synchronized with levelTransitionTimer) ---
        if (backgroundTransition && currentBackground != null && nextBackground != null && levelTransition) {
            val total = levelTransitionTimer.takeIf { it > 0 }?.let { 90f } ?: 90f
            val elapsed = (90f - levelTransitionTimer.toFloat()).coerceAtLeast(0f)
            val progress = (elapsed / total).coerceIn(0f, 1f) // 0..1

            // draw current with inverse alpha, then next with progress alpha
            val paint = Paint()
            val currAlpha = ((1f - progress) * 255).toInt().coerceIn(0, 255)
            paint.alpha = currAlpha
            canvas.drawBitmap(currentBackground!!, 0f, 0f, paint)

            paint.alpha = (progress * 255).toInt().coerceIn(0, 255)
            canvas.drawBitmap(nextBackground!!, 0f, 0f, paint)

            // when finished, finalize
            if (progress >= 1f) {
                currentBackground = nextBackground
                nextBackground = null
                backgroundTransition = false
            }
        } else {
            // normal draw
            currentBackground?.let { canvas.drawBitmap(it, 0f, 0f, null) }
        }

        // --- Draw entities ---
        for (cat in cats) cat.draw(canvas)
        player?.draw(
            canvas,
            normalBitmap = playerBitmap,
            doublePointsBitmap = playerBonusBitmap,
            sadBitmap = playerSadBitmap,
            happyBitmap = playerHappyBitmap,
            ouchBitmap = playerOuchBitmap
        )
        for (boot in boots) boot.draw(canvas)
        balloon?.draw(canvas)
        plane?.draw(canvas)
        ufo?.draw(canvas)
        activeFruit?.draw(canvas)
        activeStar?.draw(canvas)

        // --- Grass crossfade (synchronized) ---
        if (grassTransition && currentGrass != null && nextGrass != null && levelTransition) {
            val total = 90f
            val elapsed = (90f - levelTransitionTimer.toFloat()).coerceAtLeast(0f)
            val progress = (elapsed / total).coerceIn(0f, 1f)
            val paint = Paint()

            paint.alpha = ((1f - progress) * 255).toInt().coerceIn(0, 255)
            canvas.drawBitmap(currentGrass!!, -50f, screenHeight - currentGrass!!.height.toFloat() + 30f, paint)

            paint.alpha = (progress * 255).toInt().coerceIn(0, 255)
            canvas.drawBitmap(nextGrass!!, -50f, screenHeight - nextGrass!!.height.toFloat() + 30f, paint)

            if (progress >= 1f) {
                currentGrass = nextGrass
                nextGrass = null
                grassTransition = false
            }
        } else {
            currentGrass?.let { canvas.drawBitmap(it, -50f, screenHeight - it.height.toFloat() + 30f, null) }
        }

        // --- UI: score, lives, mute icon ---
        val livesText = "Lives: $lives"
        val livesTextWidth = livesPaint.measureText(livesText)
        val livesX = screenWidth - livesTextWidth - 30f
        val livesY = 150f
        canvas.drawText("Score: $score", 30f, 150f, paint)
        canvas.drawText("Streak: $streak  (Max: $maxStreak)", 30f, 220f, streakPaint)

        canvas.drawText(livesText, livesX, livesY, livesPaint)

        val icon = if (SoundManager.isMuted) muteIcon else unmuteIcon
        val muteX = livesX + livesTextWidth / 2 - muteButtonSize / 2
        val muteY = livesY + 30f
        canvas.drawBitmap(icon, muteX, muteY, null)
        muteButtonRect.set(muteX, muteY, muteX + muteButtonSize, muteY + muteButtonSize)

        // --- Overlay Game Over ---
        if (gameOver) {
            val overlay = Paint().apply { color = Color.argb(180, 0, 0, 0) }
            canvas.drawRect(0f, 0f, screenWidth.toFloat(), screenHeight.toFloat(), overlay)
        }

        // --- Mensaje de transición de nivel con fade & zoom (synchronized) ---
        if (levelTransition) {
            // levelTransitionTimer va de 90 -> 0
            val total = 90f
            val elapsed = (total - levelTransitionTimer.toFloat()).coerceAtLeast(0f) // 0 -> total
            val progress = (elapsed / total).coerceIn(0f, 1f) // 0..1

            // Alpha: fade-in (0..0.3), full (0.3..0.7), fade-out (0.7..1)
            val alpha = when {
                progress < 0.3f -> (progress / 0.3f * 255).toInt()
                progress > 0.7f -> ((1f - progress) / 0.3f * 255).toInt()
                else -> 255
            }.coerceIn(0, 255)
            levelPaint.alpha = alpha

            // Zoom: gentle ease-out
            val easeOut = 1f - (1f - progress) * (1f - progress) // quadratic ease-out
            val baseScale = 0.85f
            val scaleRange = 0.25f
            val scale = baseScale + scaleRange * easeOut

            // Draw centered scaled text using canvas save/restore
            val cx = screenWidth / 2f
            val cy = screenHeight / 2f

            canvas.save()
            canvas.translate(cx, cy)
            canvas.scale(scale, scale)
            canvas.drawText("LEVEL $level", 0f, 0f, levelPaint)
            canvas.restore()
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val x = event.x
        val y = event.y

        if (!gameOver) {
            when (event.action) {
                MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                    if (muteButtonRect.contains(x, y) && event.action == MotionEvent.ACTION_DOWN) {
                        toggleMute()
                        return true
                    }
                    player?.x = x - player!!.width / 2f
                    if (player!!.x < 0f) player!!.x = 0f
                    if (player!!.x + player!!.width > screenWidth.toFloat()) player!!.x = screenWidth.toFloat() - player!!.width
                }
            }
        }
        return true
    }

    private fun toggleMute() {
        SoundManager.toggleMute()
        if (SoundManager.isMuted) backgroundPlayer?.setVolume(0f, 0f)
        else backgroundPlayer?.setVolume(1f, 1f)
    }

    fun setBackgroundPlayer(player: MediaPlayer) {
        backgroundPlayer = player
        if (SoundManager.isMuted) backgroundPlayer?.setVolume(0f, 0f)
    }

    private fun spawnCat() {
        val baseWidth = (screenWidth / CAT_BASE_RATIO).toInt()
        val catWidth = (baseWidth * (1f - difficultyMultiplier * 0.05f)).toInt().coerceAtLeast(baseWidth / 2)
        val catHeight = catWidth
        val xPos = (0..(screenWidth - catWidth)).random().toFloat()
        val startY = -catHeight.toFloat()

        // calcular frames estimados hasta llegar al suelo (coordenada suelo ~ player Y o screenHeight - some margin)
        val groundY = screenHeight.toFloat() - /* margen si tienes */ 100f // ajustar igual que player Y margin
        val pixelsToTravel = (groundY - startY).coerceAtLeast(1f)

        // asumimos velocidad = CAT_BASE_FALL_SPEED * difficultyMultiplier
        val fallSpeed = CAT_BASE_FALL_SPEED * difficultyMultiplier
        val estimatedFramesToLand = (pixelsToTravel / fallSpeed).toInt()

        // calcular frame absoluto estimado
        val estimatedLandingFrame = frameCount + estimatedFramesToLand

        // comprobar si existe otro gato con landing cercano
        var conflict = false
        for (c in cats) {
            // si tus Cat tienen y/height actuales usamos eso para estimar su landing frame
            val remainingPixels = (groundY - c.y).coerceAtLeast(1f)
            val cFallSpeed = CAT_BASE_FALL_SPEED * difficultyMultiplier // si se usa la misma
            val cFramesToLand = (remainingPixels / cFallSpeed).toInt()
            val cLandingFrame = frameCount + cFramesToLand
            if (kotlin.math.abs(cLandingFrame - estimatedLandingFrame) < minLandingGapFrames) {
                conflict = true
                break
            }
        }

        if (conflict) {
            // si hay conflicto, no hacemos spawn ahora; simplemente salimos.
            // Esto reducirá la tasa de spawn en momentos críticos.
            return
        }

        val catBitmapScaled = Bitmap.createScaledBitmap(catBitmap, catWidth, catHeight, false)
        cats.add(
            Cat(xPos, startY, catWidth, catHeight, catBitmapScaled) {
                SoundManager.playSound("catAngry")
                streak = 0 // reiniciar racha
                player?.state = PlayerState.SAD
                lives--
                player?.stateTimer = 60 // 1 segundo a 60fps
            }
        )
    }


    fun pauseThread() { thread?.setRunning(false) }
    fun resumeThread() {
        if (thread != null && !thread!!.isAlive) {
            thread?.setRunning(true)
            thread?.start()
        }
    }
}
