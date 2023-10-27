package com.example.glastroids

import android.graphics.PointF
import android.os.SystemClock
import android.util.Log

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin


const val ROTATION_VELOCITY = 360f //TODO: game play values!
const val THRUST = 4f
const val DRAG = 0.98f
class Player(x: Float, y: Float) : GLEntity() {
    private  val TAG = "Player"
    private var _bulletCooldown = 0f
    var lives = 3
    init {
        _x = x
        _y = y
        _width = 8f; //TO DO: gameplay values! move to configs
        _height = 12f;
        _mesh = Triangle.mesh
        _mesh.setWidthHeight(_width, _height);
        _mesh.flipY()

    }

    override fun update(dt: Float) {
        _rotation += dt * ROTATION_VELOCITY * engine._inputs._horizontalFactor
        if (engine._inputs._pressingB) {

            val theta = _rotation * TO_RADIANS
            _velX += sin(theta) * THRUST
            _velY -= cos(theta) * THRUST
            Log.d(TAG, "$_x")
            engine.onGameEvent(GameEvent.Thrust, this)
        }
        _velX *= DRAG
        _velY *= DRAG
        _bulletCooldown -= dt;
        if(engine._inputs._pressingA && _bulletCooldown <= 0f){

            if(engine.maybeFireBullet(this)){
                _bulletCooldown = TIME_BETWEEN_SHOTS;
            }
        }else{
            setColors(1.0f, 1f, 1f,1f);
        }
        super.update(dt)
    }

    override fun render(viewportMatrix: FloatArray) {

        super.render(viewportMatrix)
    }

    override fun isColliding(that: GLEntity): Boolean {
        if (!areBoundingSpheresOverlapping(this, that)) {
            return false
        }
        val shipHull = getPointList()
        val asteroidHull = that.getPointList()
        if (polygonVsPolygon(shipHull, asteroidHull)) {
            lives -= 1
            return true
        }
        return polygonVsPoint(asteroidHull, _x, _y) //finally, check if we're inside the asteroid
    }

}