package io.chthonic.gamebigbox.opengl3

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.opengl.GLSurfaceView
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import coil.ImageLoader
import coil.request.ImageRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

/**
 * Compose wrapper that loads 6 textures from URLs and displays them
 * on a 3D cuboid representing a PC game's big box -- rendered with OpenGL ES 3.0.
 */
@Composable
fun BigBoxFromUrls(
    urls: List<String>,
    modifier: Modifier = Modifier,
    glossLevel: GlossLevel = GlossLevel.SEMI_GLOSS,
    autoRotate: Boolean = true,
) {
    val context = LocalContext.current
    var bitmaps by remember { mutableStateOf<List<Bitmap>?>(null) }
    var texturesUploaded by remember { mutableStateOf(false) }

    // Load all six bitmaps asynchronously (IO thread)
    LaunchedEffect(urls) {
        bitmaps = withContext(Dispatchers.IO) {
            urls.mapNotNull { loadBitmapFromUrl(context, it) }
        }
        Timber.d("Loaded ${bitmaps?.size} bitmaps from URLs")
    }

    // Once all six bitmaps are ready, create GL surface
    if (bitmaps != null && bitmaps!!.size == 6) {
        val renderer = remember {
            TexturedCuboidRenderer(bitmaps!!) {
                texturesUploaded = true
            }
        }

        AndroidView(
            factory = { ctx ->
                GLSurfaceView(ctx).apply {
                    // 🔹 Request OpenGL ES 3.0 context
                    setEGLContextClientVersion(3)

                    // 🔹 Use the GLES30-based renderer
                    setRenderer(renderer)

                    renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY

                    // 👆 Touch-based cube rotation
                    var previousX = 0f
                    var previousY = 0f

                    var isScaling = false
                    var scaleFactor = 1f
                    val scaleDetector = ScaleGestureDetector(
                        context,
                        object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
                            override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
                                isScaling = true
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
                            }
                        }
                    )

                    var isMoving = false
                    setOnTouchListener { _, event ->
                        scaleDetector.onTouchEvent(event)

                        if (!isScaling) {
                            when (event.action) {
                                MotionEvent.ACTION_DOWN -> {
                                    isMoving = true
                                    previousX = event.x
                                    previousY = event.y
                                }

                                MotionEvent.ACTION_MOVE -> {
                                    if (isMoving) {
                                        val dx = event.x - previousX
                                        val dy = event.y - previousY
                                        previousX = event.x
                                        previousY = event.y
                                        queueEvent {
                                            renderer?.handleTouchDrag(dx, dy)
                                        }
                                    }
                                }

                                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                                    isMoving = false
                                }
                            }
                        } else {
                            isMoving = false
                        }
                        true
                    }
                }
            },
            update = { glView ->
                glView.queueEvent {
                    renderer.currentGlossValue = glossLevel.glossValue
                    renderer.autoRotate = autoRotate
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
                Timber.d("Recycling bitmaps after texture upload")
                bitmaps?.forEach { if (!it.isRecycled) it.recycle() }
                // do NOT set bitmaps = null — keeps the GL surface alive
            }
        }
    } else {
        // 🔹 Loading indicator while images download
        Box(
            Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    }
}

/**
 * Loads an image from a URL into a Bitmap using Coil, safely off the UI thread.
 */
private suspend fun loadBitmapFromUrl(context: Context, url: String): Bitmap? {
    return try {
        val loader = ImageLoader(context)
        val request = ImageRequest.Builder(context)
            .data(url)
            .allowHardware(false)
            .build()
        val result = loader.execute(request)
        (result.drawable as? BitmapDrawable)?.bitmap.also {
            Timber.v("Loaded bitmap from $url -> ${it != null}")
        }
    } catch (e: Exception) {
        Timber.e(e, "Failed to load bitmap from $url")
        null
    }
}