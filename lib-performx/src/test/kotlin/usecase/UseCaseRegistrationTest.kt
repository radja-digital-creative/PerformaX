package usecase

import id.biz.yuka.libperformx.SupabaseConfig
import id.biz.yuka.libperformx.repository.AuthRepository
import id.biz.yuka.libperformx.repository.EmployeeRepository
import id.biz.yuka.libperformx.repository.RoleRepository
import id.biz.yuka.libperformx.repository.ScheduleRepository
import id.biz.yuka.libperformx.repository.StoreRepository
import id.biz.yuka.libperformx.request.LoginRequest
import id.biz.yuka.libperformx.request.RegisterEmployeeRequest
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
 * KELAS: UseCaseRegistrationTest
 * =========================================================================
 * TUJUAN:
 * Menguji alur integrasi pendaftaran karyawan baru (HRIS).
 * Memastikan bahwa hierarki hak akses (Admin -> Manager -> Employee)
 * diterapkan dengan benar, terutama terkait kewajiban memiliki relasi Toko.
 * =========================================================================
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class UseCaseRegistrationTest {

    private val authRepository = AuthRepository()
    private val employeeRepository = EmployeeRepository()
    private val roleRepository = RoleRepository()
    private val storeRepository = StoreRepository()
    private val scheduleRepository = ScheduleRepository()

    // Variabel Global Data Master
    private var adminRoleId: Int = 0
    private var managerRoleId: Int = 0
    private var employeeRoleId: Int = 0

    private var targetStoreId: String = ""
    private var targetShiftId: String = ""

    @BeforeAll
    fun setupAll() = runBlocking {
        println("--- SETUP GLOBAL: Owner Login & Memuat Data Master ---")
        val loginResult = authRepository.login(LoginRequest("test@perfomx.com", "admin1234"))
        assertTrue(loginResult is ResultState.Success, "Owner gagal login!")

        // 1. Ambil Semua Role
        val rolesResult = roleRepository.getAllRoles()
        val allRoles = (rolesResult as ResultState.Success).data
        adminRoleId = allRoles.find { it.roleName.lowercase() == "admin" }?.id ?: 0
        managerRoleId = allRoles.find { it.roleName.lowercase() == "manager" }?.id ?: 0
        employeeRoleId = allRoles.find { it.roleName.lowercase() == "employee" }?.id ?: 0
        assertTrue(adminRoleId != 0 && managerRoleId != 0 && employeeRoleId != 0, "Data Role tidak lengkap di Database!")

        // 2. Ambil Data Toko & Shift Acak untuk Manager/Employee
        val allStores = (storeRepository.getAllStores() as ResultState.Success).data
        assertTrue(allStores.isNotEmpty(), "Tidak ada toko di database!")
        targetStoreId = allStores.random().id

        val shifts = (scheduleRepository.getShifts(targetStoreId) as ResultState.Success).data
        assertTrue(shifts.isNotEmpty(), "Toko target tidak memiliki shift!")
        targetShiftId = shifts.random().id

        println("✅ Setup selesai. Siap mendaftarkan jajaran tim.")
    }

    /**
     * =========================================================================
     * TEST 1: ADMIN PUSAT (GLOBAL ACCESS)
     * Admin tidak perlu diikat ke Toko maupun Shift.
     * =========================================================================
     */
    @Test
    @Order(1)
    fun `test 1 - Pendaftaran Admin (Tanpa Assign Toko)`() = runBlocking {
        println("\n==> TEST 1: PENDAFTARAN ADMIN <==")

        val adminUidResult = authRepository.getUserIdByEmail("admin@perfomx.com")
        assertTrue(adminUidResult is ResultState.Success, "Email admin belum terdaftar di Supabase Auth!")
        val adminUid = (adminUidResult as ResultState.Success).data

        val reqAdmin = RegisterEmployeeRequest(
            p_auth_uid = adminUid,
            p_role_id = adminRoleId,
            p_full_name = "Bapak Admin Pusat",
            p_gender = "male",
            p_address = "Kantor Pusat Performx",
            p_phone_number = "08111111111",
            p_id_card_url = "https://link/ktp.jpg",
            p_face_reference_url = "https://link/wajah.jpg",
            p_store_id = null, // ADMIN TIDAK PUNYA TOKO
            p_shift_id = null, // ADMIN TIDAK PUNYA SHIFT
            p_work_date = LocalDate.now().toString(),
            p_schedule_notes = "Global Access Admin",
            p_basic_salary = 8000000.0,
            p_transport_allowance = 1000000.0,
            p_meal_allowance = 1000000.0,
            p_position_allowance = 2000000.0,
            p_deduction_ids = emptyList()
        )

        val regAdmin = employeeRepository.createEmployee(reqAdmin)
        if (regAdmin is ResultState.Error) println("🚨 ERROR: ${regAdmin.message}")

        assertTrue(regAdmin is ResultState.Success)
        println("🎉 ADMIN sukses didaftarkan tanpa ikatan toko/shift!")
    }

    /**
     * =========================================================================
     * TEST 2: MANAGER CABANG
     * Manager wajib diikat ke Toko tertentu.
     * =========================================================================
     */
    @Test
    @Order(2)
    fun `test 2 - Pendaftaran Manager (Dengan Assign Toko)`() = runBlocking {
        println("\n==> TEST 2: PENDAFTARAN MANAGER <==")

        val managerUidResult = authRepository.getUserIdByEmail("manager@perfomx.com")
        assertTrue(managerUidResult is ResultState.Success, "Email manager belum terdaftar di Auth!")
        val managerUid = (managerUidResult as ResultState.Success).data

        val reqManager = RegisterEmployeeRequest(
            p_auth_uid = managerUid,
            p_role_id = managerRoleId,
            p_full_name = "Ibu Manager Toko",
            p_gender = "female",
            p_address = "Kota Cabang",
            p_phone_number = "08222222222",
            p_id_card_url = "https://link/ktp.jpg",
            p_face_reference_url = "https://link/wajah.jpg",
            p_store_id = targetStoreId, // MANAGER WAJIB PUNYA TOKO
            p_shift_id = targetShiftId, // MANAGER PUNYA SHIFT
            p_work_date = LocalDate.now().toString(),
            p_schedule_notes = "Shift Manajemen",
            p_basic_salary = 5000000.0,
            p_transport_allowance = 500000.0,
            p_meal_allowance = 500000.0,
            p_position_allowance = 1000000.0,
            p_deduction_ids = emptyList()
        )
        val regManager = employeeRepository.createEmployee(reqManager)
        if (regManager is ResultState.Error) println("🚨 ERROR: ${regManager.message}")

        assertTrue(regManager is ResultState.Success)
        println("🎉 MANAGER sukses didaftarkan di Toko ID: $targetStoreId!")
    }

    /**
     * =========================================================================
     * TEST 3: KARYAWAN STANDAR
     * =========================================================================
     */
    @Test
    @Order(3)
    fun `test 3 - Pendaftaran Karyawan Biasa`() = runBlocking {
        println("\n==> TEST 3: PENDAFTARAN KARYAWAN <==")

        val empUidResult = authRepository.getUserIdByEmail("kariawan_toko_a@perfomx.com")
        assertTrue(empUidResult is ResultState.Success, "Email karyawan belum terdaftar di Auth!")
        val empUid = (empUidResult as ResultState.Success).data

        val reqEmp = RegisterEmployeeRequest(
            p_auth_uid = empUid,
            p_role_id = employeeRoleId,
            p_full_name = "Karyawan Dinamis",
            p_gender = "male",
            p_address = "Wonosari",
            p_phone_number = "08333333333",
            p_id_card_url = "https://link/ktp.jpg",
            p_face_reference_url = "https://link/wajah.jpg",
            p_store_id = targetStoreId,
            p_shift_id = targetShiftId,
            p_work_date = LocalDate.now().toString(),
            p_schedule_notes = "Hari pertama kerja",
            p_basic_salary = 3000000.0,
            p_transport_allowance = 200000.0,
            p_meal_allowance = 300000.0,
            p_position_allowance = 0.0,
            p_deduction_ids = emptyList()
        )
        val regEmp = employeeRepository.createEmployee(reqEmp)
        if (regEmp is ResultState.Error) println("🚨 ERROR: ${regEmp.message}")

        assertTrue(regEmp is ResultState.Success)
        println("🎉 KARYAWAN sukses didaftarkan!")
    }
}