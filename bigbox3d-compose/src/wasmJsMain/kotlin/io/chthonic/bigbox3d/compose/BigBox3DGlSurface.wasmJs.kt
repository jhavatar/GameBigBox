package io.chthonic.bigbox3d.compose

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.boundsInWindow
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

    // Sync renderer settings on every recomposition (mirrors the AndroidView update block).
    renderer.autoRotate         = autoRotate
    renderer.glossLevel         = glossLevel
    renderer.shadowOpacity      = shadowOpacity
    renderer.shadowFade         = shadowFade
    renderer.shadowXOffsetRatio = shadowXOffsetRatio
    renderer.shadowYOffsetRatio = shadowYOffsetRatio

    // Add the WebGL canvas to the DOM; create the GL surface; tear down on exit.
    DisposableEffect(glCanvas) {
        jsAppendToBody(glCanvas)
        renderer.onSurfaceCreated(glApi)
        onDispose {
            renderer.release(glApi)
            jsRemoveFromParent(glCanvas)
        }
    }

    // Gesture state stored as MutableState so the lambdas below, created once,
    // always read/write live values without needing to be recreated.
    val dragging    = remember { mutableStateOf(false) }
    val prevX       = remember { mutableStateOf(0.0) }
    val prevY       = remember { mutableStateOf(0.0) }
    val scaleFactor = remember { mutableStateOf(1f) }

    // Register pointer events directly on the canvas element using the onXxx
    // property (not addEventListener) so cleanup is a simple null-assign with
    // no need for a stable function-reference identity.
    DisposableEffect(glCanvas) {
        // Mouse
        jsSetMouseDown(glCanvas) { e ->
            if (e != null) {
                dragging.value = true
                prevX.value    = jsClientX(e)
                prevY.value    = jsClientY(e)
                onGestureActive(true)
            }
        }
        jsSetMouseMove(glCanvas) { e ->
            if (e != null && dragging.value) {
                val x = jsClientX(e); val y = jsClientY(e)
                renderer.handleTouchDrag((x - prevX.value).toFloat(), (y - prevY.value).toFloat())
                prevX.value = x; prevY.value = y
            }
        }
        jsSetMouseUpAndLeave(glCanvas) { _ ->
            if (dragging.value) { dragging.value = false; onGestureActive(false) }
        }
        jsSetWheel(glCanvas) { e ->
            if (e != null) {
                jsPreventDefault(e)
                scaleFactor.value = (scaleFactor.value * if (jsDeltaY(e) > 0) 0.9f else 1.1f)
                    .coerceIn(0.5f, 3f)
                renderer.zoomFactor = scaleFactor.value
            }
        }
        // Touch (single-finger drag; pinch-to-zoom not yet implemented)
        jsSetTouchStart(glCanvas) { e ->
            if (e != null) {
                dragging.value = true
                prevX.value    = jsTouchClientX(e, 0)
                prevY.value    = jsTouchClientY(e, 0)
                onGestureActive(true)
            }
        }
        jsSetTouchMove(glCanvas) { e ->
            if (e != null && dragging.value) {
                jsPreventDefault(e)
                val x = jsTouchClientX(e, 0); val y = jsTouchClientY(e, 0)
                renderer.handleTouchDrag((x - prevX.value).toFloat(), (y - prevY.value).toFloat())
                prevX.value = x; prevY.value = y
            }
        }
        jsSetTouchEnd(glCanvas) { _ ->
            if (dragging.value) { dragging.value = false; onGestureActive(false) }
        }
        onDispose { jsClearPointerEvents(glCanvas) }
    }

    // Render loop driven by Compose's frame clock (backs onto requestAnimationFrame).
    LaunchedEffect(renderer) {
        while (true) {
            withFrameNanos { }
            renderer.onDrawFrame(glApi)
        }
    }

    // Invisible Compose placeholder. onGloballyPositioned fires whenever the
    // layout changes so the WebGL canvas is kept in sync with the composable's
    // position and size on the page.
    Box(
        modifier = modifier.onGloballyPositioned { coords ->
            val b = coords.boundsInWindow()
            val x = b.left.toInt(); val y = b.top.toInt()
            val w = b.width.toInt(); val h = b.height.toInt()
            if (w > 0 && h > 0) {
                // CSS size governs where it appears on the page.
                jsStyleCanvas(glCanvas, x, y, w, h)
                // Backing-buffer size uses physical pixels for crisp HiDPI rendering.
                val dpr = jsDevicePixelRatio()
                val pw = (w * dpr).toInt(); val ph = (h * dpr).toInt()
                jsResizeCanvas(glCanvas, pw, ph)
                renderer.onSurfaceChanged(glApi, pw, ph)
            }
        }
    )
}

