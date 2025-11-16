package io.chthonic.gamebigbox.opengl3

enum class GlossLevel(val glossValue: Float) {
    /** Raw uncoated cardboard – flat diffuse, no shine. */
    MATTE(0.0f),

    /** Lightly coated paper – faint wide highlights. */
    SEMI_GLOSS(0.3f),

    /** Laminated box – balanced gloss and reflection. */
    GLOSSY(0.6f),

    /** Collector’s edition – tight specular reflections. */
    HIGH_GLOSS(1.0f);
}