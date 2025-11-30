package io.chthonic.gamebigbox.opengl3

import android.graphics.Bitmap
import kotlin.math.max

interface CuboidDimensions {
    val halfWidth: Float
    val halfHeight: Float
    val halfDepth: Float
}

internal class CuboidDimensionsImpl(front: Bitmap, side: Bitmap) : CuboidDimensions {
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