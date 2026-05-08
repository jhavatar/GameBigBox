package io.chthonic.bigbox3d.core

enum class GlossLevel(val glossValue: Float) {
    MATTE(0.0f),
    SEMI_GLOSS(0.3f),
    GLOSSY(0.6f),
    HIGH_GLOSS(1.0f),
}
