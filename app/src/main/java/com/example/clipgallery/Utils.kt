package com.example.clipgallery

import java.nio.ByteBuffer
import java.nio.ByteOrder

fun FloatArray.toByteArray(): ByteArray {
    val buffer = ByteBuffer.allocate(4 * size).order(ByteOrder.LITTLE_ENDIAN)
    for (f in this) buffer.putFloat(f)
    return buffer.array()
}

fun ByteArray.toFloatArray(): FloatArray {
    val buffer = ByteBuffer.wrap(this).order(ByteOrder.LITTLE_ENDIAN)
    val out = FloatArray(size / 4)
    for (i in out.indices) out[i] = buffer.getFloat()
    return out
}

fun cosineSim(a: FloatArray, b: FloatArray): Float {
    var dot = 0f
    for (i in a.indices) dot += a[i] * b[i]
    return dot // both vectors are L2-normalized, so dot product == cosine similarity
}
