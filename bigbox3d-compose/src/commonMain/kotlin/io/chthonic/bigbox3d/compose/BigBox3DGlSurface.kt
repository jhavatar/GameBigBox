package io.chthonic.bigbox3d.compose

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.chthonic.bigbox3d.core.BoxTextureAtlas
import io.chthonic.bigbox3d.core.GlossLevel
import io.chthonic.bigbox3d.core.ShadowFade
import io.chthonic.bigbox3d.core.ShadowOpacity

@Composable
internal expect fun BigBox3DGlSurface(
    atlas: BoxTextureAtlas,
    modifier: Modifier,
    autoRotate: Boolean,
    glossLevel: GlossLevel,
    shadowOpacity: ShadowOpacity,
    shadowFade: ShadowFade,
    shadowXOffsetRatio: Float,
    shadowYOffsetRatio: Float,
    onGestureActive: (Boolean) -> Unit,
)
