package com.example.clipgallery.ml

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import android.content.Context
import android.graphics.Bitmap
import java.io.File
import java.io.FileOutputStream
import java.nio.IntBuffer
import kotlin.math.sqrt

/** Wraps quantized MobileCLIP v2 ONNX Runtime sessions. */
class ClipEmbedder(private val context: Context) {
    private val env = OrtEnvironment.getEnvironment()
    private val visionSession: OrtSession
    private val textSession: OrtSession
    private val tokenizer: ClipTokenizer by lazy { ClipTokenizer(context) }

    companion object {
        private const val VISION_INPUT = "pixel_values"
        private const val TEXT_INPUT = "input_ids"
    }

    init {
        val opts = OrtSession.SessionOptions()
        visionSession = env.createSession(copyAssetToFiles("models/clip-image-int8.ort"), opts)
        textSession = env.createSession(copyAssetToFiles("models/clip-text-int8.ort"), opts)
    }

    private fun copyAssetToFiles(assetPath: String): String {
        val target = File(context.filesDir, assetPath.substringAfterLast('/'))
        if (!target.exists()) {
            context.assets.open(assetPath).use { input ->
                FileOutputStream(target).use { output -> input.copyTo(output) }
            }
        }
        return target.absolutePath
    }

    fun embedImage(bitmap: Bitmap): FloatArray {
        val tensor = ImagePreprocess.bitmapToTensor(env, bitmap)
        tensor.use { t ->
            val inputName = if (visionSession.inputNames.contains(VISION_INPUT))
                VISION_INPUT else visionSession.inputNames.iterator().next()
            visionSession.run(mapOf(inputName to t)).use { result ->
                @Suppress("UNCHECKED_CAST")
                val out = result[0].value as Array<FloatArray>
                return normalize(out[0])
            }
        }
    }

    fun embedText(query: String): FloatArray {
        val ids = tokenizer.encode(query)
        val buffer = IntBuffer.wrap(ids.map { it.toInt() }.toIntArray())
        OnnxTensor.createTensor(env, buffer, longArrayOf(1, ids.size.toLong())).use { tensor ->
            val inputName = if (textSession.inputNames.contains(TEXT_INPUT))
                TEXT_INPUT else textSession.inputNames.iterator().next()
            textSession.run(mapOf(inputName to tensor)).use { result ->
                @Suppress("UNCHECKED_CAST")
                val out = result[0].value as Array<FloatArray>
                return normalize(out[0])
            }
        }
    }

    private fun normalize(v: FloatArray): FloatArray {
        var norm = 0f
        for (x in v) norm += x * x
        norm = sqrt(norm)
        if (norm == 0f) return v
        return FloatArray(v.size) { v[it] / norm }
    }

    fun close() {
        visionSession.close()
        textSession.close()
    }
}
