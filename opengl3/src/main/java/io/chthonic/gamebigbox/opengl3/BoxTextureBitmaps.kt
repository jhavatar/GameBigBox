package io.chthonic.gamebigbox.opengl3

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import androidx.core.graphics.createBitmap
import androidx.core.graphics.scale
import io.chthonic.gamebigbox.opengl3.RegionFace.BACK
import io.chthonic.gamebigbox.opengl3.RegionFace.BOTTOM
import io.chthonic.gamebigbox.opengl3.RegionFace.FRONT
import io.chthonic.gamebigbox.opengl3.RegionFace.LEFT
import io.chthonic.gamebigbox.opengl3.RegionFace.RIGHT
import io.chthonic.gamebigbox.opengl3.RegionFace.TOP
import kotlin.math.min

sealed interface BoxTextureBitmaps {
    fun toAtlas(debugOverlay: Boolean): BoxTextureAtlasBitmap
    fun recycle()
}

/**
 * Bitmaps for all  sides are provided.
 */
data class FullBoxTextureBitmaps(
    val front: Bitmap,
    val back: Bitmap,
    val top: Bitmap,
    val bottom: Bitmap,
    val left: Bitmap,
    val right: Bitmap,
) : BoxTextureBitmaps {
    private fun toList(): List<Bitmap> = listOf(front, back, left, right, top, bottom)

    override fun toAtlas(debugOverlay: Boolean): BoxTextureAtlasBitmap {
        val dimensions = CuboidDimensionsImpl(front = front, side = left)
        val atlasMeta = toList().buildAtlas2x3(
            halfW = dimensions.halfWidth,
            halfH = dimensions.halfHeight,
            halfD = dimensions.halfDepth,
            showDebugOverlay = debugOverlay,
            normalizeUVs = true
        )
        return BoxTextureAtlasBitmap(
            bitmap = atlasMeta.bitmap,
            regions = atlasMeta.regions,
            supportsFullXAxisRotation = true,
            halfWidth = dimensions.halfWidth,
            halfHeight = dimensions.halfHeight,
            halfDepth = dimensions.halfDepth,
        )
    }

    override fun recycle() {
        toList().forEach {
            if (!it.isRecycled) {
                it.recycle()
            }
        }
    }
}

/**
 * Bitmaps provided for all sides except top and bottom.
 */
data class EquitorialBoxTextureBitmaps(
    val front: Bitmap,
    val back: Bitmap,
    val left: Bitmap,
    val right: Bitmap,
) : BoxTextureBitmaps {
    private fun toList(): List<Bitmap> = listOf(front, back, left, right)

    override fun toAtlas(debugOverlay: Boolean): BoxTextureAtlasBitmap {
        val list = toList()
        val fullList = list.padToSize(6)
        val dimensions = CuboidDimensionsImpl(front = front, side = left)
        val atlasMeta = fullList.buildAtlas2x3(
            halfW = dimensions.halfWidth,
            halfH = dimensions.halfHeight,
            halfD = dimensions.halfDepth,
            showDebugOverlay = debugOverlay,
            normalizeUVs = true
        )
        for (i in list.size until fullList.size) {
            fullList[i].recycle()
        }
        return BoxTextureAtlasBitmap(
            bitmap = atlasMeta.bitmap,
            regions = atlasMeta.regions,
            supportsFullXAxisRotation = false,
            halfWidth = dimensions.halfWidth,
            halfHeight = dimensions.halfHeight,
            halfDepth = dimensions.halfDepth,
        )
    }

    override fun recycle() {
        toList().forEach {
            if (!it.isRecycled) {
                it.recycle()
            }
        }
    }
}


private fun List<Bitmap>.padToSize(targetSize: Int): List<Bitmap> {
    return if (size >= targetSize) {
        this
    } else {
        val blackBitmap = createBitmap(1, 1).apply {
            eraseColor(0xFF000000.toInt()) // solid black pixel
        }
        this + List(targetSize - size) { blackBitmap }
    }
}

