package usecase

import id.biz.yuka.libperformx.SupabaseConfig
import id.biz.yuka.libperformx.entity.EmployeeEntity
import id.biz.yuka.libperformx.repository.AuthRepository
import id.biz.yuka.libperformx.repository.EmployeeRepository
import id.biz.yuka.libperformx.repository.RoleRepository
import id.biz.yuka.libperformx.repository.ScheduleRepository
import id.biz.yuka.libperformx.repository.StoreRepository
import id.biz.yuka.libperformx.request.LoginRequest
import id.biz.yuka.libperformx.request.RegisterEmployeeRequest
import id.biz.yuka.libperformx.request.UpdateEmployeeRequest
import id.biz.yuka.libperformx.response.EmployeeResponse
import id.biz.yuka.libperformx.utils.ResultState
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestMethodOrder
import java.time.LocalDate

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class UseCaseEmployeeManagementTest {

    private val authRepository = AuthRepository()
    private val employeeRepository = EmployeeRepository()
    private val roleRepository = RoleRepository()
    private val storeRepository = StoreRepository()
    private val scheduleRepository = ScheduleRepository()

    private val targetTestEmail = "kariawan_toko_b@perfomx.com"

    @BeforeAll
    fun setupAll() = runBlocking {
        println("--- SETUP GLOBAL: Login HR / Admin ---")
        val loginResult = authRepository.login(LoginRequest("test@perfomx.com", "admin1234"))
        assertTrue(loginResult is ResultState.Success, "Admin gagal login!")
        println("✅ Admin berhasil login. Siap mengelola karyawan.")
    }

    // =========================================================================
    // FUNGSI HELPER: MENGAMBIL 1 KARYAWAN ACAK
    // =========================================================================
    private suspend fun getRandomActiveEmployee(): EmployeeEntity? {
        val listRes = employeeRepository.getAllEmployees()
        if (listRes is ResultState.Success && listRes.data.isNotEmpty()) {
            // Mengambil satu data secara acak dari daftar
            return listRes.data.random()
        }
        return null
    }

    // =========================================================================
    // TEST 1: CREATE (Registrasi Akun Test Khusus)
    // =========================================================================
    @Test
    @Order(1)
    fun `test 1 - Registrasi Karyawan Baru (Create)`() = runBlocking {
        println("\n==> 1. REGISTRASI KARYAWAN BARU <==")

        val allRoles = (roleRepository.getAllRoles() as ResultState.Success).data
        val allStores = (storeRepository.getAllStores() as ResultState.Success).data
        val roleEmployee = allRoles.find { it.roleName.lowercase() == "employee" }!!
        val storeAwal = allStores[0]
        val shiftAwal = (scheduleRepository.getShifts(storeAwal.id) as ResultState.Success).data.random()

        val targetUidResult = authRepository.getUserIdByEmail(targetTestEmail)
        assertTrue(targetUidResult is ResultState.Success, "Email test tidak ditemukan di Auth!")
        val targetUid = (targetUidResult as ResultState.Success).data

        // Clean up data lama agar tidak error duplicate
        hardDeleteForTestCleanUp(targetUid)

        val createReq = RegisterEmployeeRequest(
            p_auth_uid = targetUid,
            p_role_id = roleEmployee.id,
            p_full_name = "Budi (Karyawan Test)",
            p_gender = "male",
            p_address = "Jl. Baru Masuk Kerja",
            p_phone_number = "08555555555",
            p_id_card_url = "https://link/ktp.jpg",
            p_face_reference_url = "https://link/wajah.jpg",
            p_store_id = storeAwal.id,
            p_shift_id = shiftAwal.id,
            p_work_date = LocalDate.now().toString(),
            p_schedule_notes = "Hari Pertama Kerja",
            p_basic_salary = 2000000.0,
            p_transport_allowance = 100000.0,
            p_meal_allowance = 100000.0,
            p_position_allowance = 0.0,
            p_deduction_ids = emptyList()
        )

        val createRes = employeeRepository.createEmployee(createReq)
        assertTrue(createRes is ResultState.Success, "Gagal Registrasi Karyawan")
        println("✅ Berhasil Mendaftarkan Karyawan: Budi (Gaji Pokok: Rp 2.000.000)")
    }

    // =========================================================================
    // TEST 2: READ (Melihat Detail 1 Karyawan Acak)
    // =========================================================================
    @Test
    @Order(2)
    fun `test 2 - Membaca Detail Salah Satu Karyawan Secara Acak (Read)`() = runBlocking {
        println("\n==> 2. MEMBACA DETAIL KARYAWAN (RANDOM) <==")

        val randomEmp = getRandomActiveEmployee()
        assertNotNull(randomEmp, "Tabel karyawan kosong, tidak ada yang bisa dibaca!")

        println("🔍 Terpilih secara acak UID: ${randomEmp!!.id}")

        val detailRes = employeeRepository.getEmployeeById(randomEmp.id)
        assertTrue(detailRes is ResultState.Success, "Gagal Membaca Detail Profil")
        val detail = (detailRes as ResultState.Success).data

        println("✅ Berhasil memuat detail:")
        println("   - Nama: ${detail.fullName}")
        println("   - Jabatan: ${detail.roleName}")
        println("   - Toko: ${detail.stores.firstOrNull()?.name ?: "Belum punya toko"}")
        println("   - Gaji Pokok: Rp ${detail.basicSalary}")
    }

    // =========================================================================
    // TEST 3: UPDATE (Mutasi/Promosi Karyawan Acak)
    // =========================================================================
    @Test
    @Order(3)
    fun `test 3 - Update Data Karyawan Secara Acak (Update)`() = runBlocking {
        println("\n==> 3. UPDATE / MUTASI KARYAWAN (RANDOM) <==")

        val randomEmp = getRandomActiveEmployee()
        assertNotNull(randomEmp, "Tabel karyawan kosong, tidak ada yang bisa di-update!")

        val allRoles = (roleRepository.getAllRoles() as ResultState.Success).data
        val allStores = (storeRepository.getAllStores() as ResultState.Success).data

        // Pilih role dan toko secara acak untuk disimulasikan sebagai mutasi
        val randomRole = allRoles.random()
        val randomStore = allStores.random()

        println("📝 Mengubah data milik: ${randomEmp!!.fullName}")

        val updateReq = UpdateEmployeeRequest(
            p_user_id = randomEmp.id,
            p_role_id = randomRole.id,
            p_full_name = "${randomEmp.fullName} (Updated)",
            p_gender = randomEmp.gender,
            p_address = "Jl. Sukses Selalu (Baru)",
            p_phone_number = "08111222333",
            p_store_id = randomStore.id,
            p_basic_salary = 5000000.0,
            p_transport_allowance = 250000.0,
            p_meal_allowance = 300000.0,
            p_position_allowance = 1000000.0,
            p_deduction_ids = emptyList()
        )

        val updateRes = employeeRepository.updateEmployee(updateReq)
        assertTrue(updateRes is ResultState.Success, "Gagal Update Karyawan")
        println("✅ Berhasil Update! Jabatan diubah menjadi: ${randomRole.roleName}, dipindah ke: ${randomStore.name}")
    }

    // =========================================================================
    // TEST 4: DELETE (Soft Delete Karyawan Acak)
    // =========================================================================
    @Test
    @Order(4)
    fun `test 4 - Memberhentikan Karyawan Secara Acak (Soft Delete)`() = runBlocking {
        println("\n==> 4. MEMBERHENTIKAN KARYAWAN (RANDOM) <==")

        val randomEmp = getRandomActiveEmployee()
        assertNotNull(randomEmp, "Tabel karyawan kosong, tidak ada yang bisa dihapus!")

        println("🗑️ Men-soft-delete karyawan: ${randomEmp!!.fullName}")

        val softDeleteRes = employeeRepository.deleteEmployee(randomEmp.id)
        assertTrue(softDeleteRes is ResultState.Success, "Gagal Soft Delete")

        println("✅ Berhasil! Karyawan telah dinonaktifkan dari sistem (is_active = false).")
    }

    /**
     * FUNGSI HELPER: Clean Up Hard Delete
     */
    private suspend fun hardDeleteForTestCleanUp(uid: String) {
        try {
            val client = SupabaseConfig.client
            val salaryRes = client.postgrest["user_salaries"].select { filter { eq("user_id", uid) } }.decodeSingleOrNull<Map<String, String>>()
            if (salaryRes != null) {
                client.postgrest["user_salary_deductions"].delete { filter { eq("user_salary_id", salaryRes["id"]!!) } }
                client.postgrest["user_salaries"].delete { filter { eq("id", salaryRes["id"]!!) } }
            }
            client.postgrest["user_schedules"].delete { filter { eq("user_id", uid) } }
            client.postgrest["user_stores"].delete { filter { eq("user_id", uid) } }
            client.postgrest["profiles"].delete { filter { eq("id", uid) } }
        } catch (e: Exception) {
            // Abaikan jika tidak ada
        }
    }
}