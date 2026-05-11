package io.chthonic.bigbox3d.compose

import androidx.compose.ui.graphics.Color
import io.chthonic.bigbox3d.core.RawImage

/**
 * Averages the RGBA values of all edge pixels (top row, bottom row, left and right columns).
 * Useful for inferring the background color of a box face to use with [CapSource.ColorFill]
 * or [SideSource.ColorFill].
 */
fun RawImage.edgeAverageColor(): Color {
    var r = 0L; var g = 0L; var b = 0L; var a = 0L; var n = 0

    fun accumulate(x: Int, y: Int) {
        val i = (y * width + x) * 4
        r += pixels[i].toInt() and 0xFF
        g += pixels[i + 1].toInt() and 0xFF
        b += pixels[i + 2].toInt() and 0xFF
        a += pixels[i + 3].toInt() and 0xFF
        n++
    }

    for (x in 0 until width) { accumulate(x, 0); accumulate(x, height - 1) }
    for (y in 1 until height - 1) { accumulate(0, y); accumulate(width - 1, y) }

    return Color(r / n / 255f, g / n / 255f, b / n / 255f, a / n / 255f)
}
