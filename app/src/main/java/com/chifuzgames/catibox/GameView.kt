package com.chifuzgames.catibox

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.graphics.drawable.Drawable
import android.media.MediaPlayer
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
import androidx.core.content.edit

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
    private lateinit var hud: HUD
    private lateinit var backgroundPlayer: MediaPlayer


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

    var score = 0
    private var lives = 5
    private var difficultyMultiplier = 2f

    // Música de fondo

    // Estado game over
    var gameOver = false
        private set

    // Mute button
    private val muteButtonSize = 100f

    // Callback Game Over
    var onGameOverListener: (() -> Unit)? = null

    // --- Variables globales de GameView ---
    private var doublePointsActive = false
    private var doublePointsTimer = 0f // en frames
    private val DOUBLE_POINTS_DURATION = 10 * 60 // 10 segundos * 60 fps

    // --- Niveles ---
    private var level = 1
    private var catsSpawned = 0
    private val catsPerLevel = listOf(10, 12, 14, 16,18,20,22,24,26, Int.MAX_VALUE) // nivel 10 sin límite
    private var levelTransition = false
    private var levelTransitionTimer = 0f

    private var lastCatSpawnTimeMs = System.currentTimeMillis()
    private var catSpawnIntervalMs = 3000L // 3 segundos por defecto, se ajusta por nivel

    var planeSpawned = false
    var ufoSpawned = false
    private var lastBalloonScore = 0


    // --- Background / Grass per level (crossfade) ---
    // resource names per level (we'll resolve ids at runtime; fallback to default if missing)
    private val backgroundNames = listOf("background", "background2", "background3", "background4", "background5","background6", "background7", "background8", "background9", "background10")
    private val grassNames = listOf("grass", "grass2", "grass3", "grass4", "grass5","grass6", "grass7", "grass8", "grass9", "grass10")

    private var currentBackground: Bitmap? = null
    private var nextBackground: Bitmap? = null
    private var backgroundTransition = false

    private var currentGrass: Bitmap? = null
    private var nextGrass: Bitmap? = null
    private var grassTransition = false


    // --- Racha (streak) ---
    private var streak = 0
     var maxStreak = 0

    // --- Dificultad ajustable ---
    // --- Dificultad ajustable ---
    private var initialLives = 5
    // Vidas iniciales del jugador al comenzar la partida.


