package io.chthonic.bigbox3d.core

import org.khronos.webgl.Float32Array
import org.khronos.webgl.Int16Array
import org.khronos.webgl.Uint8Array
import org.khronos.webgl.set

/**
 * Minimal external interface for the methods we call on a WebGL2RenderingContext.
 * We declare our own interface rather than relying on the stdlib's incomplete binding.
 * Callers cast the canvas getContext("webgl2") result to this type.
 */
external interface WebGl2Ctx : JsAny {
    fun createShader(type: Int): JsAny?
    fun shaderSource(shader: JsAny?, source: String)
    fun compileShader(shader: JsAny?)
    fun getShaderParameter(shader: JsAny?, pname: Int): JsAny?
    fun getShaderInfoLog(shader: JsAny?): String?
    fun deleteShader(shader: JsAny?)

    fun createProgram(): JsAny?
    fun attachShader(program: JsAny?, shader: JsAny?)
    fun linkProgram(program: JsAny?)
    fun getProgramParameter(program: JsAny?, pname: Int): JsAny?
    fun getProgramInfoLog(program: JsAny?): String?
    fun deleteProgram(program: JsAny?)
    fun useProgram(program: JsAny?)

    fun createTexture(): JsAny?
    fun bindTexture(target: Int, texture: JsAny?)
    fun texParameteri(target: Int, pname: Int, param: Int)
    fun texImage2D(target: Int, level: Int, internalformat: Int, width: Int, height: Int, border: Int, format: Int, type: Int, pixels: JsAny?)
    fun deleteTexture(texture: JsAny?)

    fun getUniformLocation(program: JsAny?, name: String): JsAny?
    fun uniformMatrix4fv(location: JsAny?, transpose: Boolean, value: JsAny?)
    fun uniform3f(location: JsAny?, x: Float, y: Float, z: Float)
    fun uniform2f(location: JsAny?, x: Float, y: Float)
    fun uniform1f(location: JsAny?, x: Float)

    fun createBuffer(): JsAny?
    fun bindBuffer(target: Int, buffer: JsAny?)
    fun bufferData(target: Int, data: JsAny?, usage: Int)
    fun deleteBuffer(buffer: JsAny?)

    fun enableVertexAttribArray(index: Int)
    fun disableVertexAttribArray(index: Int)
    fun vertexAttribPointer(index: Int, size: Int, type: Int, normalized: Boolean, stride: Int, offset: Int)

    fun drawElements(mode: Int, count: Int, type: Int, offset: Int)
    fun drawArrays(mode: Int, first: Int, count: Int)

    fun enable(cap: Int)
    fun disable(cap: Int)
    fun blendFunc(sfactor: Int, dfactor: Int)
    fun cullFace(mode: Int)

    fun clearColor(r: Float, g: Float, b: Float, a: Float)
    fun viewport(x: Int, y: Int, width: Int, height: Int)
    fun clear(mask: Int)

    fun getParameter(pname: Int): JsAny?
}

/**
 * WebGL2 implementation of GlApi. Takes a [WebGl2Ctx] which is the canvas's
 * WebGL2RenderingContext cast by the bigbox3d-compose layer.
 *
 * OpenGL ES uses plain integer handles; WebGL2 uses JS object references.
 * This class maintains an internal map from our integer IDs to the WebGL objects.
 */
class GlApiImpl(private val gl: WebGl2Ctx) : GlApi {

    // Single handle map for all GL objects (shaders, programs, textures, buffers, uniform locs).
    private val handles = mutableMapOf<Int, JsAny>()
    // Cache (programId, uniformName) → handle so per-frame glGetUniformLocation doesn't leak.
    private val uniformLocationCache = mutableMapOf<Pair<Int, String>, Int>()
    private var nextId = 1

    private fun alloc(obj: JsAny): Int {
        val id = nextId++
        handles[id] = obj
        return id
    }

    // --- programs & shaders ---

    override fun glCreateShader(type: Int): Int =
        gl.createShader(type)?.let { alloc(it) } ?: 0

    override fun glShaderSource(shader: Int, source: String) =
        gl.shaderSource(handles[shader], source)

    override fun glCompileShader(shader: Int) = gl.compileShader(handles[shader])

    override fun glGetShaderiv(shader: Int, pname: Int): Int {
        val result = gl.getShaderParameter(handles[shader], pname)
        return if (result?.toString() == "true") 1 else 0
    }

    override fun glGetShaderInfoLog(shader: Int): String =
        gl.getShaderInfoLog(handles[shader]) ?: ""

    override fun glDeleteShader(shader: Int) {
        gl.deleteShader(handles.remove(shader))
    }

    override fun glCreateProgram(): Int =
        gl.createProgram()?.let { alloc(it) } ?: 0

    override fun glAttachShader(program: Int, shader: Int) =
        gl.attachShader(handles[program], handles[shader])

    override fun glLinkProgram(program: Int) = gl.linkProgram(handles[program])

