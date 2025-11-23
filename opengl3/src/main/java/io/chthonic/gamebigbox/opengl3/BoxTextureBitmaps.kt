package io.chthonic.gamebigbox.opengl3

import android.graphics.Bitmap
import kotlin.math.max

sealed interface BoxTextureBitmaps : CuboidDimensions {
    val supportsFullXAxisRotation: Boolean
    fun toList(): List<Bitmap>
}

data class FullBoxTextureBitmaps(
    val front: Bitmap,
    val back: Bitmap,
    val top: Bitmap,
    val bottom: Bitmap,
    val left: Bitmap,
    val right: Bitmap,
) : BoxTextureBitmaps, CuboidDimensions by CuboidDimensionsImpl(front, left) {
    override val supportsFullXAxisRotation: Boolean = true
    override fun toList(): List<Bitmap> = listOf(front, back, left, right, top, bottom)
}

data class EquitorialTextureBitmaps(
    val front: Bitmap,
    val back: Bitmap,
    val left: Bitmap,
    val right: Bitmap,
) : BoxTextureBitmaps, CuboidDimensions by CuboidDimensionsImpl(front, left) {
    override val supportsFullXAxisRotation: Boolean = false
    override fun toList(): List<Bitmap> = listOf(front, back, left, right)
}

interface CuboidDimensions {
    val halfWidth: Float
    val halfHeight: Float
    val halfDepth: Float
}

private class CuboidDimensionsImpl(front: Bitmap, side: Bitmap) : CuboidDimensions {
    override val halfWidth: Float
    override val halfHeight: Float
    override val halfDepth: Float

    init {
        val widthToHeight = front.width.toFloat() / front.height.toFloat()
        val depthToHeight = side.width.toFloat() / side.height.toFloat()
        val maxDim = max(widthToHeight, max(depthToHeight, 1f))
        halfWidth = widthToHeight / maxDim
        halfHeight = 1f / maxDim
        halfDepth = depthToHeight / maxDim
    }
}