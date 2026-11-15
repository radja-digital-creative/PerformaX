import id.biz.yuka.libperformx.SupabaseConfig
import id.biz.yuka.libperformx.repository.ScheduleRepository
import id.biz.yuka.libperformx.request.CreateScheduleRequest
import id.biz.yuka.libperformx.utils.ResultState
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.providers.builtin.Email
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate

class ScheduleRepositoryTest {

    private val scheduleRepository = ScheduleRepository()

    @BeforeEach
    fun setup() = runBlocking {
        // Harus login sebagai Admin/Owner agar bisa insert ke user_schedules
        try {
            SupabaseConfig.client.auth.signInWith(Email) {
                email = "test@perfomx.com" // Pastikan akun ini punya role Admin/Owner
                password = "admin1234"
            }
            println("✅ Login Admin Berhasil")
        } catch (e: Exception) {
            println("❌ Login Gagal: ${e.message}")
        }
    }

    @Test
    fun `test menerbitkan jadwal baru untuk karyawan`() = runBlocking {
        println("==> TEST: Publish Jadwal Baru <==")

        // Sesuaikan UUID ini dengan data asli di database Anda
        val targetEmployeeId = "4075f1e0-2745-45ef-8fb9-91af5f3a6b43" // ID Joko
        val targetStoreId = "c6565c49-063a-4467-9c9e-2923984d7821"    // ID Toko
        val targetShiftId = "507663e8-5813-4318-80f4-a4f6d4a6f23b"    // ID Shift

        val request = CreateScheduleRequest(
            user_id = targetEmployeeId,
            store_id = targetStoreId,
            shift_id = targetShiftId,
            work_date = LocalDate.now().toString(), // Menerbitkan untuk hari ini
            notes = "Jadwal diterbitkan via Unit Test"
        )

        val result = scheduleRepository.createSchedule(request)

        when (result) {
            is ResultState.Success -> {
                println("✅ JADWAL BERHASIL DITERBITKAN!")
                println("ID Jadwal: ${result.data.id}")
                println("Tanggal: ${result.data.workDate}")
                assertTrue(result.data.id.isNotBlank())
            }
            is ResultState.Error -> {
                println("❌ GAGAL: ${result.message}")
                assertTrue(false)
            }
        }
    }
}