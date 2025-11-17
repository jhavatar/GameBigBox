package io.chthonic.gamebigbox.opengl3

enum class ShadowOpacity(val alpha: Float) {
    NONE(0f),
    FAINT(0.2f),
    SOFT(0.4f),
    STRONG(0.6f),
    HEAVY(0.8f),
    FULL(1.0f);
}