// ── Canvas / DOM ─────────────────────────────────────────────────────────────

private fun jsCreateCanvas(): JsAny = js("document.createElement('canvas')")

private fun jsGetWebGL2Ctx(canvas: JsAny): WebGl2Ctx =
    js("canvas.getContext('webgl2', {alpha: true, antialias: false})")

// Appends the canvas to <body> as a fixed-position overlay.
private fun jsAppendToBody(canvas: JsAny): Unit = js("""
    (canvas.style.position = 'fixed',
     canvas.style.pointerEvents = 'auto',
     canvas.style.zIndex = '1',
     document.body.appendChild(canvas))
""")

private fun jsRemoveFromParent(canvas: JsAny): Unit =
    js("(canvas.parentNode && canvas.parentNode.removeChild(canvas))")

// Sets CSS position/size (layout units, before DPR scaling).
private fun jsStyleCanvas(canvas: JsAny, x: Int, y: Int, w: Int, h: Int): Unit = js("""
    (canvas.style.left   = x + 'px',
     canvas.style.top    = y + 'px',
     canvas.style.width  = w + 'px',
     canvas.style.height = h + 'px')
""")

// Sets the canvas backing-buffer resolution (physical pixels).
private fun jsResizeCanvas(canvas: JsAny, w: Int, h: Int): Unit =
    js("(canvas.width = w, canvas.height = h)")

private fun jsDevicePixelRatio(): Double = js("window.devicePixelRatio || 1.0")

// ── Event wiring ─────────────────────────────────────────────────────────────

private fun jsSetMouseDown      (c: JsAny, h: (JsAny?) -> Unit): Unit = js("c.onmousedown   = h")
private fun jsSetMouseMove      (c: JsAny, h: (JsAny?) -> Unit): Unit = js("c.onmousemove   = h")
private fun jsSetMouseUpAndLeave(c: JsAny, h: (JsAny?) -> Unit): Unit = js("(c.onmouseup = h, c.onmouseleave = h)")
private fun jsSetWheel          (c: JsAny, h: (JsAny?) -> Unit): Unit = js("c.onwheel       = h")
private fun jsSetTouchStart     (c: JsAny, h: (JsAny?) -> Unit): Unit = js("c.ontouchstart  = h")
private fun jsSetTouchMove      (c: JsAny, h: (JsAny?) -> Unit): Unit = js("c.ontouchmove   = h")
private fun jsSetTouchEnd       (c: JsAny, h: (JsAny?) -> Unit): Unit = js("(c.ontouchend = h, c.ontouchcancel = h)")

private fun jsClearPointerEvents(c: JsAny): Unit = js("""
    (c.onmousedown = null,  c.onmousemove = null,
     c.onmouseup   = null,  c.onmouseleave = null, c.onwheel = null,
     c.ontouchstart = null, c.ontouchmove  = null,
     c.ontouchend   = null, c.ontouchcancel = null)
""")

// ── Event value accessors ─────────────────────────────────────────────────────

private fun jsClientX(e: JsAny): Double        = js("e.clientX")
private fun jsClientY(e: JsAny): Double        = js("e.clientY")
private fun jsDeltaY (e: JsAny): Double        = js("e.deltaY")
private fun jsPreventDefault(e: JsAny): Unit   = js("e.preventDefault()")
private fun jsTouchClientX(e: JsAny, i: Int): Double = js("e.touches[i].clientX")
private fun jsTouchClientY(e: JsAny, i: Int): Double = js("e.touches[i].clientY")
