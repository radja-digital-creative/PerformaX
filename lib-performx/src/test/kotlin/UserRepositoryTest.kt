import id.biz.yuka.libperformx.SupabaseConfig // Sesuaikan import jika Config ada di root
import id.biz.yuka.libperformx.repository.UserRepository
import id.biz.yuka.libperformx.utils.ResultState
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.providers.builtin.Email
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class UserRepositoryTest {

    private val userRepository = UserRepository()

    @Test
    fun `test mengambil profil dengan ID asal harus mengembalikan Error`() = runBlocking {
        println("Mencari user ID fiktif...")
        val result = userRepository.getUserProfile("uuid-fiktif-1234")

        when (result) {
            is ResultState.Success -> {
                println("❌ GAGAL: Kok bisa ketemu datanya?")
                assertTrue(false)
            }
            is ResultState.Error -> {
                println("✅ BERHASIL DITOLAK: ${result.message}")
                assertTrue(true)
            }
        }
    }

    @Test
    fun `test mengambil profil asli dengan Autentikasi harus sukses`() = runBlocking {
        println("1. Melakukan Autentikasi (Login) sementara untuk keperluan Test...")

        try {
            SupabaseConfig.client.auth.signInWith(Email) {
                email = "test@perfomx.com" // GANTI DENGAN EMAIL ASLI
                password = "admin1234"         // GANTI DENGAN PASSWORD ASLI
            }
        } catch (e: Exception) {
            println("❌ Gagal Login Paksa: ${e.message}")
            assertTrue(false, "Test dihentikan karena gagal mendapatkan token")
            return@runBlocking
        }

        // Ambil UUID milik user yang baru saja berhasil login paksa tadi
        val currentUser = SupabaseConfig.client.auth.currentUserOrNull()
        val validUserId = currentUser?.id ?: ""

        println("✅ Login berhasil! Token didapatkan untuk User ID: $validUserId")
        println("2. Menjalankan fungsi getUserProfile...")

        // SEKARANG kita panggil fungsinya (Sudah bawa Token!)
        val result = userRepository.getUserProfile(validUserId)

        when (result) {
            is ResultState.Success -> {
                val user = result.data
                println("✅ PROFIL BERHASIL DITEMBUS!")
                println("Nama: ${user.fullName}")
                println("Role: ${user.role}")
                println("Akses Global Admin?: ${user.hasGlobalAccess}")
                println("Jumlah Toko: ${user.stores.size}")

                assertTrue(user.fullName.isNotBlank())
            }
            is ResultState.Error -> {
                println("❌ ERROR: ${result.message}")
                assertTrue(false)
            }
        }

        // Opsional: Logout setelah selesai agar bersih
        SupabaseConfig.client.auth.signOut()
    }

    @Test
    fun `test mengambil profil lain dengan Autentikasi harus sukses`() = runBlocking {
        println("1. Melakukan Autentikasi (Login) sementara untuk keperluan Test...")

        try {
            SupabaseConfig.client.auth.signInWith(Email) {
                email = "test@perfomx.com" // GANTI DENGAN EMAIL ASLI
                password = "admin1234"         // GANTI DENGAN PASSWORD ASLI
            }
        } catch (e: Exception) {
            println("❌ Gagal Login Paksa: ${e.message}")
            assertTrue(false, "Test dihentikan karena gagal mendapatkan token")
            return@runBlocking
        }

        // Ambil UUID milik user yang baru saja berhasil login paksa tadi
        val validUserId = "4075f1e0-2745-45ef-8fb9-91af5f3a6b43"

        println("✅ Login berhasil! Token didapatkan untuk User ID: $validUserId")
        println("2. Menjalankan fungsi getUserProfile...")

        // SEKARANG kita panggil fungsinya (Sudah bawa Token!)
        val result = userRepository.getUserProfile(validUserId)

        when (result) {
            is ResultState.Success -> {
                val user = result.data
                println("✅ PROFIL BERHASIL DITEMBUS!")
                println("Nama: ${user.fullName}")
                println("Role: ${user.role}")
                println("Akses Global Admin?: ${user.hasGlobalAccess}")
                println("Jumlah Toko: ${user.stores.size}")

                assertTrue(user.fullName.isNotBlank())
            }
            is ResultState.Error -> {
                println("❌ ERROR: ${result.message}")
                assertTrue(false)
            }
        }

        // Opsional: Logout setelah selesai agar bersih
        SupabaseConfig.client.auth.signOut()
    }
}