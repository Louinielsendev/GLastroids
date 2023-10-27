package com.example.glastroids

import android.opengl.GLES20
import android.util.Log
import kotlin.random.Random

const val MAX_VEL = 1f
const val MIN_VEL = -1f

fun between(min: Float, max: Float): Float = min + Random.nextFloat() * (max - min)

class Asteroid(x: Float, y: Float, points: Int, type: Int) : GLEntity(){
    private val TAG: String = "Asteroid"
    var spawnOnKill = 0
    var score = 0
    var aType = type

    init{
        assert(points >= 3, {"triangles or more, please. :)"})
        when(type){
            0 ->{

                spawnOnKill = 0
                _width = 6f
                score = 20
                _velX = between(MIN_VEL, MAX_VEL * 60f)
                _velY = between(MIN_VEL, MAX_VEL * 60f)
            }
            1 -> {
                spawnOnKill = 2
                _width = 12f;
                score = 10
                _velX = between(MIN_VEL, MAX_VEL * 20f)
                _velY = between(MIN_VEL, MAX_VEL * 20f)
            }
            2 ->{
                spawnOnKill = 3
                _width = 20f;
                score = 5
                _velX = between(MIN_VEL, MAX_VEL * 5f )
                _velY = between(MIN_VEL, MAX_VEL * 5f)
            }

        }
        _x = x
        _y = y
        _height = _width;
        Log.d(TAG, "Asteroid type: VelX: $_velX, VelY: $_velY")
        val radius = _width*0.5f
        _mesh = Mesh(
            generateLinePolygon(points, radius),
            GLES20.GL_LINES
        )
        _mesh.setWidthHeight(_width, _height);
    }

    override fun update(dt: Float) {
        super.update(dt)
        _rotation++
    }

    override fun onCollision(that: GLEntity) {
        engine.onGameEvent(GameEvent.AsteroidExplode, this)
        if (that is Bullet){
            engine.score += score
        }
        super.onCollision(that)
    }
}