package com.example.glastroids

import android.content.Context
import android.graphics.Color
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import android.os.SystemClock
import android.util.AttributeSet
import android.util.Log
import androidx.core.graphics.blue
import androidx.core.graphics.green
import androidx.core.graphics.red
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.random.Random

val WORLD_WIDTH = 400f //all dimensions are in meters
val WORLD_HEIGHT = 225f
val METERS_TO_SHOW_X = 400f
val METERS_TO_SHOW_Y = 225f
val STAR_COUNT = 100
val ASTEROID_COUNT = 11
val RNG = Random(SystemClock.uptimeMillis())
const val TIME_BETWEEN_SHOTS = 0.25f
const val BULLET_COUNT = (TIME_TO_LIVE_BULLET / TIME_BETWEEN_SHOTS).toInt()+1
const val PARTICAL_COUNT = 10


var SECOND_IN_NANOSECONDS: Long = 1000000000
var MILLISECOND_IN_NANOSECONDS: Long = 1000000
var NANOSECONDS_TO_MILLISECONDS = 1.0f / MILLISECOND_IN_NANOSECONDS
var NANOSECONDS_TO_SECONDS = 1.0f / SECOND_IN_NANOSECONDS
lateinit var engine: Game
const val PREFS = "com.example.glastroids_preferences"

class Game(ctx: Context, attrs: AttributeSet? = null) : GLSurfaceView(ctx, attrs), GLSurfaceView.Renderer{
    private val bgColor = Color.rgb(125, 206, 235)
    private val TAG: String = "Game"
    private val _stars = ArrayList<Star>()
    private val _asteroids = ArrayList<Asteroid>()
    private val _asteroidsToAdd = ArrayList<Asteroid>()
    private val _particals = ArrayList<Partical>()
    val asteroidTypes = arrayOf(0, 1, 2)
    private val _texts = ArrayList<Text>()
    private val jukebox = Jukebox(this)
    var _bullets = ArrayList<Bullet>(BULLET_COUNT)
    private val _player = Player(WORLD_WIDTH/2f, WORLD_HEIGHT/2f)
    var score = 0

    public fun getActivity() = context as MainActivity
    public fun getAssets() = context.assets
    public fun getPreferences() = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    public fun getPreferencesEditor() = getPreferences().edit()
    public fun savePreference(key: String, v: Boolean) = getPreferencesEditor().putBoolean(key, v).commit()

    init {
        engine = this
        for (i in 0 until STAR_COUNT) {
            val x = Random.nextInt(WORLD_WIDTH.toInt()).toFloat()
            val y = Random.nextInt(WORLD_HEIGHT.toInt()).toFloat()
            _stars.add(Star(x, y))
        }
        for (i in 0 until ASTEROID_COUNT) {
            val type = RNG.nextInt(0, 3)
            val x = 20f * i
            val y = WORLD_HEIGHT / 2f
            _asteroids.add(Asteroid(x, y, i+3, asteroidTypes[type]))
        }
        for (i in 0 until BULLET_COUNT) {
            _bullets.add(Bullet())
        }
        for (i in 0 until PARTICAL_COUNT){
            _particals.add(Partical())
        }
        val s1 = "FPS: 0"
        val s2 = "LIVES: ${_player.lives}"
        val s3 = "SCORE: ${score}"
        _texts.add(Text(s1, 8f, 210f))
        _texts.add(Text(s2, 8f, 8f))
        _texts.add(Text(s3, 180f, 8f))

        setEGLContextClientVersion(2)
        setRenderer(this)

    }

    var _inputs = InputManager() //empty but valid default
    fun setControls(input: InputManager) {
        _inputs.onPause()
        _inputs.onStop()
        _inputs = input
        _inputs.onResume()
        _inputs.onStart()
    }

    fun hexColorToFloat(hex: Int) = hex / 255f

    val _border = Border(WORLD_WIDTH/2f, WORLD_HEIGHT/2f, WORLD_WIDTH, WORLD_HEIGHT)

    // Create the projection Matrix. This is used to project the scene onto a 2D viewport.
    private val _viewportMatrix = FloatArray(4 * 4) //In essence, it is our our Camera

    override fun onSurfaceCreated(unused: GL10, config: EGLConfig) {

        val red = hexColorToFloat(bgColor.red)
        val green = hexColorToFloat(bgColor.green)
        val blue = hexColorToFloat(bgColor.blue)
        val alpha = 1.0f
        GLES20.glClearColor(red, green, blue, alpha)
        GLManager.buildProgram()
    }



