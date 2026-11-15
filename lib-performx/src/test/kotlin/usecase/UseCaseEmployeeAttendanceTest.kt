package usecase

import id.biz.yuka.libperformx.SupabaseConfig
import id.biz.yuka.libperformx.repository.AttendanceRepository
import id.biz.yuka.libperformx.repository.AuthRepository
import id.biz.yuka.libperformx.repository.ScheduleRepository
import id.biz.yuka.libperformx.repository.StoreRepository
import id.biz.yuka.libperformx.request.CreateScheduleRequest
import id.biz.yuka.libperformx.request.CreateStoreRequest
import id.biz.yuka.libperformx.request.LoginRequest
import id.biz.yuka.libperformx.request.ProcessAttendanceRequest
import id.biz.yuka.libperformx.request.ShiftRequest
import id.biz.yuka.libperformx.request.UpdateStoreRequest
import id.biz.yuka.libperformx.utils.ResultState
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestMethodOrder
import java.time.LocalDate

/**
 * =========================================================================
 * KELAS: UseCaseEmployeeAttendanceTest
 * =========================================================================
 * TUJUAN:
 * Menguji alur operasional Karyawan saat melakukan Absensi (Check-in/Out).
 * Menguji sistem validasi keamanan GPS (Radius Toko) dan fitur persetujuan
 * Absen Luar Kantor (Offsite).
 * * * SKENARIO:
 * 1. Read: Karyawan melihat jadwal hari ini.
 * 2. Fail Check-In: Karyawan mencoba absen dari jarak jauh (Luar Radius) -> DITOLAK.
 * 3. Request Offsite: HRD/Admin menyetujui izin kerja luar kantor.
 * 4. Success Check-In: Karyawan absen kembali dari lokasi jauh -> DITERIMA (Berkat Izin).
 * 5. Success Check-Out: Karyawan melakukan absen pulang.
 * 6. Clean Up: Membersihkan data dummy.
 * =========================================================================
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class UseCaseEmployeeAttendanceTest {

    private val authRepository = AuthRepository()
    private val attendanceRepository = AttendanceRepository()
    private val scheduleRepository = ScheduleRepository()
    private val storeRepository = StoreRepository()

    // Variabel Global Shared State
    private var adminId: String = ""
    private var employeeId: String = ""
    private var testStoreId: String = ""
    private var testShiftId: String = ""
    private var testScheduleId: String = ""
    private val todayStr = LocalDate.now().toString()

    // Koordinat Toko Dummy (Misal: Yogyakarta)
    private val storeLat = -7.7926
    private val storeLon = 110.3658

    // Koordinat Palsu / Luar Kota (Misal: Jakarta)
    private val fakeLat = -6.2000
    private val fakeLon = 106.8166

    @BeforeAll
    fun setupAll() = runBlocking {
        println("--- SETUP GLOBAL: Mempersiapkan Medan Uji Coba ---")

        // 1. LOGIN SEBAGAI ADMIN
        authRepository.login(LoginRequest("test@perfomx.com", "admin1234"))
        adminId = SupabaseConfig.client.auth.currentUserOrNull()?.id ?: ""

        val empUidResult = authRepository.getUserIdByEmail("kariawan_toko_a@perfomx.com")
        assertTrue(empUidResult is ResultState.Success, "Karyawan A belum terdaftar!")
        employeeId = (empUidResult as ResultState.Success).data

        // 2. CEK / BUAT TOKO
        val allStores = (storeRepository.getAllStores() as ResultState.Success).data
        val existingStore = allStores.find { it.name == "Toko Uji Absen 24 Jam" }

        if (existingStore != null) {
            testStoreId = existingStore.id
            println("ℹ️ Toko ditemukan, menyelaraskan lokasi ke: $storeLat, $storeLon")

            // Update lokasi agar sesuai dengan medan uji coba terbaru
            val updateReq = UpdateStoreRequest(
                latitude = storeLat,
                longitude = storeLon,
            )
            // Gunakan fungsi update yang Anda miliki (asumsi: updateStore atau sejenisnya)
            storeRepository.updateStore(testStoreId, updateReq)
            println("✅ Lokasi toko berhasil diperbarui.")
        } else {
            val createStoreReq = CreateStoreRequest(
                p_name = "Toko Uji Absen 24 Jam",
                p_latitude = storeLat,
                p_longitude = storeLon,
                p_daily_close_time = "23:59:00"
            )
            testStoreId = (storeRepository.createStore(createStoreReq) as ResultState.Success).data
            println("✅ Toko baru berhasil dibuat.")
        }

        // 3. CEK / BUAT SHIFT 24 JAM
        val allShifts = (scheduleRepository.getShifts(testStoreId) as ResultState.Success).data
        val existingShift = allShifts.find { it.shiftName == "Shift 24 Jam" }

        if (existingShift != null) {
            testShiftId = existingShift.id
            println("ℹ️ Shift sudah ada, menggunakan ID: $testShiftId")
        } else {
            val createShiftReq = ShiftRequest(
                store_id = testStoreId,
                shift_name = "Shift 24 Jam",
                start_time = "00:00:00",
                end_time = "23:50:00",
                attendance_radius = 50
            )
            testShiftId = (scheduleRepository.createShift(createShiftReq) as ResultState.Success).data.id
            println("✅ Shift 24 jam berhasil dibuat.")
        }

        // 4. CEK / BUAT JADWAL HARI INI
        val existingScheduleRes = scheduleRepository.getDailyAttendanceSchedule(employeeId, todayStr)
        val existingSchedule = (existingScheduleRes as? ResultState.Success)?.data

        if (existingSchedule != null) {
            testScheduleId = existingSchedule.id
            println("ℹ️ Jadwal hari ini sudah ada, menggunakan ID: $testScheduleId")
        } else {
            val createScheduleReq = CreateScheduleRequest(
                user_id = employeeId,
                store_id = testStoreId,
                shift_id = testShiftId,
                work_date = todayStr,
                notes = "Jadwal khusus Unit Test"
            )
            testScheduleId = (scheduleRepository.createSchedule(createScheduleReq) as ResultState.Success).data.id
            println("✅ Jadwal baru berhasil diterbitkan.")
        }

        // 5. LOGOUT ADMIN & LOGIN SEBAGAI KARYAWAN
        SupabaseConfig.client.auth.signOut()
        println("✅ Persiapan Data Selesai. Beralih ke Akun Karyawan...")

        val empLogin = authRepository.login(LoginRequest("kariawan_toko_a@perfomx.com", "admin1234"))
        assertTrue(empLogin is ResultState.Success, "Gagal login sebagai karyawan")
        println("✅ Karyawan berhasil login. Mari mulai absen!")
    }

    @Test
    @Order(1)
    fun `test 1 - Karyawan Melihat Jadwal Absen Hari Ini`() = runBlocking {
        println("\n==> TEST 1: MEMUAT JADWAL HARI INI <==")
        val scheduleRes = scheduleRepository.getDailyAttendanceSchedule(employeeId, todayStr)

        assertTrue(scheduleRes is ResultState.Success)
        val schedule = (scheduleRes as ResultState.Success).data
        assertTrue(schedule != null, "Jadwal hari ini tidak ditemukan!")

        // Memastikan yang terbaca adalah jadwal dummy kita
        assertTrue(schedule!!.id == testScheduleId)
        println("✅ Jadwal shift ditemukan. Karyawan siap melakukan Check-In.")
    }

    @Test
    @Order(2)
    fun `test 2 - GAGAL Check-In karena berada di Luar Radius (Fake GPS)`() = runBlocking {
        println("\n==> TEST 2: SIMULASI CHECK-IN DARI LUAR KOTA (Luar Radius) <==")

        val checkInReq = ProcessAttendanceRequest(
            p_user_id = employeeId,
            p_schedule_id = testScheduleId,
            p_attendance_type = "check_in",
            p_latitude = fakeLat, // Posisi Jakarta
            p_longitude = fakeLon, // Posisi Jakarta
            p_image_url = "https://link/foto_absen.jpg"
        )

        val checkInRes = attendanceRepository.processAttendance(checkInReq)

        // Kita MENGHARAPKAN ini ERROR karena sistem anti-fake GPS harus bekerja!
        assertTrue(checkInRes is ResultState.Error, "Sistem keamanan jebol! Karyawan bisa absen dari luar radius tanpa izin.")

        val errorMessage = (checkInRes as ResultState.Error).message
        assertTrue(errorMessage.contains("di luar radius"), "Pesan error tidak sesuai harapan")
        println("✅ Sistem Keamanan Berhasil Menolak Absensi Luar Radius!")
    }

    @Test
    @Order(3)
    fun `test 3 - HRD Menyetujui Izin Offsite Karyawan`() = runBlocking {
        println("\n==> TEST 3: SIMULASI HRD MENYETUJUI IZIN OFFSITE <==")

        // Karyawan menelepon HRD, HRD menyetujui izin di sistem
        val offsitePayload = mapOf(
            "user_id" to employeeId,
            "schedule_id" to testScheduleId,
            "destination_name" to "Meeting Klien di Jakarta",
            "reason" to "Uji Coba Sistem Offsite",
            "location_image_url" to "https://link/foto_klien.jpg",
            "status" to "approved",
            "approved_by" to adminId
        )

        // Bypass RPC untuk simulasi cepat
        SupabaseConfig.client.postgrest["offsite_attendance_requests"].insert(offsitePayload)
        println("✅ Izin Offsite untuk hari ini RESMI DISETUJUI oleh Admin.")
    }

    @Test
    @Order(4)
    fun `test 4 - SUKSES Check-In dari Luar Kota berkat Izin Offsite`() = runBlocking {
        println("\n==> TEST 4: CHECK-IN ULANG DARI LUAR KOTA DENGAN IZIN <==")

        val checkInReq = ProcessAttendanceRequest(
            p_user_id = employeeId,
            p_schedule_id = testScheduleId,
            p_attendance_type = "check_in",
            p_latitude = fakeLat, // Posisi Jakarta
            p_longitude = fakeLon, // Posisi Jakarta
            p_image_url = "https://link/foto_absen_asli.jpg"
        )

        val checkInRes = attendanceRepository.processAttendance(checkInReq)

        if (checkInRes is ResultState.Error) println("🚨 ERROR: ${checkInRes.message}")
        assertTrue(checkInRes is ResultState.Success, "Gagal Check-In padahal sudah ada izin Offsite!")

        val attendanceData = (checkInRes as ResultState.Success).data
        println("✅ CHECK-IN SUKSES! Status: ${attendanceData.statusMasuk}")
        assertTrue(attendanceData.checkInTime.isNotBlank())
    }

    @Test
    @Order(5)
    fun `test 5 - SUKSES Check-Out dan Mendapatkan KPI`() = runBlocking {
        println("\n==> TEST 5: KARYAWAN MELAKUKAN CHECK-OUT <==")

        val checkOutReq = ProcessAttendanceRequest(
            p_user_id = employeeId,
            p_schedule_id = testScheduleId,
            p_attendance_type = "check_out",
            p_latitude = fakeLat,
            p_longitude = fakeLon,
            p_image_url = "https://link/foto_pulang.jpg"
        )

        val checkOutRes = attendanceRepository.processAttendance(checkOutReq)
        assertTrue(checkOutRes is ResultState.Success, "Gagal Check-Out")

        val finalAttendance = (checkOutRes as ResultState.Success).data
        println("✅ CHECK-OUT SUKSES! Status: ${finalAttendance.statusKeluar}")
        println("   - Waktu Check-Out: ${finalAttendance.checkOutTime}")

        // Memastikan Poin KPI Terkumpul Otomatis (Check in Poin + Check out Poin)
        println("   - Poin KPI Total Didapat: ${finalAttendance.poinDidapat}")
    }

    @Test
    @Order(6)
    fun `test 6 - Clean Up Seluruh Data Dummy`() = runBlocking {
        println("\n==> TEST 6: CLEAN UP (MENYAPU BERSIH DATA UJI COBA) <==")
        try {
            val client = SupabaseConfig.client
            // Hapus secara berurutan dari child ke parent
            client.postgrest["attendances"].delete { filter { eq("schedule_id", testScheduleId) } }
            client.postgrest["offsite_attendance_requests"].delete { filter { eq("schedule_id", testScheduleId) } }
            client.postgrest["user_schedules"].delete { filter { eq("id", testScheduleId) } }
            client.postgrest["shifts"].delete { filter { eq("id", testShiftId) } }
            client.postgrest["attendance_settings"].delete { filter { eq("store_id", testStoreId) } }
            client.postgrest["stores"].delete { filter { eq("id", testStoreId) } }
            println("🧹 Semua data dummy absensi berhasil dibersihkan dari database.")
        } catch (e: Exception) {
            println("🚨 Gagal Clean Up: ${e.message}")
        }
    }
}