package com.chifuzgames.catibox

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.graphics.drawable.Drawable
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.core.content.ContextCompat
import com.chifuzgames.catibox.entities.Boot
import com.chifuzgames.catibox.entities.Cat
import com.chifuzgames.catibox.entities.Fruit
import com.chifuzgames.catibox.entities.HotAirBalloon
import com.chifuzgames.catibox.entities.Plane
import com.chifuzgames.catibox.entities.Player
import com.chifuzgames.catibox.entities.PlayerState
import com.chifuzgames.catibox.entities.Star
import com.chifuzgames.catibox.entities.UFO
import com.chifuzgames.catibox.managers.SoundManager
import com.chifuzgames.catibox.ui.HUD
import kotlin.random.Random
import androidx.core.graphics.scale
import androidx.core.graphics.createBitmap
import com.chifuzgames.catibox.entities.CatState

class GameView(context: Context, attrs: AttributeSet? = null) : SurfaceView(context, attrs), SurfaceHolder.Callback {

    companion object {
        const val PLAYER_WIDTH_RATIO = 4f
        const val PLAYER_HEIGHT_MULT = 1.8f
        const val CAT_BASE_RATIO = 7f
        const val BALLOON_WIDTH_RATIO = 3.5f
        const val BALLOON_HEIGHT_MULT = 1.65f
        const val PLANE_WIDTH_RATIO = 3.5f
        const val PLANE_HEIGHT_MULT = 0.55f
        const val UFO_WIDTH_RATIO = 3.5f
        const val UFO_HEIGHT_MULT = 0.55f

        var BALLOON_SCORE_INTERVAL = 120
        var PLANE_SCORE_INTERVAL = 200
        var UFO_SCORE_INTERVAL = 310
    }
    private lateinit var hud: HUD
    private lateinit var backgroundPlayer: MediaPlayer

    // Bitmaps

    private lateinit var playerBitmap: Bitmap

    private lateinit var playerWalkBitmap: Bitmap

    private lateinit var playerBonusBitmap: Bitmap

    private lateinit var playerBonusWalkBitmap: Bitmap
    private lateinit var playerSadBitmap : Bitmap

    private lateinit var playerSadWalkBitmap : Bitmap
    private lateinit var playerOuchBitmap : Bitmap

    private lateinit var playerOuchWalkBitmap : Bitmap
    private lateinit var playerHappyBitmap : Bitmap

    private lateinit var playerHappyWalkBitmap : Bitmap

    private lateinit var playerSpaceBitmap: Bitmap
    private lateinit var playerSpaceWalkBitmap: Bitmap

    private lateinit var catBitmap: Bitmap

    private lateinit var catSpaceBitmap: Bitmap

    private lateinit var backgroundBitmap: Bitmap

    private lateinit var muteIcon: Bitmap
    private lateinit var unmuteIcon: Bitmap
    private lateinit var balloonBitmap: Bitmap
    private lateinit var planeBitmap: Bitmap
    private lateinit var ufoBitmap: Bitmap
    private lateinit var bootBitmap: Bitmap
    private lateinit var fruitBitmap: Bitmap
    private lateinit var starBitmap: Bitmap

    // GameView
    var isPaused = false
    var isMuted = false


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
    var isAbandoned = false

    var score = 0
    var lives = 5
    private var difficultyMultiplier = 2.5f

    // Música de fondo

    // Estado game over
    var gameOver = false
        private set

    // Mute button
    private val muteButtonSize = 100f

    // --- Variables globales de GameView ---
    private var doublePointsActive = false
    private var doublePointsTimer = 0f // en frames
    private val DOUBLE_POINTS_DURATION = 10 * 60 // 10 segundos * 60 fps

    // --- Niveles ---
    var level = 1
    private var catsSpawned = 0
    private val catsPerLevel = listOf(10, 12, 14, 16,18,20,22,24,26,50,14, 16, 18,20,22,24,26,28,30, 200) // nivel 20 200
    private var levelTransition = false
    private var levelTransitionTimer = 0f

    private var lastCatSpawnTimeMs = System.currentTimeMillis()
    private var catSpawnIntervalMs = 3000L // 3 segundos por defecto, se ajusta por nivel

    var planeSpawned = false
    var ufoSpawned = false
    private var lastBalloonScore = 0

    private var lastTouchX = 0f
    private var isDragging = false
    // resource names per level (we'll resolve ids at runtime; fallback to default if missing)
    private val backgroundNames = listOf("background", "background2", "background3", "background4", "background5","background6", "background7", "background8", "background9", "background10","background11", "background12", "background13", "background14", "background15","background16", "background17", "background18", "background19", "background20")

    private var currentBackground: Bitmap? = null
    private var nextBackground: Bitmap? = null
    private var backgroundTransition = false

    private val catBitmapCache = mutableMapOf<Int, Bitmap>()
    private val catSpaceBitmapCache = mutableMapOf<Int, Bitmap>()

