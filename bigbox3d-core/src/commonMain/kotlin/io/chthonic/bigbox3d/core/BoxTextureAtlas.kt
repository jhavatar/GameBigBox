package io.chthonic.bigbox3d.core

class BoxTextureAtlas(
    val image: RawImage,
    val regions: Map<RegionFace, AtlasRegion>,
    val supportsFullXAxisRotation: Boolean,
    override val halfWidth: Float,
    override val halfHeight: Float,
    override val halfDepth: Float,
) : CuboidDimensions
