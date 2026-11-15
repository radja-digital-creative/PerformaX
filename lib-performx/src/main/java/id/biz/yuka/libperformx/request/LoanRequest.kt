package id.biz.yuka.libperformx.request
import kotlinx.serialization.Serializable

@Serializable
data class CreateLoanRequest(
    val user_id: String,
    val requested_amount: Double,
    val requested_tenor_months: Int,
    val reason: String? = null
)

@Serializable
data class ApproveLoanRpcRequest(
    val p_loan_id: String,
    val p_hrd_id: String,
    val p_approved_amount: Double,
    val p_approved_tenor: Int,
    val p_start_date: String? = null
)