    private var transitionFramesOut = mutableListOf<Bitmap>()
    private var transitionFramesIn = mutableListOf<Bitmap>()
    private var currentTransitionFrame = 0
    private var isTransitionPlaying = false
    private var isTransitionOut = true
    private var transitionTimer = 0f
    private val baseTransitionFrameDuration = 0.08f * 60f // base: 0.08s * 60fps ≈ 5 frames
    private var transitionSpeedMultiplier = 6f          // 1.0 = normal, >1 = más lento, <1 = más rápido
    private var transitionFrameDuration = baseTransitionFrameDuration * transitionSpeedMultiplier

    private enum class TransitionMode { NONE, OUT_ONLY, IN }
    private var currentTransitionMode = TransitionMode.NONE
    private var transitionTargetLevel = -1
    private val completedOutTransitions = mutableSetOf<Int>() // niveles que ya mostraron OUT
    private val completedInTransitions = mutableSetOf<Int>()  // niveles que ya mostraron IN

    private var gameCompleted = false
    private var gameCompletedTimer = 0f

    // --- Racha (streak) ---
    private var streak = 0
     var maxStreak = 0

    // --- Dificultad ajustable ---
    private var initialLives = 5
    // Vidas iniciales del jugador al comenzar la partida.
    private var initialLevel = 1  // <--- nivel inicial configurable

    private var initialScore = 0      // ← puntaje inicial configurable
    private var initialStreak = 0     // ← racha inicial configurable


    //dificultad por gato
    private var difficultyIncreasePerCat = 0.05f
    //cada cuantos gatos aumenta

    private val levelDifficulty = listOf(
        Pair(0.8f, 120), // nivel 1: velocidad base x1, intervalo . (dificultad inicial por nivel, cada cuanto aparece un gato)
        Pair(1f, 100), // nivel 2: más rápido, menos intervalo
        Pair(1.2f, 90), // nivel 3
        Pair(1.5f, 70), // nivel 4
        Pair(1.8f, 60),  // nivel 5
        Pair(1.9f, 50),  // nivel 6
        Pair(2.0f, 45),  // nivel 7
        Pair(2.1f, 40),  // nivel 8
        Pair(2.2f, 30),  // nivel 9
        Pair(1f, 30),  // nivel 10+ bonus
        Pair(1.2f, 90), // nivel 11
        Pair(1.5f, 80), // nivel 12
        Pair(1.7f, 70), // nivel 13
        Pair(1.9f, 60), // nivel 14
        Pair(2.0f, 50),  // nivel 15
        Pair(2.1f, 40),  // nivel 16
        Pair(2.2f, 35),  // nivel 17
        Pair(2.3f, 30),  // nivel 18
        Pair(2.4f, 27),  // nivel 19
        Pair(1.5f, 15),  // nivel 20+ bonus

    )

    init {
        holder.addCallback(this)
        isFocusable = true
        SoundManager.init(context)
        initIcons()
    }

    private fun initIcons() {
        muteIcon = drawableToBitmap(ContextCompat.getDrawable(context, R.drawable.ic_mute)!!, muteButtonSize.toInt(), muteButtonSize.toInt())
        unmuteIcon = drawableToBitmap(ContextCompat.getDrawable(context, R.drawable.ic_unmute)!!, muteButtonSize.toInt(), muteButtonSize.toInt())
    }

    private fun drawableToBitmap(drawable: Drawable, width: Int, height: Int): Bitmap {
        val bitmap = createBitmap(width, height)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }

