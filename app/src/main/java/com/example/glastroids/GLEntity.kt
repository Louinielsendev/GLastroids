package com.example.glastroids

import android.graphics.PointF
import android.opengl.GLES20
import android.opengl.Matrix
import com.example.glastroids.GLManager.draw

//re-usable singleton TriangleMesh
object Triangle{
    val mesh = Mesh(floatArrayOf( // in counterclockwise order:
        0.0f, 0.5f, 0.0f,  // top
        -0.5f, -0.5f, 0.0f,  // bottom left
        0.5f, -0.5f, 0.0f // bottom right
    ), GLES20.GL_TRIANGLES)
}

fun areBoundingSpheresOverlapping(a: GLEntity, b: GLEntity): Boolean {
    val dx = a.centerX() - b.centerX() //delta x
    val dy = a.centerY() - b.centerY()
    val distanceSq = dx * dx + dy * dy
    val minDistance = a.radius() + b.radius()
    val minDistanceSq = minDistance * minDistance
    return distanceSq < minDistanceSq
}

//re-usable matrices
val modelMatrix = FloatArray(4 * 4)
val viewportModelMatrix = FloatArray(4 * 4)
val rotationViewportModelMatrix = FloatArray(4 * 4)

open class GLEntity {
    lateinit var _mesh: Mesh
    var _color = floatArrayOf(1.0f, 1.0f, 1.0f, 1.0f) //RGBA, default white
    var _x = 0.0f
    var _y = 0.0f
    var _depth = 0.0f //we'll use _depth for z-axis
    var _scale = 1f
    var _rotation = 0f
    var _velX = 0f
    var _velY = 0f
    var _width = 0.0f
    var _height = 0.0f

    open fun update(dt: Float) {
        _x += _velX * dt;
        _y += _velY * dt;

        if(left() > WORLD_WIDTH){
            setRight(0f);
        }else if(right() < 0f){
            setLeft(WORLD_WIDTH);
        }

        if(top() > WORLD_HEIGHT){
            setBottom(0f);
        }else if(bottom() < 0f){
            setTop(WORLD_HEIGHT);
        }



    }

    fun left() =  _x + _mesh.left()
    fun right()=  _x + _mesh.right()
    fun setLeft(leftEdgePosition: Float) {
        _x = leftEdgePosition - _mesh.left()
    }
    fun setRight(rightEdgePosition: Float) {
        _x = rightEdgePosition - _mesh.right()
    }

    fun top() =  _y + _mesh.top()
    fun bottom() = _y + _mesh.bottom()
    fun setTop(topEdgePosition: Float) {
        _y = topEdgePosition - _mesh.top()
    }
    fun setBottom(bottomEdgePosition: Float) {
        _y = bottomEdgePosition - _mesh.bottom()
    }

    open fun getPointList(): ArrayList<PointF> {
        return _mesh.getPointList(_x, _y, _rotation)
    }


    open fun render(viewportMatrix: FloatArray) {
        //reset the model matrix and then translate (move) it into world space
        Matrix.setIdentityM(modelMatrix, OFFSET) //reset model matrix
        Matrix.translateM(modelMatrix, OFFSET, _x, _y, _depth)
        //viewportMatrix * modelMatrix combines into the viewportModelMatrix
        //NOTE: projection matrix on the left side and the model matrix on the right side.
        Matrix.multiplyMM(viewportModelMatrix, OFFSET, viewportMatrix, OFFSET, modelMatrix, OFFSET)
        //apply a rotation around the Z-axis to our modelMatrix. Rotation is in degrees.
        Matrix.setRotateM(modelMatrix, OFFSET, _rotation, 0f, 0f, 1.0f)
        //apply scaling to our modelMatrix, on the x and y axis only.
        Matrix.scaleM(modelMatrix, OFFSET, _scale, _scale, 1f)
        //finally, multiply the rotated & scaled model matrix into the model-viewport matrix
        //creating the final rotationViewportModelMatrix that we pass on to OpenGL
        Matrix.multiplyMM(rotationViewportModelMatrix, OFFSET, viewportModelMatrix, OFFSET, modelMatrix, OFFSET)
        draw(_mesh, rotationViewportModelMatrix, _color)
    }



    fun setColors(colors: FloatArray) {
        assert(colors.size == 4)
        setColors(colors[0], colors[1], colors[2], colors[3])
    }

    fun setColors(r: Float, g: Float, b: Float, a: Float) {
        _color[0] = r //red
        _color[1] = g //green
        _color[2] = b //blue
        _color[3] = a //alpha (transparency)
    }

    var _isAlive = true
    open fun isDead(): Boolean {
        return !_isAlive
    }

    open fun onCollision(that: GLEntity) {
        _isAlive = false
    }

    open fun isColliding(that: GLEntity): Boolean {
        if (this === that) {
            throw AssertionError("isColliding: You shouldn't test Entities against themselves!")
        }
        return isAABBOverlapping(this, that)
    }

    open fun centerX(): Float {
        return _x //assumes our mesh has been centered on [0,0] (normalized)
    }

    open fun centerY(): Float {
        return _y //assumes our mesh has been centered on [0,0] (normalized)
    }

    open fun radius(): Float {
        //use the longest side to calculate radius
        return if (_width > _height) _width * 0.5f else _height * 0.5f
    }
}

//a basic axis-aligned bounding box intersection test.
//https://gamedev.stackexchange.com/questions/586/what-is-the-fastest-way-to-work-out-2d-bounding-box-intersection
fun isAABBOverlapping(a: GLEntity, b: GLEntity): Boolean {
    return !(a.right() <= b.left() || b.right() <= a.left() || a.bottom() <= b.top() || b.bottom() <= a.top())
}

//a more refined AABB intersection test
//returns true on intersection, and sets the least intersecting axis in overlap
val overlap = PointF(0f, 0f); //re-usable PointF for collision detection. Assumes single threading.

@SuppressWarnings("UnusedReturnValue")
fun getOverlap(a: GLEntity, b: GLEntity, overlap: PointF): Boolean {
    overlap.x = 0.0f;
    overlap.y = 0.0f;
    val centerDeltaX = a.centerX() - b.centerX();
    val halfWidths = (a._width + b._width) * 0.5f;
    var dx = Math.abs(centerDeltaX); //cache the abs, we need it twice

    if (dx > halfWidths) return false; //no overlap on x == no collision

    val centerDeltaY = a.centerY() - b.centerY();
    val halfHeights = (a._height + b._height) * 0.5f;
    var dy = Math.abs(centerDeltaY);

    if (dy > halfHeights) return false; //no overlap on y == no collision

    dx = halfWidths - dx; //overlap on x
    dy = halfHeights - dy; //overlap on y
    if (dy < dx) {
        overlap.y = if (centerDeltaY < 0f) -dy else dy;
    } else if (dy > dx) {
        overlap.x = if (centerDeltaX < 0) -dx else dx;
    } else {
        overlap.x = if (centerDeltaX < 0) -dx else dx;
        overlap.y = if (centerDeltaY < 0) -dy else dy;
    }
    return true;
}
