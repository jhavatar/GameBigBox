@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package io.chthonic.bigbox3d.compose

import androidx.compose.foundation.gestures.detectDragGestures
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import io.chthonic.bigbox3d.core.BoxTextureAtlas
import io.chthonic.bigbox3d.core.CuboidRenderer
import io.chthonic.bigbox3d.core.GlApiImpl
import io.chthonic.bigbox3d.core.GlossLevel
import io.chthonic.bigbox3d.core.ShadowFade
import io.chthonic.bigbox3d.core.ShadowOpacity
import io.chthonic.bigbox3d.core.WebGl2Ctx

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
    val glCanvas = remember { jsCreateCanvas() }
    val glCtx    = remember { jsGetWebGL2Ctx(glCanvas) }
    val glApi    = remember { GlApiImpl(glCtx) }
    val renderer = remember(atlas) { CuboidRenderer(atlas) }

    renderer.autoRotate         = autoRotate
    renderer.glossLevel         = glossLevel
    renderer.shadowOpacity      = shadowOpacity
    renderer.shadowFade         = shadowFade
    renderer.shadowXOffsetRatio = shadowXOffsetRatio
    renderer.shadowYOffsetRatio = shadowYOffsetRatio

    // In Compose MP 1.10.x, DisposableEffect runs BEFORE the first onGloballyPositioned.
    // Setting canvas.width/height (jsResizeCanvas) resets the WebGL context, wiping all
    // GL state created by onSurfaceCreated. So onSurfaceCreated is called inside
    // onGloballyPositioned, after the first backing-buffer resize.
    //
    // The canvas is attached to <html> (not <body>) because Compose MP 1.10.x sets
    // position:relative; overflow:hidden on <body>, which causes a Firefox layout
    // bug where position:fixed children have offsetWidth=0 and are invisible.
    //
    // The canvas uses pointer-events:none so all pointer events pass through to Compose.
    // Gestures are handled by Modifier.pointerInput on the Box below, which lets the
    // LazyColumn scroll freely and lets detectDragGestures claim the drag for rotation.
    val glReady   = remember { mutableStateOf(false) }
    val lastPw    = remember { mutableStateOf(0) }
    val lastPh    = remember { mutableStateOf(0) }
    val zoomScope = rememberCoroutineScope()

    DisposableEffect(glCanvas) {
        jsAppendToHtml(glCanvas)
        onDispose {
            if (glReady.value) renderer.release(glApi)
            glReady.value = false
            jsRemoveFromParent(glCanvas)
        }
    }

    LaunchedEffect(renderer) {
        while (true) {
            withFrameNanos { }
            if (glReady.value) renderer.onDrawFrame(glApi)
        }
    }

    Box(
        modifier = modifier
            .onGloballyPositioned { coords ->
                // Use the composable's full size (not the clipped visible area from
                // boundsInWindow) so the canvas stays 400dp tall while scrolling.
                // localToWindow gives the true window position even when off-screen (negative y).
                val windowPos = coords.localToWindow(Offset.Zero)
                val x = windowPos.x.toInt()
                val y = windowPos.y.toInt()
                val w = coords.size.width
                val h = coords.size.height
                if (w > 0 && h > 0) {
                    jsStyleCanvas(glCanvas, x, y, w, h)
                    val dpr = jsDevicePixelRatio()
                    val pw = (w * dpr).toInt(); val ph = (h * dpr).toInt()
                    if (pw != lastPw.value || ph != lastPh.value) {
                        // canvas.width/height reset the WebGL context — release old GL
                        // objects first, then recreate everything after the resize.
                        if (glReady.value) renderer.release(glApi)
                        jsResizeCanvas(glCanvas, pw, ph)
                        lastPw.value = pw; lastPh.value = ph
                        renderer.onSurfaceCreated(glApi)
                        glReady.value = true
                    }
                    renderer.onSurfaceChanged(glApi, pw, ph)
                }
            }
            .pointerInput(Unit) {
                // Drag to rotate. detectDragGestures claims the gesture so the
                // LazyColumn doesn't scroll while rotating.
                detectDragGestures(
                    onDragStart  = { onGestureActive(true) },
                    onDragEnd    = { onGestureActive(false) },
                    onDragCancel = { onGestureActive(false) },
                ) { _, dragAmount ->
                    renderer.handleTouchDrag(dragAmount.x, dragAmount.y)
                }
            }
            .pointerInput(Unit) {
                // Scroll-wheel zoom with list-scroll detection via debounce:
                // - While scroll events arrive faster than 300 ms apart the list is
                //   "in motion" and zoom is suppressed.
                // - Once 300 ms pass with no scroll event the next wheel tick zooms.
                // The event is never consumed so LazyColumn always receives it.
                var scaleFactor = 1f
                var debounceJob: Job? = null
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        val scrollY = event.changes.fold(Offset.Zero) { acc, c ->
                            acc + c.scrollDelta
                        }.y
                        if (scrollY != 0f) {
                            val listScrolling = debounceJob?.isActive == true
                            if (!listScrolling) {
                                scaleFactor = (scaleFactor * if (scrollY > 0) 0.9f else 1.1f)
                                    .coerceIn(0.5f, 3f)
                                renderer.zoomFactor = scaleFactor
                            }
                            debounceJob?.cancel()
                            debounceJob = zoomScope.launch { delay(300) }
                        }
                    }
                }
            }
    )
}

// ── Canvas / DOM ─────────────────────────────────────────────────────────────

private fun jsCreateCanvas(): JsAny = js("document.createElement('canvas')")

private fun jsGetWebGL2Ctx(canvas: JsAny): WebGl2Ctx =
    js("canvas.getContext('webgl2', {alpha: true, antialias: false})")

// Attaches to <html> with pointer-events:none so Compose receives all pointer
// events and the LazyColumn can scroll freely.
private fun jsAppendToHtml(canvas: JsAny): Unit = js("""
    (canvas.style.position = 'fixed',
     canvas.style.pointerEvents = 'none',
     canvas.style.zIndex = '1',
     document.documentElement.appendChild(canvas))
""")

private fun jsRemoveFromParent(canvas: JsAny): Unit =
    js("(canvas.parentNode && canvas.parentNode.removeChild(canvas))")

private fun jsStyleCanvas(canvas: JsAny, x: Int, y: Int, w: Int, h: Int): Unit = js("""
    (canvas.style.left   = x + 'px',
     canvas.style.top    = y + 'px',
     canvas.style.width  = w + 'px',
     canvas.style.height = h + 'px')
""")

private fun jsResizeCanvas(canvas: JsAny, w: Int, h: Int): Unit =
    js("(canvas.width = w, canvas.height = h)")

private fun jsDevicePixelRatio(): Double = js("window.devicePixelRatio || 1.0")