    override fun onSurfaceChanged(unused: GL10, width: Int, height: Int) {
        // Set the OpenGL viewport to the same size as the surface.
        GLES20.glViewport(0, 0, width, height)
    }

    override fun onDrawFrame(unused: GL10?) {
        update()
        render()

    }

    val dt = 0.01f
    var accumulator = 0.0f
    var currentTime = (System.nanoTime() * NANOSECONDS_TO_SECONDS).toFloat()
    var elapsedTime = 0f
    var frameCount = 0

    private fun update() {
        val newTime = (System.nanoTime() * NANOSECONDS_TO_SECONDS).toFloat()
        val frameTime = newTime - currentTime
        currentTime = newTime
        accumulator += frameTime
        frameCount++
        while (accumulator >= dt) {
            for (a in _asteroids) {
                a.update(dt)
            }
            for (b in _bullets) {
                if (b.isDead()) {
                    continue //skip
                }
                b.update(dt)
            }
            for (p in _particals){
                if(p.isDead()){
                    continue
                }
                p.update(dt)
            }
            _player.update(dt)

            collisionDetection()
            removeDeadEntities()
            addAsteroids()

            accumulator -= dt
        }

        elapsedTime += frameTime

        if (elapsedTime >= 1.0f) {
            val fps = frameCount.toFloat() / elapsedTime
            fps.toInt()
            _texts.elementAt(0).setString("FPS: $fps")
            frameCount = 0
            elapsedTime = 0.0f
        }
    }

    private fun render() {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT) //clear buffer to background color
        val offset = 0
        val left = 0f
        val right = METERS_TO_SHOW_X
        val bottom = METERS_TO_SHOW_Y
        val top = 0f
        val near = 0f
        val far = 1f
        Matrix.orthoM(_viewportMatrix, offset, left, right, bottom, top, near, far)
        _border.render(_viewportMatrix)
        for (s in _stars) {
            s.render(_viewportMatrix)
        }
        for (a in _asteroids) {
            a.render(_viewportMatrix)
        }
        for (b in _bullets) {
            if (b.isDead()) {
                continue
            }
            b.render(_viewportMatrix)
        }
        for (p in _particals){
            if (p.isDead()) {
                continue
            }
            p.render(_viewportMatrix)
        }
        _player.render(_viewportMatrix)
        for (t in _texts) {
            t.render(_viewportMatrix)
        }
    }

    fun pause() {
        jukebox.pauseBgMusic()

    }

    fun resume() {
        jukebox.resumeBgMusic()


    }

    fun maybeFireBullet(source: GLEntity): Boolean {
        for (b in _bullets) {
            if (b.isDead()) {
                b.fireFrom(source)
                engine.onGameEvent(GameEvent.Bullet, source)
                return true

            }
        }
        return false
    }

    private fun collisionDetection() {
        for (b in _bullets) {
            if (b.isDead()) { continue } //skip dead bullets
            for (a in _asteroids) {
                if (a.isDead()) { continue } //skip dead asteroids
                if (b.isColliding(a)) {
                    for (p in _particals){
                        p.explode(b)
                    }
                    for (i in 0 until a.spawnOnKill){
                            val newType = a.aType - 1
                        _asteroidsToAdd.add(Asteroid(a._x, a._y, i+3, newType))

                        Log.d(TAG, "hello")
                    }
                    b.onCollision(a) //notify each entity so they can decide what to do

                    a.onCollision(b)

                    _texts.elementAt(2).setString("SCORE: $score")
                }
            }
        }
        for (a in _asteroids) {
            if (a.isDead()) { continue }
            if (_player.isColliding(a)) {

                _player.onCollision(a)
                a.onCollision(_player)
                _texts.elementAt(1).setString("LIVES: ${_player.lives}")
            }
        }
    }

    fun addAsteroids(){
        for (a in _asteroidsToAdd){
            _asteroids.add(a)
        }
        _asteroidsToAdd.clear()
    }

    fun removeDeadEntities() {
        val count = _asteroids.size
        for (i in count - 1 downTo 0) {
            if (_asteroids[i].isDead()) {
                _asteroids.removeAt(i)
            }
        }
    }

    fun onGameEvent(event: GameEvent, e: GLEntity? /*can be null!*/) {
        //TODO: really should schedule these by adding to an list, avoiding duplicates, and then start all unique sounds once per frame.
        jukebox.playEventSound(event)
    }
}

enum class GameEvent {
    Thrust, AsteroidExplode, Bullet,
}