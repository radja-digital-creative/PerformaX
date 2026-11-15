package id.biz.yuka.libperformx.repository
import id.biz.yuka.libperformx.SupabaseConfig
import id.biz.yuka.libperformx.entity.EmployeeLoanEligibilityEntity
import id.biz.yuka.libperformx.entity.EmployeeLoanEntity
import id.biz.yuka.libperformx.entity.toEntity
import id.biz.yuka.libperformx.request.ApproveLoanRpcRequest
import id.biz.yuka.libperformx.request.CreateLoanRequest
import id.biz.yuka.libperformx.response.EmployeeLoanEligibilityResponse
import id.biz.yuka.libperformx.response.EmployeeLoanResponse
import id.biz.yuka.libperformx.utils.ResultState
import id.biz.yuka.libperformx.utils.safeSupabaseCall
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.rpc

class LoanRepository {

    /** 1. READ ALL (OWNER): Mendapatkan semua karyawan yang diizinkan mengajukan piutang (can_request_loan = true) */
    suspend fun getEligibleLoanEmployees(): ResultState<List<EmployeeLoanEligibilityEntity>> {
        return safeSupabaseCall {
            val responseList = SupabaseConfig.client.postgrest["vw_employee_details"]
                .select {
                    filter {
                        eq("can_request_loan", true)
                        eq("profile_active", true)
                    }
                }
                .decodeList<EmployeeLoanEligibilityResponse>()

            responseList.map { it.toEntity() }
        }
    }

    /** 2. READ DETAILS BY USER ID (OWNER): Membuka riwayat pengajuan loan dan rincian tagihannya milik karyawan tertentu */
    suspend fun getLoansAndInstallmentsByUserId(userId: String): ResultState<List<EmployeeLoanEntity>> {
        return safeSupabaseCall {
            val responseList = SupabaseConfig.client.postgrest["employee_loans"]
                .select(Columns.raw("*, employee_loan_installments(*)")) {
                    filter {
                        eq("user_id", userId)
                    }
                }
                .decodeList<EmployeeLoanResponse>()

            responseList.map { it.toEntity() }
        }
    }

    /** 3. CREATE (EMPLOYEE): Karyawan mengajukan nominal piutang dan tenor baru */
    suspend fun createLoanRequest(request: CreateLoanRequest): ResultState<Boolean> {
        return safeSupabaseCall {
            SupabaseConfig.client.postgrest["employee_loans"]
                .insert(request)
            true
        }
    }

    /** 4. UPDATE / ACC (OWNER): Owner me-approve pengajuan loan & generate jadwal cicilan via Stored Procedure (RPC) */
    suspend fun approveLoan(request: ApproveLoanRpcRequest): ResultState<Boolean> {
        return safeSupabaseCall {
            SupabaseConfig.client.postgrest.rpc(
                function = "approve_employee_loan",
                parameters = request
            )
            true
        }
    }

    /** 5. READ MY DETAILS (EMPLOYEE): Karyawan dapat mengecek status loan aktif beserta daftar rincian cicilan milik mereka sendiri */
    suspend fun getMyLoanDetails(currentUserId: String): ResultState<List<EmployeeLoanEntity>> {
        return safeSupabaseCall {
            val responseList = SupabaseConfig.client.postgrest["employee_loans"]
                .select(Columns.raw("*, employee_loan_installments(*)")) {
                    filter {
                        eq("user_id", currentUserId)
                    }
                }
                .decodeList<EmployeeLoanResponse>()

            responseList.map { it.toEntity() }
        }
    }

    @kotlinx.serialization.Serializable
    private data class UpdateLoanStatusRequest(
        val status: String
    )

    /** 6. RESPOND LOAN BY EMPLOYEE (EMPLOYEE): Karyawan menyetujui ("approved") atau menolak ("rejected_by_employee") tawaran loan dari Owner */
    suspend fun respondLoanByEmployee(loanId: String, isApproved: Boolean): ResultState<Boolean> {
        return safeSupabaseCall {
            val targetStatus = if (isApproved) "approved" else "rejected_by_employee"
            SupabaseConfig.client.postgrest["employee_loans"]
                .update(UpdateLoanStatusRequest(status = targetStatus)) {
                    filter {
                        eq("id", loanId)
                    }
                }
            true
        }
    }
}