//dificultad por gato
    private var difficultyIncreasePerCat = 0.05f
    //cada cuantos gatos aumenta


    private val levelDifficulty = listOf(
        Pair(0.5f, 110), // nivel 1: velocidad base x1, intervalo . (dificultad inicial por nivel, cada cuanto aparece un gato)
        Pair(0.7f, 100), // nivel 2: más rápido, menos intervalo
        Pair(0.9f, 90), // nivel 3
        Pair(1f, 80), // nivel 4
        Pair(1.2f, 70),  // nivel 5
        Pair(1.4f, 60),  // nivel 6
        Pair(1.6f, 52),  // nivel 7
        Pair(1.8f, 45),  // nivel 8
        Pair(1.9f, 40),  // nivel 9
        Pair(2f, 35),  // nivel 10+

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
        hud.score = 0
        hud.streak = 0
        hud.maxStreak = 0

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

        playerBitmap = playerBitmap.scale(playerWidth, playerHeight, false)
        grassBitmap = grassBitmap.scale(screenWidth + 100, 400, false)
        balloonBitmap = balloonBitmap.scale(balloonWidth, balloonHeight, false)
        planeBitmap = planeBitmap.scale(planeWidth, planeHeight, false)
        ufoBitmap = ufoBitmap.scale(ufoWidth, ufoHeight, false)
        bootBitmap = bootBitmap.scale(balloonWidth / 3, balloonHeight / 3, false)
        fruitBitmap = fruitBitmap.scale(60, 60, false)
        starBitmap = starBitmap.scale(60, 60, false)
        playerBonusBitmap = playerBonusBitmap.scale(playerWidth, playerHeight, false)
        playerSadBitmap = playerSadBitmap.scale(playerWidth, playerHeight, false)
        playerHappyBitmap = playerHappyBitmap.scale(playerWidth, playerHeight, false)
        playerOuchBitmap = playerOuchBitmap.scale(playerWidth, playerHeight, false)

        // init player
        player = Player(
            x = screenWidth / 2f - playerWidth / 2f,
            y = screenHeight.toFloat() - playerHeight - 200f,
            width = playerWidth,
            height = playerHeight
        )
        // inicializar línea de partida
// inicializar línea de partida y dificultad del nivel 1
        lives = initialLives
        level = 1
        catsSpawned = 0
        levelTransition = false

// aplicar dificultad inicial desde levelDifficulty[0]
        difficultyMultiplier = levelDifficulty[0].first
        catSpawnIntervalMs = (levelDifficulty[0].second / 60f * 1000).toLong()


        // Initialize current background/grass from the default (keeps compatibility)
        currentBackground =
            BitmapFactory.decodeResource(resources, getDrawableIdByName(backgroundNames[0]))
                .scale(screenWidth, screenHeight, false)

        currentGrass = BitmapFactory.decodeResource(resources, getDrawableIdByName(grassNames[0]))
            .scale(screenWidth + 100, 400, false)

        thread = GameThread(holder, this)
        thread?.setRunning(true)
        thread?.start()
    }

    @SuppressLint("DiscouragedApi")
    private fun getDrawableIdByName(name: String): Int {
        val id = resources.getIdentifier(name, "drawable", context.packageName)
        return if (id != 0) id else R.drawable.background // fallback to background if not found
    }

    @SuppressLint("DiscouragedApi")
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
        thread = null // <- importante
    }

    // Load next level assets into nextBackground/nextGrass and enable transitions
    private fun loadLevelAssets(nextLevel: Int) {
        val bgName = backgroundNames.getOrNull(nextLevel - 1) ?: backgroundNames.last()
        val grassName = grassNames.getOrNull(nextLevel - 1) ?: grassNames.last()

        val bgId = getDrawableIdByName(bgName)
        val grassId = getGrassIdByName(grassName)

        // prepare nextBackground and nextGrass scaled to screen
        nextBackground = BitmapFactory.decodeResource(resources, bgId)
        nextBackground = nextBackground!!.scale(screenWidth, screenHeight, false)

        nextGrass = BitmapFactory.decodeResource(resources, grassId)
        nextGrass = nextGrass!!.scale(screenWidth + 100, 400, false)

        // enable transitions
        backgroundTransition = true
        grassTransition = true
    }

    fun update(deltaTime: Float) {
        if (player == null || gameOver || isPaused) return

        // --- Transición de nivel ---
        if (levelTransition) {
            levelTransitionTimer -= deltaTime * 60
            if (levelTransitionTimer <= 0) {
                levelTransition = false
                catsSpawned = 0
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
        if (score > 0 && score % BALLOON_SCORE_INTERVAL == 0 && score != lastBalloonScore) {
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
          //  lastBalloonScore = score // <- importante, evita repetir en el mismo múltiplo


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
                    if (b.isOffScreen(screenWidth)) balloon = null

                }
            }
        }

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


       // activeFruit?.update(deltaTime, screenWidth)
      //  ufo?.update(deltaTime)
        //activeStar?.update(deltaTime, screenWidth, screenHeight)

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
                prefs.edit { putInt("MAX_SCORE_HIST", score) }
            }

            if (maxStreak > maxStreakHist) {
                prefs.edit { putInt("MAX_STREAK_HIST", maxStreak) }
            }

            val intent = android.content.Intent(context, GameOverActivity::class.java)
            intent.putExtra("SCORE", score)
            intent.putExtra("MAX_STREAK", maxStreak)  //
            intent.putExtra("NEW_HIGH_SCORE", isNewHighScore)
            intent.putExtra("NEW_HIGH_STREAK", isNewHighStreak)
            context.startActivity(intent)
            pauseThread()
        }



        // --- Nivel ---
        if (catsSpawned >= catsPerLevel[level - 1] && cats.isEmpty() && level < catsPerLevel.size) {
            level++
            loadLevelAssets(level)
            levelTransition = true
            levelTransitionTimer = 1.5f * 60f
            if (level < levelDifficulty.size) {
                difficultyMultiplier = levelDifficulty[level - 1].first
                catSpawnIntervalMs = (levelDifficulty[level - 1].second / 60f * 1000).toLong()
            }
        }
    }


    override fun draw(canvas: Canvas) {
        super.draw(canvas)
        if (player == null) return

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
            ouchBitmap = playerOuchBitmap
        )
        for (boot in boots) boot.draw(canvas)
        balloon?.draw(canvas)
        plane?.draw(canvas)
        ufo?.draw(canvas)
        activeFruit?.draw(canvas)
        activeStar?.draw(canvas)

        // --- Grass crossfade ---
        if (grassTransition && currentGrass != null && nextGrass != null && levelTransition) {
            val total = 90f
            val elapsed = (90f - levelTransitionTimer).coerceAtLeast(0f)
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
        val y = event.y

        // Primero, que el HUD maneje el toque (mute/pause)
        if (hud.handleTouch(x, y, event)) return true

        // Solo si no es game over
        if (!gameOver) {
            when (event.action) {
                MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                    // Movimiento del jugador (solo si no tocamos un botón)
                    if (!hud.isTouchingButton(x, y)) {
                        player?.x = x - player!!.width / 2f
                        if (player!!.x < 0f) player!!.x = 0f
                        if (player!!.x + player!!.width > screenWidth.toFloat())
                            player!!.x = screenWidth.toFloat() - player!!.width
                    }
                }
            }
        }
        return true
    }



    fun setBackgroundPlayer(player: MediaPlayer) {
        backgroundPlayer = player
        if (SoundManager.isMuted) backgroundPlayer.setVolume(0f, 0f)
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

            // crear cat y velocidad en píxeles/segundo
            val catBitmapScaled = catBitmap.scale(catWidth, catHeight, false)
            cats.add(
                Cat(xPos, startY, catWidth, catHeight, catBitmapScaled) {
                    SoundManager.playSound("catAngry")
                    streak = 0
                    player?.state = PlayerState.SAD
                    lives--
                    player?.stateTimer = 1.0f // 1 segundo
                }
            )
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


    fun pauseThread() { thread?.setRunning(false) }

    fun resumeThread() {
        if (thread == null || !thread!!.isAlive) {
            thread = GameThread(holder, this)
            thread?.setRunning(true)
            thread?.start()
        }
    }

}
