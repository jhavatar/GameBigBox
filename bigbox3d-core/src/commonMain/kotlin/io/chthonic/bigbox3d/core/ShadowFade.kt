package io.chthonic.bigbox3d.core

enum class ShadowFade(val startRatio: Float, val endRatio: Float) {
    SUPER_SOFT(-0.5f, 1.5f),
    SOFT(0f, 1f),
    REALISTIC(0.2f, 1.1f),
    DRAMATIC(0.3f, 1.4f),
}
