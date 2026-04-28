package io.chthonic.bigbox3d.core

enum class RegionFace { FRONT, BACK, LEFT, RIGHT, TOP, BOTTOM }

data class AtlasRegion(val u0: Float, val v0: Float, val u1: Float, val v1: Float)

data class AtlasMeta(val image: RawImage, val regions: Map<RegionFace, AtlasRegion>)
