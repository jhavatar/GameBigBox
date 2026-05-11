package io.chthonic.bigbox3d.compose

import coil3.PlatformContext
import io.chthonic.bigbox3d.core.RawImage
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.ColorAlphaType
import org.jetbrains.skia.ColorType
import org.jetbrains.skia.Image as SkiaImage
import org.jetbrains.skia.ImageInfo
import org.jetbrains.skia.Rect
import org.jetbrains.skia.Surface
import java.net.URI

private const val MAX_IMAGE_PX = 1024

internal actual suspend fun loadRawImageFromUrl(url: String, context: PlatformContext): RawImage {
    val bytes = try {
        val conn = URI(url).toURL().openConnection()
        conn.connectTimeout = 10_000
        conn.readTimeout = 30_000
        conn.getInputStream().use { it.readBytes() }
    } catch (e: Exception) {
        throw ImageLoadException(e)
    }
    // SkiaImage.makeFromEncoded handles WebP (and all other formats) via bundled
    // Skia/libwebp — unlike javax.imageio which has no WebP support.
    val src = SkiaImage.makeFromEncoded(bytes) ?: throw ImageLoadException()
    val (w, h) = scaledDimensions(src.width, src.height, MAX_IMAGE_PX)
    val info = ImageInfo(w, h, ColorType.RGBA_8888, ColorAlphaType.UNPREMUL)
    val bitmap = Bitmap()
    return try {
        bitmap.allocPixels(info)
        Surface.makeRasterN32Premul(w, h).use { surface ->
            surface.canvas.drawImageRect(src, Rect.makeWH(w.toFloat(), h.toFloat()))
            surface.readPixels(bitmap, 0, 0)
        }
        val pixels = bitmap.readPixels(info, w * 4, 0, 0) ?: throw ImageLoadException()
        RawImage(w, h, pixels)
    } finally {
        bitmap.close()
        src.close()
    }
}

private fun scaledDimensions(width: Int, height: Int, maxPx: Int): Pair<Int, Int> {
    if (width <= maxPx && height <= maxPx) return width to height
    val scale = maxPx.toFloat() / maxOf(width, height)
    return (width * scale).toInt().coerceAtLeast(1) to (height * scale).toInt().coerceAtLeast(1)
}
