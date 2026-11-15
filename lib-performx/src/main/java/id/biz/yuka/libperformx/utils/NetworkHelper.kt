package id.biz.yuka.libperformx.utils

import android.util.Log

suspend fun <T> safeSupabaseCall(apiCall: suspend () -> T): ResultState<T> {
    return try {
        // Jika berhasil, langsung bungkus dengan Success
        ResultState.Success(apiCall())
    } catch (e: Exception) {
        // Cetak log lengkap ke Console dengan TAG "SUPABASE"
        Log.e("SUPABASE", "Error detail: ${e.message}", e)

        val rawMessage = e.message ?: "Terjadi kesalahan pada sistem. Silakan coba lagi."
        
        // Bersihkan pesan dari URL dan Header yang biasanya ada di error Supabase/Ktor
        val cleanedMessage = cleanSupabaseError(rawMessage)

        ResultState.Error(
            message = cleanedMessage,
            exception = e
        )
    }
}

/**
 * Membersihkan pesan error dari noise seperti URL, Header, dan API Key.
 */
private fun cleanSupabaseError(message: String): String {
    // 1. Potong bagian setelah "URL :" atau "(URL:" atau "URL:"
    val urlMarkers = listOf("URL :", "(URL:", "URL:", "http://", "https://")
    var cleaned = message
    
    for (marker in urlMarkers) {
        val index = cleaned.indexOf(marker, ignoreCase = true)
        if (index != -1) {
            cleaned = cleaned.substring(0, index).trim()
        }
    }

    // 2. Jika pesan kosong setelah dipotong, kembalikan default
    if (cleaned.isEmpty() || cleaned.length < 3) {
        return "Terjadi kesalahan pada koneksi atau server."
    }

    // 3. Mapping pesan umum ke Bahasa Indonesia agar lebih user-friendly
    return when {
        cleaned.contains("Invalid login credentials", ignoreCase = true) -> 
            "Email atau password salah."
        cleaned.contains("User not found", ignoreCase = true) -> 
            "Pengguna tidak ditemukan."
        cleaned.contains("Email not confirmed", ignoreCase = true) -> 
            "Email belum dikonfirmasi. Silakan cek email Anda."
        cleaned.contains("Rate limit exceeded", ignoreCase = true) -> 
            "Terlalu banyak mencoba. Silakan tunggu beberapa saat."
        cleaned.contains("Network Error", ignoreCase = true) || cleaned.contains("timeout", ignoreCase = true) -> 
            "Koneksi internet bermasalah. Silakan cek koneksi Anda."

        // Izin / RLS
        cleaned.contains("permission denied", ignoreCase = true) || cleaned.contains("row-level security", ignoreCase = true) ->
            "Anda tidak memiliki izin untuk melakukan tindakan ini."

        // Struktur Database (Table/Column)
        cleaned.contains("does not exist", ignoreCase = true) && (cleaned.contains("relation", ignoreCase = true) || cleaned.contains("table", ignoreCase = true)) ->
            "Gagal memproses data (Tabel tidak ditemukan)."
        cleaned.contains("does not exist", ignoreCase = true) && cleaned.contains("column", ignoreCase = true) ->
            "Gagal memproses data (Kolom tidak ditemukan)."

        // Fungsi / RPC Mismatch
        cleaned.contains("function", ignoreCase = true) && (cleaned.contains("does not exist", ignoreCase = true) || cleaned.contains("could not identify", ignoreCase = true)) ->
            "Gagal memproses permintaan (Fungsi sistem tidak cocok)."

        // Duplikat Data (Unique Constraint)
        cleaned.contains("already exists", ignoreCase = true) || cleaned.contains("duplicate key", ignoreCase = true) ->
            "Data sudah ada atau sudah pernah diinput."

        else -> cleaned
    }
}
