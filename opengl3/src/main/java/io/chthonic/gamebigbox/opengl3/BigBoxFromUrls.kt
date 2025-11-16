package io.chthonic.gamebigbox.opengl3

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.opengl.GLSurfaceView
import android.view.MotionEvent
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
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var bitmaps by remember { mutableStateOf<List<Bitmap>?>(null) }
    var texturesUploaded by remember { mutableStateOf(false) }
    var renderer by remember { mutableStateOf<TexturedCuboidRenderer?>(null) }

    // Load all six bitmaps asynchronously (IO thread)
    LaunchedEffect(urls) {
        bitmaps = withContext(Dispatchers.IO) {
            urls.mapNotNull { loadBitmapFromUrl(context, it) }
        }
        Timber.d("Loaded ${bitmaps?.size} bitmaps from URLs")
    }

    // Once all six bitmaps are ready, create GL surface
    if (bitmaps != null && bitmaps!!.size == 6) {
        AndroidView(
            factory = { ctx ->
                GLSurfaceView(ctx).apply {
                    // 🔹 Request OpenGL ES 3.0 context
                    setEGLContextClientVersion(3)

                    // 🔹 Use the GLES30-based renderer
                    val r = TexturedCuboidRenderer(bitmaps!!) { texturesUploaded = true }
                    renderer = r
                    setRenderer(r)

                    renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY

                    // 👆 Touch-based cube rotation
                    var previousX = 0f
                    var previousY = 0f

                    setOnTouchListener { _, event ->
                        when (event.action) {
                            MotionEvent.ACTION_DOWN -> {
                                previousX = event.x
                                previousY = event.y
                            }

                            MotionEvent.ACTION_MOVE -> {
                                val dx = event.x - previousX
                                val dy = event.y - previousY
                                previousX = event.x
                                previousY = event.y
                                renderer?.handleTouchDrag(dx, dy)
                            }
                        }
                        true
                    }
                }
            },
            modifier = modifier
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
suspend fun loadBitmapFromUrl(context: Context, url: String): Bitmap? {
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