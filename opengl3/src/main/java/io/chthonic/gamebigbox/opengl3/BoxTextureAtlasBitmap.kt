package io.chthonic.gamebigbox.opengl3

import android.graphics.Bitmap


class BoxTextureAtlasBitmap(
    val bitmap: Bitmap,
    val regions: Map<RegionFace, AtlasRegion>,
    val supportsFullXAxisRotation: Boolean,
    override val halfWidth: Float,
    override val halfHeight: Float,
    override val halfDepth: Float,
) : CuboidDimensions {

    fun recycle() {
        if (!bitmap.isRecycled) {
            bitmap.recycle()
        }
    }
}

enum class RegionFace {
    FRONT,
    BACK,
    LEFT,
    RIGHT,
    TOP,
    BOTTOM,
}

data class AtlasRegion(
    val u0: Float, val v0: Float,
    val u1: Float, val v1: Float
)