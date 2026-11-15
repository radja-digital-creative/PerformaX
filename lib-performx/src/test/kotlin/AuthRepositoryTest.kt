import id.biz.yuka.libperformx.repository.AuthRepository
import id.biz.yuka.libperformx.request.LoginRequest
import id.biz.yuka.libperformx.utils.ResultState
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class AuthRepositoryTest {

    private val authRepo = AuthRepository()
    @Test
    fun `test login dengan email dan password SALAH harus mengembalikan Error`() = runBlocking {
        println("Mencoba login dengan data palsu...")

        val request = LoginRequest("hacker@email.com", "password_salah")
        // Eksekusi fungsi login
        val result = authRepo.login(request)

        // Cek hasilnya
        when (result) {
            is ResultState.Success -> {
                println("Aneh, kok berhasil masuk? Data: ${result.data}")
                assertTrue(false) // Sengaja disalahkan jika malah tembus
            }
            is ResultState.Error -> {
                println("✅ BENAR SEKALI! Ditolak oleh Supabase.")
                println("Pesan Error: ${result.message}")
                assertTrue(true)
            }
        }
    }

    @Test
    fun `test login dengan akun BENAR harus mengembalikan Success`() = runBlocking {

        val request = LoginRequest("test@perfomx.com", "admin1234")
        val result = authRepo.login(request)

        when (result) {
            is ResultState.Success -> {
                println("✅ LOGIN SUKSES!")
                println("Token Anda: ${result.data.accessToken.take(15)}...") // Ambil 15 huruf token saja
                assertTrue(true)
            }
            is ResultState.Error -> {
                println("❌ GAGAL: ${result.message}")
                assertTrue(false)
            }
        }
    }
}