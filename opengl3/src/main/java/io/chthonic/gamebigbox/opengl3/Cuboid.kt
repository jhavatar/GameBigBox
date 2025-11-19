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
 * OpenGL ES 3.0 cuboid renderer — six independent textures (front/back/left/right/top/bottom).
 * @param halfW half of the dimension of the width. for a cube it is 1f
 * @param halfH half of the dimension of the height. for a cube it is 1f
 * @param halfD half of the dimension of the depth. for a cube it is 1f
 * @param onUploaded callback after textures uploaded to GPU memory
 */
internal class Cuboid(
    bitmaps: List<Bitmap>,
    val halfW: Float = 0.778f,
    val halfH: Float = 1.0f,
    val halfD: Float = 0.222f,
    onUploaded: (() -> Unit)? = null,
) {

    private val vertexBuffer: FloatBuffer
    private val texBuffer: FloatBuffer
    private val normalBuffer: FloatBuffer
    private val indexBuffer: ShortBuffer
    private val textures = IntArray(6)
    private val program: Int

    // 2D shadow
    private val shadowProgram: Int
    private val shadowVertexBuf: FloatBuffer

    init {
        // 24 vertices (4 per face)
        val vertices = floatArrayOf(
            // Front (+Z)
            -halfW, halfH, halfD, halfW, halfH, halfD,
            -halfW, -halfH, halfD, halfW, -halfH, halfD,
            // Back (−Z)
            halfW, halfH, -halfD, -halfW, halfH, -halfD,
            halfW, -halfH, -halfD, -halfW, -halfH, -halfD,
            // Left (−X)
            -halfW, halfH, -halfD, -halfW, halfH, halfD,
            -halfW, -halfH, -halfD, -halfW, -halfH, halfD,
            // Right (+X)
            halfW, halfH, halfD, halfW, halfH, -halfD,
            halfW, -halfH, halfD, halfW, -halfH, -halfD,
            // Top (+Y)
            -halfW, halfH, -halfD, halfW, halfH, -halfD,
            -halfW, halfH, halfD, halfW, halfH, halfD,
            // Bottom (−Y)
            -halfW, -halfH, halfD, halfW, -halfH, halfD,
            -halfW, -halfH, -halfD, halfW, -halfH, -halfD,
        )

        // 24 UVs (4 per face)
        val texCoords = floatArrayOf(
            // Front
            0f, 0f, 1f, 0f, 0f, 1f, 1f, 1f,
            // Back
            0f, 0f, 1f, 0f, 0f, 1f, 1f, 1f,
            // Left
            0f, 0f, 1f, 0f, 0f, 1f, 1f, 1f,
            // Right
            0f, 0f, 1f, 0f, 0f, 1f, 1f, 1f,
            // Top
            0f, 0f, 1f, 0f, 0f, 1f, 1f, 1f,
            // Bottom
            0f, 0f, 1f, 0f, 0f, 1f, 1f, 1f
        )

        // 24 normals (one per vertex)
        val normals = floatArrayOf(
            // Front
            0f, 0f, 1f, 0f, 0f, 1f, 0f, 0f, 1f, 0f, 0f, 1f,
            // Back
            0f, 0f, -1f, 0f, 0f, -1f, 0f, 0f, -1f, 0f, 0f, -1f,
            // Left
            -1f, 0f, 0f, -1f, 0f, 0f, -1f, 0f, 0f, -1f, 0f, 0f,
            // Right
            1f, 0f, 0f, 1f, 0f, 0f, 1f, 0f, 0f, 1f, 0f, 0f,
            // Top
            0f, 1f, 0f, 0f, 1f, 0f, 0f, 1f, 0f, 0f, 1f, 0f,
            // Bottom
            0f, -1f, 0f, 0f, -1f, 0f, 0f, -1f, 0f, 0f, -1f, 0f
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
        normalBuffer = normals.toFloatBuffer()
        indexBuffer = indices.toShortBuffer()

        // Shaders with glossy lighting
        val vShader = """
            #version 300 es
            layout(location = 0) in vec4 aPos;
            layout(location = 1) in vec2 aTex;
            layout(location = 2) in vec3 aNormal;
            uniform mat4 uMVP;
            uniform mat4 uModel;
            out vec2 vTex;
            out vec3 vNormal;
            out vec3 vFragPos;
            void main(){
                gl_Position = uMVP * aPos;
                vTex = aTex;
                vFragPos = vec3(uModel * aPos);
                vNormal = mat3(transpose(inverse(uModel))) * aNormal;
            }
        """.trimIndent()

        val fShader = """
            #version 300 es
            precision mediump float;
            uniform sampler2D uTex;
            uniform vec3 uLightPos;
            uniform vec3 uViewPos;
            uniform vec3 uLightColor;
            uniform float uMaterialGloss;
            in vec2 vTex;
            in vec3 vNormal;
            in vec3 vFragPos;
            out vec4 fragColor;
            void main(){
                vec3 texColor = texture(uTex, vTex).rgb;
                vec3 norm = normalize(vNormal);
                vec3 lightDir = normalize(uLightPos - vFragPos);
                vec3 viewDir = normalize(uViewPos - vFragPos);
                vec3 reflectDir = reflect(-lightDir, norm);
                float diff = max(dot(norm, lightDir), 0.0);
                float shininess = mix(8.0,128.0,uMaterialGloss);
                float specPower = mix(0.05,1.0,uMaterialGloss);
                float spec = pow(max(dot(viewDir, reflectDir), 0.0), shininess) * specPower;
                vec3 result = texColor * (0.4 + 0.6 * diff) + uLightColor * spec;
                fragColor = vec4(result,1.0);
            }
        """.trimIndent()

        program = createProgram(vShader, fShader)

        // screen-space radial shadow shader
        val shadowVert = """
            #version 300 es
            layout(location = 0) in vec2 aPos;
            out vec2 vPos;
            void main() {
                vPos = aPos;
                gl_Position = vec4(aPos, 0.0, 1.0);
            }
        """.trimIndent()

        val shadowFrag = """
            #version 300 es
            precision mediump float;
            in vec2 vPos;
            uniform vec2 uCenter;
            uniform vec2 uScale;
            uniform float uAlpha;
            uniform vec2 uSmoothStep;
            out vec4 fragColor;
            void main(){
                vec2 rel = (vPos - uCenter) / uScale;
                float r = length(rel);
                float fade = smoothstep(uSmoothStep[0], uSmoothStep[1], r);
                fragColor = vec4(0.0,0.0,0.0,(1.0-fade)*uAlpha);
            }
        """.trimIndent()
        shadowProgram = createProgram(shadowVert, shadowFrag)

        val quad = floatArrayOf(-1f, -1f, 1f, -1f, -1f, 1f, 1f, 1f)
        shadowVertexBuf = ByteBuffer.allocateDirect(quad.size * 4)
            .order(ByteOrder.nativeOrder()).asFloatBuffer()
            .apply { put(quad); position(0) }

        // Upload 6 textures to GPU memory
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

    /**
     * Draw soft projected oval shadow behind cube
     */
    fun drawProjectedShadow(
        vp: FloatArray,
        rotX: Float,
        rotY: Float,
        shadowOpacity: Float,
        shadowFadeStartRatio: Float,
        shadowFadeEndRatio: Float,
        xOffsetRatio: Float,
        yOffsetRatio: Float,
    ) {
        val model = FloatArray(16)
        Matrix.setIdentityM(model, 0)
        Matrix.rotateM(model, 0, rotX, 1f, 0f, 0f)
        Matrix.rotateM(model, 0, rotY, 0f, 1f, 0f)
        val mvp = FloatArray(16)
        Matrix.multiplyMM(mvp, 0, vp, 0, model, 0)

        // Project 8 corners
        val corners = arrayOf(
            floatArrayOf(-halfW, -halfH, -halfD, 1f),
            floatArrayOf(halfW, -halfH, -halfD, 1f),
            floatArrayOf(-halfW, halfH, -halfD, 1f),
            floatArrayOf(halfW, halfH, -halfD, 1f),
            floatArrayOf(-halfW, -halfH, halfD, 1f),
            floatArrayOf(halfW, -halfH, halfD, 1f),
            floatArrayOf(-halfW, halfH, halfD, 1f),
            floatArrayOf(halfW, halfH, halfD, 1f)
        )

        var minX = 1f;
        var maxX = -1f
        var minY = 1f;
        var maxY = -1f
        val tmp = FloatArray(4)
        for (v in corners) {
            Matrix.multiplyMV(tmp, 0, mvp, 0, v, 0)
            val ndcX = tmp[0] / tmp[3]
            val ndcY = tmp[1] / tmp[3]
            minX = minOf(minX, ndcX)
            maxX = maxOf(maxX, ndcX)
            minY = minOf(minY, ndcY)
            maxY = maxOf(maxY, ndcY)
        }

        val cx = (minX + maxX) * 0.5f + xOffsetRatio
        val cy = (minY + maxY) * 0.5f + yOffsetRatio
        val sx = (maxX - minX) * 1.0f
        val sy = (maxY - minY) * 1.0f

        GLES30.glUseProgram(shadowProgram)
        GLES30.glDisable(GLES30.GL_DEPTH_TEST)
        GLES30.glEnable(GLES30.GL_BLEND)
        GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE_MINUS_SRC_ALPHA)

        GLES30.glUniform2f(GLES30.glGetUniformLocation(shadowProgram, "uCenter"), cx, cy)
        GLES30.glUniform2f(GLES30.glGetUniformLocation(shadowProgram, "uScale"), sx, sy)
        GLES30.glUniform1f(
            GLES30.glGetUniformLocation(shadowProgram, "uAlpha"),
            shadowOpacity.coerceIn(0f, 1f),
        )
        GLES30.glUniform2f(
            GLES30.glGetUniformLocation(shadowProgram, "uSmoothStep"),
            shadowFadeStartRatio,
            shadowFadeEndRatio,
        )

        GLES30.glEnableVertexAttribArray(0)
        GLES30.glVertexAttribPointer(0, 2, GLES30.GL_FLOAT, false, 0, shadowVertexBuf)
        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4)
        GLES30.glDisableVertexAttribArray(0)

        GLES30.glDisable(GLES30.GL_BLEND)
        GLES30.glEnable(GLES30.GL_DEPTH_TEST)
    }

    /**
     * @param gloss glossiness between 0f and 1f.
     * 0.0f	diffuse, flat -- e.g. matte raw cardboard mid-80s Sierra, SSI
     * 0.3f	faint wide sheen -- e.g. semi-gloss paper
     * 0.6f	soft reflection -- e.g.	laminated mid-90s EA/Interplay boxes
     * 1.0f	tight bright highlight -- e.g. full-gloss Origin / Wing Commander III box
     */
    fun draw(vp: FloatArray, rotX: Float, rotY: Float, gloss: Float) {
        GLES30.glUseProgram(program)
        GLES30.glEnableVertexAttribArray(0)
        GLES30.glEnableVertexAttribArray(1)
        GLES30.glEnableVertexAttribArray(2)
        GLES30.glVertexAttribPointer(0, 3, GLES30.GL_FLOAT, false, 0, vertexBuffer)
        GLES30.glVertexAttribPointer(1, 2, GLES30.GL_FLOAT, false, 0, texBuffer)
        GLES30.glVertexAttribPointer(2, 3, GLES30.GL_FLOAT, false, 0, normalBuffer)

        val uMVP = GLES30.glGetUniformLocation(program, "uMVP")
        val uModel = GLES30.glGetUniformLocation(program, "uModel")
        val uLightPos = GLES30.glGetUniformLocation(program, "uLightPos")
        val uViewPos = GLES30.glGetUniformLocation(program, "uViewPos")
        val uLightColor = GLES30.glGetUniformLocation(program, "uLightColor")
        val uMaterialGloss = GLES30.glGetUniformLocation(program, "uMaterialGloss")

        val model = FloatArray(16)
        val mvp = FloatArray(16)
        Matrix.setIdentityM(model, 0)
        Matrix.rotateM(model, 0, rotX, 1f, 0f, 0f)
        Matrix.rotateM(model, 0, rotY, 0f, 1f, 0f)
        Matrix.multiplyMM(mvp, 0, vp, 0, model, 0)

        GLES30.glUniformMatrix4fv(uMVP, 1, false, mvp, 0)
        GLES30.glUniformMatrix4fv(uModel, 1, false, model, 0)

        // Simple static light and camera
        GLES30.glUniform3f(uLightPos, 3f, 3f, 5f)
        GLES30.glUniform3f(uViewPos, 0f, 0f, 4f)
        GLES30.glUniform3f(uLightColor, 1f, 1f, 1f)
        GLES30.glUniform1f(uMaterialGloss, gloss.coerceIn(0f, 1f))

        // Draw each textured face
        for (i in 0 until 6) {
            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textures[i])
            indexBuffer.position(i * 6)
            GLES30.glDrawElements(GLES30.GL_TRIANGLES, 6, GLES30.GL_UNSIGNED_SHORT, indexBuffer)
        }

        GLES30.glDisableVertexAttribArray(0)
        GLES30.glDisableVertexAttribArray(1)
        GLES30.glDisableVertexAttribArray(2)
    }

    /**
     * Important: This must run after the GLSurfaceView’s context is active,
     * typically inside the renderer’s onSurfaceDestroyed() or from queueEvent { ... } on the GLSurfaceView.
     */
    fun release() {
        // Must be called on the GL thread
        if (textures.isNotEmpty()) {
            GLES30.glDeleteTextures(textures.size, textures, 0)
        }
        GLES30.glDeleteProgram(program)
        GLES30.glDeleteProgram(shadowProgram)
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
            .apply {
                put(this@toFloatBuffer)
                position(0)
            }

    private fun ShortArray.toShortBuffer(): ShortBuffer =
        ByteBuffer.allocateDirect(size * 2)
            .order(ByteOrder.nativeOrder())
            .asShortBuffer()
            .apply {
                put(this@toShortBuffer)
                position(0)
            }
}