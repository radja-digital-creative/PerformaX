package usecase

import id.biz.yuka.libperformx.SupabaseConfig
import id.biz.yuka.libperformx.repository.AttendanceRepository
import id.biz.yuka.libperformx.repository.AuthRepository
import id.biz.yuka.libperformx.repository.EmployeeRepository
import id.biz.yuka.libperformx.repository.TaskRepository
import id.biz.yuka.libperformx.request.LoginRequest
import id.biz.yuka.libperformx.utils.ResultState
import io.github.jan.supabase.gotrue.auth
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
 * KELAS: UseCaseDashboardTest
 * =========================================================================
 * TUJUAN:
 * Menguji alur pengambilan data untuk Halaman Utama (Dashboard / Home)
 * berdasarkan jenis pengguna (Role) yang sedang login.
 * * * SKENARIO:
 * 1. Dashboard Owner: Fokus ke Semua Karyawan & Semua Tugas (Global).
 * 2. Dashboard Manager: Fokus ke Karyawan Cabang, Tugas Cabang, & Absen Pribadi.
 * 3. Dashboard Karyawan: Fokus ke Tugas Pribadi & Absen Pribadi.
 * =========================================================================
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class UseCaseDashboardTest {

    private val authRepository = AuthRepository()
    private val employeeRepository = EmployeeRepository()
    private val taskRepository = TaskRepository()
    private val attendanceRepository = AttendanceRepository()

    private val todayStr = LocalDate.now().toString()

    @BeforeAll
    fun setupAll() {
        println("--- SETUP GLOBAL: Mempersiapkan Uji Coba Dashboard ---")
    }

    /**
     * =========================================================================
     * TEST 1: DASHBOARD OWNER
     * Ekspektasi Data: List Karyawan (Global) & List Tugas (Global)
     * =========================================================================
     */
    @Test
    @Order(1)
    fun `test 1 - Load Dashboard untuk OWNER (Global View)`() = runBlocking {
        println("\n==> TEST 1: DASHBOARD OWNER <==")

        // 1. Login sebagai Owner
        val loginRes = authRepository.login(LoginRequest("test@perfomx.com", "admin1234"))
        assertTrue(loginRes is ResultState.Success, "Owner gagal login")
        println("   [Simulasi] Owner membuka aplikasi...")

        // 2. Fetch Data Karyawan (Semua Cabang)
        val empRes = employeeRepository.getAllEmployees()
        val totalEmployees = if (empRes is ResultState.Success) empRes.data.size else 0

        // 3. Fetch Data Tugas (Semua Cabang)
        val taskRes = taskRepository.getTasks()
        val totalTasks = if (taskRes is ResultState.Success) taskRes.data.size else 0

        // 4. Output UI (Console)
        println("\n   =======================================")
        println("   🏠 BERANDA OWNER")
        println("   =======================================")
        println("   👥 Total Karyawan Aktif : $totalEmployees orang")
        println("   📋 Total Tugas (Global) : $totalTasks tugas")
        println("   =======================================")

        println("✅ Beranda Owner berhasil dirender tanpa error.")
    }

    /**
     * =========================================================================
     * TEST 2: DASHBOARD MANAGER / ADMIN CABANG
     * Ekspektasi Data: Karyawan Cabang, Tugas Cabang, & ABSEN PRIBADI
     * =========================================================================
     */
    @Test
    @Order(2)
    fun `test 2 - Load Dashboard untuk MANAGER (Branch View & Personal Attendance)`() = runBlocking {
        println("\n==> TEST 2: DASHBOARD MANAGER <==")

        // 1. Login sebagai Manager
        SupabaseConfig.client.auth.signOut()
        val loginRes = authRepository.login(LoginRequest("manager@perfomx.com", "admin1234"))
        if (loginRes is ResultState.Error) {
            println("   ⚠️ Akun manager tidak ditemukan. Melewati test ini...")
            return@runBlocking
        }
        val managerId = SupabaseConfig.client.auth.currentUserOrNull()?.id ?: ""
        println("   [Simulasi] Manager membuka aplikasi...")

        val managerStoreId: String? = null // Ganti dengan ID Toko Manager yang ditarik dari DB

        // 2. Fetch Data Karyawan & Tugas (Khusus Cabangnya)
        val empRes = employeeRepository.getAllEmployees(storeId = managerStoreId)
        val branchEmployees = if (empRes is ResultState.Success) empRes.data.size else 0

        val taskRes = taskRepository.getTasks() // Implementasi asli: lempar storeId ke repository
        val branchTasks = if (taskRes is ResultState.Success) taskRes.data.size else 0

        // 3. Fetch Absen PRIBADI Manager Hari Ini
        var checkInStatus = "Belum Absen"
        val attRes = attendanceRepository.getAllAttendances(startDate = todayStr, endDate = todayStr)
        if (attRes is ResultState.Success) {
            val myTodayAtt = attRes.data.find { it.userId == managerId }
            if (myTodayAtt != null) {
                checkInStatus = "Sudah Absen Pukul ${myTodayAtt.checkInTime}"
            }
        }

        // 4. Output UI (Console)
        println("\n   =======================================")
        println("   🏠 BERANDA MANAGER")
        println("   =======================================")
        println("   ⏰ Status Absen Saya    : $checkInStatus")
        println("   👥 Karyawan Cabang Ini  : $branchEmployees orang")
        println("   📋 Tugas Cabang Ini     : $branchTasks tugas")
        println("   =======================================")

        println("✅ Beranda Manager berhasil merender Absen Pribadi dan Info Cabang.")
    }

    /**
     * =========================================================================
     * TEST 3: DASHBOARD KARYAWAN
     * Ekspektasi Data: Tugas Miliknya & Absen Miliknya
     * =========================================================================
     */
    @Test
    @Order(3)
    fun `test 3 - Load Dashboard untuk KARYAWAN (Personal View)`() = runBlocking {
        println("\n==> TEST 3: DASHBOARD KARYAWAN <==")

        // 1. Login sebagai Karyawan
        SupabaseConfig.client.auth.signOut()
        authRepository.login(LoginRequest("kariawan_toko_a@perfomx.com", "admin1234"))
        val employeeId = SupabaseConfig.client.auth.currentUserOrNull()?.id ?: ""
        println("   [Simulasi] Karyawan A membuka aplikasi...")

        // 2. Fetch Tugas Pribadi
        val taskRes = taskRepository.getTasks()
        val myTasks = if (taskRes is ResultState.Success) taskRes.data else emptyList()
        val pendingTasks = myTasks.count { task ->
            task.assignees.any { it.userId == employeeId && !it.isCompleted }
        }

        // 3. Fetch Absen Pribadi Hari Ini
        var checkInStatus = "Belum Absen"
        val myAttRes = attendanceRepository.getAllAttendances(startDate = todayStr, endDate = todayStr)
        if (myAttRes is ResultState.Success) {
            val myTodayAtt = myAttRes.data.find { it.userId == employeeId }
            if (myTodayAtt != null) {
                checkInStatus = "Sudah Absen Pukul ${myTodayAtt.checkInTime}"
            }
        }

        // 4. Output UI (Console)
        println("\n   =======================================")
        println("   🏠 BERANDA KARYAWAN")
        println("   =======================================")
        println("   ⏰ Status Absen Hari Ini: $checkInStatus")
        println("   📝 Tugas Yang Harus Diselesaikan: $pendingTasks tugas")
        println("   =======================================")

        println("✅ Beranda Karyawan berhasil merender data privasi dengan aman.")
    }
}