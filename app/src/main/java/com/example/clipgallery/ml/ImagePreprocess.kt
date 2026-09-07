package com.example.clipgallery.ml

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import android.graphics.Bitmap
import java.nio.FloatBuffer
import kotlin.math.ceil

object ImagePreprocess {
    private val MEAN = floatArrayOf(0.48145466f, 0.4578275f, 0.40821073f)
    private val STD = floatArrayOf(0.26862954f, 0.26130258f, 0.27577711f)

    fun bitmapToTensor(env: OrtEnvironment, bitmap: Bitmap, size: Int = 224): OnnxTensor {
        val scale = maxOf(size.toFloat() / bitmap.width, size.toFloat() / bitmap.height)
            val resizedWidth = maxOf(size, ceil(bitmap.width * scale).toInt())
            val resizedHeight = maxOf(size, ceil(bitmap.height * scale).toInt())
        val resized = Bitmap.createScaledBitmap(bitmap, resizedWidth, resizedHeight, true)
        val left = (resizedWidth - size) / 2
        val top = (resizedHeight - size) / 2
        val cropped = Bitmap.createBitmap(resized, left, top, size, size)
        val pixels = IntArray(size * size)
        cropped.getPixels(pixels, 0, size, 0, 0, size, size)

        val floatBuffer = FloatBuffer.allocate(3 * size * size)
        for (c in 0 until 3) {
            for (i in 0 until size * size) {
                val p = pixels[i]
                val v = when (c) {
                    0 -> ((p shr 16) and 0xFF) / 255f
                    1 -> ((p shr 8) and 0xFF) / 255f
                    else -> (p and 0xFF) / 255f
                }
                floatBuffer.put(c * size * size + i, (v - MEAN[c]) / STD[c])
            }
        }
        floatBuffer.rewind()
        cropped.recycle()
        if (resized !== bitmap) resized.recycle()
        return OnnxTensor.createTensor(env, floatBuffer, longArrayOf(1, 3, size.toLong(), size.toLong()))
    }
}
