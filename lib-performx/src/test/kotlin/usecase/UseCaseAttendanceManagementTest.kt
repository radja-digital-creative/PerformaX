package usecase

import id.biz.yuka.libperformx.SupabaseConfig
import id.biz.yuka.libperformx.repository.AttendanceRepository
import id.biz.yuka.libperformx.repository.AuthRepository
import id.biz.yuka.libperformx.repository.ScheduleRepository
import id.biz.yuka.libperformx.repository.StoreRepository
import id.biz.yuka.libperformx.request.AnnulAttendanceRequest
import id.biz.yuka.libperformx.request.LoginRequest
import id.biz.yuka.libperformx.utils.ResultState
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestMethodOrder
import java.time.LocalDate
import java.time.OffsetDateTime

@kotlinx.serialization.Serializable
data class ExistingAttendanceTarget(
    val id: String,
    val check_in_image_url: String? = null
)

/**
 * =========================================================================
 * KELAS: UseCaseAttendanceManagementTest
 * =========================================================================
 * TUJUAN:
 * Menguji alur kerja HR/Owner dalam memantau dan menindak absensi karyawan.
 * Dipecah menjadi fungsi-fungsi modular agar lebih terorganisir.
 * =========================================================================
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class UseCaseAttendanceManagementTest {

    private val authRepository = AuthRepository()
    private val attendanceRepository = AttendanceRepository()
    private val scheduleRepository = ScheduleRepository()

    // ==========================================
    // SHARED STATE (Variabel Antar Test)
    // ==========================================
    private var ownerId: String = ""
    private var targetUid: String = ""
    private var targetScheduleId: String = ""
    private var targetAttendanceId: String = ""

    private val todayStr = LocalDate.now().toString()

    // Flag untuk Clean Up
    private var isUsingDummyData: Boolean = false
    private var originalImageUrl: String? = null

    // ==========================================
    // SETUP GLOBAL: Login & Cari Karyawan Target
    // ==========================================
    @BeforeAll
    fun setupAll() = runBlocking {
        println("--- SETUP GLOBAL: Owner Login ---")
        val loginResult = authRepository.login(LoginRequest("test@perfomx.com", "admin1234"))
        assertTrue(loginResult is ResultState.Success, "Owner gagal login!")
        ownerId = SupabaseConfig.client.auth.currentUserOrNull()?.id ?: ""

        val targetUserResult = authRepository.getUserIdByEmail("kariawan_toko_a@perfomx.com")
        assertTrue(targetUserResult is ResultState.Success)
        targetUid = (targetUserResult as ResultState.Success).data

        val scheduleRes = scheduleRepository.getDailyAttendanceSchedule(targetUid, todayStr)
        val schedule = (scheduleRes as ResultState.Success).data
        assertNotNull(schedule, "Karyawan target tidak memiliki jadwal hari ini!")
        targetScheduleId = schedule!!.id

        println("✅ Setup Selesai. Target Uji: Karyawan A.")
    }

    // ==========================================
    // TEST 1: SIAPKAN DATA ABSENSI (ASLI / DUMMY)
    // ==========================================
    @Test
    @Order(1)
    fun `test 1 - Persiapan Data Absensi (Injeksi Dummy Fake GPS)`() = runBlocking {
        println("\n==> TEST 1: PENYIAPAN DATA ABSENSI <==")

        // Cek apakah Karyawan sudah absen hari ini
        val existingAttendance = try {
            SupabaseConfig.client.postgrest["attendances"].select {
                filter {
                    eq("user_id", targetUid)
                    eq("schedule_id", targetScheduleId)
                    eq("attendance_date", todayStr)
                }
            }.decodeSingleOrNull<ExistingAttendanceTarget>() // ✅ PAKAI CLASS BARU
        } catch (e: Exception) {
            println("   ⚠️ Peringatan Pengecekan: ${e.message}") // Agar error tidak bisu lagi
            null
        }

        if (existingAttendance != null) {
            println("   ℹ️ Karyawan SUDAH ABSEN. Menggunakan data asli & diubah ke Fake GPS...")
            targetAttendanceId = existingAttendance.id // ✅ Sesuaikan pemanggilan properti
            originalImageUrl = existingAttendance.check_in_image_url
            isUsingDummyData = false

            // Ubah gambar menjadi gambar fake gps
            val updateFakeGps = mapOf("check_in_image_url" to "https://fake_gps_image.jpg")
            SupabaseConfig.client.postgrest["attendances"].update(updateFakeGps) {
                filter { eq("id", targetAttendanceId) }
            }
        } else {
            println("   ℹ️ Karyawan BELUM ABSEN. Membuat absen Dummy Fake GPS...")
            isUsingDummyData = true

            val dummyReq = mapOf(
                "user_id" to targetUid,
                "schedule_id" to targetScheduleId,
                "attendance_date" to todayStr,
                "check_in_time" to OffsetDateTime.now().toString(),
                "check_in_status" to "Tepat Waktu",
                "check_in_image_url" to "https://fake_gps_image.jpg"
            )
            val insertedDummy = SupabaseConfig.client.postgrest["attendances"]
                .insert(dummyReq) { select() }
                .decodeSingle<ExistingAttendanceTarget>() // ✅ PADA INSERT JUGA GUNAKAN CLASS BARU

            targetAttendanceId = insertedDummy.id // ✅ Sesuaikan pemanggilan properti
        }
        println("✅ Data absensi siap di-sidak. (ID: $targetAttendanceId)")
    }

    // ==========================================
    // TEST 2: HRD MEMANTAU LAPORAN ABSENSI
    // ==========================================
    @Test
    @Order(2)
    fun `test 2 - HRD Membaca Laporan Absensi Harian`() = runBlocking {
        println("\n==> TEST 2: HRD MEMBUKA LAPORAN ABSENSI <==")
        val listRes = attendanceRepository.getAllAttendances(startDate = todayStr, endDate = todayStr)
        assertTrue(listRes is ResultState.Success)

        val attendancesToday = (listRes as ResultState.Success).data
        val testAbsen = attendancesToday.find { it.id == targetAttendanceId }

        assertNotNull(testAbsen, "Absen karyawan tidak muncul di laporan!")
        println("✅ Ditemukan absensi milik: ${testAbsen!!.userName} (${testAbsen.checkInStatus})")
    }

    // ==========================================
    // TEST 3: HRD MENINDAK (MENGANULIR ABSENSI)
    // ==========================================
    @Test
    @Order(3)
    fun `test 3 - HRD Menganulir Absensi (Intervensi)`() = runBlocking {
        println("\n==> TEST 3: HRD MENGANULIR ABSENSI KARENA FAKE GPS <==")

        val annulReq = AnnulAttendanceRequest(
            is_annulled = true,
            annulled_by = ownerId,
            annul_reason = "Terdeteksi menggunakan aplikasi Fake GPS. Fix it boy !"
        )
        val annulRes = attendanceRepository.annulAttendance(targetAttendanceId, annulReq)

        assertTrue(annulRes is ResultState.Success, "Gagal menganulir absensi!")
        println("✅ Absensi berhasil dianulir!")
    }

    // ==========================================
    // TEST 4: VERIFIKASI HASIL ANULIR
    // ==========================================
    @Test
    @Order(4)
    fun `test 4 - Memastikan Status Anulir Tersimpan`() = runBlocking {
        println("\n==> TEST 4: VERIFIKASI HASIL ANULIR <==")

        val detailRes = attendanceRepository.getAttendanceDetail(targetAttendanceId)
        assertTrue(detailRes is ResultState.Success)
        val finalDetail = (detailRes as ResultState.Success).data

        println("   - Dianulir Oleh: ${finalDetail.annulledByName}")
        println("   - Alasan: ${finalDetail.annulReason}")

        assertTrue(finalDetail.isAnnulled, "Status is_annulled harusnya True!")
        assertTrue(finalDetail.annulReason.contains("Fake GPS"), "Alasan anulir tidak cocok!")
        println("✅ Verifikasi sukses. Data anulir terkunci di sistem.")
    }

    // ==========================================
    // TEST 5: CLEAN UP (KEMBALIKAN SEPERTI SEMULA)
    // ==========================================
    @Test
    @Order(5)
    fun `test 5 - Clean Up & Restore Database State`() = runBlocking {
        println("\n==> TEST 5: CLEAN UP (RESTORE DATABASE) <==")

        if (isUsingDummyData) {
            println("   🧹 Menghapus data absen Dummy sepenuhnya...")
            SupabaseConfig.client.postgrest["attendances"].delete {
                filter { eq("id", targetAttendanceId) }
            }
        } else {
            println("   🧹 Mengembalikan absen asli Karyawan (Un-Annul & Restore Foto)...")
            val restoreReq = mapOf(
                "is_annulled" to false,
                "annulled_by" to null,
                "annul_reason" to null,
                "check_in_image_url" to originalImageUrl
            )
            SupabaseConfig.client.postgrest["attendances"].update(restoreReq) {
                filter { eq("id", targetAttendanceId) }
            }
        }
        println("🎉 SIKLUS MONITORING ABSENSI BERHASIL SEMPURNA!")
    }
}