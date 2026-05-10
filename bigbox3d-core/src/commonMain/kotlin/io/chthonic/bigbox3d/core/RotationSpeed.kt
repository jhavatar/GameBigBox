package io.chthonic.bigbox3d.core

enum class RotationSpeed(internal val deltaX: Float) {
    NONE(0f),
    VERY_SLOW(0.4f),
    SLOW(0.8f),
    NORMAL(1.5f),
    FAST(4.0f),
    VERY_FAST(10.0f),
}
