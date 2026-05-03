package io.chthonic.bigbox3d.core

import io.chthonic.bigbox3d.core.GlApi.Companion.GL_ARRAY_BUFFER
import io.chthonic.bigbox3d.core.GlApi.Companion.GL_BACK
import io.chthonic.bigbox3d.core.GlApi.Companion.GL_BLEND
import io.chthonic.bigbox3d.core.GlApi.Companion.GL_COMPILE_STATUS
import io.chthonic.bigbox3d.core.GlApi.Companion.GL_DEPTH_TEST
import io.chthonic.bigbox3d.core.GlApi.Companion.GL_ELEMENT_ARRAY_BUFFER
import io.chthonic.bigbox3d.core.GlApi.Companion.GL_FLOAT
import io.chthonic.bigbox3d.core.GlApi.Companion.GL_FRAGMENT_SHADER
import io.chthonic.bigbox3d.core.GlApi.Companion.GL_LINEAR
import io.chthonic.bigbox3d.core.GlApi.Companion.GL_LINK_STATUS
import io.chthonic.bigbox3d.core.GlApi.Companion.GL_ONE_MINUS_SRC_ALPHA
import io.chthonic.bigbox3d.core.GlApi.Companion.GL_SRC_ALPHA
import io.chthonic.bigbox3d.core.GlApi.Companion.GL_STATIC_DRAW
import io.chthonic.bigbox3d.core.GlApi.Companion.GL_TEXTURE_2D
import io.chthonic.bigbox3d.core.GlApi.Companion.GL_TEXTURE_MAG_FILTER
import io.chthonic.bigbox3d.core.GlApi.Companion.GL_TEXTURE_MIN_FILTER
import io.chthonic.bigbox3d.core.GlApi.Companion.GL_TRIANGLE_STRIP
import io.chthonic.bigbox3d.core.GlApi.Companion.GL_TRIANGLES
import io.chthonic.bigbox3d.core.GlApi.Companion.GL_UNSIGNED_SHORT
import io.chthonic.bigbox3d.core.GlApi.Companion.GL_VERTEX_SHADER
import io.chthonic.bigbox3d.core.RegionFace.BACK
import io.chthonic.bigbox3d.core.RegionFace.BOTTOM
import io.chthonic.bigbox3d.core.RegionFace.FRONT
import io.chthonic.bigbox3d.core.RegionFace.LEFT
import io.chthonic.bigbox3d.core.RegionFace.RIGHT
import io.chthonic.bigbox3d.core.RegionFace.TOP

