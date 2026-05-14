package io.chthonic.bigbox3d.compose

import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.chthonic.bigbox3d.core.GlossLevel
import io.chthonic.bigbox3d.core.RotationSpeed
import io.chthonic.bigbox3d.core.ShadowFade
import io.chthonic.bigbox3d.core.ShadowOpacity

/**
 * A convenience wrapper around [BigBox3D] intended for use as a loading indicator.
 *
 * The widget stays permanently in the composition so its GL state and texture atlas are
 * preserved across show/hide cycles. When [visible] becomes false the render loop is
 * paused immediately (zero GPU cost), the last rendered frame fades out over
 * [fadeDurationMs] milliseconds, and the composable collapses to zero size only after
 * the fade completes so it no longer occupies layout space or intercepts touch events.
 *
 * @param textures face textures for the box
 * @param visible whether the progress indicator is shown
 * @param modifier positioning modifier — size is managed internally
 * @param size rendered size of the box when visible
 * @param fadeDurationMs duration of the fade-in / fade-out animation in milliseconds
 * @param rotationSpeed auto-rotation speed; defaults to [RotationSpeed.VERY_FAST]
 * @param glossLevel surface glossiness
 * @param shadowOpacity opacity of the projected shadow
 * @param shadowFade softness of the shadow falloff
 */
@Composable
fun BigBox3DProgress(
    textures: BoxTexture,
    visible: Boolean,
    modifier: Modifier = Modifier,
    size: Dp = 200.dp,
    fadeDurationMs: Int = 300,
    rotationSpeed: RotationSpeed = RotationSpeed.VERY_FAST,
    glossLevel: GlossLevel = GlossLevel.SEMI_GLOSS,
    shadowOpacity: ShadowOpacity = ShadowOpacity.STRONG,
    shadowFade: ShadowFade = ShadowFade.REALISTIC,
) {
    val transition = updateTransition(visible, label = "BigBox3DProgress")
    val alpha by transition.animateFloat(
        transitionSpec = { tween(fadeDurationMs) },
        label = "alpha",
    ) { if (it) 1f else 0f }
    val isAnimating = transition.currentState != transition.targetState

    BigBox3D(
        textures = textures,
        paused = !visible,
        rotationSpeed = rotationSpeed,
        glossLevel = glossLevel,
        shadowOpacity = shadowOpacity,
        shadowFade = shadowFade,
        modifier = modifier
            .size(if (visible || isAnimating) size else 0.dp)
            .alpha(alpha),
    )
}
