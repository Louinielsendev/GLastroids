package com.example.glastroids

import kotlin.math.cos
import kotlin.math.sin

private val PARTICL_MESH = Dot.mesh //reusing the Dot (defined in Star.kt, but available throughout the package)
const val TIME_TO_LIVE_PARTICAL = 3f //seconds

class Partical: GLEntity() {
    var _ttl = TIME_TO_LIVE_PARTICAL
    init {

        _mesh = PARTICL_MESH //all bullets use the exact same mesh

    }

    fun explode(source: GLEntity){
        _x = source._x
        _y = source._y
        _velX = between(MIN_VEL, MAX_VEL * 50f )
        _velY = between(MIN_VEL, MAX_VEL * 50f)
        _ttl = TIME_TO_LIVE_PARTICAL
    }

    override fun update(dt: Float) {
        super.update(dt)
        if (_ttl > 0) {
            _ttl -= dt
           
        }
    }

    override fun render(viewportMatrix: FloatArray) {
        if (_ttl > 0) {
            super.render(viewportMatrix)
        }
    }

    override fun isDead(): Boolean {
        return _ttl < 1
    }
}