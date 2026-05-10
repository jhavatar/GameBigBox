package io.chthonic.bigbox3d.core

import kotlin.math.max

/** Derive dimensions from front (width×height) and side (depth×height) images. */
fun cuboidDimensions(front: RawImage, side: RawImage): CuboidDimensions =
    cuboidDimensionsFromRatios(
        widthToHeight = front.width.toFloat() / front.height.toFloat(),
        depthToHeight = side.width.toFloat() / side.height.toFloat(),
    )

/** Derive dimensions from front (width×height) and an explicit depth-to-height ratio. */
fun cuboidDimensions(front: RawImage, depthRatio: Float): CuboidDimensions =
    cuboidDimensionsFromRatios(
        widthToHeight = front.width.toFloat() / front.height.toFloat(),
        depthToHeight = depthRatio,
    )

/** Derive dimensions from front (width×height) and top (width×depth) images. */
fun cuboidDimensionsFromTop(front: RawImage, top: RawImage): CuboidDimensions {
    val widthToHeight = front.width.toFloat() / front.height.toFloat()
    return cuboidDimensionsFromRatios(
        widthToHeight = widthToHeight,
        depthToHeight = widthToHeight * (top.height.toFloat() / top.width.toFloat()),
    )
}

interface CuboidDimensions {
    val halfWidth: Float
    val halfHeight: Float
    val halfDepth: Float
}

private fun cuboidDimensionsFromRatios(widthToHeight: Float, depthToHeight: Float): CuboidDimensions {
    val maxDim = max(widthToHeight, max(depthToHeight, 1f))
    return object : CuboidDimensions {
        override val halfWidth  = widthToHeight / maxDim
        override val halfHeight = 1f / maxDim
        override val halfDepth  = depthToHeight / maxDim
    }
}
