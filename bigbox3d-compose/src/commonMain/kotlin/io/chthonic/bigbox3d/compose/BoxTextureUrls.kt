package io.chthonic.bigbox3d.compose

import io.chthonic.bigbox3d.core.RawImage
import io.chthonic.bigbox3d.core.blackRawImage

sealed interface BoxTextureUrls {
    val supportsFullXAxisRotation: Boolean
    /** Returns images in atlas order: [front, back, left, right, top, bottom]. */
    suspend fun toRawImages(loader: suspend (String) -> RawImage): List<RawImage>
}

data class FullBoxTextureUrls(
    val front: String,
    val back: String,
    val top: String,
    val bottom: String,
    val left: String,
    val right: String,
) : BoxTextureUrls {
    override val supportsFullXAxisRotation = true
    override suspend fun toRawImages(loader: suspend (String) -> RawImage): List<RawImage> =
        listOf(loader(front), loader(back), loader(left), loader(right), loader(top), loader(bottom))
}

data class EquatorialBoxTextureUrls(
    val front: String,
    val back: String,
    val left: String,
    val right: String,
) : BoxTextureUrls {
    override val supportsFullXAxisRotation = false
    override suspend fun toRawImages(loader: suspend (String) -> RawImage): List<RawImage> {
        val black = blackRawImage()
        return listOf(loader(front), loader(back), loader(left), loader(right), black, black)
    }
}
