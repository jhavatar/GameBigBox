package io.chthonic.bigbox3d.core

import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL15
import org.lwjgl.opengl.GL20
import org.lwjgl.system.MemoryStack
import java.nio.ByteBuffer

class GlApiImpl : GlApi {

    // --- programs & shaders ---
    override fun glCreateShader(type: Int) = GL20.glCreateShader(type)
    override fun glShaderSource(shader: Int, source: String) = GL20.glShaderSource(shader, source)
    override fun glCompileShader(shader: Int) = GL20.glCompileShader(shader)
    override fun glGetShaderiv(shader: Int, pname: Int) = GL20.glGetShaderi(shader, pname)
    override fun glGetShaderInfoLog(shader: Int): String = GL20.glGetShaderInfoLog(shader)
    override fun glDeleteShader(shader: Int) = GL20.glDeleteShader(shader)

    override fun glCreateProgram() = GL20.glCreateProgram()
    override fun glAttachShader(program: Int, shader: Int) = GL20.glAttachShader(program, shader)
    override fun glLinkProgram(program: Int) = GL20.glLinkProgram(program)
    override fun glGetProgramiv(program: Int, pname: Int) = GL20.glGetProgrami(program, pname)
    override fun glGetProgramInfoLog(program: Int): String = GL20.glGetProgramInfoLog(program)
    override fun glDeleteProgram(program: Int) = GL20.glDeleteProgram(program)
    override fun glUseProgram(program: Int) = GL20.glUseProgram(program)

    // --- textures ---
    override fun glGenTextures(n: Int): IntArray = MemoryStack.stackPush().use { stack ->
        val buf = stack.mallocInt(n)
        GL11.glGenTextures(buf)
        IntArray(n) { buf.get(it) }
    }
    override fun glBindTexture(target: Int, texture: Int) = GL11.glBindTexture(target, texture)
    override fun glTexParameteri(target: Int, pname: Int, param: Int) = GL11.glTexParameteri(target, pname, param)
    override fun glTexImage2D(width: Int, height: Int, pixels: ByteArray) {
        val buf = ByteBuffer.allocateDirect(pixels.size)
        buf.put(pixels)
        buf.flip()
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA, width, height, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, buf)
    }
    override fun glDeleteTextures(textures: IntArray) = textures.forEach { GL11.glDeleteTextures(it) }

    // --- uniforms ---
    override fun glGetUniformLocation(program: Int, name: String) = GL20.glGetUniformLocation(program, name)
    override fun glUniformMatrix4fv(location: Int, count: Int, transpose: Boolean, value: FloatArray, offset: Int) {
        MemoryStack.stackPush().use { stack ->
            val buf = stack.mallocFloat(count * 16)
            buf.put(value, offset, count * 16)
            buf.flip()
            GL20.glUniformMatrix4fv(location, transpose, buf)
        }
    }
    override fun glUniform3f(location: Int, x: Float, y: Float, z: Float) = GL20.glUniform3f(location, x, y, z)
    override fun glUniform2f(location: Int, x: Float, y: Float) = GL20.glUniform2f(location, x, y)
    override fun glUniform1f(location: Int, x: Float) = GL20.glUniform1f(location, x)

    // --- buffers (VBO) ---
    override fun glGenBuffers(n: Int): IntArray = MemoryStack.stackPush().use { stack ->
        val buf = stack.mallocInt(n)
        GL15.glGenBuffers(buf)
        IntArray(n) { buf.get(it) }
    }
    override fun glBindBuffer(target: Int, buffer: Int) = GL15.glBindBuffer(target, buffer)
    override fun glBufferData(target: Int, data: FloatArray, usage: Int) {
        MemoryStack.stackPush().use { stack ->
            val buf = stack.mallocFloat(data.size)
            buf.put(data)
            buf.flip()
            GL15.glBufferData(target, buf, usage)
        }
    }
    override fun glBufferData(target: Int, data: ShortArray, usage: Int) {
        MemoryStack.stackPush().use { stack ->
            val buf = stack.mallocShort(data.size)
            buf.put(data)
            buf.flip()
            GL15.glBufferData(target, buf, usage)
        }
    }
    override fun glDeleteBuffers(buffers: IntArray) = buffers.forEach { GL15.glDeleteBuffers(it) }

    // --- vertex attributes ---
    override fun glEnableVertexAttribArray(index: Int) = GL20.glEnableVertexAttribArray(index)
    override fun glDisableVertexAttribArray(index: Int) = GL20.glDisableVertexAttribArray(index)
    override fun glVertexAttribPointer(index: Int, size: Int, type: Int, normalized: Boolean, stride: Int, offset: Int) =
        GL20.glVertexAttribPointer(index, size, type, normalized, stride, offset.toLong())

    // --- drawing ---
    override fun glDrawElements(mode: Int, count: Int, type: Int, offset: Int) =
        GL11.glDrawElements(mode, count, type, offset.toLong())
    override fun glDrawArrays(mode: Int, first: Int, count: Int) = GL11.glDrawArrays(mode, first, count)

    // --- render state ---
    override fun glEnable(cap: Int) = GL11.glEnable(cap)
    override fun glDisable(cap: Int) = GL11.glDisable(cap)
    override fun glBlendFunc(sfactor: Int, dfactor: Int) = GL11.glBlendFunc(sfactor, dfactor)
    override fun glCullFace(mode: Int) = GL11.glCullFace(mode)

    // --- frame ---
    override fun glClearColor(r: Float, g: Float, b: Float, a: Float) = GL11.glClearColor(r, g, b, a)
    override fun glViewport(x: Int, y: Int, width: Int, height: Int) = GL11.glViewport(x, y, width, height)
    override fun glClear(mask: Int) = GL11.glClear(mask)

    // --- info ---
    override fun glGetString(name: Int): String = GL11.glGetString(name) ?: ""
    override fun isGlEs() = false
}