    override fun glGetProgramiv(program: Int, pname: Int): Int {
        val result = gl.getProgramParameter(handles[program], pname)
        return if (result?.toString() == "true") 1 else 0
    }

    override fun glGetProgramInfoLog(program: Int): String =
        gl.getProgramInfoLog(handles[program]) ?: ""

    override fun glDeleteProgram(program: Int) {
        // Evict cached uniform locations for this program.
        uniformLocationCache.keys.filter { it.first == program }.forEach { key ->
            uniformLocationCache.remove(key)?.let { handles.remove(it) }
        }
        gl.deleteProgram(handles.remove(program))
    }

    override fun glUseProgram(program: Int) = gl.useProgram(handles[program])

    // --- textures ---

    override fun glGenTextures(n: Int): IntArray =
        IntArray(n) { gl.createTexture()?.let { alloc(it) } ?: 0 }

    override fun glBindTexture(target: Int, texture: Int) =
        gl.bindTexture(target, if (texture == 0) null else handles[texture])

    override fun glTexParameteri(target: Int, pname: Int, param: Int) =
        gl.texParameteri(target, pname, param)

    override fun glTexImage2D(width: Int, height: Int, pixels: ByteArray) {
        val data = Uint8Array(pixels.size)
        for (i in pixels.indices) data[i] = pixels[i]
        gl.texImage2D(GlApi.GL_TEXTURE_2D, 0, GlApi.GL_RGBA, width, height, 0, GlApi.GL_RGBA, GlApi.GL_UNSIGNED_BYTE, data)
    }

    override fun glDeleteTextures(textures: IntArray) {
        for (id in textures) gl.deleteTexture(handles.remove(id))
    }

    // --- uniforms ---

    override fun glGetUniformLocation(program: Int, name: String): Int =
        uniformLocationCache.getOrPut(program to name) {
            gl.getUniformLocation(handles[program], name)?.let { alloc(it) } ?: -1
        }

    override fun glUniformMatrix4fv(location: Int, count: Int, transpose: Boolean, value: FloatArray, offset: Int) {
        val loc = handles[location] ?: return
        val data = Float32Array(count * 16)
        for (i in 0 until count * 16) data[i] = value[offset + i]
        gl.uniformMatrix4fv(loc, transpose, data)
    }

    override fun glUniform3f(location: Int, x: Float, y: Float, z: Float) =
        gl.uniform3f(handles[location], x, y, z)

    override fun glUniform2f(location: Int, x: Float, y: Float) =
        gl.uniform2f(handles[location], x, y)

    override fun glUniform1f(location: Int, x: Float) =
        gl.uniform1f(handles[location], x)

    // --- buffers (VBO) ---

    override fun glGenBuffers(n: Int): IntArray =
        IntArray(n) { gl.createBuffer()?.let { alloc(it) } ?: 0 }

    override fun glBindBuffer(target: Int, buffer: Int) =
        gl.bindBuffer(target, if (buffer == 0) null else handles[buffer])

    override fun glBufferData(target: Int, data: FloatArray, usage: Int) {
        val typed = Float32Array(data.size)
        for (i in data.indices) typed[i] = data[i]
        gl.bufferData(target, typed, usage)
    }

    override fun glBufferData(target: Int, data: ShortArray, usage: Int) {
        val typed = Int16Array(data.size)
        for (i in data.indices) typed[i] = data[i]
        gl.bufferData(target, typed, usage)
    }

    override fun glDeleteBuffers(buffers: IntArray) {
        for (id in buffers) gl.deleteBuffer(handles.remove(id))
    }

    // --- vertex attributes ---

    override fun glEnableVertexAttribArray(index: Int) = gl.enableVertexAttribArray(index)

    override fun glDisableVertexAttribArray(index: Int) = gl.disableVertexAttribArray(index)

    override fun glVertexAttribPointer(index: Int, size: Int, type: Int, normalized: Boolean, stride: Int, offset: Int) =
        gl.vertexAttribPointer(index, size, type, normalized, stride, offset)

    // --- drawing ---

    override fun glDrawElements(mode: Int, count: Int, type: Int, offset: Int) =
        gl.drawElements(mode, count, type, offset)

    override fun glDrawArrays(mode: Int, first: Int, count: Int) =
        gl.drawArrays(mode, first, count)

    // --- render state ---

    override fun glEnable(cap: Int) = gl.enable(cap)
    override fun glDisable(cap: Int) = gl.disable(cap)
    override fun glBlendFunc(sfactor: Int, dfactor: Int) = gl.blendFunc(sfactor, dfactor)
    override fun glCullFace(mode: Int) = gl.cullFace(mode)

    // --- frame ---

    override fun glClearColor(r: Float, g: Float, b: Float, a: Float) = gl.clearColor(r, g, b, a)
    override fun glViewport(x: Int, y: Int, width: Int, height: Int) = gl.viewport(x, y, width, height)
    override fun glClear(mask: Int) = gl.clear(mask)

    // --- info ---

    override fun glGetString(name: Int): String = gl.getParameter(name)?.toString() ?: ""
}
