@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, kotlinx.cinterop.BetaInteropApi::class)

package io.chthonic.bigbox3d.core

import kotlinx.cinterop.*
import platform.Foundation.NSError
import platform.Metal.*
import platform.posix.memcpy

// ─── MSL source for both programs ───────────────────────────────────────────
//
// Two programs are compiled into one Metal library:
//   cuboid_vertex / cuboid_fragment  — Blinn-Phong textured cuboid
//   shadow_vertex / shadow_fragment  — radial-fade projected shadow
//
// Uniform struct layouts must match flushMainUniforms() / flushShadowUniforms().
//
// MainUniforms (176 bytes = 44 floats):
//   float4x4 uMVP              offset   0  (64 bytes)
//   float4x4 uModel            offset  64  (64 bytes)
//   float4   uLightPos         offset 128  (16 bytes — xyz=pos,  w=0)
//   float4   uViewPos          offset 144  (16 bytes — xyz=pos,  w=0)
//   float4   uLightColorAndGloss offset 160 (16 bytes — xyz=color, w=gloss)
//
// NOTE: In Metal, float3 inside a struct has size=16 (padded to float4 width),
// so `float3 x; float _pad;` is NOT a 16-byte pair — it is 20 bytes.
// All former float3 fields are declared as float4 to get a predictable layout.
//
// ShadowUniforms (32 bytes = 8 floats):
//   float2 uCenter     offset  0
//   float2 uScale      offset  8
//   float  uAlpha      offset 16
//   float  _pad        offset 20
//   float2 uSmoothStep offset 24

// Kept as fallback for apps that don't bundle Shaders.metal in their Xcode project.
// When Shaders.metal is present, device.newDefaultLibrary() loads the pre-compiled
// version and this string is never used.
private val MSL_SOURCE = """
#include <metal_stdlib>
using namespace metal;

// ── Cuboid ──────────────────────────────────────────────────────────────────

struct MainUniforms {
    float4x4 uMVP;
    float4x4 uModel;
    float4   uLightPos;            // .xyz = position, .w = unused
    float4   uViewPos;             // .xyz = position, .w = unused
    float4   uLightColorAndGloss;  // .xyz = light colour, .w = materialGloss
};

struct MainVert {
    float3 position [[attribute(0)]];
    float2 texCoord [[attribute(1)]];
    float3 normal   [[attribute(2)]];
};

struct MainVary {
    float4 position [[position]];
    float2 texCoord;
    float3 normal;
    float3 fragPos;
};

vertex MainVary cuboid_vertex(
        MainVert in              [[stage_in]],
        constant MainUniforms& u [[buffer(3)]])
{
    MainVary out;
    out.position = u.uMVP * float4(in.position, 1.0);
    out.texCoord = in.texCoord;
    out.fragPos  = (u.uModel * float4(in.position, 1.0)).xyz;
    float3x3 nm  = float3x3(u.uModel[0].xyz, u.uModel[1].xyz, u.uModel[2].xyz);
    out.normal   = nm * in.normal;
    return out;
}

fragment float4 cuboid_fragment(
        MainVary          in   [[stage_in]],
        texture2d<float>  tex  [[texture(0)]],
        sampler           samp [[sampler(0)]],
        constant MainUniforms& u [[buffer(3)]])
{
    float3 texColor   = tex.sample(samp, in.texCoord).rgb;
    float3 norm       = normalize(in.normal);
    float3 lightDir   = normalize(u.uLightPos.xyz - in.fragPos);
    float3 viewDir    = normalize(u.uViewPos.xyz  - in.fragPos);
    float3 reflectDir = reflect(-lightDir, norm);
    float  diff       = max(dot(norm, lightDir), 0.0);
    float  gloss      = u.uLightColorAndGloss.w;
    float  shininess  = mix(8.0, 128.0, gloss);
    float  specPower  = mix(0.05, 1.0,  gloss);
    float  spec       = pow(max(dot(viewDir, reflectDir), 0.0), shininess) * specPower;
    float3 result     = texColor * (0.4 + 0.6 * diff) + u.uLightColorAndGloss.xyz * spec;
    return float4(result, 1.0);
}

// ── Shadow ───────────────────────────────────────────────────────────────────

struct ShadowUniforms {
    float2 uCenter;
    float2 uScale;
    float  uAlpha; float _pad;
    float2 uSmoothStep;
};

struct ShadowVert {
    float2 position [[attribute(0)]];
};

struct ShadowVary {
    float4 position [[position]];
    float2 vPos;
};

vertex ShadowVary shadow_vertex(
        ShadowVert in              [[stage_in]],
        constant ShadowUniforms& u [[buffer(1)]])
{
    return { float4(in.position, 0.0, 1.0), in.position };
}

fragment float4 shadow_fragment(
        ShadowVary in              [[stage_in]],
        constant ShadowUniforms& u [[buffer(1)]])
{
    float2 rel  = (in.vPos - u.uCenter) / u.uScale;
    float  r    = length(rel);
    float  fade = smoothstep(u.uSmoothStep[0], u.uSmoothStep[1], r);
    return float4(0.0, 0.0, 0.0, (1.0 - fade) * u.uAlpha);
}
""".trimIndent()

