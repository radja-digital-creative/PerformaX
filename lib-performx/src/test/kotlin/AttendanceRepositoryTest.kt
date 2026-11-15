import id.biz.yuka.libperformx.SupabaseConfig
import id.biz.yuka.libperformx.repository.AttendanceRepository
import id.biz.yuka.libperformx.repository.ScheduleRepository
import id.biz.yuka.libperformx.request.ProcessAttendanceRequest
import id.biz.yuka.libperformx.utils.ResultState
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.providers.builtin.Email
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.OffsetDateTime

class AttendanceRepositoryTest {

    private val scheduleRepository = ScheduleRepository()
    private val attendanceRepository = AttendanceRepository()
    private var testUserId: String = ""

    // @BeforeEach akan dijalankan otomatis SEBELUM setiap @Test di bawahnya.
    // Sangat cocok untuk menaruh logika Login Penyelundup kita.
    @BeforeEach
    fun setupAuthentication() = runBlocking {
        println("--- SETUP: Melakukan Login ---")
        try {
            SupabaseConfig.client.auth.signInWith(Email) {
                email = "test@perfomx.com" // Sesuaikan Email
                password = "admin1234"     // Sesuaikan Password
            }
            testUserId = SupabaseConfig.client.auth.currentUserOrNull()?.id ?: ""
            println("✅ Setup Login Berhasil! User ID: $testUserId")
        } catch (e: Exception) {
            println("❌ Setup Gagal: ${e.message}")
        }
    }

    @Test
    fun `test melakukan Check-In via RPC secara Dinamis`() = runBlocking {
        println("==> TEST: Simulasi Alur Check-In Harian <==")
        val today = LocalDate.now().toString() // "YYYY-MM-DD"

        // 1. CEK JADWAL HARI INI DULU
        println("1. Mengecek jadwal untuk hari ini ($today)...")
        val scheduleResult = scheduleRepository.getDailyAttendanceSchedule(testUserId, today)

        val validScheduleId = when (scheduleResult) {
            is ResultState.Success -> {
                if (scheduleResult.data == null) {
                    println("❌ GAGAL TEST: User ini tidak punya jadwal di tabel user_schedules untuk hari ini.")
                    assertTrue(false, "Buat dulu data jadwal hari ini di Supabase untuk user test Anda!")
                    return@runBlocking
                }
                println("✅ Jadwal ditemukan! Schedule ID: ${scheduleResult.data.id}")
                scheduleResult.data.id // Ambil ID jadwalnya
            }
            is ResultState.Error -> {
                println("❌ ERROR ngambil jadwal: ${scheduleResult.message}")
                assertTrue(false)
                return@runBlocking
            }
        }

        // 2. LAKUKAN CHECK-IN MENGGUNAKAN ID JADWAL TERSEBUT
        println("2. Menekan tombol Check-In...")
        val request = ProcessAttendanceRequest(
            p_user_id = testUserId,
            p_schedule_id = validScheduleId, // <-- Otomatis menggunakan ID dari database!
            p_latitude = -7.795580,
            p_longitude = 110.369490,
            p_image_url = "https://bucket/selfie.jpg",
            p_attendance_type = "check_in"
        )

        val result = attendanceRepository.processAttendance(request)

        when (result) {
            is ResultState.Success -> {
                val absen = result.data
                println("✅ CHECK-IN BERHASIL DIPROSES SERVER!")
                println("Status Masuk: ${absen.statusMasuk}")
                println("Poin Didapat: ${absen.poinDidapat}")
                assertTrue(absen.id.isNotBlank())
            }
            is ResultState.Error -> {
                println("❌ GAGAL CHECK-IN: ${result.message}")
                assertTrue(false)
            }
        }
    }
}