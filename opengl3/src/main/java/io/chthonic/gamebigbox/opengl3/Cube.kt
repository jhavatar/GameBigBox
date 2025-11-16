package io.chthonic.gamebigbox.opengl3

import android.graphics.Bitmap
import android.opengl.GLES30
import android.opengl.GLUtils
import android.opengl.Matrix
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.ShortBuffer

/**
 * OpenGL ES 3.0 cube renderer — six independent textures (front/back/left/right/top/bottom).
 */
class Cube(
    bitmaps: List<Bitmap>,
    onUploaded: (() -> Unit)? = null
) {

    private val vertexBuffer: FloatBuffer
    private val texBuffer: FloatBuffer
    private val indexBuffer: ShortBuffer
    private val textures = IntArray(6)
    private val program: Int

    // 6 faces = 2 triangles × 3 vertices
    private val faces = arrayOf(
        shortArrayOf(0, 2, 1, 1, 2, 3), // front  ✅ reversed for CCW
        shortArrayOf(5, 7, 4, 4, 7, 6), // back   ✅ reversed
        shortArrayOf(4, 6, 0, 0, 6, 2), // left
        shortArrayOf(1, 3, 5, 5, 3, 7), // right
        shortArrayOf(4, 0, 5, 5, 0, 1), // top
        shortArrayOf(2, 6, 3, 3, 6, 7)  // bottom
    )

    init {
        // 24 vertices (4 per face)
        val vertices = floatArrayOf(
            // Front (+Z)
            -1f, 1f, 1f, 1f, 1f, 1f, -1f, -1f, 1f, 1f, -1f, 1f,
            // Back (−Z)
            1f, 1f, -1f, -1f, 1f, -1f, 1f, -1f, -1f, -1f, -1f, -1f,
            // Left (−X)
            -1f, 1f, -1f, -1f, 1f, 1f, -1f, -1f, -1f, -1f, -1f, 1f,
            // Right (+X)
            1f, 1f, 1f, 1f, 1f, -1f, 1f, -1f, 1f, 1f, -1f, -1f,
            // Top (+Y)
            -1f, 1f, -1f, 1f, 1f, -1f, -1f, 1f, 1f, 1f, 1f, 1f,
            // Bottom (−Y)
            -1f, -1f, 1f, 1f, -1f, 1f, -1f, -1f, -1f, 1f, -1f, -1f
        )

        // 24 UVs (4 per face)
        val texCoords = floatArrayOf(
            // Front
            0f, 0f, 1f, 0f, 0f, 1f, 1f, 1f,
            // Back
            1f, 0f, 0f, 0f, 1f, 1f, 0f, 1f,
            // Left
            0f, 0f, 1f, 0f, 0f, 1f, 1f, 1f,
            // Right
            0f, 0f, 1f, 0f, 0f, 1f, 1f, 1f,
            // Top
            0f, 1f, 1f, 1f, 0f, 0f, 1f, 0f,
            // Bottom
            0f, 0f, 1f, 0f, 0f, 1f, 1f, 1f
        )

        // 36 indices (6 faces × 2 triangles × 3 vertices)
        val indices = shortArrayOf(
            0, 2, 1, 1, 2, 3,      // Front
            4, 6, 5, 5, 6, 7,      // Back
            8, 10, 9, 9, 10, 11,      // Left
            12, 14, 13, 13, 14, 15,      // Right
            16, 18, 17, 17, 18, 19,      // Top
            20, 22, 21, 21, 22, 23       // Bottom
        )

        vertexBuffer = vertices.toFloatBuffer()
        texBuffer = texCoords.toFloatBuffer()
        indexBuffer = indices.toShortBuffer()

        // --- GLSL ES 3.00 shaders ---
        val vShader = """
            #version 300 es
            uniform mat4 uMVP;
            layout(location = 0) in vec4 aPos;
            layout(location = 1) in vec2 aTex;
            out vec2 vTex;
            void main() {
                gl_Position = uMVP * aPos;
                vTex = aTex;
            }
        """.trimIndent()

        val fShader = """
            #version 300 es
            precision mediump float;
            uniform sampler2D uTex;
            in vec2 vTex;
            out vec4 fragColor;
            void main() {
                fragColor = texture(uTex, vTex);
            }
        """.trimIndent()

        program = createProgram(vShader, fShader)

        // --- Upload 6 Textures ---
        GLES30.glGenTextures(6, textures, 0)
        for (i in 0 until 6) {
            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textures[i])
            GLES30.glTexParameteri(
                GLES30.GL_TEXTURE_2D,
                GLES30.GL_TEXTURE_MIN_FILTER,
                GLES30.GL_LINEAR
            )
            GLES30.glTexParameteri(
                GLES30.GL_TEXTURE_2D,
                GLES30.GL_TEXTURE_MAG_FILTER,
                GLES30.GL_LINEAR
            )
            GLUtils.texImage2D(GLES30.GL_TEXTURE_2D, 0, bitmaps[i], 0)
            if (!bitmaps[i].isRecycled) bitmaps[i].recycle()
        }

        onUploaded?.invoke()
    }

    fun draw(vp: FloatArray, rotX: Float, rotY: Float) {
        GLES30.glUseProgram(program)

        // Attribute locations (bound by layout qualifiers)
        GLES30.glEnableVertexAttribArray(0)
        GLES30.glEnableVertexAttribArray(1)
        GLES30.glVertexAttribPointer(0, 3, GLES30.GL_FLOAT, false, 0, vertexBuffer)
        GLES30.glVertexAttribPointer(1, 2, GLES30.GL_FLOAT, false, 0, texBuffer)

        val mvpHandle = GLES30.glGetUniformLocation(program, "uMVP")

        val model = FloatArray(16)
        val mvp = FloatArray(16)
        Matrix.setIdentityM(model, 0)
        Matrix.rotateM(model, 0, rotX, 1f, 0f, 0f)
        Matrix.rotateM(model, 0, rotY, 0f, 1f, 0f)

        // Draw each face (6 textures)
        for (i in 0 until 6) {
            Matrix.multiplyMM(mvp, 0, vp, 0, model, 0)
            GLES30.glUniformMatrix4fv(mvpHandle, 1, false, mvp, 0)
            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textures[i])
            indexBuffer.position(i * 6)
            GLES30.glDrawElements(
                GLES30.GL_TRIANGLES,
                6,
                GLES30.GL_UNSIGNED_SHORT,
                indexBuffer
            )
        }

        GLES30.glDisableVertexAttribArray(0)
        GLES30.glDisableVertexAttribArray(1)
    }

    private fun createProgram(vs: String, fs: String): Int {
        fun compile(type: Int, code: String): Int {
            val shader = GLES30.glCreateShader(type)
            GLES30.glShaderSource(shader, code)
            GLES30.glCompileShader(shader)
            val status = IntArray(1)
            GLES30.glGetShaderiv(shader, GLES30.GL_COMPILE_STATUS, status, 0)
            if (status[0] == 0) {
                val log = GLES30.glGetShaderInfoLog(shader)
                GLES30.glDeleteShader(shader)
                throw RuntimeException("Shader compile error: $log")
            }
            return shader
        }

        val vsId = compile(GLES30.GL_VERTEX_SHADER, vs)
        val fsId = compile(GLES30.GL_FRAGMENT_SHADER, fs)
        val prog = GLES30.glCreateProgram()
        GLES30.glAttachShader(prog, vsId)
        GLES30.glAttachShader(prog, fsId)
        GLES30.glLinkProgram(prog)
        val linkStatus = IntArray(1)
        GLES30.glGetProgramiv(prog, GLES30.GL_LINK_STATUS, linkStatus, 0)
        if (linkStatus[0] == 0) {
            val log = GLES30.glGetProgramInfoLog(prog)
            GLES30.glDeleteProgram(prog)
            throw RuntimeException("Program link error: $log")
        }
        return prog
    }

    private fun FloatArray.toFloatBuffer(): FloatBuffer =
        ByteBuffer.allocateDirect(size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .apply { put(this@toFloatBuffer); position(0) }

    private fun ShortArray.toShortBuffer(): ShortBuffer =
        ByteBuffer.allocateDirect(size * 2)
            .order(ByteOrder.nativeOrder())
            .asShortBuffer()
            .apply { put(this@toShortBuffer); position(0) }
}