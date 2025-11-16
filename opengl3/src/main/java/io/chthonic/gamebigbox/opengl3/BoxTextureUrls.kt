package io.chthonic.gamebigbox.opengl3

import android.graphics.Bitmap

sealed interface BoxTextureUrls {
    val front: String
    val back: String
    val top: String
    val bottom: String
    val left: String
    val right: String

    suspend fun toBitmap(urlToBitmap: suspend (String) -> Bitmap): BoxTextureBitmaps
}

data class FullBoxTextureUrls(
    override val front: String,
    override val back: String,
    override val top: String,
    override val bottom: String,
    override val left: String,
    override val right: String,
) : BoxTextureUrls {

    override suspend fun toBitmap(urlToBitmap: suspend (String) -> Bitmap): BoxTextureBitmaps {
        return FullBoxTextureBitmaps(
            front = urlToBitmap(front),
            back = urlToBitmap(back),
            top = urlToBitmap(top),
            bottom = urlToBitmap(bottom),
            left = urlToBitmap(left),
            right = urlToBitmap(right),
        )
    }
}