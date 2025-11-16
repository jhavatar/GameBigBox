package io.chthonic.gamebigbox.opengl3

import android.graphics.Bitmap

sealed interface BoxTextureBitmaps {
    val front: Bitmap
    val back: Bitmap
    val top: Bitmap
    val bottom: Bitmap
    val left: Bitmap
    val right: Bitmap

    fun toList(): List<Bitmap> = listOf(front, back, left, right, top, bottom)
}

data class FullBoxTextureBitmaps(
    override val front: Bitmap,
    override val back: Bitmap,
    override val top: Bitmap,
    override val bottom: Bitmap,
    override val left: Bitmap,
    override val right: Bitmap,
) : BoxTextureBitmaps