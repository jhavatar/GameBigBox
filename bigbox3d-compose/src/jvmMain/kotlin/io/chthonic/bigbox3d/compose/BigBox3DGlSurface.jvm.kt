@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package io.chthonic.bigbox3d.compose

import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import io.chthonic.bigbox3d.core.BoxTextureAtlas
import io.chthonic.bigbox3d.core.CuboidRenderer
import io.chthonic.bigbox3d.core.GlApiImpl
import io.chthonic.bigbox3d.core.GlossLevel
import io.chthonic.bigbox3d.core.ShadowFade
import io.chthonic.bigbox3d.core.ShadowOpacity
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.withContext
import org.lwjgl.glfw.GLFW.GLFW_CONTEXT_VERSION_MAJOR
import org.lwjgl.glfw.GLFW.GLFW_CONTEXT_VERSION_MINOR
import org.lwjgl.glfw.GLFW.GLFW_FALSE
import org.lwjgl.glfw.GLFW.GLFW_OPENGL_CORE_PROFILE
import org.lwjgl.glfw.GLFW.GLFW_OPENGL_FORWARD_COMPAT
import org.lwjgl.glfw.GLFW.GLFW_OPENGL_PROFILE
import org.lwjgl.glfw.GLFW.GLFW_TRUE
import org.lwjgl.glfw.GLFW.GLFW_VISIBLE
import org.lwjgl.glfw.GLFW.glfwCreateWindow
import org.lwjgl.glfw.GLFW.glfwDefaultWindowHints
import org.lwjgl.glfw.GLFW.glfwDestroyWindow
import org.lwjgl.glfw.GLFW.glfwInit
import org.lwjgl.glfw.GLFW.glfwMakeContextCurrent
import org.lwjgl.glfw.GLFW.glfwWindowHint
import org.lwjgl.opengl.GL
import org.lwjgl.opengl.GL11.GL_DEPTH_COMPONENT
import org.lwjgl.opengl.GL11.GL_LINEAR
import org.lwjgl.opengl.GL11.GL_RGBA
import org.lwjgl.opengl.GL11.GL_TEXTURE_2D
import org.lwjgl.opengl.GL11.GL_TEXTURE_MAG_FILTER
import org.lwjgl.opengl.GL11.GL_TEXTURE_MIN_FILTER
import org.lwjgl.opengl.GL11.GL_UNSIGNED_BYTE
import org.lwjgl.opengl.GL11.glBindTexture
import org.lwjgl.opengl.GL11.glDeleteTextures
import org.lwjgl.opengl.GL11.glGenTextures
import org.lwjgl.opengl.GL11.glReadPixels
import org.lwjgl.opengl.GL11.glTexImage2D
import org.lwjgl.opengl.GL11.glTexParameteri
import org.lwjgl.opengl.GL30.GL_COLOR_ATTACHMENT0
import org.lwjgl.opengl.GL30.GL_DEPTH_ATTACHMENT
import org.lwjgl.opengl.GL30.GL_DEPTH_COMPONENT16
import org.lwjgl.opengl.GL30.GL_FRAMEBUFFER
import org.lwjgl.opengl.GL30.GL_RENDERBUFFER
import org.lwjgl.opengl.GL30.glBindFramebuffer
import org.lwjgl.opengl.GL30.glBindRenderbuffer
import org.lwjgl.opengl.GL30.glBindVertexArray
import org.lwjgl.opengl.GL30.glDeleteFramebuffers
import org.lwjgl.opengl.GL30.glDeleteRenderbuffers
import org.lwjgl.opengl.GL30.glDeleteVertexArrays
import org.lwjgl.opengl.GL30.glFramebufferRenderbuffer
import org.lwjgl.opengl.GL30.glFramebufferTexture2D
import org.lwjgl.opengl.GL30.glGenFramebuffers
import org.lwjgl.opengl.GL30.glGenRenderbuffers
import org.lwjgl.opengl.GL30.glGenVertexArrays
import org.lwjgl.opengl.GL30.glRenderbufferStorage
import org.lwjgl.system.Configuration
import java.awt.image.BufferedImage
import java.nio.ByteBuffer
import java.util.concurrent.atomic.AtomicBoolean

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

    renderer.autoRotate         = autoRotate
    renderer.glossLevel         = glossLevel
    renderer.shadowOpacity      = shadowOpacity
    renderer.shadowFade         = shadowFade
    renderer.shadowXOffsetRatio = shadowXOffsetRatio
    renderer.shadowYOffsetRatio = shadowYOffsetRatio

    var size by remember { mutableStateOf(IntSize.Zero) }
    var frame by remember { mutableStateOf<ImageBitmap?>(null) }
    val scaleFactor = remember { mutableStateOf(1f) }

    val ctx = remember { GlContext() }

    LaunchedEffect(renderer) {
        var vao = 0
        withContext(ctx.dispatcher) {
            ctx.init()
            // VAO required before any vertex attribute calls on macOS core profile
            vao = glGenVertexArrays()
            glBindVertexArray(vao)
            renderer.onSurfaceCreated(glApi)
        }
        try {
            while (true) {
                withFrameNanos { }
                val s = size
                if (s.width > 0 && s.height > 0) {
                    val newFrame = withContext(ctx.dispatcher) {
                        ctx.ensureFbo(s.width, s.height, renderer, glApi)
                        renderer.onDrawFrame(glApi)
                        ctx.readFrame(s.width, s.height)
                    }
                    frame = newFrame
                }
            }
        } finally {
            withContext(NonCancellable + ctx.dispatcher) {
                renderer.release(glApi)
                glBindVertexArray(0)
                if (vao != 0) glDeleteVertexArrays(vao)
                ctx.destroy()
            }
        }
    }

    Box(
        modifier = modifier
            .onSizeChanged { size = it }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { onGestureActive(true) },
                    onDragEnd   = { onGestureActive(false) },
                    onDragCancel = { onGestureActive(false) },
                ) { _, dragAmount ->
                    renderer.handleTouchDrag(dragAmount.x, dragAmount.y)
                }
            }
            .pointerInput(scaleFactor) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        val scrollY = event.changes.fold(Offset.Zero) { acc, c -> acc + c.scrollDelta }.y
                        if (scrollY != 0f) {
                            scaleFactor.value = (scaleFactor.value * if (scrollY > 0) 0.9f else 1.1f)
                                .coerceIn(0.5f, 3f)
                            renderer.zoomFactor = scaleFactor.value
                        }
                    }
                }
            },
    ) {
        frame?.let {
            Image(
                bitmap = it,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.FillBounds,
            )
        }
    }
}

