package io.chthonic.bigbox3d.compose

import coil3.PlatformContext
import io.chthonic.bigbox3d.core.RawImage
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.net.URI
import javax.imageio.ImageIO

private const val MAX_IMAGE_PX = 1024

internal actual suspend fun loadRawImageFromUrl(url: String, context: PlatformContext): RawImage {
    val original = try {
        ImageIO.read(URI(url).toURL()) ?: throw ImageLoadException()
    } catch (e: Exception) {
        throw ImageLoadException(e)
    }
    return original.scaledTo(MAX_IMAGE_PX).toRawImage()
}

private fun BufferedImage.scaledTo(maxPx: Int): BufferedImage {
    if (width <= maxPx && height <= maxPx) return this
    val scale = maxPx.toFloat() / maxOf(width, height)
    val w = (width * scale).toInt().coerceAtLeast(1)
    val h = (height * scale).toInt().coerceAtLeast(1)
    val scaled = BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB)
    val g = scaled.createGraphics()
    g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
    g.drawImage(this, 0, 0, w, h, null)
    g.dispose()
    return scaled
}

private fun BufferedImage.toRawImage(): RawImage {
    val src = if (type == BufferedImage.TYPE_INT_ARGB) this else {
        val c = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
        val g = c.createGraphics()
        g.drawImage(this, 0, 0, null)
        g.dispose()
        c
    }
    val argbPixels = IntArray(width * height)
    src.getRGB(0, 0, width, height, argbPixels, 0, width)
    val rgba = ByteArray(width * height * 4)
    for (i in argbPixels.indices) {
        val px = argbPixels[i]
        rgba[i * 4 + 0] = ((px shr 16) and 0xFF).toByte() // R
        rgba[i * 4 + 1] = ((px shr 8)  and 0xFF).toByte() // G
        rgba[i * 4 + 2] = (px          and 0xFF).toByte() // B
        rgba[i * 4 + 3] = ((px shr 24) and 0xFF).toByte() // A
    }
    return RawImage(width, height, rgba)
}
