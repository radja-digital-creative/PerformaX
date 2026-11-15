package usecase

import id.biz.yuka.libperformx.SupabaseConfig
import id.biz.yuka.libperformx.repository.AuthRepository
import id.biz.yuka.libperformx.repository.SalaryRepository
import id.biz.yuka.libperformx.request.AddDeductionToEmployeeRequest
import id.biz.yuka.libperformx.request.CreateDeductionComponentRequest
import id.biz.yuka.libperformx.request.LoginRequest
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

/**
 * =========================================================================
 * KELAS: UseCasePayrollManagementTest
 * =========================================================================
 * TUJUAN:
 * Menguji Alur Penggajian. Melibatkan interaksi antara Admin (yang mengatur
 * master potongan & nominal gaji) dan Karyawan (yang melihat slip gajinya).
 * * * SKENARIO:
 * 1. Admin membuat Master Potongan (BPJS).
 * 2. Admin melihat daftar gaji dan menambahkan potongan ke Karyawan A.
 * 3. Karyawan A Login, lalu melihat Slip Gaji (Sistem menghitung otomatis).
 * 4. Clean Up Data Dummy.
 * =========================================================================
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class UseCasePayrollManagementTest {

    private val authRepository = AuthRepository()
    private val salaryRepository = SalaryRepository()

    // Variabel Global (Shared State antar Test)
    private var employeeId: String = ""
    private var testDeductionId: String = ""
    private var targetSalaryId: String = ""

    @BeforeAll
    fun setupAll() = runBlocking {
        println("--- SETUP GLOBAL: Login Admin & Cari Target ---")
        val loginRes = authRepository.login(LoginRequest("test@perfomx.com", "admin1234"))
        assertTrue(loginRes is ResultState.Success, "Admin gagal login!")

        val empResult = authRepository.getUserIdByEmail("kariawan_toko_a@perfomx.com")
        assertTrue(empResult is ResultState.Success, "Email karyawan tidak ditemukan!")
        employeeId = (empResult as ResultState.Success).data

        println("✅ Setup selesai. Target Karyawan ID: $employeeId")
    }

    @Test
    @Order(1)
    fun `test 1 - Admin Membuat Master Data Potongan (BPJS)`() = runBlocking {
        println("\n==> TEST 1: ADMIN MEMBUAT MASTER POTONGAN <==")

        val reqDeduction = CreateDeductionComponentRequest(
            name = "BPJS Kesehatan (Unit Test)",
            description = "Potongan wajib bulanan",
            calculation_type = "fixed",
            fixed_amount = 150000.0,
            percentage_value = 0.0,
            is_active = true
        )

        val res = salaryRepository.createDeductionComponent(reqDeduction)
        if (res is ResultState.Error) println("🚨 ERROR CREATE DEDUCTION: ${res.message}")
        assertTrue(res is ResultState.Success)

        testDeductionId = (res as ResultState.Success).data
        println("✅ Master Potongan berhasil dibuat! ID: $testDeductionId")
    }

    @Test
    @Order(2)
    fun `test 2 - Admin Melihat Daftar Gaji & Menambahkan Potongan ke Karyawan`() = runBlocking {
        println("\n==> TEST 2: ADMIN MENGEDIT GAJI KARYAWAN <==")

        // 1. Admin melihat daftar gaji untuk mencari Salary ID milik Karyawan Target
        val listRes = salaryRepository.getAllSalaries()
        assertTrue(listRes is ResultState.Success, "Gagal mengambil daftar gaji karyawan")

        val allSalaries = (listRes as ResultState.Success).data
        val targetSalary = allSalaries.find { it.userId == employeeId }
        assertTrue(targetSalary != null, "Data gaji karyawan target tidak ditemukan di View!")

        targetSalaryId = targetSalary!!.salaryId

        // 2. Admin memasukkan Potongan BPJS tadi ke Karyawan ini
        val reqAddDeduction = AddDeductionToEmployeeRequest(
            user_salary_id = targetSalaryId,
            deduction_id = testDeductionId
        )

        val addRes = salaryRepository.addDeductionToEmployee(reqAddDeduction)
        if (addRes is ResultState.Error) println("🚨 ERROR ADD DEDUCTION: ${addRes.message}")
        assertTrue(addRes is ResultState.Success)

        println("✅ Potongan BPJS berhasil diterapkan ke Gaji Karyawan!")
    }

    @Test
    @Order(3)
    fun `test 3 - Karyawan Login dan Melihat Slip Gaji Miliknya Sendiri`() = runBlocking {
        println("\n==> TEST 3: KARYAWAN MELIHAT SLIP GAJI <==")

        // 1. Switch Akun: Logout Admin, Login Karyawan
        SupabaseConfig.client.auth.signOut()
        val empLogin = authRepository.login(LoginRequest("kariawan_toko_a@perfomx.com", "admin1234"))
        assertTrue(empLogin is ResultState.Success, "Karyawan gagal login!")
        println("   [Simulasi] Karyawan berhasil login dan membuka menu Slip Gaji...")

        // 2. Karyawan menarik data gajinya sendiri
        val detailRes = salaryRepository.getSalaryDetail(employeeId)
        assertTrue(detailRes is ResultState.Success, "Gagal menarik detail slip gaji karyawan")

        val mySalary = (detailRes as ResultState.Success).data

        // Output ke Console (Tampilan Slip Gaji)
        println("\n   =======================================")
        println("   SLIP GAJI: ${mySalary.userName}")
        println("   =======================================")
        println("   (+) Gaji Pokok       : Rp ${mySalary.basicSalary}")
        println("   (+) Total Tunjangan  : Rp ${mySalary.totalAllowance}")
        println("   (-) Total Potongan   : Rp ${mySalary.totalDeduction}")
        println("   =======================================")
        println("   => GAJI BERSIH (THP) : Rp ${mySalary.netSalary}")
        println("   =======================================")

        // 3. Assert Validasi Logika Mapper & Karyawan
        assertTrue(mySalary.deductions.isNotEmpty(), "Array potongan di Entity tidak boleh kosong!")

        // Memastikan potongan BPJS sebesar 150.000 benar-benar masuk dan terhitung
        val isBpjsExist = mySalary.deductions.any { it.componentId == testDeductionId }
        assertTrue(isBpjsExist, "Potongan BPJS (Unit Test) tidak terbaca di slip Karyawan!")
        assertTrue(mySalary.totalDeduction >= 150000.0, "Kalkulasi totalDeduction di Entity gagal/salah hitung!")

        println("✅ Karyawan berhasil memvalidasi slip gajinya dengan akurat!")
    }

    @Test
    @Order(4)
    fun `test 4 - Clean Up (Menghapus Potongan Dummy)`() = runBlocking {
        println("\n==> TEST 4: CLEAN UP (MENGEMBALIKAN DB KE SEMULA) <==")

        // Switch kembali ke Admin (Diperlukan karena RLS: Karyawan tidak bisa hapus tabel master)
        SupabaseConfig.client.auth.signOut()
        authRepository.login(LoginRequest("test@perfomx.com", "admin1234"))

        try {
            val client = SupabaseConfig.client
            // Hapus relasinya dulu dari gaji karyawan
            client.postgrest["user_salary_deductions"].delete {
                filter { eq("deduction_id", testDeductionId) }
            }

            // Baru hapus tabel master-nya
            client.postgrest["deduction_components"].delete {
                filter { eq("id", testDeductionId) }
            }

            println("🧹 Potongan dummy (BPJS Unit Test) berhasil dibersihkan dari database.")
            println("🎉 SELURUH SIKLUS PENGGAJIAN BERHASIL SEMPURNA!")
        } catch (e: Exception) {
            println("🚨 Gagal Clean Up: ${e.message}")
        }
    }
}