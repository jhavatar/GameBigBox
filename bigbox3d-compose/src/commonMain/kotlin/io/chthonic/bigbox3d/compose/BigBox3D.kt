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
import io.chthonic.bigbox3d.core.ShadowFade
import io.chthonic.bigbox3d.core.ShadowOpacity
import io.chthonic.bigbox3d.core.buildAtlas2x3
import io.chthonic.bigbox3d.core.cuboidDimensions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Compose Multiplatform widget that renders a 3D PC game big box.
 *
 * @param textureUrls textures for each face of the box
 * @param autoRotate continuously rotate the box when true
 * @param glossLevel surface glossiness
 * @param shadowOpacity opacity of the projected shadow
 * @param shadowFade softness of the shadow falloff
 * @param shadowXOffsetRatio shadow center X offset relative to box width (+right)
 * @param shadowYOffsetRatio shadow center Y offset relative to box height (+up)
 * @param onGestureActive fires when a touch gesture starts or ends
 */
@Composable
fun BigBox3D(
    textureUrls: BoxTextureUrls,
    modifier: Modifier = Modifier,
    autoRotate: Boolean = true,
    glossLevel: GlossLevel = GlossLevel.SEMI_GLOSS,
    shadowOpacity: ShadowOpacity = ShadowOpacity.STRONG,
    shadowFade: ShadowFade = ShadowFade.REALISTIC,
    shadowXOffsetRatio: Float = 0f,
    shadowYOffsetRatio: Float = 0f,
    onGestureActive: (Boolean) -> Unit = {},
) {
    val platformContext = LocalPlatformContext.current
    var atlas by remember { mutableStateOf<BoxTextureAtlas?>(null) }

    LaunchedEffect(textureUrls) {
        atlas = null
        try {
            val rawImages = withContext(Dispatchers.IO) {
                textureUrls.toRawImages { url -> loadRawImageFromUrl(url, platformContext) }
            }
            atlas = withContext(Dispatchers.Default) {
                val dims = cuboidDimensions(front = rawImages[0], side = rawImages[2])
                val meta = rawImages.buildAtlas2x3(
                    halfW = dims.halfWidth,
                    halfH = dims.halfHeight,
                    halfD = dims.halfDepth,
                )
                BoxTextureAtlas(
                    image = meta.image,
                    regions = meta.regions,
                    supportsFullXAxisRotation = textureUrls.supportsFullXAxisRotation,
                    halfWidth = dims.halfWidth,
                    halfHeight = dims.halfHeight,
                    halfDepth = dims.halfDepth,
                )
            }
        } catch (e: Exception) {
            atlas = null
        }
    }

    atlas?.let { a ->
        BigBox3DGlSurface(
            atlas = a,
            modifier = modifier,
            autoRotate = autoRotate,
            glossLevel = glossLevel,
            shadowOpacity = shadowOpacity,
            shadowFade = shadowFade,
            shadowXOffsetRatio = shadowXOffsetRatio,
            shadowYOffsetRatio = shadowYOffsetRatio,
            onGestureActive = onGestureActive,
        )
    } ?: Box(modifier = modifier, contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}
