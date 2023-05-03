package com.voidcom.videoproject.ui.rtp

object YUVUtil {
    fun YUV420pRotate90(dst: ByteArray, src: ByteArray, width: Int, height: Int) {
        var n = 0
        val wh = width * height
        val halfWidth = width / 2
        val halfHeight = height / 2
        // y
        for (j in 0 until width) {
            for (i in height - 1 downTo 0) {
                dst[n++] = src[width * i + j]
            }
        }
        // u
        for (i in 0 until halfWidth) {
            for (j in 1..halfHeight) {
                dst[n++] = src[wh + ((halfHeight - j) * halfWidth + i)]
            }
        }
        // v
        for (i in 0 until halfWidth) {
            for (j in 1..halfHeight) {
                dst[n++] = src[wh + wh / 4 + ((halfHeight - j) * halfWidth + i)]
            }
        }
    }

    fun YUV420pRotate180(dst: ByteArray, src: ByteArray, width: Int, height: Int) {
        var n = 0
        val halfWidth = width / 2
        val halfHeight = height / 2
        // y
        for (j in height - 1 downTo 0) {
            for (i in width downTo 1) {
                dst[n++] = src[width * j + i - 1]
            }
        }
        // u
        var offset = width * height
        for (j in halfHeight - 1 downTo 0) {
            for (i in halfWidth downTo 1) {
                dst[n++] = src[offset + halfWidth * j + i - 1]
            }
        }
        // v
        offset += halfWidth * halfHeight
        for (j in halfHeight - 1 downTo 0) {
            for (i in halfWidth downTo 1) {
                dst[n++] = src[offset + halfWidth * j + i - 1]
            }
        }
    }

}