    override fun surfaceCreated(holder: SurfaceHolder) {

        hud = HUD(context)
        hud.initIcons()
        hud.lives = initialLives
        hud.score = initialScore
        hud.streak = initialStreak
        hud.maxStreak = initialStreak

        score = initialScore
        streak = initialStreak
        maxStreak = initialStreak

        screenWidth = width
        screenHeight = height

        hud.onPauseToggle = {
            isPaused = !isPaused   // invertir valor real
            hud.isPaused = isPaused // actualizar HUD para que dibuje correctamente
        }
        hud.onMuteToggle = {
            isMuted = !isMuted        // invertir estado real
            hud.isMuted = isMuted     // actualizar HUD para mostrar icono correcto
            toggleMute()              // activar/desactivar sonido
        }

        for (i in 1..7) {
            val resId = getDrawableIdByName("bg_transition_out$i")
            transitionFramesOut.add(decodeSampledBitmapFromResource(resId, screenWidth, screenHeight))
        }
        for (i in 1..4) {
            val resId = getDrawableIdByName("bg_transition_in$i")
            transitionFramesIn.add(decodeSampledBitmapFromResource(resId, screenWidth, screenHeight))
        }

        val playerWidth = (screenWidth / PLAYER_WIDTH_RATIO).toInt()
        val playerHeight = (playerWidth * PLAYER_HEIGHT_MULT).toInt()
        playerBitmap = decodeSampledBitmapFromResource(R.drawable.player, playerWidth, playerHeight)
        playerWalkBitmap = decodeSampledBitmapFromResource(R.drawable.player_walk, playerWidth, playerHeight)
        playerBonusBitmap = decodeSampledBitmapFromResource(R.drawable.player_bonus, playerWidth, playerHeight)
        playerBonusWalkBitmap = decodeSampledBitmapFromResource(R.drawable.player_bonus_walk, playerWidth, playerHeight)
        playerSadBitmap = decodeSampledBitmapFromResource(R.drawable.player_sad, playerWidth, playerHeight)
        playerSadWalkBitmap = decodeSampledBitmapFromResource(R.drawable.player_sad_walk, playerWidth, playerHeight)
        playerHappyBitmap = decodeSampledBitmapFromResource(R.drawable.player_happy, playerWidth, playerHeight)
        playerHappyWalkBitmap = decodeSampledBitmapFromResource(R.drawable.player_happy_walk, playerWidth, playerHeight)
        playerOuchBitmap = decodeSampledBitmapFromResource(R.drawable.player_ouch, playerWidth, playerHeight)
        playerOuchWalkBitmap = decodeSampledBitmapFromResource(R.drawable.player_ouch_walk, playerWidth, playerHeight)
        playerSpaceBitmap = decodeSampledBitmapFromResource(R.drawable.player_space,playerWidth,playerHeight)
        playerSpaceWalkBitmap = decodeSampledBitmapFromResource(R.drawable.player_space_walk,playerWidth,playerHeight)
        catBitmap = decodeSampledBitmapFromResource(R.drawable.cat, (screenWidth / CAT_BASE_RATIO).toInt(), (screenWidth / CAT_BASE_RATIO).toInt())
        catSpaceBitmap = decodeSampledBitmapFromResource(R.drawable.cat_space, (screenWidth / CAT_BASE_RATIO).toInt(), (screenWidth / CAT_BASE_RATIO).toInt())


        // load defaults (kept for compatibility)
        backgroundBitmap = decodeSampledBitmapFromResource(R.drawable.background, screenWidth, screenHeight, true)

        balloonBitmap = BitmapFactory.decodeResource(resources, R.drawable.hot_air_balloon)
        planeBitmap = BitmapFactory.decodeResource(resources, R.drawable.plane)
        ufoBitmap = BitmapFactory.decodeResource(resources, R.drawable.ufo)
        bootBitmap = BitmapFactory.decodeResource(resources, R.drawable.boot)
        fruitBitmap = BitmapFactory.decodeResource(resources, R.drawable.fruit)
        starBitmap = BitmapFactory.decodeResource(resources, R.drawable.star)

        val balloonWidth = (screenWidth / BALLOON_WIDTH_RATIO).toInt()
        val balloonHeight = (balloonWidth * BALLOON_HEIGHT_MULT).toInt()
        val planeWidth = (screenWidth / PLANE_WIDTH_RATIO).toInt()
        val planeHeight = (planeWidth * PLANE_HEIGHT_MULT).toInt()
        val ufoWidth = (screenWidth / UFO_WIDTH_RATIO).toInt()
        val ufoHeight = (ufoWidth * UFO_HEIGHT_MULT).toInt()

        balloonBitmap = balloonBitmap.scale(balloonWidth, balloonHeight, false)
        planeBitmap = planeBitmap.scale(planeWidth, planeHeight, false)
        ufoBitmap = ufoBitmap.scale(ufoWidth, ufoHeight, false)
        bootBitmap = bootBitmap.scale(balloonWidth / 3, balloonHeight / 3, false)
        fruitBitmap = fruitBitmap.scale(60, 60, false)
        starBitmap = starBitmap.scale(60, 60, false)

        // init player
        player = Player(
            x = screenWidth / 2f - playerWidth / 2f,
            y = screenHeight.toFloat() - playerHeight - 150f,
            width = playerWidth,
            height = playerHeight
        )
        player?.setWalkBitmap(playerWalkBitmap, playerOuchWalkBitmap,playerSadWalkBitmap, playerHappyWalkBitmap,playerBonusWalkBitmap,playerSpaceWalkBitmap)

        // inicializar línea de partida
// inicializar línea de partida y dificultad del nivel 1
        lives = initialLives
        level = initialLevel
        catsSpawned = 0
        levelTransition = false

// aplicar dificultad inicial desde levelDifficulty[0]
        difficultyMultiplier = levelDifficulty[0].first
        catSpawnIntervalMs = (levelDifficulty[0].second / 60f * 1000).toLong()


// preferRgb565 = false para conservar alfa o mejor color en el fondo
// Ajustar dificultad y parámetros al nivel inicial
        val idx = (initialLevel - 1).coerceIn(0, levelDifficulty.size - 1)
        difficultyMultiplier = levelDifficulty[idx].first
        catSpawnIntervalMs = (levelDifficulty[idx].second / 60f * 1000).toLong()

// Cargar los fondos correspondientes al nivel inicial
        currentBackground = decodeSampledBitmapFromResource(
            getDrawableIdByName(backgroundNames.getOrElse(idx) { backgroundNames.last() }),
            screenWidth, screenHeight, preferRgb565 = false
        )

        thread = GameThread(holder, this)
        thread?.stopThread(true)
        thread?.start()
    }

