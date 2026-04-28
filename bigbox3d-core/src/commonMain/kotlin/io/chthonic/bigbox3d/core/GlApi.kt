package io.chthonic.bigbox3d.core

import java.nio.FloatBuffer
import java.nio.ShortBuffer

/**
 * Platform-agnostic subset of OpenGL ES 3.0 / WebGL2 used by bigbox3d-core.
 *
 * Note: FloatBuffer/ShortBuffer are JVM types and will need platform abstraction
 * when jsMain/wasmMain targets are added.
 */
interface GlApi {

    // --- programs & shaders ---
    fun glCreateShader(type: Int): Int
    fun glShaderSource(shader: Int, source: String)
    fun glCompileShader(shader: Int)
    fun glGetShaderiv(shader: Int, pname: Int): Int
    fun glGetShaderInfoLog(shader: Int): String
    fun glDeleteShader(shader: Int)
    fun glCreateProgram(): Int
    fun glAttachShader(program: Int, shader: Int)
    fun glLinkProgram(program: Int)
    fun glGetProgramiv(program: Int, pname: Int): Int
    fun glGetProgramInfoLog(program: Int): String
    fun glDeleteProgram(program: Int)
    fun glUseProgram(program: Int)

    // --- textures ---
    fun glGenTextures(n: Int): IntArray
    fun glBindTexture(target: Int, texture: Int)
    fun glTexParameteri(target: Int, pname: Int, param: Int)
    /** Upload RGBA8888 pixel data. Assumes GL_TEXTURE_2D is already bound. */
    fun glTexImage2D(width: Int, height: Int, pixels: ByteArray)
    fun glDeleteTextures(textures: IntArray)

    // --- uniforms ---
    fun glGetUniformLocation(program: Int, name: String): Int
    fun glUniformMatrix4fv(location: Int, count: Int, transpose: Boolean, value: FloatArray, offset: Int)
    fun glUniform3f(location: Int, x: Float, y: Float, z: Float)
    fun glUniform2f(location: Int, x: Float, y: Float)
    fun glUniform1f(location: Int, x: Float)

    // --- vertex attributes ---
    fun glEnableVertexAttribArray(index: Int)
    fun glDisableVertexAttribArray(index: Int)
    fun glVertexAttribPointer(index: Int, size: Int, type: Int, normalized: Boolean, stride: Int, buffer: FloatBuffer)

    // --- drawing ---
    fun glDrawElements(mode: Int, count: Int, type: Int, indices: ShortBuffer)
    fun glDrawArrays(mode: Int, first: Int, count: Int)

    // --- render state ---
    fun glEnable(cap: Int)
    fun glDisable(cap: Int)
    fun glBlendFunc(sfactor: Int, dfactor: Int)
    fun glCullFace(mode: Int)

    // --- frame ---
    fun glClearColor(r: Float, g: Float, b: Float, a: Float)
    fun glViewport(x: Int, y: Int, width: Int, height: Int)
    fun glClear(mask: Int)

    // --- info ---
    fun glGetString(name: Int): String

    companion object {
        // Texture
        const val GL_TEXTURE_2D         = 0x0DE1
        const val GL_TEXTURE_MIN_FILTER = 0x2801
        const val GL_TEXTURE_MAG_FILTER = 0x2800
        const val GL_LINEAR             = 0x2601

        // Pixel formats
        const val GL_RGBA               = 0x1908
        const val GL_UNSIGNED_BYTE      = 0x1401

        // Shaders
        const val GL_VERTEX_SHADER      = 0x8B31
        const val GL_FRAGMENT_SHADER    = 0x8B30
        const val GL_COMPILE_STATUS     = 0x8B81
        const val GL_LINK_STATUS        = 0x8B82

        // Types
        const val GL_FLOAT              = 0x1406
        const val GL_UNSIGNED_SHORT     = 0x1403

        // Draw modes
        const val GL_TRIANGLES          = 0x0004
        const val GL_TRIANGLE_STRIP     = 0x0005

        // Capabilities
        const val GL_DEPTH_TEST         = 0x0B71
        const val GL_BLEND              = 0x0BE2
        const val GL_CULL_FACE          = 0x0B44

        // Cull / winding
        const val GL_BACK               = 0x0405

        // Blend factors
        const val GL_SRC_ALPHA          = 0x0302
        const val GL_ONE_MINUS_SRC_ALPHA = 0x0303

        // Clear bits
        const val GL_COLOR_BUFFER_BIT   = 0x4000
        const val GL_DEPTH_BUFFER_BIT   = 0x0100

        // Info
        const val GL_VERSION            = 0x1F02
    }
}
