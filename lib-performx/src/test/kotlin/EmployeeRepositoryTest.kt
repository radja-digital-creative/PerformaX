package id.biz.yuka.libperformx

import id.biz.yuka.libperformx.repository.EmployeeRepository
import id.biz.yuka.libperformx.request.UpdateEmployeeRequest
import id.biz.yuka.libperformx.utils.ResultState
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.providers.builtin.Email
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class EmployeeRepositoryTest {

    private val employeeRepository = EmployeeRepository()

    @BeforeEach
    fun setup() = runBlocking {
        println("--- SETUP: Login sebagai Admin/HR ---")
        try {
            SupabaseConfig.client.auth.signInWith(Email) {
                email = "test@perfomx.com"
                password = "admin1234"
            }
        } catch (e: Exception) {
            println("❌ Login Gagal: ${e.message}")
        }
    }

    @Test
    fun `test siklus HRIS Karyawan (GetAll, GetDetail, Update)`() = runBlocking {
        // GANTI INI DENGAN UUID KARYAWAN ASLI DI DATABASE ANDA
        val targetEmployeeId = "4075f1e0-2745-45ef-8fb9-91af5f3a6b43"
        val targetStoreId = "c6565c49-063a-4467-9c9e-2923984d7821"

        println("\n==> 1. TEST GET ALL <==")
        val getAllResult = employeeRepository.getAllEmployees()
        when (getAllResult) {
            is ResultState.Success -> {
                println("✅ Berhasil mengambil ${getAllResult.data.size} karyawan aktif.")
                assertTrue(getAllResult.data.isNotEmpty())
            }
            is ResultState.Error -> {
                println("❌ GAGAL GET ALL: ${getAllResult.message}")
                assertTrue(false)
            }
        }

        println("\n==> 2. TEST GET BY ID <==")
        val getDetailResult = employeeRepository.getEmployeeById(targetEmployeeId)
        when (getDetailResult) {
            is ResultState.Success -> {
                println("✅ Detail ditemukan: ${getDetailResult.data.fullName}")
                println("Gaji Pokok: Rp ${getDetailResult.data.basicSalary}")
                println("Total Tunjangan: Rp ${getDetailResult.data.totalAllowances}")
                assertTrue(getDetailResult.data.id == targetEmployeeId)
            }
            is ResultState.Error -> {
                println("❌ GAGAL GET DETAIL: ${getDetailResult.message}")
                assertTrue(false)
            }
        }

        println("\n==> 3. TEST UPDATE (Naik Gaji via RPC) <==")
        val updateRequest = UpdateEmployeeRequest(
            p_user_id = targetEmployeeId,
            p_role_id = 2, // Sesuaikan dengan ID role di tabel Anda
            p_full_name = "Joko (Sudah Naik Gaji)",
            p_gender = "Laki-laki",
            p_address = "Semanu, Gunungkidul",
            p_phone_number = "08111222333",
            p_store_id = targetStoreId,
            p_basic_salary = 5000000.0, // Naik gaji!
            p_transport_allowance = 500000.0,
            p_meal_allowance = 500000.0,
            p_position_allowance = 0.0,
            p_deduction_ids = emptyList() // Kosong
        )

        val updateResult = employeeRepository.updateEmployee(updateRequest)
        if (updateResult is ResultState.Success && updateResult.data) {
            println("✅ Update Karyawan & Gaji Berhasil!")
            assertTrue(true)
        } else {
            println("❌ GAGAL UPDATE")
            assertTrue(false)
        }
    }
}