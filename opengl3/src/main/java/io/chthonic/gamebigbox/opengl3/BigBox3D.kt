package io.chthonic.gamebigbox.opengl3

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.opengl.GLSurfaceView
import android.util.Log
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import coil.ImageLoader
import coil.request.ImageRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Compose wrapper that loads textures from URLs and displays them
 * on a 3D cuboid representing a PC game's big box -- rendered with OpenGL ES 3.0.
 * @param textureUrls textures of box sides
 * @param autoRotate auto rotate box if true
 * @param glossLevel glossiness of the box
 * @param shadowOpacity opacity of the shadow the box casts
 * @param shadowFade how the shadow fades
 * @param shadowXOffsetRatio x offset of shadow relative to box width. positive value is to the right. default is 0f at center of box.
 * @param shadowYOffsetRatio y offset of shadow relative to box height. positive value is up. default is 0f at center of box.
 * @param onGestureActive callback with parameter that indicates if a touch gesture is active. Fires (at least) when gesture state changes.
 */
@OptIn(ExperimentalComposeUiApi::class)
@SuppressLint("ClickableViewAccessibility")
@Composable
fun BigBox3D(
    textureUrls: BoxTextureUrls,
    modifier: Modifier = Modifier,
    autoRotate: Boolean = true,
    glossLevel: GlossLevel = GlossLevel.SEMI_GLOSS,
    shadowOpacity: ShadowOpacity = ShadowOpacity.STRONG,
    shadowFade: ShadowFade = ShadowFade.REALISTIC,
    shadowXOffsetRatio: Float = 0f,
    shadowYOffsetRatio: Float = 0f,
    onGestureActive: (Boolean) -> Unit = {},
) {
    val context = LocalContext.current
    var textureBitmaps by remember { mutableStateOf<BoxTextureBitmaps?>(null) }
    var texturesUploaded by remember { mutableStateOf(false) }

    // Load all bitmaps asynchronously (IO thread)
    LaunchedEffect(textureUrls) {
        try {
            textureBitmaps = withContext(Dispatchers.IO) {
                textureUrls.toBitmap { url -> loadBitmapFromUrl(context, url) }
            }
            Log.d("BigBox3D", "Loaded bitmaps $textureBitmaps from URLs")
        } catch (e: BitmapLoadingFailedException) {
            Log.e("BigBox3D", "Loading bitmaps failed", e)
            textureBitmaps = null
        }
    }

    // Once all six bitmaps are ready, create GL surface
    textureBitmaps?.let { bitmaps ->
        val renderer = remember {
            TexturedCuboidRenderer(bitmaps) {
                texturesUploaded = true
            }
        }

        AndroidView(
            factory = { ctx ->
                GLSurfaceView(ctx).apply {
                    // 🔹 Request OpenGL ES 3.0 context
                    setEGLContextClientVersion(3)

                    // ✅ request RGBA8888 surface with depth buffer
                    setEGLConfigChooser(8, 8, 8, 8, 16, 0)

                    // ✅ make the surface translucent to compositor
                    holder.setFormat(android.graphics.PixelFormat.TRANSLUCENT)

                    // ✅ draw above or within Compose as needed
                    setZOrderOnTop(true)  // or setZOrderMediaOverlay(true)

                    // 🔹 Use the GLES30-based renderer
                    setRenderer(renderer)

                    renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY

                    // 👆 Touch-based cube rotation
                    var previousX = 0f
                    var previousY = 0f
                    var expectRotating = false
                    var isRotating = false
                    var isScaling = false
                    var scaleFactor = 1f
                    val gestureActive: () -> Boolean = {
                        isScaling || isRotating
                    }
                    val notifyParentOfGestureState = {
                        onGestureActive(gestureActive())
                    }
                    val updateDisallowInterceptTouchEvent = {
                        this@apply.parent.requestDisallowInterceptTouchEvent(gestureActive())
                    }
                    val scaleDetector = ScaleGestureDetector(
                        context,
                        object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
                            override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
                                isScaling = true
                                updateDisallowInterceptTouchEvent()
                                notifyParentOfGestureState()
                                return true
                            }

                            override fun onScale(detector: ScaleGestureDetector): Boolean {
                                scaleFactor =
                                    (scaleFactor * detector.scaleFactor).coerceIn(0.5f, 3f)
                                queueEvent {
                                    renderer.zoomFactor = scaleFactor
                                }
                                return true
                            }

                            override fun onScaleEnd(detector: ScaleGestureDetector) {
                                isScaling = false
                                updateDisallowInterceptTouchEvent()
                                notifyParentOfGestureState()
                            }
                        }
                    )

                    setOnTouchListener { v, event ->
                        scaleDetector.onTouchEvent(event)

                        if (!isScaling) {
                            when (event.actionMasked) {
                                MotionEvent.ACTION_DOWN -> {
                                    expectRotating = true
                                    previousX = event.x
                                    previousY = event.y
                                }

                                MotionEvent.ACTION_MOVE -> {
                                    if (expectRotating) {
                                        isRotating = true
                                        val dx = event.x - previousX
                                        val dy = event.y - previousY
                                        previousX = event.x
                                        previousY = event.y
                                        queueEvent {
                                            renderer.handleTouchDrag(dx, dy)
                                        }
                                        updateDisallowInterceptTouchEvent()
                                        notifyParentOfGestureState()
                                    }
                                }

                                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                                    isRotating = false
                                    expectRotating = false
                                    updateDisallowInterceptTouchEvent()
                                    notifyParentOfGestureState()
                                }
                            }
                        } else {
                            isRotating = false
                            expectRotating = false
                            updateDisallowInterceptTouchEvent()
                            notifyParentOfGestureState()
                        }
                        true
                    }
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
                glView.queueEvent {
                    renderer.release() // delete textures/programs safely on GL thread
                }
                glView.onPause()
            }
        )

        // Cleanup heap memory (after upload, but no recomposition)
        if (texturesUploaded) {
            LaunchedEffect(Unit) {
                Log.d("BigBox3D", "Recycling bitmaps after texture upload")
                bitmaps.toList().forEach { if (!it.isRecycled) it.recycle() }
                // do NOT set bitmaps = null — keeps the GL surface alive
            }
        }
    } ?: run {
        // 🔹 Loading indicator while images download
        Box(
            modifier = modifier,
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    }
}

/**
 * Loads an image from a URL into a Bitmap using Coil, safely off the UI thread.
 */
private suspend fun loadBitmapFromUrl(context: Context, url: String): Bitmap {
    return try {
        val loader = ImageLoader(context)
        val request = ImageRequest.Builder(context)
            .data(url)
            .allowHardware(false)
            .build()
        val result = loader.execute(request)
        requireNotNull((result.drawable as? BitmapDrawable)?.bitmap).also {
            Log.v("loadBitmapFromUrl", "Loaded bitmap from $url -> $it")
        }
    } catch (e: Exception) {
        Log.e("loadBitmapFromUrl", "Failed to load bitmap from $url", e)
        throw BitmapLoadingFailedException()
    }
}