private class GlContext {
    val dispatcher = newSingleThreadContext("BigBox3D-GL")

    private var window = 0L
    private var fbo = 0
    private var fboTex = 0
    private var fboRbo = 0
    private var fboW = 0
    private var fboH = 0

    fun init() {
        GlfwManager.ensureInit()
        window = GlfwManager.createOffscreenWindow()
        glfwMakeContextCurrent(window)
        GL.createCapabilities()
    }

    fun ensureFbo(w: Int, h: Int, renderer: CuboidRenderer, glApi: GlApiImpl) {
        if (w == fboW && h == fboH) return
        destroyFbo()
        fbo = glGenFramebuffers()
        glBindFramebuffer(GL_FRAMEBUFFER, fbo)

        fboTex = glGenTextures()
        glBindTexture(GL_TEXTURE_2D, fboTex)
        val emptyBuf: ByteBuffer? = null
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, w, h, 0, GL_RGBA, GL_UNSIGNED_BYTE, emptyBuf)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR)
        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, fboTex, 0)

        fboRbo = glGenRenderbuffers()
        glBindRenderbuffer(GL_RENDERBUFFER, fboRbo)
        glRenderbufferStorage(GL_RENDERBUFFER, GL_DEPTH_COMPONENT16, w, h)
        glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_RENDERBUFFER, fboRbo)

        fboW = w; fboH = h
        renderer.onSurfaceChanged(glApi, w, h)
    }

    fun readFrame(w: Int, h: Int): ImageBitmap {
        val buf = ByteBuffer.allocateDirect(w * h * 4)
        glReadPixels(0, 0, w, h, GL_RGBA, GL_UNSIGNED_BYTE, buf)
        val argbPixels = IntArray(w * h)
        // GL reads bottom-up; flip Y when building the image
        for (y in 0 until h) {
            val srcRow = (h - 1 - y) * w
            for (x in 0 until w) {
                val i = (srcRow + x) * 4
                val r = buf.get(i    ).toInt() and 0xFF
                val g = buf.get(i + 1).toInt() and 0xFF
                val b = buf.get(i + 2).toInt() and 0xFF
                val a = buf.get(i + 3).toInt() and 0xFF
                argbPixels[y * w + x] = (a shl 24) or (r shl 16) or (g shl 8) or b
            }
        }
        val image = BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB)
        image.setRGB(0, 0, w, h, argbPixels, 0, w)
        return image.toComposeImageBitmap()
    }

    fun destroy() {
        destroyFbo()
        if (window != 0L) {
            glfwDestroyWindow(window)
            window = 0L
        }
        dispatcher.close()
    }

    private fun destroyFbo() {
        if (fbo != 0) {
            glDeleteFramebuffers(fbo);    fbo    = 0
            glDeleteTextures(fboTex);     fboTex = 0
            glDeleteRenderbuffers(fboRbo); fboRbo = 0
            fboW = 0; fboH = 0
        }
    }
}

private object GlfwManager {
    private val initialized = AtomicBoolean(false)

    fun ensureInit() {
        if (initialized.compareAndSet(false, true)) {
            // Allow GLFW to be initialized from a non-main thread (required for offscreen use)
            Configuration.GLFW_CHECK_THREAD0.set(false)
            check(glfwInit()) { "GLFW init failed" }
        }
    }

    @Synchronized
    fun createOffscreenWindow(): Long {
        glfwDefaultWindowHints()
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE)
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3)
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 2)
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE)
        glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GLFW_TRUE)
        val w = glfwCreateWindow(1, 1, "", 0L, 0L)
        check(w != 0L) { "GLFW offscreen window creation failed" }
        return w
    }
}
