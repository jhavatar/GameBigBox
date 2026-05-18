@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, kotlinx.cinterop.BetaInteropApi::class)

package io.chthonic.bigbox3d.compose

import coil3.PlatformContext
import io.chthonic.bigbox3d.core.RawImage
import kotlinx.cinterop.*
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.CoreGraphics.*
import platform.Foundation.*
import platform.UIKit.UIImage
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

private const val MAX_IMAGE_PX = 1024

// context is unused on iOS — NSURLSession.sharedSession needs no app context
internal actual suspend fun loadRawImageFromUrl(url: String, @Suppress("UNUSED_PARAMETER") context: PlatformContext): RawImage {
    val nsUrl = NSURL.URLWithString(url) ?: throw ImageLoadException()
    val data: NSData = suspendCancellableCoroutine { cont ->
        val task = NSURLSession.sharedSession.dataTaskWithURL(nsUrl) { data, _, error ->
            when {
                error != null || data == null -> cont.resumeWithException(ImageLoadException())
                else                          -> cont.resume(data)
            }
        }
        cont.invokeOnCancellation { task.cancel() }
        task.resume()
    }
    return decodeNSData(data)
}

actual suspend fun loadRawImageFromBytes(bytes: ByteArray): RawImage {
    val data = bytes.usePinned { pinned ->
        NSData.dataWithBytes(pinned.addressOf(0), bytes.size.toULong())
    }
    return decodeNSData(data)
}

private fun decodeNSData(data: NSData): RawImage {
    val uiImage = UIImage.imageWithData(data) ?: throw ImageLoadException()
    return renderToRawImage(uiImage)
}

private fun renderToRawImage(uiImage: UIImage): RawImage {
    val srcW = uiImage.size.useContents { width.toInt() }
    val srcH = uiImage.size.useContents { height.toInt() }
    if (srcW <= 0 || srcH <= 0) throw ImageLoadException()

    val scale = if (srcW > MAX_IMAGE_PX || srcH > MAX_IMAGE_PX)
        MAX_IMAGE_PX.toFloat() / maxOf(srcW, srcH) else 1f
    val w = (srcW * scale).toInt().coerceAtLeast(1)
    val h = (srcH * scale).toInt().coerceAtLeast(1)

    val cgImage = uiImage.CGImage ?: throw ImageLoadException()
    val pixelData = ByteArray(w * h * 4)

    pixelData.usePinned { pinned ->
        val colorSpace = CGColorSpaceCreateDeviceRGB() ?: throw ImageLoadException()
        // bitmapInfo = kCGImageAlphaPremultipliedLast (= 1) — RGBA8 with premultiplied alpha
        val ctx = CGBitmapContextCreate(
            data             = pinned.addressOf(0),
            width            = w.toULong(),
            height           = h.toULong(),
            bitsPerComponent = 8uL,
            bytesPerRow      = (w * 4).toULong(),
            space            = colorSpace,
            bitmapInfo       = 1u,
        )
        CGColorSpaceRelease(colorSpace)
        if (ctx == null) throw ImageLoadException()
        CGContextDrawImage(ctx, CGRectMake(0.0, 0.0, w.toDouble(), h.toDouble()), cgImage)
        CGContextRelease(ctx)
    }

    return RawImage(w, h, pixelData)
}