//--------------------------------------------------------------------
// Build 2×3 Atlas Bitmap with optional debug grid overlay
//--------------------------------------------------------------------
fun List<Bitmap>.buildAtlas2x3(
    halfW: Float,
    halfH: Float,
    halfD: Float,
    showDebugOverlay: Boolean = false,
    normalizeUVs: Boolean = true,
): AtlasMeta {
    require(size == 6) { "Expected 6 bitmaps, got $size" }

    val faces = listOf(FRONT, BACK, LEFT, RIGHT, TOP, BOTTOM)

    // Physical ratios for each face (full size, not half)
    val ratios = mapOf(
        FRONT to (halfW to halfH),
        BACK to (halfW to halfH),
        LEFT to (halfD to halfH),
        RIGHT to (halfD to halfH),
        TOP to (halfW to halfD),
        BOTTOM to (halfW to halfD)
    )

    // Normalize by the largest physical dimension so the atlas is proportional
    val maxDim = listOf(halfW, halfH, halfD).maxOrNull() ?: 1f

    val cols = 3
    val rows = 2

    val basePixels = maxOf { it.height }

    // compute each face’s target pixel size based on physical proportions
    val targetSizes = faces.map {
        val (rw, rh) = ratios[it]!!
        val widthPx = ((rw / maxDim) * basePixels).toInt().coerceAtLeast(1)
        val heightPx = ((rh / maxDim) * basePixels).toInt().coerceAtLeast(1)
        widthPx to heightPx
    }

    // Determine per-column and per-row maxima
    val colWidths =
        IntArray(cols) { c -> (0 until rows).maxOf { r -> targetSizes[r * cols + c].first } }
    val rowHeights =
        IntArray(rows) { r -> (0 until cols).maxOf { c -> targetSizes[r * cols + c].second } }

    val atlasWidth = colWidths.sum()
    val atlasHeight = rowHeights.sum()

    val atlas = createBitmap(atlasWidth, atlasHeight)
    val canvas = Canvas(atlas)
    val regions = mutableMapOf<RegionFace, AtlasRegion>()

    val debugOverlay: DebugOverlay? = if (showDebugOverlay) {
        val minDim = targetSizes.minOf { min(it.first, it.second) }.toFloat()
        DebugOverlay(
            borderPaint = Paint().apply {
                color = Color.RED
                strokeWidth = minDim * 0.1f
                style = Paint.Style.STROKE
            },
            textPaint = Paint().apply {
                color = Color.RED
                style = Paint.Style.FILL
                textSize = minDim * 0.5f
                isFakeBoldText = true
            }
        )
    } else {
        null
    }

    var yOffset = 0
    for (r in 0 until rows) {
        var xOffset = 0
        for (c in 0 until cols) {
            val i = r * cols + c
            val bmp = this[i]
            val (w, h) = targetSizes[i]
            val scaled = bmp.scale(w, h)
            canvas.drawBitmap(scaled, xOffset.toFloat(), yOffset.toFloat(), null)

            val u0 = if (normalizeUVs) xOffset.toFloat() / atlasWidth else xOffset.toFloat()
            val v0 = if (normalizeUVs) yOffset.toFloat() / atlasHeight else yOffset.toFloat()
            val u1 =
                if (normalizeUVs) (xOffset + w).toFloat() / atlasWidth else (xOffset + w).toFloat()
            val v1 =
                if (normalizeUVs) (yOffset + h).toFloat() / atlasHeight else (yOffset + h).toFloat()

            regions[faces[i]] = AtlasRegion(u0, v0, u1, v1)

            debugOverlay?.let {
                canvas.drawRect(
                    android.graphics.Rect(
                        xOffset, yOffset, xOffset + w, yOffset + h,
                    ),
                    it.borderPaint,
                )
                val text = faces[i].debugLabel
                val textW = it.textPaint.measureText(text)
                canvas.drawText(
                    text,
                    xOffset + w / 2f - textW / 2f,
                    yOffset + h / 2f + it.textPaint.textSize / 2f,
                    it.textPaint,
                )
            }

            xOffset += colWidths[c]
        }
        yOffset += rowHeights[r]
    }

    return AtlasMeta(atlas, regions)
}

data class AtlasMeta(
    val bitmap: Bitmap,
    val regions: Map<RegionFace, AtlasRegion>
)

private val RegionFace.debugLabel: String
    get() = name.first().toString()

private data class DebugOverlay(val borderPaint: Paint, val textPaint: Paint)