    private fun getCatBitmapForWidth(width: Int): Bitmap {
        return catBitmapCache[width] ?: run {
            val bmp = catBitmap.scale(width, width, false)
            catBitmapCache[width] = bmp
            bmp
        }
    }
    private fun getCatSpaceBitmapForWidth(width: Int): Bitmap {
        return catSpaceBitmapCache[width] ?: run {
            val bmp = catSpaceBitmap.scale(width, width, false)
            catSpaceBitmapCache[width] = bmp
            bmp
        }
    }

    @SuppressLint("DiscouragedApi")
    private fun getDrawableIdByName(name: String): Int {
        val id = resources.getIdentifier(name, "drawable", context.packageName)
        return if (id != 0) id else R.drawable.background // fallback to background if not found
    }


    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {}
    override fun surfaceDestroyed(holder: SurfaceHolder) {
        // 1. Detener hilo
        var retry = true
        thread?.stopThread(false)
        while (retry) {
            try {
                thread?.join()
                retry = false
            } catch (_: InterruptedException) {}
        }
        thread = null

        // 2. Reciclar bitmaps sin reflexión
        listOf(
            playerBitmap, playerBonusBitmap, playerSadBitmap, playerHappyBitmap, playerOuchBitmap,playerSpaceBitmap,
            catBitmap, backgroundBitmap, balloonBitmap, planeBitmap,
            ufoBitmap, bootBitmap, fruitBitmap, starBitmap
        ).forEach { bmp ->
            bmp.takeIf { !it.isRecycled }?.recycle()
        }

        // 3. Limpiar caches
        catBitmapCache.values.forEach { it.recycle() }
        catSpaceBitmapCache.values.forEach { it.recycle() }

        catBitmapCache.clear()
        catSpaceBitmapCache.clear()

    }

    private fun loadLevelAssets(nextLevel: Int) {
        if (isTransitionPlaying) return  // <-- no hacer nada mientras transiciona

        val bgName = backgroundNames.getOrNull(nextLevel - 1) ?: backgroundNames.last()

        val bgId = getDrawableIdByName(bgName)

        nextBackground = decodeSampledBitmapFromResource(bgId, screenWidth, screenHeight, preferRgb565 = true)
        // enable transitions
        backgroundTransition = true
    }

