package io.chthonic.bigbox3d.compose

import android.annotation.SuppressLint
import android.opengl.GLSurfaceView
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import io.chthonic.bigbox3d.core.BoxTextureAtlas
import io.chthonic.bigbox3d.core.CuboidRenderer
import io.chthonic.bigbox3d.core.GlApiImpl
import io.chthonic.bigbox3d.core.GlossLevel
import io.chthonic.bigbox3d.core.ShadowFade
import io.chthonic.bigbox3d.core.ShadowOpacity
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

@SuppressLint("ClickableViewAccessibility")
@Composable
internal actual fun BigBox3DGlSurface(
    atlas: BoxTextureAtlas,
    modifier: Modifier,
    autoRotate: Boolean,
    glossLevel: GlossLevel,
    shadowOpacity: ShadowOpacity,
    shadowFade: ShadowFade,
    shadowXOffsetRatio: Float,
    shadowYOffsetRatio: Float,
    onGestureActive: (Boolean) -> Unit,
) {
    val glApi = remember { GlApiImpl() }
    val renderer = remember(atlas) { CuboidRenderer(atlas) }

    AndroidView(
        factory = { ctx ->
            GLSurfaceView(ctx).apply {
                setEGLContextClientVersion(3)
                setEGLConfigChooser(8, 8, 8, 8, 16, 0)
                holder.setFormat(android.graphics.PixelFormat.TRANSLUCENT)
                setZOrderOnTop(true)
                setRenderer(object : GLSurfaceView.Renderer {
                    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) =
                        renderer.onSurfaceCreated(glApi)
                    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) =
                        renderer.onSurfaceChanged(glApi, width, height)
                    override fun onDrawFrame(gl: GL10?) =
                        renderer.onDrawFrame(glApi)
                })
                renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
                setupTouchHandling(renderer, onGestureActive)
            }
        },
        update = { glView ->
            glView.queueEvent {
                renderer.glossLevel = glossLevel
                renderer.autoRotate = autoRotate
                renderer.shadowOpacity = shadowOpacity
                renderer.shadowFade = shadowFade
                renderer.shadowXOffsetRatio = shadowXOffsetRatio
                renderer.shadowYOffsetRatio = shadowYOffsetRatio
            }
        },
        modifier = modifier,
        onRelease = { glView ->
            glView.queueEvent { renderer.release(glApi) }
            glView.onPause()
        },
    )
}

@SuppressLint("ClickableViewAccessibility")
private fun GLSurfaceView.setupTouchHandling(
    renderer: CuboidRenderer,
    onGestureActive: (Boolean) -> Unit,
) {
    var previousX = 0f
    var previousY = 0f
    var expectRotating = false
    var isRotating = false
    var isScaling = false
    var scaleFactor = 1f

    val isGestureActive = { isScaling || isRotating }
    val notifyGesture = { onGestureActive(isGestureActive()) }
    val updateDisallow = { parent.requestDisallowInterceptTouchEvent(isGestureActive()) }

    val scaleDetector = ScaleGestureDetector(
        context,
        object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
                isScaling = true; updateDisallow(); notifyGesture(); return true
            }
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                scaleFactor = (scaleFactor * detector.scaleFactor).coerceIn(0.5f, 3f)
                queueEvent { renderer.zoomFactor = scaleFactor }
                return true
            }
            override fun onScaleEnd(detector: ScaleGestureDetector) {
                isScaling = false; updateDisallow(); notifyGesture()
            }
        },
    )

    setOnTouchListener { _, event ->
        scaleDetector.onTouchEvent(event)
        if (!isScaling) {
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    expectRotating = true
                    previousX = event.x; previousY = event.y
                }
                MotionEvent.ACTION_MOVE -> if (expectRotating) {
                    isRotating = true
                    val dx = event.x - previousX
                    val dy = event.y - previousY
                    previousX = event.x; previousY = event.y
                    queueEvent { renderer.handleTouchDrag(dx, dy) }
                    updateDisallow(); notifyGesture()
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    isRotating = false; expectRotating = false
                    updateDisallow(); notifyGesture()
                }
            }
        } else {
            isRotating = false; expectRotating = false
            updateDisallow(); notifyGesture()
        }
        true
    }
}
