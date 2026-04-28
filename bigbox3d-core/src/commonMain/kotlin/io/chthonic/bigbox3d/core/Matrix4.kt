package io.chthonic.bigbox3d.core

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

/**
 * Pure-Kotlin port of android.opengl.Matrix operations.
 * Matrices are 4x4 column-major FloatArrays with an offset, matching OpenGL convention.
 * Element at row r, column c: m[offset + c*4 + r]
 */
internal object Matrix4 {

    fun setIdentityM(m: FloatArray, mOffset: Int) {
        for (i in 0 until 16) m[mOffset + i] = 0f
        m[mOffset + 0] = 1f
        m[mOffset + 5] = 1f
        m[mOffset + 10] = 1f
        m[mOffset + 15] = 1f
    }

    fun multiplyMM(
        result: FloatArray, resultOffset: Int,
        lhs: FloatArray, lhsOffset: Int,
        rhs: FloatArray, rhsOffset: Int,
    ) {
        for (col in 0 until 4) {
            for (row in 0 until 4) {
                var sum = 0f
                for (k in 0 until 4) {
                    sum += lhs[lhsOffset + k * 4 + row] * rhs[rhsOffset + col * 4 + k]
                }
                result[resultOffset + col * 4 + row] = sum
            }
        }
    }

    fun multiplyMV(
        resultVec: FloatArray, resultVecOffset: Int,
        lhsMat: FloatArray, lhsMatOffset: Int,
        rhsVec: FloatArray, rhsVecOffset: Int,
    ) {
        for (row in 0 until 4) {
            var sum = 0f
            for (k in 0 until 4) {
                sum += lhsMat[lhsMatOffset + k * 4 + row] * rhsVec[rhsVecOffset + k]
            }
            resultVec[resultVecOffset + row] = sum
        }
    }

    fun rotateM(m: FloatArray, mOffset: Int, angleDeg: Float, x: Float, y: Float, z: Float) {
        val rot = FloatArray(16)
        setRotateM(rot, 0, angleDeg, x, y, z)
        val tmp = FloatArray(16)
        multiplyMM(tmp, 0, m, mOffset, rot, 0)
        tmp.copyInto(m, mOffset)
    }

    fun setLookAtM(
        rm: FloatArray, rmOffset: Int,
        eyeX: Float, eyeY: Float, eyeZ: Float,
        centerX: Float, centerY: Float, centerZ: Float,
        upX: Float, upY: Float, upZ: Float,
    ) {
        var fx = centerX - eyeX
        var fy = centerY - eyeY
        var fz = centerZ - eyeZ
        val rlf = 1f / sqrt((fx * fx + fy * fy + fz * fz).toDouble()).toFloat()
        fx *= rlf; fy *= rlf; fz *= rlf

        var sx = fy * upZ - fz * upY
        var sy = fz * upX - fx * upZ
        var sz = fx * upY - fy * upX
        val rls = 1f / sqrt((sx * sx + sy * sy + sz * sz).toDouble()).toFloat()
        sx *= rls; sy *= rls; sz *= rls

        val ux = sy * fz - sz * fy
        val uy = sz * fx - sx * fz
        val uz = sx * fy - sy * fx

        rm[rmOffset + 0] = sx;  rm[rmOffset + 1] = ux;  rm[rmOffset + 2]  = -fx; rm[rmOffset + 3]  = 0f
        rm[rmOffset + 4] = sy;  rm[rmOffset + 5] = uy;  rm[rmOffset + 6]  = -fy; rm[rmOffset + 7]  = 0f
        rm[rmOffset + 8] = sz;  rm[rmOffset + 9] = uz;  rm[rmOffset + 10] = -fz; rm[rmOffset + 11] = 0f
        rm[rmOffset + 12] = -(sx * eyeX + sy * eyeY + sz * eyeZ)
        rm[rmOffset + 13] = -(ux * eyeX + uy * eyeY + uz * eyeZ)
        rm[rmOffset + 14] =   (fx * eyeX + fy * eyeY + fz * eyeZ)
        rm[rmOffset + 15] = 1f
    }

    fun perspectiveM(m: FloatArray, offset: Int, fovyDeg: Float, aspect: Float, zNear: Float, zFar: Float) {
        val f = 1f / tan(fovyDeg * (PI / 360.0)).toFloat()
        val rangeReciprocal = 1f / (zNear - zFar)
        m[offset + 0]  = f / aspect
        m[offset + 1]  = 0f; m[offset + 2]  = 0f; m[offset + 3]  = 0f
        m[offset + 4]  = 0f
        m[offset + 5]  = f
        m[offset + 6]  = 0f; m[offset + 7]  = 0f
        m[offset + 8]  = 0f; m[offset + 9]  = 0f
        m[offset + 10] = (zFar + zNear) * rangeReciprocal
        m[offset + 11] = -1f
        m[offset + 12] = 0f; m[offset + 13] = 0f
        m[offset + 14] = 2f * zFar * zNear * rangeReciprocal
        m[offset + 15] = 0f
    }

    private fun setRotateM(rm: FloatArray, rmOffset: Int, angleDeg: Float, x: Float, y: Float, z: Float) {
        val radians = (angleDeg * PI / 180.0).toFloat()
        val c = cos(radians.toDouble()).toFloat()
        val s = sin(radians.toDouble()).toFloat()
        val len = sqrt((x * x + y * y + z * z).toDouble()).toFloat()
        val nx = x / len; val ny = y / len; val nz = z / len
        val nc = 1f - c

        rm[rmOffset + 0]  = nx * nx * nc + c;      rm[rmOffset + 1]  = ny * nx * nc + nz * s; rm[rmOffset + 2]  = nz * nx * nc - ny * s; rm[rmOffset + 3]  = 0f
        rm[rmOffset + 4]  = nx * ny * nc - nz * s; rm[rmOffset + 5]  = ny * ny * nc + c;      rm[rmOffset + 6]  = nz * ny * nc + nx * s; rm[rmOffset + 7]  = 0f
        rm[rmOffset + 8]  = nx * nz * nc + ny * s; rm[rmOffset + 9]  = ny * nz * nc - nx * s; rm[rmOffset + 10] = nz * nz * nc + c;      rm[rmOffset + 11] = 0f
        rm[rmOffset + 12] = 0f;                    rm[rmOffset + 13] = 0f;                    rm[rmOffset + 14] = 0f;                    rm[rmOffset + 15] = 1f
    }
}
