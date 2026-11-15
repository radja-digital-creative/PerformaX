package usecase

import id.biz.yuka.libperformx.SupabaseConfig
import id.biz.yuka.libperformx.repository.AuthRepository
import id.biz.yuka.libperformx.repository.LeaveRepository
import id.biz.yuka.libperformx.request.CreateLeaveRequest
import id.biz.yuka.libperformx.request.LoginRequest
import id.biz.yuka.libperformx.request.RespondLeaveRequest
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

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class UseCaseLeaveManagementTest {

    private val authRepository = AuthRepository()
    private val leaveRepository = LeaveRepository()

    private var employeeId: String = ""
    private var managerId: String = ""
    private var testLeaveId: String = ""
    private var leaveTypeId: String = ""

    @BeforeAll
    fun setupAll() = runBlocking {
        // 1. Dapatkan ID Master
        authRepository.login(LoginRequest("test@perfomx.com", "admin1234"))
        managerId = SupabaseConfig.client.auth.currentUserOrNull()?.id ?: ""

        val types = leaveRepository.getLeaveTypes()
        leaveTypeId = (types as ResultState.Success).data.first().id

        val empResult = authRepository.getUserIdByEmail("kariawan_toko_a@perfomx.com")
        employeeId = (empResult as ResultState.Success).data
        SupabaseConfig.client.auth.signOut()
    }

    @Test
    @Order(1)
    fun `test 1 - Karyawan Mengajukan Izin Sakit`() = runBlocking {
        println("\n==> TEST 1: KARYAWAN SUBMIT IZIN <==")
        authRepository.login(LoginRequest("kariawan_toko_a@perfomx.com", "admin1234"))

        val request = CreateLeaveRequest(
            user_id = employeeId,
            leave_type_id = leaveTypeId,
            start_date = LocalDate.now().toString(),
            end_date = LocalDate.now().plusDays(2).toString(),
            reason = "Demam tinggi, perlu istirahat total.",
            attachment_url = "https://link/surat_dokter.jpg"
        )

        val result = leaveRepository.submitLeave(request)
        assertTrue(result is ResultState.Success)
        println("✅ Izin berhasil diajukan oleh Karyawan.")
    }

    @Test
    @Order(2)
    fun `test 2 - Manager Melihat Daftar Pending`() = runBlocking {
        println("\n==> TEST 2: MANAGER CEK PENGAJUAN <==")
        SupabaseConfig.client.auth.signOut()
        authRepository.login(LoginRequest("test@perfomx.com", "admin1234"))

        val listRes = leaveRepository.getLeaveRequests(status = "pending")
        assertTrue(listRes is ResultState.Success)

        val pendingList = (listRes as ResultState.Success).data
        val myTestLeave = pendingList.find { it.employeeName.contains("Karyawan") }
        assertTrue(myTestLeave != null)

        testLeaveId = myTestLeave!!.id
        println("✅ Manager menemukan pengajuan dari: ${myTestLeave.employeeName}")
    }

    @Test
    @Order(3)
    fun `test 3 - Manager Menyetujui Izin`() = runBlocking {
        println("\n==> TEST 3: MANAGER MERESPON IZIN <==")
        val responseReq = RespondLeaveRequest(
            status = "approved",
            approved_by = managerId,
            approval_notes = "Semoga lekas sembuh. Pekerjaan akan dihandle rekan tim."
        )

        val result = leaveRepository.respondLeave(testLeaveId, responseReq)
        assertTrue(result is ResultState.Success)

        // Verifikasi Final
        val verifyRes = leaveRepository.getLeaveRequests()
        val approvedLeave = (verifyRes as ResultState.Success).data.find { it.id == testLeaveId }
        assertTrue(approvedLeave?.status == "approved")
        println("✅ Izin resmi DISETUJUI oleh Manager.")
    }

    @Test
    @Order(4)
    fun `test 4 - Clean Up`() = runBlocking {
        SupabaseConfig.client.postgrest["leave_requests"].delete { filter { eq("id", testLeaveId) } }
        println("🧹 Data test izin dibersihkan.")
    }
}