package io.chthonic.gamebigbox.opengl3

enum class ShadowFade(val startRatio: Float, val endRatio: Float) {
    SUPER_SOFT(-0.5f, 1.5f),
    SOFT(0f, 1f), // Even fade, subtle
    REALISTIC(0.2f, 1.1f), // Slightly delayed start, wide fade
    DRAMATIC(0.3f, 1.4f), // Deep core, big blur
}