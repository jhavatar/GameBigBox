package io.chthonic.bigbox3d.core

import io.chthonic.bigbox3d.core.RegionFace.BACK
import io.chthonic.bigbox3d.core.RegionFace.BOTTOM
import io.chthonic.bigbox3d.core.RegionFace.FRONT
import io.chthonic.bigbox3d.core.RegionFace.LEFT
import io.chthonic.bigbox3d.core.RegionFace.RIGHT
import io.chthonic.bigbox3d.core.RegionFace.TOP

/**
 * Packs 6 face images into a 2-column x 3-row atlas.
 * ┌───────┬───────┬───────┐
 * │ Front │ Back  │ Left  │
 * ├───────┼───────┼───────┤
 * │ Right │ Top   │Bottom │
 * └───────┴───────┴───────┘
 * Input list order: [front, back, left, right, top, bottom]
 */
fun List<RawImage>.buildAtlas2x3(
    halfW: Float,
    halfH: Float,
    halfD: Float,
): AtlasMeta {
    require(size == 6) { "Expected 6 images, got $size" }

    val faces = listOf(FRONT, BACK, LEFT, RIGHT, TOP, BOTTOM)
    val ratios = mapOf(
        FRONT  to (halfW to halfH),
        BACK   to (halfW to halfH),
        LEFT   to (halfD to halfH),
        RIGHT  to (halfD to halfH),
        TOP    to (halfW to halfD),
        BOTTOM to (halfW to halfD),
    )

    val maxDim = listOf(halfW, halfH, halfD).max()
    val cols = 3
    val rows = 2
    val basePixels = maxOf { it.height }

    val targetSizes = faces.map { face ->
        val (rw, rh) = ratios[face]!!
        val w = ((rw / maxDim) * basePixels).toInt().coerceAtLeast(1)
        val h = ((rh / maxDim) * basePixels).toInt().coerceAtLeast(1)
        w to h
    }

    val colWidths  = IntArray(cols) { c -> (0 until rows).maxOf { r -> targetSizes[r * cols + c].first } }
    val rowHeights = IntArray(rows) { r -> (0 until cols).maxOf { c -> targetSizes[r * cols + c].second } }

    val atlasWidth  = colWidths.sum()
    val atlasHeight = rowHeights.sum()
    val atlasPixels = ByteArray(atlasWidth * atlasHeight * 4)

    val regions = mutableMapOf<RegionFace, AtlasRegion>()
    var yOffset = 0

    for (r in 0 until rows) {
        var xOffset = 0
        for (c in 0 until cols) {
            val i = r * cols + c
            val (targetW, targetH) = targetSizes[i]
            this[i].scaleAndBlitInto(atlasPixels, atlasWidth, xOffset, yOffset, targetW, targetH)

            regions[faces[i]] = AtlasRegion(
                u0 = xOffset.toFloat() / atlasWidth,
                v0 = yOffset.toFloat() / atlasHeight,
                u1 = (xOffset + targetW).toFloat() / atlasWidth,
                v1 = (yOffset + targetH).toFloat() / atlasHeight,
            )
            xOffset += colWidths[c]
        }
        yOffset += rowHeights[r]
    }

    return AtlasMeta(RawImage(atlasWidth, atlasHeight, atlasPixels), regions)
}

/**
 * Nearest-neighbour scale to [targetWidth] x [targetHeight] and blit directly into [dest]
 * at ([dstX], [dstY]) — no intermediate ByteArray allocation.
 */
private fun RawImage.scaleAndBlitInto(
    dest: ByteArray, destWidth: Int,
    dstX: Int, dstY: Int,
    targetWidth: Int, targetHeight: Int,
) {
    if (width == targetWidth && height == targetHeight) {
        // No scaling needed — fast row-copy path
        for (row in 0 until height) {
            val srcOffset = row * width * 4
            val dstOffset = (dstY + row) * destWidth * 4 + dstX * 4
            pixels.copyInto(dest, dstOffset, srcOffset, srcOffset + width * 4)
        }
        return
    }
    val xRatio = width.toFloat() / targetWidth
    val yRatio = height.toFloat() / targetHeight
    for (y in 0 until targetHeight) {
        val srcY = (y * yRatio).toInt().coerceIn(0, height - 1)
        for (x in 0 until targetWidth) {
            val srcX = (x * xRatio).toInt().coerceIn(0, width - 1)
            val src = (srcY * width + srcX) * 4
            val dst = (dstY + y) * destWidth * 4 + (dstX + x) * 4
            dest[dst]     = pixels[src]
            dest[dst + 1] = pixels[src + 1]
            dest[dst + 2] = pixels[src + 2]
            dest[dst + 3] = pixels[src + 3]
        }
    }
}

/** Creates a 1x1 black RGBA image (used to pad equatorial atlas to 6 faces). */
fun blackRawImage(): RawImage = RawImage(1, 1, byteArrayOf(0, 0, 0, -1))
