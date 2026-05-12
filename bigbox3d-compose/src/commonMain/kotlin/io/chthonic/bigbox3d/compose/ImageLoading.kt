package io.chthonic.bigbox3d.compose

import coil3.PlatformContext
import io.chthonic.bigbox3d.core.RawImage

internal class ImageLoadException(cause: Throwable? = null) : RuntimeException(cause)

internal expect suspend fun loadRawImageFromUrl(url: String, context: PlatformContext): RawImage

/** Decodes image bytes into a [RawImage], scaling down to 1024 px on the longest side. */
expect suspend fun loadRawImageFromBytes(bytes: ByteArray): RawImage
