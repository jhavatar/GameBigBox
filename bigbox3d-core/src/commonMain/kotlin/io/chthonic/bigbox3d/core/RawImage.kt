package io.chthonic.bigbox3d.core

/** Platform-agnostic image: RGBA8888 pixels, row-major, top-left origin. */
data class RawImage(val width: Int, val height: Int, val pixels: ByteArray) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is RawImage) return false
        return width == other.width && height == other.height && pixels.contentEquals(other.pixels)
    }
    override fun hashCode(): Int {
        var result = width
        result = 31 * result + height
        result = 31 * result + pixels.contentHashCode()
        return result
    }
}
