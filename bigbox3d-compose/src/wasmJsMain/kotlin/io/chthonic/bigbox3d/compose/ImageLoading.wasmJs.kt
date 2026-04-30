package io.chthonic.bigbox3d.compose

import coil3.PlatformContext
import io.chthonic.bigbox3d.core.RawImage
import io.chthonic.bigbox3d.network.resolveExternalUrl
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

internal actual suspend fun loadRawImageFromUrl(url: String, context: PlatformContext): RawImage {
    val imageData = fetchImageData(resolveExternalUrl(url))
    val width = jsWidth(imageData)
    val height = jsHeight(imageData)
    val len = jsDataLength(imageData)
    val pixels = ByteArray(len) { i -> jsDataPixel(imageData, i).toByte() }
    return RawImage(width, height, pixels)
}

/**
 * Suspends until the browser has fetched, decoded, and rasterised [url] into an ImageData object
 * (RGBA, capped at 1024 px on the longest side).
 */
private suspend fun fetchImageData(url: String): JsAny =
    suspendCancellableCoroutine { cont ->
        val promise = jsBuildFetchPromise(url)
        jsPromiseThen(
            promise = promise,
            onFulfilled = { v: JsAny? ->
                if (v != null) cont.resume(v)
                else cont.resumeWithException(ImageLoadException())
            },
            onRejected = { _: JsAny? ->
                cont.resumeWithException(ImageLoadException())
            },
        )
    }

// Returns Promise<ImageData>: fetch → blob → ImageBitmap → OffscreenCanvas → ImageData.
private fun jsBuildFetchPromise(url: String): JsAny = js(
    """
    fetch(url)
        .then(function(r) { return r.blob(); })
        .then(function(b) { return createImageBitmap(b); })
        .then(function(bmp) {
            var maxPx = 1024;
            var s = Math.min(1.0, maxPx / Math.max(bmp.width, bmp.height));
            var w = Math.max(1, Math.round(bmp.width  * s));
            var h = Math.max(1, Math.round(bmp.height * s));
            var canvas = new OffscreenCanvas(w, h);
            var ctx = canvas.getContext('2d');
            ctx.drawImage(bmp, 0, 0, w, h);
            bmp.close();
            return ctx.getImageData(0, 0, w, h);
        })
"""
)

// Attaches resolve/reject callbacks to a JS Promise.
// Returns the chained Promise (ignored by callers).
private fun jsPromiseThen(
    promise: JsAny,
    onFulfilled: (JsAny?) -> Unit,
    onRejected: (JsAny?) -> Unit,
): JsAny = js("promise.then(onFulfilled, onRejected)")

// ImageData property/pixel accessors — js() auto-converts JS number → Kotlin Int.
private fun jsWidth(data: JsAny): Int = js("data.width")
private fun jsHeight(data: JsAny): Int = js("data.height")
private fun jsDataLength(data: JsAny): Int = js("data.data.length")
private fun jsDataPixel(data: JsAny, i: Int): Int = js("data.data[i]")
