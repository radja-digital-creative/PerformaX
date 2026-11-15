package id.biz.yuka.performax.utils

import android.content.Context
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import android.graphics.Bitmap
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class FaceDetectionService(private val context: Context) {
    private val options = FaceDetectorOptions.Builder()
        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
        .build()
    private val detector = FaceDetection.getClient(options)

    suspend fun getFaceCrop(bitmap: Bitmap): Bitmap? = suspendCancellableCoroutine { continuation ->
        val image = InputImage.fromBitmap(bitmap, 0)

        detector.process(image)
            .addOnSuccessListener { faces ->
                if (faces.isEmpty()) {
                    continuation.resume(null) // Tidak ada wajah
                } else {
                    val face = faces[0]
                    val bounds = face.boundingBox

                    val marginX = (bounds.width() * 0.20f).toInt()
                    val marginY = (bounds.height() * 0.20f).toInt()

                    // Hitung koordinat crop yang aman
                    val left = maxOf(0, bounds.left - marginX)
                    val top = maxOf(0, bounds.top - marginY)
                    val right = minOf(bounds.right + marginX, bitmap.width)
                    val bottom = minOf(bounds.bottom + marginY, bitmap.height)

                    val width = right - left
                    val height = bottom - top

                    if (width <= 0 || height <= 0) {
                        continuation.resume(null)
                    } else {
                        val cropped = Bitmap.createBitmap(bitmap, left, top, width, height)
                        continuation.resume(cropped) // Kembalikan bitmap hasil crop
                    }
                }
            }
            .addOnFailureListener { e ->
                e.printStackTrace()
                continuation.resume(null) // Jika error, kembalikan null
            }
    }
}