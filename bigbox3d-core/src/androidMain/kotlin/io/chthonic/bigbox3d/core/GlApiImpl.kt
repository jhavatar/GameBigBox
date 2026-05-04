package io.chthonic.bigbox3d.core

import android.opengl.GLES30
import java.nio.ByteBuffer
import java.nio.ByteOrder

class GlApiImpl : GlApi {

    override fun glCreateShader(type: Int) = GLES30.glCreateShader(type)
    override fun glShaderSource(shader: Int, source: String) = GLES30.glShaderSource(shader, source)
    override fun glCompileShader(shader: Int) = GLES30.glCompileShader(shader)
    override fun glGetShaderiv(shader: Int, pname: Int): Int {
        val result = IntArray(1)
        GLES30.glGetShaderiv(shader, pname, result, 0)
        return result[0]
    }
    override fun glGetShaderInfoLog(shader: Int): String = GLES30.glGetShaderInfoLog(shader)
    override fun glDeleteShader(shader: Int) = GLES30.glDeleteShader(shader)

    override fun glCreateProgram() = GLES30.glCreateProgram()
    override fun glAttachShader(program: Int, shader: Int) = GLES30.glAttachShader(program, shader)
    override fun glLinkProgram(program: Int) = GLES30.glLinkProgram(program)
    override fun glGetProgramiv(program: Int, pname: Int): Int {
        val result = IntArray(1)
        GLES30.glGetProgramiv(program, pname, result, 0)
        return result[0]
    }
    override fun glGetProgramInfoLog(program: Int): String = GLES30.glGetProgramInfoLog(program)
    override fun glDeleteProgram(program: Int) = GLES30.glDeleteProgram(program)
    override fun glUseProgram(program: Int) = GLES30.glUseProgram(program)

    override fun glGenTextures(n: Int): IntArray {
        val result = IntArray(n)
        GLES30.glGenTextures(n, result, 0)
        return result
    }
    override fun glBindTexture(target: Int, texture: Int) = GLES30.glBindTexture(target, texture)
    override fun glTexParameteri(target: Int, pname: Int, param: Int) = GLES30.glTexParameteri(target, pname, param)
    override fun glTexImage2D(width: Int, height: Int, pixels: ByteArray) {
        GLES30.glTexImage2D(GLES30.GL_TEXTURE_2D, 0, GLES30.GL_RGBA, width, height, 0, GLES30.GL_RGBA, GLES30.GL_UNSIGNED_BYTE, ByteBuffer.wrap(pixels))
    }
    override fun glDeleteTextures(textures: IntArray) = GLES30.glDeleteTextures(textures.size, textures, 0)

    override fun glGetUniformLocation(program: Int, name: String) = GLES30.glGetUniformLocation(program, name)
    override fun glUniformMatrix4fv(location: Int, count: Int, transpose: Boolean, value: FloatArray, offset: Int) =
        GLES30.glUniformMatrix4fv(location, count, transpose, value, offset)
    override fun glUniform3f(location: Int, x: Float, y: Float, z: Float) = GLES30.glUniform3f(location, x, y, z)
    override fun glUniform2f(location: Int, x: Float, y: Float) = GLES30.glUniform2f(location, x, y)
    override fun glUniform1f(location: Int, x: Float) = GLES30.glUniform1f(location, x)

    override fun glGenBuffers(n: Int): IntArray {
        val result = IntArray(n)
        GLES30.glGenBuffers(n, result, 0)
        return result
    }
    override fun glBindBuffer(target: Int, buffer: Int) = GLES30.glBindBuffer(target, buffer)
    override fun glBufferData(target: Int, data: FloatArray, usage: Int) {
        val buf = ByteBuffer.allocateDirect(data.size * 4).order(ByteOrder.nativeOrder()).asFloatBuffer()
        buf.put(data)
        buf.position(0)
        GLES30.glBufferData(target, data.size * 4, buf, usage)
    }
    override fun glBufferData(target: Int, data: ShortArray, usage: Int) {
        val buf = ByteBuffer.allocateDirect(data.size * 2).order(ByteOrder.nativeOrder()).asShortBuffer()
        buf.put(data)
        buf.position(0)
        GLES30.glBufferData(target, data.size * 2, buf, usage)
    }
    override fun glDeleteBuffers(buffers: IntArray) = GLES30.glDeleteBuffers(buffers.size, buffers, 0)

    override fun glEnableVertexAttribArray(index: Int) = GLES30.glEnableVertexAttribArray(index)
    override fun glDisableVertexAttribArray(index: Int) = GLES30.glDisableVertexAttribArray(index)
    override fun glVertexAttribPointer(index: Int, size: Int, type: Int, normalized: Boolean, stride: Int, offset: Int) =
        GLES30.glVertexAttribPointer(index, size, type, normalized, stride, offset)

    override fun glDrawElements(mode: Int, count: Int, type: Int, offset: Int) =
        GLES30.glDrawElements(mode, count, type, offset)
    override fun glDrawArrays(mode: Int, first: Int, count: Int) = GLES30.glDrawArrays(mode, first, count)

    override fun glEnable(cap: Int) = GLES30.glEnable(cap)
    override fun glDisable(cap: Int) = GLES30.glDisable(cap)
    override fun glBlendFunc(sfactor: Int, dfactor: Int) = GLES30.glBlendFunc(sfactor, dfactor)
    override fun glCullFace(mode: Int) = GLES30.glCullFace(mode)

    override fun glClearColor(r: Float, g: Float, b: Float, a: Float) = GLES30.glClearColor(r, g, b, a)
    override fun glViewport(x: Int, y: Int, width: Int, height: Int) = GLES30.glViewport(x, y, width, height)
    override fun glClear(mask: Int) = GLES30.glClear(mask)

    override fun glGetString(name: Int): String = GLES30.glGetString(name) ?: ""
    override fun isGlEs() = true
}
