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

// Cached once for the process lifetime — CGColorSpaceCreateDeviceRGB is stateless and
// identical on every call; creating and releasing it per-decode is unnecessary overhead.
private val deviceRGBColorSpace = CGColorSpaceCreateDeviceRGB()!!

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

actual suspend fun loadRawImageFromBytes(bytes: ByteArray): RawImage =
    bytes.usePinned { pinned ->
        // dataWithBytesNoCopy avoids copying the ByteArray into a new NSData buffer.
        // freeWhenDone=false because K/N owns the memory via usePinned.
        // decodeNSData is called inside the block so the array stays pinned for the
        // full UIImage.imageWithData decode, preventing a use-after-unpin.
        val data = NSData.dataWithBytesNoCopy(pinned.addressOf(0), bytes.size.toULong(), false)!!
        decodeNSData(data)
    }

private fun decodeNSData(data: NSData): RawImage {
    val uiImage = UIImage.imageWithData(data) ?: throw ImageLoadException()
    return renderToRawImage(uiImage)
}

private fun renderToRawImage(uiImage: UIImage): RawImage {
    val (srcW, srcH) = uiImage.size.useContents { width.toInt() to height.toInt() }
    if (srcW <= 0 || srcH <= 0) throw ImageLoadException()

    val scale = if (srcW > MAX_IMAGE_PX || srcH > MAX_IMAGE_PX)
        MAX_IMAGE_PX.toFloat() / maxOf(srcW, srcH) else 1f
    val w = (srcW * scale).toInt().coerceAtLeast(1)
    val h = (srcH * scale).toInt().coerceAtLeast(1)

    val cgImage = uiImage.CGImage ?: throw ImageLoadException()
    val pixelData = ByteArray(w * h * 4)

    pixelData.usePinned { pinned ->
        // bitmapInfo = kCGImageAlphaPremultipliedLast (= 1) — RGBA8 with premultiplied alpha
        val ctx = CGBitmapContextCreate(
            data             = pinned.addressOf(0),
            width            = w.toULong(),
            height           = h.toULong(),
            bitsPerComponent = 8uL,
            bytesPerRow      = (w * 4).toULong(),
            space            = deviceRGBColorSpace,
            bitmapInfo       = 1u,
        ) ?: throw ImageLoadException()
        CGContextDrawImage(ctx, CGRectMake(0.0, 0.0, w.toDouble(), h.toDouble()), cgImage)
        CGContextRelease(ctx)
    }

    return RawImage(w, h, pixelData)
}
