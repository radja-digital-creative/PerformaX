package id.biz.yuka.performax.ui.utils

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.pdf.PdfDocument
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

object PdfSaveHelper {

    /**
     * Simpan PdfDocument ke folder Downloads perangkat.
     * Return: Uri string untuk share, atau null jika gagal.
     */
    fun savePdfToDownloads(
        context: Context,
        pdfDocument: PdfDocument,
        fileName: String
    ): String? {
        return try {
            val outputStream: OutputStream
            val filePath: String

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Android 10+ — gunakan MediaStore (tidak perlu WRITE_EXTERNAL_STORAGE)
                val contentValues = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                    put(MediaStore.Downloads.MIME_TYPE, "application/pdf")
                    put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
                val resolver = context.contentResolver
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                    ?: return null
                outputStream = resolver.openOutputStream(uri) ?: return null
                filePath = uri.toString()
            } else {
                // Android < 10 — langsung tulis ke direktori Downloads publik
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                downloadsDir.mkdirs()
                val file = File(downloadsDir, fileName)
                outputStream = FileOutputStream(file)
                filePath = file.absolutePath
            }

            outputStream.use { pdfDocument.writeTo(it) }
            pdfDocument.close()
            filePath
        } catch (e: Exception) {
            e.printStackTrace()
            pdfDocument.close()
            null
        }
    }

    /**
     * Share PDF langsung via Intent (tanpa menyimpan, dari file sementara di cache).
     */
    fun sharePdf(context: Context, pdfDocument: PdfDocument, fileName: String) {
        try {
            val cacheDir = File(context.cacheDir, "pdf_share").also { it.mkdirs() }
            val file = File(cacheDir, fileName)
            FileOutputStream(file).use { pdfDocument.writeTo(it) }
            pdfDocument.close()

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Bagikan Slip Gaji"))
        } catch (e: Exception) {
            e.printStackTrace()
            pdfDocument.close()
        }
    }
}
