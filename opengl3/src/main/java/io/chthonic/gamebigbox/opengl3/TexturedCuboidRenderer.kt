package io.chthonic.gamebigbox.opengl3

import android.opengl.GLES30
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import android.util.Log
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

private const val ROTATION_SENSITIVITY = 0.4f
private const val DEFAULT_ZOOM = 5f
private const val DEFAULT_ZOOM_FACTOR = 1f // 1x = normal
private const val REFRESH_LAST_ZOOM_FACTOR = -1f // force refresh on first frame

/**
 * OpenGL ES 3.0 renderer for the Cuboid class.
 * Handles camera setup, projection, rotation, and draw loop.
 */
internal class TexturedCuboidRenderer(
    private val bitmaps: BoxTextureBitmaps,
    private val onTexturesUploaded: (() -> Unit)? = null
) : GLSurfaceView.Renderer {

    private var cuboid: Cuboid? = null
    var glossLevel: GlossLevel = GlossLevel.SEMI_GLOSS
    var autoRotate: Boolean = true
    var zoomFactor: Float = DEFAULT_ZOOM_FACTOR
    private var lastZoomFactor: Float = REFRESH_LAST_ZOOM_FACTOR
    var shadowOpacity: ShadowOpacity = ShadowOpacity.STRONG
    var shadowFade: ShadowFade = ShadowFade.REALISTIC
    var shadowXOffsetRatio: Float = 0f
    var shadowYOffsetRatio: Float = 0f


    // Matrices for transformations
    private val viewMatrix = FloatArray(16)
    private val projMatrix = FloatArray(16)
    private val vpMatrix = FloatArray(16)

    private var angleX = if (bitmaps.supportsFullXAxisRotation) 20f else 0f
    private var angleY = 30f

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES30.glClearColor(0f, 0f, 0f, 0f)
        GLES30.glEnable(GLES30.GL_DEPTH_TEST)
        GLES30.glEnable(GLES30.GL_CULL_FACE)
        GLES30.glCullFace(GLES30.GL_BACK)
        // by default assumed GLES30.glFrontFace(GLES30.GL_CCW)

        // Upload all textures immediately
        cuboid = Cuboid(
            bitmaps.toList(),
            halfW = bitmaps.halfWidth,
            halfH = bitmaps.halfHeight,
            halfD = bitmaps.halfDepth,
            onUploaded = onTexturesUploaded,
        )

        Log.d("TexturedCubeRenderer", "OpenGL ES ${GLES30.glGetString(GLES30.GL_VERSION)} ready.")
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES30.glViewport(0, 0, width, height)
        val ratio = width.toFloat() / height
        Matrix.perspectiveM(projMatrix, 0, 35f, ratio, 0.1f, 100f)
        lastZoomFactor = REFRESH_LAST_ZOOM_FACTOR
    }

    override fun onDrawFrame(gl: GL10?) {
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT or GLES30.GL_DEPTH_BUFFER_BIT)
        if (autoRotate) {
            rotate(ROTATION_SENSITIVITY, 0f)
        }

        // Recalculate camera distance based on zoom only when zoom changes
        if (zoomFactor != lastZoomFactor) {
            val cameraZ = DEFAULT_ZOOM / zoomFactor
            Matrix.setLookAtM(
                viewMatrix,
                0,
                0f, 0f, cameraZ,
                0f, 0f, 0f,
                0f, 1f, 0f
            )
            Matrix.multiplyMM(vpMatrix, 0, projMatrix, 0, viewMatrix, 0)
            lastZoomFactor = zoomFactor
        }

        if (shadowOpacity != ShadowOpacity.NONE) {
            cuboid?.drawProjectedShadow(
                vp = vpMatrix,
                rotX = angleX,
                rotY = angleY,
                shadowOpacity = shadowOpacity.alpha,
                shadowFadeStartRatio = shadowFade.startRatio,
                shadowFadeEndRatio = shadowFade.endRatio,
                xOffsetRatio = shadowXOffsetRatio,
                yOffsetRatio = shadowYOffsetRatio,
            )
        }
        cuboid?.draw(
            vp = vpMatrix,
            rotX = angleX,
            rotY = angleY,
            gloss = glossLevel.glossValue,
        )
    }

    fun handleTouchDrag(deltaX: Float, deltaY: Float) {
        rotate(deltaX, deltaY)
    }

    fun release() {
        cuboid?.release()
        cuboid = null
    }

    private fun rotate(deltaX: Float, deltaY: Float) {
        // Simple rotation sensitivity tuning
        angleX += deltaY * ROTATION_SENSITIVITY
        angleY += deltaX * ROTATION_SENSITIVITY

        // clamp angles
        angleX = if (bitmaps.supportsFullXAxisRotation) {
            angleX.coerceIn(-90f, 90f)
        } else {
            angleX.coerceIn(-7f, 7f)
        }
    }
}
