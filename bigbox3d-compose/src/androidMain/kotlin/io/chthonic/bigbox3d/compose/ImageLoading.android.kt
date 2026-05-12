package io.chthonic.bigbox3d.compose

import android.graphics.Bitmap
import coil3.BitmapImage
import java.nio.ByteBuffer
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
    // copyPixelsToBuffer writes RGBA bytes directly for ARGB_8888 (Android channel order:
    // R, G, B, A), eliminating the intermediate IntArray that getPixels() requires.
    // Premultiplied alpha is irrelevant here because game box textures are fully opaque.
    val rgba = ByteArray(byteCount)
    copyPixelsToBuffer(ByteBuffer.wrap(rgba))
    return RawImage(width, height, rgba)
}