private const val MAIN_UNIFORM_FLOATS   = 44   // 176 bytes
private const val SHADOW_UNIFORM_FLOATS = 8    //  32 bytes

/**
 * Metal-backed [GlApi] for iOS.
 *
 * The OpenGL state-machine is emulated: calls accumulate state (bound buffers, uniform
 * values, enabled attribs, depth/blend flags) and the actual Metal GPU work happens only
 * at [glDrawElements] / [glDrawArrays].
 *
 * Two Metal render-pipeline states are compiled once at construction:
 *   - cuboidPipeline  — first program created  (depth on, blend off, cull back)
 *   - shadowPipeline  — second program created (depth off, blend on,  no cull)
 *
 * The owning surface must call [beginFrame] with the current [MTLRenderCommandEncoderProtocol]
 * before delegating to [CuboidRenderer.onDrawFrame], then call [endFrame] afterwards.
 * The surface must also read [clearColorR/G/B/A] and apply them to the MTKView's clearColor
 * before each frame so the render-pass load action performs the clear.
 */
class GlApiImpl(
    val device: MTLDeviceProtocol,
    private val commandQueue: MTLCommandQueueProtocol,
    colorPixelFormat: MTLPixelFormat = MTLPixelFormatBGRA8Unorm,
    depthPixelFormat: MTLPixelFormat = MTLPixelFormatDepth32Float,
) : GlApi {

    // ── Frame interface (set by surface) ────────────────────────────────────

    var encoder: MTLRenderCommandEncoderProtocol? = null

    /** Read by the surface to configure MTKView.clearColor before each frame. */
    var clearColorR = 0.0; var clearColorG = 0.0
    var clearColorB = 0.0; var clearColorA = 0.0

    // ── Metal objects ────────────────────────────────────────────────────────

    private val library:         MTLLibraryProtocol
    private val cuboidPipeline:  MTLRenderPipelineStateProtocol
    private val shadowPipeline:  MTLRenderPipelineStateProtocol
    private val depthOnState:    MTLDepthStencilStateProtocol
    private val depthOffState:   MTLDepthStencilStateProtocol
    private val linearSampler:   MTLSamplerStateProtocol
    private val mainUniformBuf:  MTLBufferProtocol
    private val shadowUniformBuf: MTLBufferProtocol

    // ── Handle tables ────────────────────────────────────────────────────────

    private var nextHandle = 1
    private fun newHandle() = nextHandle++

    private val mtlBuffers  = HashMap<Int, MTLBufferProtocol>()
    private val mtlTextures = HashMap<Int, MTLTextureProtocol>()

    // ── Program tracking ─────────────────────────────────────────────────────
    // programOrder[0] = first glLinkProgram call  → cuboid
    // programOrder[1] = second glLinkProgram call → shadow

    private val programOrder = ArrayList<Int>(2)
    private var currentProgram = 0

    // ── GL state ─────────────────────────────────────────────────────────────

    private var boundArrayBuf     = 0
    private var boundElementBuf   = 0
    private var boundTex          = 0
    private var depthTestEnabled  = false

    // Issue 3: replace HashMap<Int, Pair<Int,Int>> with three parallel primitive arrays —
    // eliminates per-draw Pair boxing and HashMap iteration overhead.
    private val attribBufHandles = IntArray(8)
    private val attribBufOffsets = IntArray(8)
    private val attribActive     = BooleanArray(8)

    // ── Viewport ─────────────────────────────────────────────────────────────

    private var vpX = 0; private var vpY = 0
    private var vpW = 0; private var vpH = 0

    // ── Uniform stores ───────────────────────────────────────────────────────

    private val uMVP        = FloatArray(16)
    private val uModel      = FloatArray(16)
    private val uLightPos   = FloatArray(3)
    private val uViewPos    = FloatArray(3)
    private val uLightColor = FloatArray(3)
    private var uGloss      = 0f

    private val uCenter     = FloatArray(2)
    private val uScale      = FloatArray(2)
    private var uAlpha      = 0f
    private val uSmoothStep = FloatArray(2)

    // Issue 1: pre-allocated uniform staging buffers reused every frame.
    private val mainUniformData   = FloatArray(MAIN_UNIFORM_FLOATS)
    private val shadowUniformData = FloatArray(SHADOW_UNIFORM_FLOATS)

    // ── Uniform location registry ─────────────────────────────────────────────

    private var nextUniformLoc = 1
    // Issue 2: replace HashMap<Int, Pair<Int,String>> (boxed Int keys, per-frame lookups)
    // with a plain array indexed by the location integer — O(1) array access, no boxing.
    private val locNames  = arrayOfNulls<String>(64)          // index = location integer
    private val infoToLoc = HashMap<Pair<Int, String>, Int>() // (programId, name) → loc; queried once per name

    // ── Init ─────────────────────────────────────────────────────────────────

    init {
        // Issue 5: load pre-compiled library from the app bundle when Shaders.metal is
        // present in the Xcode project (zero startup cost); fall back to runtime compilation
        // so the library works without that file (e.g. when consumed as a Maven dependency).
        library = device.newDefaultLibrary()
            ?: memScoped {
                val err = alloc<ObjCObjectVar<NSError?>>()
                device.newLibraryWithSource(MSL_SOURCE, null, err.ptr)
                    ?: error("Metal shader compile failed: ${err.value?.localizedDescription}")
            }

        cuboidPipeline = buildCuboidPipeline(colorPixelFormat, depthPixelFormat)
        shadowPipeline = buildShadowPipeline(colorPixelFormat, depthPixelFormat)

        depthOnState = device.newDepthStencilStateWithDescriptor(
            MTLDepthStencilDescriptor().apply {
                depthCompareFunction = MTLCompareFunctionLessEqual
                depthWriteEnabled    = true
            }
        )!!

        depthOffState = device.newDepthStencilStateWithDescriptor(
            MTLDepthStencilDescriptor().apply {
                depthCompareFunction = MTLCompareFunctionAlways
                depthWriteEnabled    = false
            }
        )!!

        linearSampler = device.newSamplerStateWithDescriptor(
            MTLSamplerDescriptor().apply {
                minFilter = MTLSamplerMinMagFilterLinear
                magFilter = MTLSamplerMinMagFilterLinear
            }
        )!!

        // Pre-allocated shared uniform buffers updated via memcpy each frame
        mainUniformBuf   = device.newBufferWithLength((MAIN_UNIFORM_FLOATS   * 4).toULong(), MTLResourceStorageModeShared)!!
        shadowUniformBuf = device.newBufferWithLength((SHADOW_UNIFORM_FLOATS * 4).toULong(), MTLResourceStorageModeShared)!!
    }

    private fun buildCuboidPipeline(colorFmt: MTLPixelFormat, depthFmt: MTLPixelFormat): MTLRenderPipelineStateProtocol {
        val desc = MTLRenderPipelineDescriptor()
        desc.vertexFunction           = library.newFunctionWithName("cuboid_vertex")
        desc.fragmentFunction         = library.newFunctionWithName("cuboid_fragment")
        desc.depthAttachmentPixelFormat = depthFmt

        desc.colorAttachments.objectAtIndexedSubscript(0uL).apply {
            pixelFormat     = colorFmt
            blendingEnabled = false
        }

        // Three separate vertex-buffer streams: positions (slot 0), texcoords (slot 1), normals (slot 2)
        val vd = MTLVertexDescriptor.vertexDescriptor()
        vd.attributes.objectAtIndexedSubscript(0uL).apply { format = MTLVertexFormatFloat3; bufferIndex = 0uL; offset = 0uL }
        vd.attributes.objectAtIndexedSubscript(1uL).apply { format = MTLVertexFormatFloat2; bufferIndex = 1uL; offset = 0uL }
        vd.attributes.objectAtIndexedSubscript(2uL).apply { format = MTLVertexFormatFloat3; bufferIndex = 2uL; offset = 0uL }
        vd.layouts.objectAtIndexedSubscript(0uL).stride = 12uL  // float3
        vd.layouts.objectAtIndexedSubscript(1uL).stride = 8uL   // float2
        vd.layouts.objectAtIndexedSubscript(2uL).stride = 12uL  // float3
        desc.vertexDescriptor = vd

        return memScoped {
            val err = alloc<ObjCObjectVar<NSError?>>()
            device.newRenderPipelineStateWithDescriptor(desc, err.ptr)
                ?: error("Cuboid pipeline build failed: ${err.value?.localizedDescription}")
        }
    }

    private fun buildShadowPipeline(colorFmt: MTLPixelFormat, depthFmt: MTLPixelFormat): MTLRenderPipelineStateProtocol {
        val desc = MTLRenderPipelineDescriptor()
        desc.vertexFunction             = library.newFunctionWithName("shadow_vertex")
        desc.fragmentFunction           = library.newFunctionWithName("shadow_fragment")
        desc.depthAttachmentPixelFormat = depthFmt

        desc.colorAttachments.objectAtIndexedSubscript(0uL).apply {
            pixelFormat                 = colorFmt
            blendingEnabled             = true
            sourceRGBBlendFactor        = MTLBlendFactorSourceAlpha
            destinationRGBBlendFactor   = MTLBlendFactorOneMinusSourceAlpha
            sourceAlphaBlendFactor      = MTLBlendFactorSourceAlpha
            destinationAlphaBlendFactor = MTLBlendFactorOneMinusSourceAlpha
        }

        val vd = MTLVertexDescriptor.vertexDescriptor()
        vd.attributes.objectAtIndexedSubscript(0uL).apply { format = MTLVertexFormatFloat2; bufferIndex = 0uL; offset = 0uL }
        vd.layouts.objectAtIndexedSubscript(0uL).stride = 8uL   // float2
        desc.vertexDescriptor = vd

        return memScoped {
            val err = alloc<ObjCObjectVar<NSError?>>()
            device.newRenderPipelineStateWithDescriptor(desc, err.ptr)
                ?: error("Shadow pipeline build failed: ${err.value?.localizedDescription}")
        }
    }

    // ── Surface lifecycle ─────────────────────────────────────────────────────

    /** Called by the surface before delegating to CuboidRenderer.onDrawFrame. */
    fun beginFrame(enc: MTLRenderCommandEncoderProtocol) {
        encoder = enc
        // Issue 4: set winding once per frame — it never changes, so there's no need
        // to emit this Metal command inside every glDrawElements call.
        enc.setFrontFacingWinding(MTLWindingCounterClockwise)
        if (vpW > 0 && vpH > 0) {
            enc.setViewport(cValue {
                originX = vpX.toDouble(); originY = vpY.toDouble()
                width   = vpW.toDouble(); height  = vpH.toDouble()
                znear   = 0.0;           zfar    = 1.0
            })
        }
    }

    /** Called by the surface after CuboidRenderer.onDrawFrame returns. */
    fun endFrame() {
        encoder?.endEncoding()
        encoder = null
    }

    // ── GlApi — shaders / programs ────────────────────────────────────────────
    // GLSL source is ignored; we use the pre-compiled MSL pipelines.
    // glLinkProgram tracks creation order to select the right pipeline at draw time.

    override fun glCreateShader(type: Int): Int = newHandle()
    override fun glShaderSource(shader: Int, source: String) = Unit
    override fun glCompileShader(shader: Int) = Unit
    override fun glGetShaderiv(shader: Int, pname: Int): Int = 1   // always report success
    override fun glGetShaderInfoLog(shader: Int): String = ""
    override fun glDeleteShader(shader: Int) = Unit

    override fun glCreateProgram(): Int = newHandle()
    override fun glAttachShader(program: Int, shader: Int) = Unit
    override fun glLinkProgram(program: Int) { programOrder.add(program) }
    override fun glGetProgramiv(program: Int, pname: Int): Int = 1  // always report success
    override fun glGetProgramInfoLog(program: Int): String = ""
    override fun glDeleteProgram(program: Int) { programOrder.remove(program) }
    override fun glUseProgram(program: Int) { currentProgram = program }

    // ── GlApi — textures ──────────────────────────────────────────────────────

    override fun glGenTextures(n: Int): IntArray = IntArray(n) { newHandle() }
    override fun glBindTexture(target: Int, texture: Int) { boundTex = texture }
    override fun glTexParameteri(target: Int, pname: Int, param: Int) = Unit  // handled by sampler state

    override fun glTexImage2D(width: Int, height: Int, pixels: ByteArray) {
        val desc = MTLTextureDescriptor.texture2DDescriptorWithPixelFormat(
            pixelFormat = MTLPixelFormatRGBA8Unorm,
            width       = width.toULong(),
            height      = height.toULong(),
            mipmapped   = false,
        )
        // Issue 6: private storage lets Metal place the texture in fast GPU-only memory.
        // Upload via a shared staging buffer + async blit, ordered before any render command
        // on the same queue so the texture is ready when the first frame draws.
        desc.storageMode = MTLStorageModePrivate
        val mtlTex    = device.newTextureWithDescriptor(desc)!!
        val byteCount = (width * height * 4).toULong()
        val staging   = pixels.usePinned { pinned ->
            device.newBufferWithBytes(pinned.addressOf(0), byteCount, MTLResourceStorageModeShared)!!
        }
        val cmdBuf  = commandQueue.commandBuffer()!!
        val blitter = cmdBuf.blitCommandEncoder()!!
        blitter.copyFromBuffer(
            staging,
            sourceOffset        = 0uL,
            sourceBytesPerRow   = (width * 4).toULong(),
            sourceBytesPerImage = byteCount,
            sourceSize          = cValue<MTLSize> { this.width = width.toULong(); this.height = height.toULong(); depth = 1uL },
            toTexture           = mtlTex,
            destinationSlice    = 0uL,
            destinationLevel    = 0uL,
            destinationOrigin   = cValue<MTLOrigin> { x = 0uL; y = 0uL; z = 0uL },
        )
        blitter.endEncoding()
        cmdBuf.commit()
        mtlTextures[boundTex] = mtlTex
    }

    override fun glDeleteTextures(textures: IntArray) = textures.forEach { mtlTextures.remove(it) }

    // ── GlApi — uniforms ──────────────────────────────────────────────────────

    override fun glGetUniformLocation(program: Int, name: String): Int {
        val key = program to name
        return infoToLoc.getOrPut(key) {
            val loc = nextUniformLoc++
            locNames[loc] = name  // Issue 2: array write instead of HashMap put
            loc
        }
    }

    override fun glUniformMatrix4fv(location: Int, count: Int, transpose: Boolean, value: FloatArray, offset: Int) {
        // Issue 2: array lookup — no Int boxing, no HashMap overhead
        when (locNames.getOrNull(location)) {
            "uMVP"   -> value.copyInto(uMVP,   destinationOffset = 0, startIndex = offset, endIndex = offset + 16)
            "uModel" -> value.copyInto(uModel,  destinationOffset = 0, startIndex = offset, endIndex = offset + 16)
        }
    }

    override fun glUniform3f(location: Int, x: Float, y: Float, z: Float) {
        when (locNames.getOrNull(location)) {
            "uLightPos"   -> { uLightPos[0]   = x; uLightPos[1]   = y; uLightPos[2]   = z }
            "uViewPos"    -> { uViewPos[0]     = x; uViewPos[1]     = y; uViewPos[2]     = z }
            "uLightColor" -> { uLightColor[0]  = x; uLightColor[1]  = y; uLightColor[2]  = z }
        }
    }

    override fun glUniform2f(location: Int, x: Float, y: Float) {
        when (locNames.getOrNull(location)) {
            "uCenter"     -> { uCenter[0]     = x; uCenter[1]     = y }
            "uScale"      -> { uScale[0]      = x; uScale[1]      = y }
            "uSmoothStep" -> { uSmoothStep[0] = x; uSmoothStep[1] = y }
        }
    }

    override fun glUniform1f(location: Int, x: Float) {
        when (locNames.getOrNull(location)) {
            "uMaterialGloss" -> uGloss = x
            "uAlpha"         -> uAlpha = x
        }
    }

    // ── GlApi — buffers ───────────────────────────────────────────────────────

    override fun glGenBuffers(n: Int): IntArray = IntArray(n) { newHandle() }

    override fun glBindBuffer(target: Int, buffer: Int) {
        when (target) {
            GlApi.GL_ARRAY_BUFFER         -> boundArrayBuf   = buffer
            GlApi.GL_ELEMENT_ARRAY_BUFFER -> boundElementBuf = buffer
        }
    }

    override fun glBufferData(target: Int, data: FloatArray, usage: Int) {
        val id = if (target == GlApi.GL_ARRAY_BUFFER) boundArrayBuf else boundElementBuf
        mtlBuffers[id] = data.usePinned { pinned ->
            device.newBufferWithBytes(pinned.addressOf(0), (data.size * 4).toULong(), MTLResourceStorageModeShared)!!
        }
    }

    override fun glBufferData(target: Int, data: ShortArray, usage: Int) {
        val id = if (target == GlApi.GL_ELEMENT_ARRAY_BUFFER) boundElementBuf else boundArrayBuf
        mtlBuffers[id] = data.usePinned { pinned ->
            device.newBufferWithBytes(pinned.addressOf(0), (data.size * 2).toULong(), MTLResourceStorageModeShared)!!
        }
    }

    override fun glDeleteBuffers(buffers: IntArray) = buffers.forEach { mtlBuffers.remove(it) }

    // ── GlApi — vertex attributes ─────────────────────────────────────────────

    // Issue 3: enable/disable/pointer all write into primitive arrays — no heap allocation.
    override fun glEnableVertexAttribArray(index: Int)  { attribActive[index] = true  }
    override fun glDisableVertexAttribArray(index: Int) { attribActive[index] = false }

    override fun glVertexAttribPointer(index: Int, size: Int, type: Int, normalized: Boolean, stride: Int, offset: Int) {
        attribBufHandles[index] = boundArrayBuf
        attribBufOffsets[index] = offset
    }

    // ── GlApi — drawing ───────────────────────────────────────────────────────

    override fun glDrawElements(mode: Int, count: Int, type: Int, offset: Int) {
        val enc      = encoder ?: return
        val isCuboid = programOrder.getOrNull(0) == currentProgram

        enc.setRenderPipelineState(if (isCuboid) cuboidPipeline else shadowPipeline)
        enc.setDepthStencilState(if (depthTestEnabled) depthOnState else depthOffState)
        // Issue 4: setFrontFacingWinding moved to beginFrame — not repeated here.
        enc.setCullMode(if (isCuboid) MTLCullModeBack else MTLCullModeNone)

        // Issue 3: iterate primitive BooleanArray — no boxing, no HashMap entry allocation.
        for (i in attribActive.indices) {
            if (!attribActive[i]) continue
            val mtlBuf = mtlBuffers[attribBufHandles[i]] ?: continue
            enc.setVertexBuffer(mtlBuf, attribBufOffsets[i].toULong(), i.toULong())
        }

        // Uniforms at vertex/fragment buffer slot 3
        flushMainUniforms()
        enc.setVertexBuffer(mainUniformBuf,   0uL, 3uL)
        enc.setFragmentBuffer(mainUniformBuf, 0uL, 3uL)

        // Texture + sampler
        mtlTextures[boundTex]?.let { enc.setFragmentTexture(it, 0uL) }
        enc.setFragmentSamplerState(linearSampler, 0uL)

        val indexBuf = mtlBuffers[boundElementBuf] ?: return
        enc.drawIndexedPrimitives(
            primitiveType     = MTLPrimitiveTypeTriangle,
            indexCount        = count.toULong(),
            indexType         = MTLIndexTypeUInt16,
            indexBuffer       = indexBuf,
            indexBufferOffset = offset.toULong(),
        )
    }

    override fun glDrawArrays(mode: Int, first: Int, count: Int) {
        val enc = encoder ?: return

        enc.setRenderPipelineState(shadowPipeline)
        enc.setDepthStencilState(depthOffState)
        enc.setCullMode(MTLCullModeNone)

        // Issue 3: iterate primitive BooleanArray — no boxing, no HashMap entry allocation.
        for (i in attribActive.indices) {
            if (!attribActive[i]) continue
            val mtlBuf = mtlBuffers[attribBufHandles[i]] ?: continue
            enc.setVertexBuffer(mtlBuf, attribBufOffsets[i].toULong(), i.toULong())
        }

        // Shadow uniforms at vertex/fragment buffer slot 1
        flushShadowUniforms()
        enc.setVertexBuffer(shadowUniformBuf,   0uL, 1uL)
        enc.setFragmentBuffer(shadowUniformBuf, 0uL, 1uL)

        enc.drawPrimitives(
            primitiveType = MTLPrimitiveTypeTriangleStrip,
            vertexStart   = first.toULong(),
            vertexCount   = count.toULong(),
        )
    }

    // ── GlApi — render state ──────────────────────────────────────────────────

    override fun glEnable(cap: Int)  { if (cap == GlApi.GL_DEPTH_TEST) depthTestEnabled = true  }
    override fun glDisable(cap: Int) { if (cap == GlApi.GL_DEPTH_TEST) depthTestEnabled = false }
    override fun glBlendFunc(sfactor: Int, dfactor: Int) = Unit  // baked into pipeline states
    override fun glCullFace(mode: Int) = Unit                    // selected per draw call

    // ── GlApi — frame ─────────────────────────────────────────────────────────

    override fun glClearColor(r: Float, g: Float, b: Float, a: Float) {
        clearColorR = r.toDouble(); clearColorG = g.toDouble()
        clearColorB = b.toDouble(); clearColorA = a.toDouble()
    }

    override fun glViewport(x: Int, y: Int, width: Int, height: Int) {
        vpX = x; vpY = y; vpW = width; vpH = height
        encoder?.setViewport(cValue {
            originX = x.toDouble(); originY = y.toDouble()
            this.width = width.toDouble(); this.height = height.toDouble()
            znear = 0.0; zfar = 1.0
        })
    }

    // glClear is a no-op: the render-pass load action (configured by the surface via
    // MTKView.clearColor + MTLLoadActionClear) performs the framebuffer clear.
    override fun glClear(mask: Int) = Unit

    // ── GlApi — info ─────────────────────────────────────────────────────────

    override fun glGetString(name: Int): String = if (name == GlApi.GL_VERSION) "Metal" else ""
    override fun isGlEs(): Boolean = true   // selects the #version 300 es shader preamble

    // ── Uniform flush ─────────────────────────────────────────────────────────

    private fun flushMainUniforms() {
        // Issue 1: write into pre-allocated mainUniformData — no FloatArray allocated per frame.
        // Pack into the MainUniforms struct layout (44 floats / 176 bytes):
        //   [0..15]  uMVP
        //   [16..31] uModel
        //   [32..34] uLightPos,  [35] _pad
        //   [36..38] uViewPos,   [39] _pad
        //   [40..42] uLightColor [43] uMaterialGloss
        uMVP.copyInto(mainUniformData, 0)
        uModel.copyInto(mainUniformData, 16)
        uLightPos.copyInto(mainUniformData, 32);   mainUniformData[35] = 0f
        uViewPos.copyInto(mainUniformData, 36);    mainUniformData[39] = 0f
        uLightColor.copyInto(mainUniformData, 40); mainUniformData[43] = uGloss
        mainUniformData.usePinned { pinned ->
            memcpy(mainUniformBuf.contents(), pinned.addressOf(0), (MAIN_UNIFORM_FLOATS * 4).toULong())
        }
    }

    private fun flushShadowUniforms() {
        // Issue 1: write into pre-allocated shadowUniformData — no FloatArray allocated per frame.
        // Pack into the ShadowUniforms struct layout (8 floats / 32 bytes):
        //   [0..1] uCenter, [2..3] uScale, [4] uAlpha, [5] _pad, [6..7] uSmoothStep
        uCenter.copyInto(shadowUniformData, 0)
        uScale.copyInto(shadowUniformData, 2)
        shadowUniformData[4] = uAlpha
        shadowUniformData[5] = 0f
        uSmoothStep.copyInto(shadowUniformData, 6)
        shadowUniformData.usePinned { pinned ->
            memcpy(shadowUniformBuf.contents(), pinned.addressOf(0), (SHADOW_UNIFORM_FLOATS * 4).toULong())
        }
    }
}
