package id.biz.yuka.performax.utils

import android.util.Log
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

data class NotificationEvent(val type: String, val id: String)

object ChatEventBus {
    // Menggunakan MAP untuk menyimpan ID yang aktif berdasarkan tipenya.
    private val activeRooms = mutableMapOf<String, String>()

    // Saluran untuk memancarkan sinyal ada chat baru
    private val _incomingEvents = MutableSharedFlow<NotificationEvent>()
    val incomingEvents = _incomingEvents.asSharedFlow()

    suspend fun emitNewEvent(type: String, id: String) {
        Log.d("CHAT_BUS", "🚀 Menembakkan sinyal refresh ke UI -> Type: $type | ID: $id")
        _incomingEvents.emit(NotificationEvent(type, id))
    }

    // Fungsi dinamis untuk mengeset room yang aktif
    fun setActiveRoom(type: String, id: String) {
        val cleanId = id.trim()
        activeRooms[type] = cleanId
        Log.d("CHAT_BUS", "🟢 ROOM AKTIF DIDAFTARKAN -> Type: $type | ID: $cleanId")
        Log.d("CHAT_BUS", "Daftar Room Saat Ini: $activeRooms")
    }

    // Fungsi dinamis untuk menghapus room yang aktif
    fun clearActiveRoom(type: String) {
        activeRooms.remove(type)
        Log.d("CHAT_BUS", "🔴 ROOM AKTIF DIHAPUS -> Type: $type")
        Log.d("CHAT_BUS", "Daftar Room Saat Ini: $activeRooms")
    }

    // Cek apakah user sedang buka layar spesifik ini (DIBUAT KEBAL HURUF BESAR/KECIL & SPASI)
    fun isRoomActive(type: String, id: String): Boolean {
        val cleanIncomingId = id.trim()
        val currentActiveId = activeRooms[type]

        // Pengecekan tanpa mempedulikan kapitalisasi (ignoreCase = true)
        val isMatch = currentActiveId.equals(cleanIncomingId, ignoreCase = true)

        Log.d("CHAT_BUS", "========================================")
        Log.d("CHAT_BUS", "🔍 PENGECEKAN ROOM AKTIF")
        Log.d("CHAT_BUS", "Target Type    : $type")
        Log.d("CHAT_BUS", "ID dari Notif  : [$cleanIncomingId]")
        Log.d("CHAT_BUS", "ID di Aplikasi : [$currentActiveId]")
        Log.d("CHAT_BUS", "Hasil Cocok?   : $isMatch")
        Log.d("CHAT_BUS", "========================================")

        return isMatch
    }
}