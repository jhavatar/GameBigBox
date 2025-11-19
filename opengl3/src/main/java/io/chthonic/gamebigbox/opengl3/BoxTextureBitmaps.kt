package io.chthonic.gamebigbox.opengl3

import android.graphics.Bitmap

sealed interface BoxTextureBitmaps {
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
) : BoxTextureBitmaps {
    override val supportsFullXAxisRotation: Boolean = true
    override fun toList(): List<Bitmap> = listOf(front, back, left, right, top, bottom)
}

data class EquitorialTextureBitmaps(
    val front: Bitmap,
    val back: Bitmap,
    val left: Bitmap,
    val right: Bitmap,
) : BoxTextureBitmaps {
    override val supportsFullXAxisRotation: Boolean = false
    override fun toList(): List<Bitmap> = listOf(front, back, left, right)
}