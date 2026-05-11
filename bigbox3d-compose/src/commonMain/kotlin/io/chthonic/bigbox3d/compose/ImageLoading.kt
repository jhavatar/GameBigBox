package io.chthonic.bigbox3d.compose

import coil3.PlatformContext
import io.chthonic.bigbox3d.core.RawImage

internal class ImageLoadException(cause: Throwable? = null) : RuntimeException(cause)

internal expect suspend fun loadRawImageFromUrl(url: String, context: PlatformContext): RawImage