    fun update(deltaTime: Float) {
        if (player == null || gameOver || isPaused) return
        
        if (isTransitionPlaying) {
            backgroundPlayer.setVolume(0.4f, 0.4f)

            transitionTimer -= deltaTime * 60f
            if (transitionTimer <= 0f) {
                transitionTimer = transitionFrameDuration
                currentTransitionFrame++
                val maxFrames = if (isTransitionOut) transitionFramesOut.size else transitionFramesIn.size

                if (currentTransitionFrame >= maxFrames) {
                    when (currentTransitionMode) {
                        TransitionMode.OUT_ONLY -> {
                            // OUT terminó: lo marcamos como completado y detenemos la animación.
                            completedOutTransitions.add(transitionTargetLevel)

                            // CARGAR nextBackground para el siguiente nivel (por ejemplo 10 -> 11).
                            // Usamos transitionTargetLevel + 1 como el level siguiente que deberá mostrarse en IN.
                            val nextLevel = transitionTargetLevel
                            if (nextBackground == null && nextLevel <= backgroundNames.size) {
                                val bgName = backgroundNames.getOrNull(nextLevel - 1) ?: backgroundNames.last()
                                val bgId = getDrawableIdByName(bgName)
                                // decodificamos el bitmap preparado (preferRgb565 = true para gastar menos memoria si está ok)
                                nextBackground = decodeSampledBitmapFromResource(bgId, screenWidth, screenHeight, preferRgb565 = true)
                                // indicamos que hay fondo preparado para la crossfade IN más adelante
                                backgroundTransition = true
                            }

                            isTransitionPlaying = false
                            // no auto-switch a IN
                            currentTransitionMode = TransitionMode.NONE
                            // Si querés, podrías forzar que el player quede en estado SPACE u otra cosa
                        }

                        TransitionMode.IN -> {
                            // IN terminó: aplicamos el nextBackground y marcamos completado
                            completedInTransitions.add(transitionTargetLevel)
                            isTransitionPlaying = false
                            currentTransitionMode = TransitionMode.NONE
                            levelTransition = false
                            catsSpawned = 0
                            currentBackground?.recycle()
                            currentBackground = nextBackground
                            nextBackground = null
                            backgroundTransition = false
                        }
                        else -> {
                            isTransitionPlaying = false
                            currentTransitionMode = TransitionMode.NONE
                        }
                    }
                }
            }
            return // mientras se anima la transición, no actualizar nada más
        }else{
            if (::backgroundPlayer.isInitialized) {
                if (SoundManager.isMuted) backgroundPlayer.setVolume(0f, 0f)
                else backgroundPlayer.setVolume(1f, 1f)
            }
        }

        // --- Comprobar final del juego ---
        if (level == 20 && catsSpawned >= 200 && !gameCompleted) {
            // iniciar contador de 3 segundos
            gameCompleted = true
            gameCompletedTimer = 3f * 60f // 3 segundos * 60 fps
        }

        if (gameCompleted) {
            gameCompletedTimer -= deltaTime * 60f
            if (gameCompletedTimer <= 0f) {
                triggerGameCompleted()
            }
        }
        // --- Transición de nivel ---
        if (levelTransition) {
            levelTransitionTimer -= deltaTime * 120
            if (levelTransitionTimer <= 0) {
                levelTransition = false
                catsSpawned = 0
                if (backgroundTransition && nextBackground != null) {
                    currentBackground?.recycle()
                    currentBackground = nextBackground
                    nextBackground = null
                    backgroundTransition = false
                }

            }
            return
        }

        if (level == 10 || level == 20){
            player?.state = PlayerState.SPACE
            deleteFlyingObject()
        }else{
            when(player?.state) {
                PlayerState.HAPPY, PlayerState.SAD, PlayerState.OUCH, PlayerState.DOUBLE_POINTS -> {} // dejar tal cual
                else -> player?.state = PlayerState.NORMAL
            }
        }


        player?.update(deltaTime)

        // --- Spawn de gatos usando tiempo absoluto ---
        val now = System.currentTimeMillis()
        if (now - lastCatSpawnTimeMs >= catSpawnIntervalMs && catsSpawned < catsPerLevel[level - 1]) {
            val spawned = spawnCat()
            if (spawned) catsSpawned++
            lastCatSpawnTimeMs = now
        }

        // --- Globo + Bota ---
        val balloonWidth = (screenWidth / BALLOON_WIDTH_RATIO).toInt()
        val balloonHeight = (balloonWidth * BALLOON_HEIGHT_MULT).toInt()
        if (score > 0 &&
            score % BALLOON_SCORE_INTERVAL == 0 &&
            score != lastBalloonScore &&
            balloon == null && level != 10 && level != 20) {

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

            lastBalloonScore = score  // <- actualizar para no repetir
        }


        balloon?.update(deltaTime)
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
                            speed = 9f
                        )
                    )
                    b.hasDroppedBoot = true

                }
            }
            if (b.isOffScreen(screenWidth)) balloon = null

        }

        // --- Avión + Fruta ---
        val planeWidth = (screenWidth / PLANE_WIDTH_RATIO).toInt()
        val planeHeight = (planeWidth * PLANE_HEIGHT_MULT).toInt()
        if (score > 0 && score % PLANE_SCORE_INTERVAL == 0 && plane == null && level != 10 && level != 20) {

            if(!planeSpawned){
                PLANE_SCORE_INTERVAL = PLANE_SCORE_INTERVAL - 20
                planeSpawned = true
            }

            val excludeY = balloon?.y?.let { it..(it + balloon!!.height) }
            val yPlane = Plane.randomY(excludeY)
            plane = Plane(screenWidth.toFloat(), yPlane, planeWidth, planeHeight, planeBitmap)
        }
        plane?.let { p ->
            p.update(deltaTime)
            if (!p.hasDroppedItem) {
                val reachedMiddle = p.x + p.width / 2f <= screenWidth / 2f
                if (reachedMiddle) {
                    activeFruit = Fruit(
                        p.x + p.width / 2f - 30f,
                        p.y + p.height / 2f,
                        70, 70, fruitBitmap,
                        8f
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
                    player?.stateTimer = 60f
                    activeFruit = null
                    return@let
                }
            }
            f.update(deltaTime, screenWidth)
            if (f.isOffScreen(screenHeight)) activeFruit = null
        }


        // --- OVNI + Estrella ---
        val ufoWidth = (screenWidth / UFO_WIDTH_RATIO).toInt()
        val ufoHeight = (ufoWidth * UFO_HEIGHT_MULT).toInt()
        if (score > 0 && score % UFO_SCORE_INTERVAL == 0 && ufo == null && level != 10 && level != 20) {
            if(!ufoSpawned){
                UFO_SCORE_INTERVAL = UFO_SCORE_INTERVAL - 60
                ufoSpawned = true
            }
            val fromLeft = Random.nextBoolean()
            val yPos = Random.nextInt(100, (screenHeight * 0.6).toInt()).toFloat()
            val xPos = if (fromLeft) -ufoWidth.toFloat() else screenWidth.toFloat()
            ufo = UFO(xPos, yPos, ufoWidth, ufoHeight, ufoBitmap, fromLeft, screenWidth)
        }
        ufo?.let { u ->
            u.update(deltaTime)
            if (!u.hasDroppedItem) {
                val reachedMiddle = (u.fromLeft && u.x + u.width / 2f >= screenWidth / 2f) ||
                        (!u.fromLeft && u.x + u.width / 2f <= screenWidth / 2f)
                if (reachedMiddle) {
                    activeStar = Star(
                        u.x + u.width / 2f - 30f,
                        u.y + u.height / 2f,
                        70, 70, starBitmap,
                        8f
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
                    doublePointsTimer = DOUBLE_POINTS_DURATION.toFloat()
                    SoundManager.playSound("starCatch")
                    player?.state = PlayerState.DOUBLE_POINTS
                    player?.stateTimer = DOUBLE_POINTS_DURATION.toFloat()
                    activeStar = null
                    return@let
                }
            }
            s.update(deltaTime, screenWidth, screenHeight)
            if (s.isOffScreen(screenHeight)) activeStar = null
        }


        val catIterator = cats.iterator()
        while (catIterator.hasNext()) {
            val cat = catIterator.next()
            val prevY = cat.y
            cat.update(deltaTime, difficultyMultiplier, screenWidth, screenHeight, player!!)
            if (cat.isOffScreen()) catIterator.remove()
            else if (cat.isCaught(player!!, prevY)) {
                catIterator.remove()
                val points = if (doublePointsActive) 20 else 10
                score += points
                SoundManager.playSound("catHappy")
                streak++
                if (streak > maxStreak) maxStreak = streak

                // dificultad
                difficultyMultiplier += difficultyIncreasePerCat

            }
        }

        // --- Boots ---
        val bootIterator = boots.iterator()
        while (bootIterator.hasNext()) {
            val boot = bootIterator.next()
            boot.update(deltaTime)
            if (boot.isOffScreen(screenWidth, screenHeight)) bootIterator.remove()
            else if (player != null && boot.hasHitPlayer(player!!)) {
                SoundManager.playSound("bootCrash")
                player?.state = PlayerState.OUCH
                player?.stateTimer = 1.0f
                lives--
                bootIterator.remove()
            }
        }

        // --- Bonus timer ---
        if (doublePointsActive) {
            doublePointsTimer -= deltaTime * 60f
            if (doublePointsTimer <= 0) doublePointsActive = false
        }

        // --- Game Over ---
        if (lives <= 0 && !gameOver && !isAbandoned) {
           // stopGame()

            triggerGameOver()
        }


        // --- Nivel ---
        if (catsSpawned >= catsPerLevel[level - 1] && cats.isEmpty() && level < catsPerLevel.size) {
            val newLevel = level + 1

            // caso 9 -> 10 o 19 -> 20: queremos mostrar solo OUT (no cargar bg entrada todavía)
            if ((newLevel == 10 || newLevel == 20) && !completedOutTransitions.contains(newLevel)) {
                level = newLevel
                // no llamar loadLevelAssets todavía (evitamos cargar fondo siguiente)
                levelTransition = true
                levelTransitionTimer = 1.5f * 60f
                applyLevelSettings(level)
                startTransition(TransitionMode.OUT_ONLY, level)
                // marcamos catsSpawned para el nuevo nivel
                catsSpawned = 0
            }
            // caso 10 -> 11 (mostrar IN): cargamos nextBackground y mostramos IN
            else if (level == 10 && newLevel == 11 && !completedInTransitions.contains(newLevel)) {
                level = newLevel
                // aplicamos settings (pero no sobreescribimos el fondo hasta que termine IN)
                levelTransition = true
                levelTransitionTimer = 1.5f * 60f
                applyLevelSettings(level)
                // start IN (esto cargará nextBackground dentro de startTransition)
                startTransition(TransitionMode.IN, level)
                catsSpawned = 0
            }
            // caso general
            else {
                level = newLevel
                // carga normal del fondo y transición cruzada
                loadLevelAssets(level)
                levelTransition = true
                levelTransitionTimer = 1.5f * 60f
                if (level < levelDifficulty.size) {
                    difficultyMultiplier = levelDifficulty[level - 1].first
                    catSpawnIntervalMs = (levelDifficulty[level - 1].second / 60f * 1000).toLong()
                }
                applyLevelSettings(level)
                catsSpawned = 0
            }
        }

    }

    override fun draw(canvas: Canvas) {
        super.draw(canvas)
        if (player == null) return

        // --- Dibujar transición entre niveles ---
        if (isTransitionPlaying) {
            val frame = if (isTransitionOut) transitionFramesOut.getOrNull(currentTransitionFrame) else transitionFramesIn.getOrNull(currentTransitionFrame)
            frame?.let { canvas.drawBitmap(it, 0f, 0f, null) }
            return
        }

        // --- Background crossfade (nivel) ---
        if (backgroundTransition && currentBackground != null && nextBackground != null && levelTransition) {
            val total = 90f
            val elapsed = (90f - levelTransitionTimer).coerceAtLeast(0f)
            val progress = (elapsed / total).coerceIn(0f, 1f)
            val paint = Paint()
            paint.alpha = ((1f - progress) * 255).toInt().coerceIn(0, 255)
            canvas.drawBitmap(currentBackground!!, 0f, 0f, paint)
            paint.alpha = (progress * 255).toInt().coerceIn(0, 255)
            canvas.drawBitmap(nextBackground!!, 0f, 0f, paint)
            if (progress >= 1f) {
                currentBackground = nextBackground
                nextBackground = null
                backgroundTransition = false
            }
        } else {
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
            ouchBitmap = playerOuchBitmap,
            spaceBitmap = playerSpaceBitmap
        )
        for (boot in boots) boot.draw(canvas)
        balloon?.draw(canvas)
        plane?.draw(canvas)
        ufo?.draw(canvas)
        activeFruit?.draw(canvas)
        activeStar?.draw(canvas)

        // --- HUD ---
        // Antes de hud.draw()
        hud.score = score
        hud.streak = streak
        hud.maxStreak = maxStreak
        hud.lives = lives
        hud.level = level
        hud.levelTransition = levelTransition
        hud.levelTransitionTimer = levelTransitionTimer
        hud.draw(canvas, width)


        // --- Overlay Game Over ---
        if (gameOver) {
            val overlay = Paint().apply { color = Color.argb(180, 0, 0, 0) }
            canvas.drawRect(0f, 0f, screenWidth.toFloat(), screenHeight.toFloat(), overlay)
        }


    }

    @SuppressLint("ClickableViewAccessibility")

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val x = event.x

        // Primero, que el HUD maneje el toque (mute/pause)
        if (hud.handleTouch(x, event.y, event)) return true

        if (!gameOver) {
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    lastTouchX = x
                    isDragging = true
                    player?.isSliding = false  // no caminar solo por tocar

                }
                MotionEvent.ACTION_MOVE -> {
                    if (isDragging) {
                        val deltaX = x - lastTouchX
                        // solo caminar si hay movimiento real
                        player?.isSliding = deltaX != 0f
                        player?.x = (player?.x ?: 0f) + deltaX
                        // Limitar al ancho de pantalla
                        player?.x = player!!.x.coerceIn(0f, screenWidth.toFloat() - player!!.width)
                        lastTouchX = x
                    }
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    isDragging = false
                    player?.isSliding = false     // actualizar player

                }
            }
        }
        return true
    }


    fun setBackgroundPlayer(player: MediaPlayer) {
        backgroundPlayer = player
        if (SoundManager.isMuted) backgroundPlayer.setVolume(0f, 0f)
    }

    fun setStats(startLives: Int = 5, startLevel:Int = 1, startScore:Int = 0){
        initialLives=startLives
        initialLevel = startLevel
        initialScore=startScore
      //  initialStreak=startStreak
    }
    /**
     * Intenta crear y añadir un gato. Devuelve true si efectivamente se creó y añadió.
     * Implementa hasta N intentos para encontrar una posición válida (no conflictiva).
     */
    private fun spawnCat(): Boolean {
        val maxAttempts = 6
        val baseWidth = (screenWidth / CAT_BASE_RATIO).toInt()

        repeat(maxAttempts) {
            val catWidth = (baseWidth * (1f - difficultyMultiplier * 0.05f)).toInt().coerceAtLeast(baseWidth / 2)
            val catHeight = catWidth
            val xPos = (0..(screenWidth - catWidth)).random().toFloat()
            val startY = -catHeight.toFloat()

            // Escalamos ambos bitmaps
            val catBitmapScaled = getCatBitmapForWidth(catWidth)
            val catSpaceBitmapScaled = getCatSpaceBitmapForWidth(catWidth)

            // Creamos el gato
            val cat = Cat(
                xPos, startY, catWidth, catHeight,
                catBitmapScaled,
                catSpaceBitmapScaled
            ) {
                // callback al tocar el suelo
                if (level != 10 && level != 20) {
                    SoundManager.playSound("catAngry")
                    streak = 0
                    player?.state = PlayerState.SAD
                    lives--
                    player?.stateTimer = 1.0f // 1 segundo
                }
            }

            // Si es nivel 10 o 20, activar el estado SPACE
            if (level == 10 || level == 20) {
                cat.state = CatState.SPACE
            }

            cats.add(cat)
            return true
        }
        return false
    }


    fun toggleMute() {
        SoundManager.toggleMute() // pausa/reanuda efectos

        // controlar música de fondo
        if (::backgroundPlayer.isInitialized) {
            if (SoundManager.isMuted) backgroundPlayer.setVolume(0f, 0f)
            else backgroundPlayer.setVolume(1f, 1f)
        }
    }

    fun pauseThread() { thread?.stopThread(false) }

    fun resumeThread() {
        if (thread == null || !thread!!.isAlive) {
            thread = GameThread(holder, this)
            thread?.stopThread(true)
            thread?.start()
        }
    }

    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val (height: Int, width: Int) = options.outHeight to options.outWidth
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2

            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }

    private fun decodeSampledBitmapFromResource(resId: Int, reqWidth: Int, reqHeight: Int, preferRgb565: Boolean = true): Bitmap {
        // 1) solo bounds
        val options = BitmapFactory.Options()
        options.inJustDecodeBounds = true
        BitmapFactory.decodeResource(resources, resId, options)

        // 2) calcular sample
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight)

        // 3) decodificar real
        options.inJustDecodeBounds = false
        if (preferRgb565) {
            options.inPreferredConfig = Bitmap.Config.RGB_565
        } else {
            options.inPreferredConfig = Bitmap.Config.ARGB_8888
        }

        val decoded = BitmapFactory.decodeResource(resources, resId, options)

        // 4) si el tamaño exacto no coincide, escalar y reciclar original
        if (decoded.width != reqWidth || decoded.height != reqHeight) {
            val scaled = decoded.scale(reqWidth, reqHeight)
            if (scaled != decoded) {
                decoded.recycle()
            }
            return scaled
        }
        return decoded
    }

    //resetea la dificultad en los niveles, se usa para despues del bonus
    private fun applyLevelSettings(level: Int) {
        val idx = (level - 1).coerceIn(0, levelDifficulty.size - 1)
        difficultyMultiplier = levelDifficulty[idx].first
        catSpawnIntervalMs = (levelDifficulty[idx].second / 60f * 1000).toLong()
        lastCatSpawnTimeMs = System.currentTimeMillis()

    }
    private fun triggerGameOver() {
        if (gameOver) return
        gameOver = true
        thread?.stopCompletely()

        thread?.stopThread(false)
        thread = null
        SoundManager.playSound("gameOver")
        holder.lockCanvas()?.let { canvas ->
            val paint = Paint()
            paint.color = Color.argb(200, 0, 0, 0)
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
            holder.unlockCanvasAndPost(canvas)
        }
        // Guardar datos de puntaje
        val prefs = context.getSharedPreferences("CATIBOX_PREFS", Context.MODE_PRIVATE)
        val isNewHighScore = score > prefs.getInt("MAX_SCORE_HIST", 0)
        val isNewHighStreak = maxStreak > prefs.getInt("MAX_STREAK_HIST", 0)
        prefs.edit().apply {
            if (isNewHighScore) putInt("MAX_SCORE_HIST", score)
            if (isNewHighStreak) putInt("MAX_STREAK_HIST", maxStreak)
            apply()
        }

        // Pausar hilo
        pauseThread()

        // Lanzar GameOverActivity en UI thread
        Handler(Looper.getMainLooper()).post {
            val intent = Intent(context, GameOverActivity::class.java).apply {
                putExtra("SCORE", score)
                putExtra("MAX_STREAK", maxStreak)
                putExtra("LEVEL", level)
                putExtra("NEW_HIGH_SCORE", isNewHighScore)
                putExtra("NEW_HIGH_STREAK", isNewHighStreak)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    }

    private fun startTransition(mode: TransitionMode, targetLevel: Int) {
        // si ya ejecutamos este tipo para ese nivel, no volvemos a hacerlo
        if (mode == TransitionMode.OUT_ONLY && completedOutTransitions.contains(targetLevel)) return
        if (mode == TransitionMode.IN && completedInTransitions.contains(targetLevel)) return

        currentTransitionMode = mode
        transitionTargetLevel = targetLevel
        isTransitionPlaying = true
        isTransitionOut = (mode == TransitionMode.OUT_ONLY)
        currentTransitionFrame = 0
        transitionTimer = transitionFrameDuration
        deleteFlyingObject()

        // Si es IN, asegurarnos de cargar el nextBackground antes de mostrar IN
        if (mode == TransitionMode.IN) {
            // carga assets para targetLevel (ahora sí preparamos el fondo entrante)
            val bgName = backgroundNames.getOrNull(targetLevel - 1) ?: backgroundNames.last()
            val bgId = getDrawableIdByName(bgName)
            nextBackground = decodeSampledBitmapFromResource(bgId, screenWidth, screenHeight, preferRgb565 = true)
            backgroundTransition = true
        }
    }

    private fun triggerGameCompleted() {
        if (gameOver) return // si ya terminó por vidas, no hacemos nada
        gameOver = true
        thread?.stopCompletely()
        thread = null

        SoundManager.playSound("win")

        // Guardar stats como en gameOver
        val prefs = context.getSharedPreferences("CATIBOX_PREFS", Context.MODE_PRIVATE)
        val isNewHighScore = score > prefs.getInt("MAX_SCORE_HIST", 0)
        val isNewHighStreak = maxStreak > prefs.getInt("MAX_STREAK_HIST", 0)
        prefs.edit().apply {
            if (isNewHighScore) putInt("MAX_SCORE_HIST", score)
            if (isNewHighStreak) putInt("MAX_STREAK_HIST", maxStreak)
            apply()
        }

        // Mostrar overlay de felicitaciones
        pauseThread()
        Handler(Looper.getMainLooper()).post {
            val intent = Intent(context, GameCompletedActivity::class.java).apply {
                putExtra("SCORE", score)
                putExtra("MAX_STREAK", maxStreak)
                putExtra("LEVEL", level)
                putExtra("NEW_HIGH_SCORE", isNewHighScore)
                putExtra("NEW_HIGH_STREAK", isNewHighStreak)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    }

    private fun deleteFlyingObject() {
        // Reinicia el avión, el OVNI y el globo
        plane = null
        ufo = null
        balloon = null

        // También puedes limpiar los items que hayan dejado
        activeFruit = null
        activeStar = null

        // Si quieres limpiar todas las botas que estén en vuelo
        boots.clear()
    }


    fun abandonGame() {
        if (gameOver) return  // si ya terminó, no hacemos nada
        isAbandoned = true
        pauseThread()
        val intent = Intent(context, MainMenuActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

}




