package io.chthonic.gamebigbox.opengl3

import android.content.Context
import android.graphics.Bitmap
import android.opengl.GLES30
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import android.util.Log
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

private const val ROTATION_SENSITIVITY = 0.4f

/**
 * OpenGL ES 3.0 renderer for the Cube class.
 * Handles camera setup, projection, rotation, and draw loop.
 */
class TexturedCubeRenderer(
    private val ctx: Context,
    private val bitmaps: List<Bitmap>,
    private val onTexturesUploaded: (() -> Unit)? = null
) : GLSurfaceView.Renderer {

    private lateinit var cube: Cube

    // Matrices for transformations
    private val viewMatrix = FloatArray(16)
    private val projMatrix = FloatArray(16)
    private val vpMatrix = FloatArray(16)

    private var angleX = 20f
    private var angleY = 30f

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES30.glClearColor(0f, 0f, 0f, 1f)
        GLES30.glEnable(GLES30.GL_DEPTH_TEST)
        GLES30.glEnable(GLES30.GL_CULL_FACE)
        GLES30.glCullFace(GLES30.GL_BACK)
        // by default assumed GLES30.glFrontFace(GLES30.GL_CCW)

        // Upload all six textures immediately
        cube = Cube(bitmaps, onUploaded = onTexturesUploaded)

        Log.d("TexturedCubeRenderer", "OpenGL ES ${GLES30.glGetString(GLES30.GL_VERSION)} ready.")
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES30.glViewport(0, 0, width, height)
        val ratio = width.toFloat() / height
        // lens
        Matrix.perspectiveM(projMatrix, 0, 45f, ratio, 0.1f, 100f)
        // position: at position 0,0,8 looking at 0,0,0 where y-axis is "up"
        Matrix.setLookAtM(
            viewMatrix,
            0,
            0f, 0f, 8f,
            0f, 0f, 0f,
            0f, 1f, 0f,
        )
        // combine
        Matrix.multiplyMM(vpMatrix, 0, projMatrix, 0, viewMatrix, 0)
    }

    override fun onDrawFrame(gl: GL10?) {
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT or GLES30.GL_DEPTH_BUFFER_BIT)
        rotate(ROTATION_SENSITIVITY, 0f)
        cube.draw(vpMatrix, angleX, angleY)
    }

    fun handleTouchDrag(deltaX: Float, deltaY: Float) {
        rotate(deltaX, deltaY)
        // Simple rotation sensitivity tuning
        angleY += deltaX * ROTATION_SENSITIVITY
        angleX += deltaY * ROTATION_SENSITIVITY

        // clamp angles if desired
        angleX = angleX.coerceIn(-90f, 90f)
    }

    private fun rotate(deltaX: Float, deltaY: Float) {
        // Simple rotation sensitivity tuning
        angleY += deltaX * ROTATION_SENSITIVITY
        angleX += deltaY * ROTATION_SENSITIVITY

        // clamp angles if desired
        angleX = angleX.coerceIn(-90f, 90f)
    }
}
