package io.chthonic.bigbox3d.compose

import android.graphics.Bitmap
import coil3.BitmapImage
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware
import coil3.size.Size
import io.chthonic.bigbox3d.core.RawImage

private const val MAX_IMAGE_PX = 1024

internal actual suspend fun loadRawImageFromUrl(url: String, context: PlatformContext): RawImage {
    val request = ImageRequest.Builder(context)
        .data(url)
        .size(Size(MAX_IMAGE_PX, MAX_IMAGE_PX))
        .allowHardware(false)
        .build()
    val result = SingletonImageLoader.get(context).execute(request)
    if (result !is SuccessResult) throw ImageLoadException(cause = null)
    return (result.image as BitmapImage).bitmap.toRawImage()
}

private fun Bitmap.toRawImage(): RawImage {
    val argbPixels = IntArray(width * height)
    getPixels(argbPixels, 0, width, 0, 0, width, height)
    val rgba = ByteArray(width * height * 4)
    for (i in argbPixels.indices) {
        val argb = argbPixels[i]
        rgba[i * 4 + 0] = ((argb shr 16) and 0xFF).toByte() // R
        rgba[i * 4 + 1] = ((argb shr 8)  and 0xFF).toByte() // G
        rgba[i * 4 + 2] = (argb          and 0xFF).toByte() // B
        rgba[i * 4 + 3] = ((argb shr 24) and 0xFF).toByte() // A
    }
    return RawImage(width, height, rgba)
}
