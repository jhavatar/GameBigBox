package io.chthonic.gamebigbox.opengl3

import android.graphics.Bitmap

sealed interface BoxTextureUrls {
    suspend fun toBitmap(urlToBitmap: suspend (String) -> Bitmap): BoxTextureBitmaps
}

data class FullBoxTextureUrls(
    val front: String,
    val back: String,
    val top: String,
    val bottom: String,
    val left: String,
    val right: String,
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

data class EquatorialBoxTextureUrls(
    val front: String,
    val back: String,
    val left: String,
    val right: String,
) : BoxTextureUrls {
    override suspend fun toBitmap(urlToBitmap: suspend (String) -> Bitmap): BoxTextureBitmaps {
        return EquitorialTextureBitmaps(
            front = urlToBitmap(front),
            back = urlToBitmap(back),
            left = urlToBitmap(left),
            right = urlToBitmap(right),
        )
    }
}