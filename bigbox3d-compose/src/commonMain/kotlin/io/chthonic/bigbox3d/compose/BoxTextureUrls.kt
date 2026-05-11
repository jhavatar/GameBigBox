package io.chthonic.bigbox3d.compose

import androidx.compose.ui.graphics.Color
import io.chthonic.bigbox3d.core.RawImage

sealed interface SideSource {
    /** Explicit left and right face URLs. */
    data class Explicit(val left: String, val right: String) : SideSource
    /** Single spine URL; right face is generated as the horizontal mirror of left. */
    data class Spine(val url: String) : SideSource
    /** Both side faces filled with a solid color; defaults to the front image's edge average. */
    data class ColorFill(val color: Color? = null) : SideSource
}

sealed interface CapSource {
    /** Explicit top and bottom face URLs. */
    data class Explicit(val top: String, val bottom: String) : CapSource
    /** Both cap faces filled with a solid color; defaults to the front image's edge average. */
    data class ColorFill(val color: Color? = null) : CapSource
}

data class BoxTextureUrls(
    val front: String,
    val back: String,
    val sides: SideSource,
    val caps: CapSource,
) {
    val supportsFullXAxisRotation: Boolean = true

    /** Returns images in atlas order: [front, back, left, right, top, bottom]. */
    suspend fun toRawImages(loader: suspend (String) -> RawImage): List<RawImage> {
        val frontImg = loader(front)
        val backImg = loader(back)

        val (leftImg, rightImg) = when (sides) {
            is SideSource.Explicit -> loader(sides.left) to loader(sides.right)
            is SideSource.Spine -> {
                val spineImg = loader(sides.url)
                spineImg to spineImg.flipHorizontal()
            }
            is SideSource.ColorFill -> {
                val img = colorFillRawImage(sides.color ?: frontImg.edgeAverageColor())
                img to img
            }
        }

        val (topImg, bottomImg) = when (caps) {
            is CapSource.Explicit -> loader(caps.top) to loader(caps.bottom)
            is CapSource.ColorFill -> {
                val img = colorFillRawImage(caps.color ?: frontImg.edgeAverageColor())
                img to img
            }
        }

        return listOf(frontImg, backImg, leftImg, rightImg, topImg, bottomImg)
    }
}

private fun RawImage.flipHorizontal(): RawImage {
    val flipped = ByteArray(pixels.size)
    for (y in 0 until height) {
        for (x in 0 until width) {
            val src = (y * width + x) * 4
            val dst = (y * width + (width - 1 - x)) * 4
            flipped[dst] = pixels[src]
            flipped[dst + 1] = pixels[src + 1]
            flipped[dst + 2] = pixels[src + 2]
            flipped[dst + 3] = pixels[src + 3]
        }
    }
    return RawImage(width, height, flipped)
}

private fun colorFillRawImage(color: Color): RawImage {
    val r = (color.red * 255f + 0.5f).toInt().coerceIn(0, 255).toByte()
    val g = (color.green * 255f + 0.5f).toInt().coerceIn(0, 255).toByte()
    val b = (color.blue * 255f + 0.5f).toInt().coerceIn(0, 255).toByte()
    val a = (color.alpha * 255f + 0.5f).toInt().coerceIn(0, 255).toByte()
    return RawImage(1, 1, byteArrayOf(r, g, b, a))
}
