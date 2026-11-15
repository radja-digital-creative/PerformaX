package id.biz.yuka.performax.utils // Sesuaikan dengan package Komandan

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import kotlin.random.Random

/**
 * Custom Modifier untuk membuat motif background chat ala doodle geometri.
 */
fun Modifier.chatBackgroundPattern(
    patternColor: Color = Color.Black,
    patternAlpha: Float = 0.04f, // Transparansi sangat tipis agar elegan
    shapeCount: Int = 25 // Jumlah bangun datar (jarang-jarang)
) = this.drawBehind {
    // Menggunakan fixed seed (contoh: 12345) agar posisi tidak berubah-ubah saat recomposition
    val random = Random(12345)
    val path = Path()

    for (i in 0 until shapeCount) {
        val type = random.nextInt(3) // 0: Lingkaran, 1: Kotak, 2: Segitiga
        val x = random.nextFloat() * size.width
        val y = random.nextFloat() * size.height
        val radius = random.nextFloat() * 30f + 15f // Besar kecil acak (15px - 45px)
        val strokeWidth = random.nextFloat() * 2f + 2f // Ketebalan garis acak
        val rotationAngle = random.nextFloat() * 360f

        rotate(degrees = rotationAngle, pivot = Offset(x, y)) {
            when (type) {
                0 -> { // Lingkaran
                    drawCircle(
                        color = patternColor, radius = radius, center = Offset(x, y),
                        alpha = patternAlpha, style = Stroke(width = strokeWidth)
                    )
                }
                1 -> { // Kotak
                    drawRect(
                        color = patternColor, topLeft = Offset(x - radius, y - radius),
                        size = Size(radius * 2, radius * 2), alpha = patternAlpha,
                        style = Stroke(width = strokeWidth)
                    )
                }
                2 -> { // Segitiga
                    path.reset()
                    path.moveTo(x, y - radius) // Ujung atas
                    path.lineTo(x + radius, y + radius) // Kanan bawah
                    path.lineTo(x - radius, y + radius) // Kiri bawah
                    path.close()
                    drawPath(
                        path = path, color = patternColor, alpha = patternAlpha,
                        style = Stroke(width = strokeWidth)
                    )
                }
            }
        }
    }
}