internal class Cuboid(
    gl: GlApi,
    atlas: BoxTextureAtlas,
    onTexturesUploaded: (() -> Unit)? = null,
) {
    private val halfW = atlas.halfWidth
    private val halfH = atlas.halfHeight
    private val halfD = atlas.halfDepth

    // VBO IDs: [vertex, texCoord, normal, index, shadowVertex]
    private val vboIds: IntArray

    private val textureId: Int
    private val program: Int
    private val shadowProgram: Int

    init {
        val vertices = floatArrayOf(
            -halfW,  halfH,  halfD,   halfW,  halfH,  halfD,  -halfW, -halfH,  halfD,   halfW, -halfH,  halfD, // Front
             halfW,  halfH, -halfD,  -halfW,  halfH, -halfD,   halfW, -halfH, -halfD,  -halfW, -halfH, -halfD, // Back
            -halfW,  halfH, -halfD,  -halfW,  halfH,  halfD,  -halfW, -halfH, -halfD,  -halfW, -halfH,  halfD, // Left
             halfW,  halfH,  halfD,   halfW,  halfH, -halfD,   halfW, -halfH,  halfD,   halfW, -halfH, -halfD, // Right
            -halfW,  halfH, -halfD,   halfW,  halfH, -halfD,  -halfW,  halfH,  halfD,   halfW,  halfH,  halfD, // Top
            -halfW, -halfH,  halfD,   halfW, -halfH,  halfD,  -halfW, -halfH, -halfD,   halfW, -halfH, -halfD, // Bottom
        )

        val texCoords = FloatArray(4 * 2 * 6)
        listOf(FRONT, BACK, LEFT, RIGHT, TOP, BOTTOM).forEachIndexed { i, face ->
            val r = atlas.regions[face] ?: AtlasRegion(0f, 0f, 1f, 1f)
            val base = i * 8
            texCoords[base + 0] = r.u0; texCoords[base + 1] = r.v0
            texCoords[base + 2] = r.u1; texCoords[base + 3] = r.v0
            texCoords[base + 4] = r.u0; texCoords[base + 5] = r.v1
            texCoords[base + 6] = r.u1; texCoords[base + 7] = r.v1
        }

        val normals = floatArrayOf(
            0f,0f,1f, 0f,0f,1f, 0f,0f,1f, 0f,0f,1f,
            0f,0f,-1f, 0f,0f,-1f, 0f,0f,-1f, 0f,0f,-1f,
            -1f,0f,0f, -1f,0f,0f, -1f,0f,0f, -1f,0f,0f,
            1f,0f,0f, 1f,0f,0f, 1f,0f,0f, 1f,0f,0f,
            0f,1f,0f, 0f,1f,0f, 0f,1f,0f, 0f,1f,0f,
            0f,-1f,0f, 0f,-1f,0f, 0f,-1f,0f, 0f,-1f,0f,
        )

        val indices = shortArrayOf(
            0,2,1, 1,2,3,    4,6,5, 5,6,7,    8,10,9, 9,10,11,
            12,14,13, 13,14,15,   16,18,17, 17,18,19,   20,22,21, 21,22,23,
        )

        vboIds = gl.glGenBuffers(5)

        gl.glBindBuffer(GL_ARRAY_BUFFER, vboIds[0])
        gl.glBufferData(GL_ARRAY_BUFFER, vertices, GL_STATIC_DRAW)

        gl.glBindBuffer(GL_ARRAY_BUFFER, vboIds[1])
        gl.glBufferData(GL_ARRAY_BUFFER, texCoords, GL_STATIC_DRAW)

        gl.glBindBuffer(GL_ARRAY_BUFFER, vboIds[2])
        gl.glBufferData(GL_ARRAY_BUFFER, normals, GL_STATIC_DRAW)

        gl.glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, vboIds[3])
        gl.glBufferData(GL_ELEMENT_ARRAY_BUFFER, indices, GL_STATIC_DRAW)

        gl.glBindBuffer(GL_ARRAY_BUFFER, vboIds[4])
        gl.glBufferData(GL_ARRAY_BUFFER, floatArrayOf(-1f,-1f, 1f,-1f, -1f,1f, 1f,1f), GL_STATIC_DRAW)

        // Desktop OpenGL uses GLSL 330 core; OpenGL ES / WebGL uses GLSL 300 es.
        // "ES" appears in Android ("OpenGL ES 3.x") and WebGL ("WebGL 2.0 …ES…") version strings.
        val isEs = gl.glGetString(GlApi.GL_VERSION).let {
            it.contains("OpenGL ES", ignoreCase = true) || it.startsWith("WebGL", ignoreCase = true)
        }
        val ver       = if (isEs) "#version 300 es"                        else "#version 330 core"
        val verPrec   = if (isEs) "#version 300 es\nprecision mediump float;" else "#version 330 core"

        val vShader = """
            $ver
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
            $verPrec
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
                float shininess = mix(8.0, 128.0, uMaterialGloss);
                float specPower = mix(0.05, 1.0, uMaterialGloss);
                float spec = pow(max(dot(viewDir, reflectDir), 0.0), shininess) * specPower;
                vec3 result = texColor * (0.4 + 0.6 * diff) + uLightColor * spec;
                fragColor = vec4(result, 1.0);
            }
        """.trimIndent()

        val shadowVert = """
            $ver
            layout(location = 0) in vec2 aPos;
            out vec2 vPos;
            void main() { vPos = aPos; gl_Position = vec4(aPos, 0.0, 1.0); }
        """.trimIndent()

        val shadowFrag = """
            $verPrec
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
                fragColor = vec4(0.0, 0.0, 0.0, (1.0 - fade) * uAlpha);
            }
        """.trimIndent()

        program       = gl.createProgram(vShader, fShader)
        shadowProgram = gl.createProgram(shadowVert, shadowFrag)

        val texIds = gl.glGenTextures(1)
        textureId = texIds[0]
        gl.glBindTexture(GL_TEXTURE_2D, textureId)
        gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR)
        gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR)
        gl.glTexImage2D(atlas.image.width, atlas.image.height, atlas.image.pixels)
        onTexturesUploaded?.invoke()
    }

    fun drawProjectedShadow(
        gl: GlApi,
        vp: FloatArray,
        rotX: Float,
        rotY: Float,
        shadowOpacity: Float,
        shadowFadeStartRatio: Float,
        shadowFadeEndRatio: Float,
        xOffsetRatio: Float,
        yOffsetRatio: Float,
    ) {
        val model = FloatArray(16).also {
            Matrix4.setIdentityM(it, 0)
            Matrix4.rotateM(it, 0, rotX, 1f, 0f, 0f)
            Matrix4.rotateM(it, 0, rotY, 0f, 1f, 0f)
        }
        val mvp = FloatArray(16).also { Matrix4.multiplyMM(it, 0, vp, 0, model, 0) }

        val corners = arrayOf(
            floatArrayOf(-halfW, -halfH, -halfD, 1f), floatArrayOf(halfW, -halfH, -halfD, 1f),
            floatArrayOf(-halfW,  halfH, -halfD, 1f), floatArrayOf(halfW,  halfH, -halfD, 1f),
            floatArrayOf(-halfW, -halfH,  halfD, 1f), floatArrayOf(halfW, -halfH,  halfD, 1f),
            floatArrayOf(-halfW,  halfH,  halfD, 1f), floatArrayOf(halfW,  halfH,  halfD, 1f),
        )
        var minX = 1f; var maxX = -1f; var minY = 1f; var maxY = -1f
        val tmp = FloatArray(4)
        for (v in corners) {
            Matrix4.multiplyMV(tmp, 0, mvp, 0, v, 0)
            val nx = tmp[0] / tmp[3]; val ny = tmp[1] / tmp[3]
            minX = minOf(minX, nx); maxX = maxOf(maxX, nx)
            minY = minOf(minY, ny); maxY = maxOf(maxY, ny)
        }

        val cx = (minX + maxX) * 0.5f + xOffsetRatio
        val cy = (minY + maxY) * 0.5f + yOffsetRatio
        val sx = maxX - minX
        val sy = maxY - minY

        gl.glUseProgram(shadowProgram)
        gl.glDisable(GL_DEPTH_TEST)
        gl.glEnable(GL_BLEND)
        gl.glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)

        gl.glUniform2f(gl.glGetUniformLocation(shadowProgram, "uCenter"), cx, cy)
        gl.glUniform2f(gl.glGetUniformLocation(shadowProgram, "uScale"), sx, sy)
        gl.glUniform1f(gl.glGetUniformLocation(shadowProgram, "uAlpha"), shadowOpacity.coerceIn(0f, 1f))
        gl.glUniform2f(gl.glGetUniformLocation(shadowProgram, "uSmoothStep"), shadowFadeStartRatio, shadowFadeEndRatio)

        gl.glEnableVertexAttribArray(0)
        gl.glBindBuffer(GL_ARRAY_BUFFER, vboIds[4])
        gl.glVertexAttribPointer(0, 2, GL_FLOAT, false, 0, 0)
        gl.glDrawArrays(GL_TRIANGLE_STRIP, 0, 4)
        gl.glDisableVertexAttribArray(0)

        gl.glDisable(GL_BLEND)
        gl.glEnable(GL_DEPTH_TEST)
    }

    fun draw(gl: GlApi, vp: FloatArray, rotX: Float, rotY: Float, cameraZ: Float, gloss: Float) {
        gl.glUseProgram(program)
        gl.glEnableVertexAttribArray(0)
        gl.glEnableVertexAttribArray(1)
        gl.glEnableVertexAttribArray(2)

        gl.glBindBuffer(GL_ARRAY_BUFFER, vboIds[0])
        gl.glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0)

        gl.glBindBuffer(GL_ARRAY_BUFFER, vboIds[1])
        gl.glVertexAttribPointer(1, 2, GL_FLOAT, false, 0, 0)

        gl.glBindBuffer(GL_ARRAY_BUFFER, vboIds[2])
        gl.glVertexAttribPointer(2, 3, GL_FLOAT, false, 0, 0)

        val model = FloatArray(16).also {
            Matrix4.setIdentityM(it, 0)
            Matrix4.rotateM(it, 0, rotX, 1f, 0f, 0f)
            Matrix4.rotateM(it, 0, rotY, 0f, 1f, 0f)
        }
        val mvp = FloatArray(16).also { Matrix4.multiplyMM(it, 0, vp, 0, model, 0) }

        gl.glUniformMatrix4fv(gl.glGetUniformLocation(program, "uMVP"),   1, false, mvp,   0)
        gl.glUniformMatrix4fv(gl.glGetUniformLocation(program, "uModel"), 1, false, model, 0)
        gl.glUniform3f(gl.glGetUniformLocation(program, "uLightPos"),   0f, 0f, cameraZ * 1.6f)
        gl.glUniform3f(gl.glGetUniformLocation(program, "uViewPos"),    0f, 0f, cameraZ)
        gl.glUniform3f(gl.glGetUniformLocation(program, "uLightColor"), 1f, 1f, 1f)
        gl.glUniform1f(gl.glGetUniformLocation(program, "uMaterialGloss"), gloss.coerceIn(0f, 1f))

        gl.glBindTexture(GL_TEXTURE_2D, textureId)
        gl.glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, vboIds[3])
        gl.glDrawElements(GL_TRIANGLES, 36, GL_UNSIGNED_SHORT, 0)

        gl.glDisableVertexAttribArray(0)
        gl.glDisableVertexAttribArray(1)
        gl.glDisableVertexAttribArray(2)
    }

    fun release(gl: GlApi) {
        gl.glDeleteTextures(intArrayOf(textureId))
        gl.glDeleteProgram(program)
        gl.glDeleteProgram(shadowProgram)
        gl.glDeleteBuffers(vboIds)
    }
}

private fun GlApi.createProgram(vs: String, fs: String): Int {
    fun compile(type: Int, code: String): Int {
        val shader = glCreateShader(type)
        glShaderSource(shader, code)
        glCompileShader(shader)
        if (glGetShaderiv(shader, GL_COMPILE_STATUS) == 0) {
            val log = glGetShaderInfoLog(shader)
            glDeleteShader(shader)
            throw RuntimeException("Shader compile error: $log")
        }
        return shader
    }
    val vsId = compile(GL_VERTEX_SHADER, vs)
    val fsId = compile(GL_FRAGMENT_SHADER, fs)
    val prog = glCreateProgram()
    glAttachShader(prog, vsId)
    glAttachShader(prog, fsId)
    glLinkProgram(prog)
    if (glGetProgramiv(prog, GL_LINK_STATUS) == 0) {
        val log = glGetProgramInfoLog(prog)
        glDeleteProgram(prog)
        throw RuntimeException("Program link error: $log")
    }
    return prog
}
