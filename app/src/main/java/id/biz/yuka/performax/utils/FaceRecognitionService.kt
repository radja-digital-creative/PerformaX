package id.biz.yuka.performax.utils

import android.content.Context
import android.graphics.Bitmap
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import java.nio.ByteBuffer
import java.nio.ByteOrder

class FaceRecognitionService(private val context: Context) {

    private var interpreter: Interpreter? = null

    init {
        // Load model dari assets folder (assets/mobile_facenet.tflite)
        val model = FileUtil.loadMappedFile(context, "mobile_facenet.tflite")
        interpreter = Interpreter(model)
    }

    // Fungsi utama: Mengubah Bitmap menjadi 192 Float (Embedding)
    fun getFaceEmbedding(bitmap: Bitmap): FloatArray {
        val inputBuffer = preprocessBitmap(bitmap)
        val output = Array(1) { FloatArray(192) } // MobileFaceNet output biasanya 192

        interpreter?.run(inputBuffer, output)
        return output[0]
    }

    private fun preprocessBitmap(bitmap: Bitmap): ByteBuffer {
        // FIX: Konversi bitmap HARDWARE ke ARGB_8888 agar bisa dibaca CPU
        val softwareBitmap = if (bitmap.config == Bitmap.Config.HARDWARE) {
            bitmap.copy(Bitmap.Config.ARGB_8888, false)
        } else {
            bitmap
        }

        val inputSize = 112
        // Gunakan softwareBitmap, bukan bitmap asli
        val scaled = Bitmap.createScaledBitmap(softwareBitmap, inputSize, inputSize, true)

        val buffer = ByteBuffer.allocateDirect(inputSize * inputSize * 3 * 4)
        buffer.order(ByteOrder.nativeOrder())

        val pixels = IntArray(inputSize * inputSize)
        scaled.getPixels(pixels, 0, inputSize, 0, 0, inputSize, inputSize)

        // ... (sisa logika loop tetap sama) ...
        for (pixel in pixels) {
            val r = (pixel shr 16 and 0xFF)
            val g = (pixel shr 8 and 0xFF)
            val b = (pixel and 0xFF)
            buffer.putFloat((r - 127.5f) / 128.0f)
            buffer.putFloat((g - 127.5f) / 128.0f)
            buffer.putFloat((b - 127.5f) / 128.0f)
        }
        return buffer
    }

    // Fungsi menghitung kemiripan (Cosine Similarity)
    fun calculateSimilarity(embedding1: FloatArray, embedding2: FloatArray): Float {
        var dotProduct = 0f
        var normA = 0f
        var normB = 0f
        for (i in embedding1.indices) {
            dotProduct += embedding1[i] * embedding2[i]
            normA += embedding1[i] * embedding1[i]
            normB += embedding2[i] * embedding2[i]
        }
        return dotProduct / (Math.sqrt(normA.toDouble()).toFloat() * Math.sqrt(normB.toDouble()).toFloat())
    }
}