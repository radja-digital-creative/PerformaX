package id.biz.yuka.performax.ui.screen.leave.create

import android.graphics.Bitmap
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.biz.yuka.libperformx.SupabaseConfig
import id.biz.yuka.libperformx.entity.LeaveEntity
import id.biz.yuka.libperformx.repository.EmployeeRepository
import id.biz.yuka.libperformx.repository.LeaveRepository
import id.biz.yuka.libperformx.request.CreateLeaveRequest
import id.biz.yuka.libperformx.response.LeaveTypeResponse
import id.biz.yuka.libperformx.utils.ResultState
import io.github.jan.supabase.gotrue.auth
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import javax.inject.Inject

@HiltViewModel
class CreateLeaveViewModel @Inject constructor(
    private val leaveRepository: LeaveRepository,
    private val employeeRepository: EmployeeRepository
) : ViewModel() {

    var leaveTypes by mutableStateOf<List<LeaveTypeResponse>>(emptyList())
    var selectedType by mutableStateOf<LeaveTypeResponse?>(null)

    // Data pendukung validasi
    private var userJoinDate: LocalDate? = null
    private var myApprovedLeaves = listOf<LeaveEntity>()

    var startDate by mutableStateOf<LocalDate?>(null)
    var endDate by mutableStateOf<LocalDate?>(null)
    var reason by mutableStateOf("")
    
    // State Lampiran
    var capturedImage by mutableStateOf<Bitmap?>(null)

    var isLoading by mutableStateOf(false)
    var isDataLoading by mutableStateOf(false)
    var successMessage by mutableStateOf<String?>(null)
    var errorMessage by mutableStateOf<String?>(null)

    init {
        loadInitialData()
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            isDataLoading = true
            val userId = SupabaseConfig.client.auth.currentUserOrNull()?.id ?: return@launch
            
            val typesJob = async { leaveRepository.getLeaveTypes() }
            val profileJob = async { employeeRepository.getEmployeeById(userId) }
            val historyJob = async { leaveRepository.getMyLeaveRequests(userId, status = "approved") }

            val typesRes = typesJob.await()
            val profileRes = profileJob.await()
            val historyRes = historyJob.await()

            if (typesRes is ResultState.Success) leaveTypes = typesRes.data
            if (profileRes is ResultState.Success) {
                userJoinDate = profileRes.data.joinDate?.take(10)?.let { LocalDate.parse(it) }
            }
            if (historyRes is ResultState.Success) myApprovedLeaves = historyRes.data

            isDataLoading = false
        }
    }

    fun isTypeLocked(type: LeaveTypeResponse): Pair<Boolean, String?> {
        // 1. Cek Masa Kerja (quota_min_months)
        if (type.quota_min_months != null && type.quota_min_months!! > 0) {
            val joinDate = userJoinDate ?: return true to "Data masa kerja tidak ditemukan"
            val monthsWorking = ChronoUnit.MONTHS.between(joinDate, LocalDate.now())
            if (monthsWorking < type.quota_min_months!!) {
                return true to "Minimal kerja ${type.quota_min_months} bulan"
            }
        }

        // 2. Cek Kuota (quota_limit & quota_period)
        if (type.quota_period != null && type.quota_limit != null) {
            val now = LocalDate.now()
            val filteredLeaves = myApprovedLeaves.filter { leave ->
                // Filter by type (Based on name since ID is not in LeaveEntity yet)
                val matchesType = leave.typeName.equals(type.name, ignoreCase = true) 
                if (!matchesType) return@filter false

                // Parse period to get dates (Format: "YYYY-MM-DD s/d YYYY-MM-DD")
                val parts = leave.period.split(" s/d ")
                if (parts.size < 1) return@filter false
                
                try {
                    val leaveDate = LocalDate.parse(parts[0].trim()) // Pakai start date untuk hitungan periode

                    if (type.quota_period == "monthly") {
                        leaveDate.month == now.month && leaveDate.year == now.year
                    } else if (type.quota_period == "yearly") {
                        leaveDate.year == now.year
                    } else {
                        false
                    }
                } catch (e: Exception) {
                    false
                }
            }

            if (filteredLeaves.size >= type.quota_limit!!) {
                val periodText = if (type.quota_period == "monthly") "bulan" else "tahun"
                return true to "Sudah mencapai limit ${type.quota_limit}x per $periodText"
            }
        }

        return false to null
    }

    fun submitLeave() {
        if (selectedType == null || startDate == null || endDate == null || reason.isBlank()) {
            errorMessage = "Mohon lengkapi semua data"
            return
        }

        viewModelScope.launch {
            isLoading = true
            errorMessage = null

            val userId = SupabaseConfig.client.auth.currentUserOrNull()?.id
            if (userId != null) {
                try {
                    var finalAttachmentUrl: String? = null

                    // 1. Jika ada gambar, upload dulu
                    if (capturedImage != null) {
                        val stream = ByteArrayOutputStream()
                        capturedImage!!.compress(Bitmap.CompressFormat.JPEG, 80, stream)
                        val byteArray = stream.toByteArray()

                        val uploadRes = leaveRepository.uploadLeaveAttachment(userId, byteArray)
                        if (uploadRes is ResultState.Success) {
                            finalAttachmentUrl = uploadRes.data
                        } else if (uploadRes is ResultState.Error) {
                            errorMessage = "Gagal mengunggah lampiran: ${uploadRes.message}"
                            isLoading = false
                            return@launch
                        }
                    }

                    // 2. Submit Data Request
                    val request = CreateLeaveRequest(
                        user_id = userId,
                        leave_type_id = selectedType!!.id,
                        start_date = startDate!!.format(DateTimeFormatter.ISO_LOCAL_DATE),
                        end_date = endDate!!.format(DateTimeFormatter.ISO_LOCAL_DATE),
                        reason = reason,
                        attachment_url = finalAttachmentUrl
                    )

                    val result = leaveRepository.submitLeave(request)
                    if (result is ResultState.Success) {
                        successMessage = "Izin berhasil diajukan"
                    } else if (result is ResultState.Error) {
                        errorMessage = result.message
                    }
                } catch (e: Exception) {
                    errorMessage = "Terjadi kesalahan: ${e.localizedMessage}"
                }
            }
            isLoading = false
        }
    }
}