package io.chthonic.bigbox3d.compose

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import coil3.compose.LocalPlatformContext
import io.chthonic.bigbox3d.core.BoxTextureAtlas
import io.chthonic.bigbox3d.core.GlossLevel
import io.chthonic.bigbox3d.core.RotationSpeed
import io.chthonic.bigbox3d.core.ShadowFade
import io.chthonic.bigbox3d.core.ShadowOpacity
import io.chthonic.bigbox3d.core.buildAtlas2x3
import io.chthonic.bigbox3d.core.cuboidDimensions
import io.chthonic.bigbox3d.core.cuboidDimensionsFromTop
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Compose Multiplatform widget that renders a 3D PC game big box.
 *
 * @param textures face textures — either [BoxTextureUrls] (loaded from URLs) or
 *   [BoxRawImages] (pre-loaded, e.g. from bundled resources via [loadRawImageFromBytes])
 * @param rotationSpeed auto-rotation speed; [RotationSpeed.NONE] stops rotation
 * @param glossLevel surface glossiness
 * @param shadowOpacity opacity of the projected shadow
 * @param shadowFade softness of the shadow falloff
 * @param shadowXOffsetRatio shadow center X offset relative to box width (+right)
 * @param shadowYOffsetRatio shadow center Y offset relative to box height (+up)
 * @param onGestureActive fires when a touch gesture starts or ends
 * @param loadingContent composable shown while the atlas is being built; defaults to [CircularProgressIndicator]
 */
@Composable
fun BigBox3D(
    textures: BoxTexture,
    modifier: Modifier = Modifier,
    rotationSpeed: RotationSpeed = RotationSpeed.VERY_SLOW,
    glossLevel: GlossLevel = GlossLevel.SEMI_GLOSS,
    shadowOpacity: ShadowOpacity = ShadowOpacity.STRONG,
    shadowFade: ShadowFade = ShadowFade.REALISTIC,
    shadowXOffsetRatio: Float = 0f,
    shadowYOffsetRatio: Float = 0f,
    onGestureActive: (Boolean) -> Unit = {},
    loadingContent: @Composable () -> Unit = { CircularProgressIndicator() },
) {
    val platformContext = LocalPlatformContext.current
    var atlas by remember { mutableStateOf<BoxTextureAtlas?>(null) }

    LaunchedEffect(textures) {
        atlas = null
        try {
            val rawImages = when (textures) {
                is BoxTextureUrls -> withContext(ioDispatcher) {
                    textures.toRawImages { url -> loadRawImageFromUrl(url, platformContext) }
                }
                is BoxRawImages -> listOf(
                    textures.front, textures.back,
                    textures.left,  textures.right,
                    textures.top,   textures.bottom,
                )
            }
            atlas = withContext(Dispatchers.Default) {
                val dims = when (textures) {
                    is BoxTextureUrls -> when {
                        textures.sides !is SideSource.ColorFill ->
                            cuboidDimensions(front = rawImages[0], side = rawImages[2])
                        textures.caps !is CapSource.ColorFill ->
                            cuboidDimensionsFromTop(front = rawImages[0], top = rawImages[4])
                        else ->
                            cuboidDimensions(front = rawImages[0], depthRatio = 0.18f)
                    }
                    // BoxRawImages always provides all 6 faces; derive depth from the side image.
                    is BoxRawImages -> cuboidDimensions(front = rawImages[0], side = rawImages[2])
                }
                val meta = rawImages.buildAtlas2x3(
                    halfW = dims.halfWidth,
                    halfH = dims.halfHeight,
                    halfD = dims.halfDepth,
                )
                BoxTextureAtlas(
                    image = meta.image,
                    regions = meta.regions,
                    supportsFullXAxisRotation = true,
                    halfWidth = dims.halfWidth,
                    halfHeight = dims.halfHeight,
                    halfDepth = dims.halfDepth,
                )
            }
        } catch (e: Exception) {
            // Never swallow coroutine cancellation — propagate it so Compose can
            // restart the effect with the new key if textures changed.
            if (e is kotlinx.coroutines.CancellationException) throw e
            atlas = null
        }
    }

    atlas?.let { a ->
        BigBox3DGlSurface(
            atlas = a,
            modifier = modifier,
            rotationSpeed = rotationSpeed,
            glossLevel = glossLevel,
            shadowOpacity = shadowOpacity,
            shadowFade = shadowFade,
            shadowXOffsetRatio = shadowXOffsetRatio,
            shadowYOffsetRatio = shadowYOffsetRatio,
            onGestureActive = onGestureActive,
        )
    } ?: Box(modifier = modifier, contentAlignment = Alignment.Center) {
        loadingContent()
    }
}
