package id.biz.yuka.performax.utils

/**
 * Kontrak wajib untuk ViewModel yang menangani halaman obrolan (Room).
 * Memastikan semua fitur chat memiliki fungsi untuk menahan notifikasi
 * dan me-refresh layar saat aplikasi sedang dibuka di room tersebut.
 */
interface ChatRoomObserver {
    fun observeIncomingFCMChats()
    fun setAsActiveRoom()
    fun clearActiveRoom()
}