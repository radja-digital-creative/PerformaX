package id.biz.yuka.performax.ui.screen.profile.edit

import android.graphics.Bitmap
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.biz.yuka.libperformx.repository.AuthRepository
import id.biz.yuka.libperformx.repository.EmployeeRepository
import id.biz.yuka.libperformx.request.UpdateEmployeeRequest
import id.biz.yuka.libperformx.request.UpdateKycRequest
import id.biz.yuka.libperformx.utils.ResultState
import id.biz.yuka.performax.utils.FaceDetectionService
import id.biz.yuka.performax.utils.FaceRecognitionService
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import javax.inject.Inject

@HiltViewModel
class EditProfileViewModel @Inject constructor(
    private val employeeRepository: EmployeeRepository,
    private val authRepository: AuthRepository,
    private val faceRecognitionService: FaceRecognitionService,
    private val faceDetectionService: FaceDetectionService
) : ViewModel() {

    // Stepper State (1 = Form, 2 = Profile Photo, 3 = KTP, 4 = Success)
    var currentStep by mutableStateOf(1)

    var isLoading by mutableStateOf(false)
    var errorMessage by mutableStateOf<String?>(null)

    var currentEmail by mutableStateOf("")
        private set
    var editEmail by mutableStateOf("")
    var editPassword by mutableStateOf("")
    var isSavingAccount by mutableStateOf(false)
        private set
    var accountMessage by mutableStateOf<String?>(null)

    fun saveAccount() {
        if (isSavingAccount) return
        val userId = authRepository.getCurrentUserId() ?: return
        val email = editEmail.trim().takeIf { it.isNotBlank() }
        val password = editPassword.takeIf { it.isNotBlank() }
        if (email == null && password == null) return
        viewModelScope.launch {
            isSavingAccount = true
            accountMessage = null
            try {
                when (val result = employeeRepository.updateEmployeeCredentials(userId, email, password)) {
                    is ResultState.Success -> {
                        editEmail = ""
                        editPassword = ""
                        accountMessage = "Akun diperbarui. Jika diminta, konfirmasikan email baru melalui tautan email."
                        when (val refreshed = employeeRepository.getEmployeeAccountEmail(userId)) {
                            is ResultState.Success -> currentEmail = refreshed.data
                            is ResultState.Error -> accountMessage = refreshed.message
                        }
                    }
                    is ResultState.Error -> accountMessage = result.message
                }
            } finally { isSavingAccount = false }
        }
    }

    // Form State
    var editFullName by mutableStateOf("")
    var editPhoneNumber by mutableStateOf("")
    var editAddress by mutableStateOf("")
    var editGender by mutableStateOf("")

    // Image State
    var profileBitmap by mutableStateOf<Bitmap?>(null)
    var ktpBitmap by mutableStateOf<Bitmap?>(null)

    init {
        loadInitialData()
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            isLoading = true
            val userId = authRepository.getCurrentUserId()
            if (userId != null) {
                when (val account = employeeRepository.getEmployeeAccountEmail(userId)) {
                    is ResultState.Success -> currentEmail = account.data
                    is ResultState.Error -> accountMessage = account.message
                }
                val result = employeeRepository.getEmployeeById(userId)
                if (result is ResultState.Success) {
                    val profile = result.data
                    editFullName = profile.fullName
                    editPhoneNumber = profile.phoneNumber
                    editAddress = profile.address
                    editGender = profile.gender
                }
            }
            isLoading = false
        }
    }

    fun nextStep() {
        if (currentStep < 4) currentStep++
    }

    fun previousStep() {
        if (currentStep > 1) currentStep--
    }

    fun processAndSaveAll() {
        viewModelScope.launch {
            isLoading = true
            errorMessage = null

            val userId = authRepository.getCurrentUserId() ?: return@launch

            try {
                // 1. UPDATE DATA DIRI Teks
                val request = UpdateEmployeeRequest(
                    p_user_id = userId,
                    p_full_name = editFullName,
                    p_phone_number = editPhoneNumber,
                    p_address = editAddress,
                    p_gender = editGender
                )
                val updateRes = employeeRepository.updateEmployee(request)
                if (updateRes is ResultState.Error) throw Exception(updateRes.message)

                // 2. DETEKSI WAJAH AI & SKOR (KTP vs Foto Profil)[cite: 6]
                val croppedKtp = faceDetectionService.getFaceCrop(ktpBitmap!!)
                val croppedProfile = faceDetectionService.getFaceCrop(profileBitmap!!)

                if (croppedKtp == null || croppedProfile == null) {
                    throw Exception("Wajah tidak terdeteksi. Pastikan foto Profil dan KTP jelas!")
                }

                val ktpEmbedding = faceRecognitionService.getFaceEmbedding(croppedKtp)
                val profileEmbedding = faceRecognitionService.getFaceEmbedding(croppedProfile)
                val score = faceRecognitionService.calculateSimilarity(ktpEmbedding, profileEmbedding)

                // 3. UPLOAD FOTO PROFIL (Pastikan repository punya fungsi ini)
                val profileStream = ByteArrayOutputStream()
                profileBitmap!!.compress(Bitmap.CompressFormat.JPEG, 70, profileStream)
                val uploadProfileRes = employeeRepository.uploadProfileImage(userId, profileStream.toByteArray())
                val profileUrl = if (uploadProfileRes is ResultState.Success) uploadProfileRes.data else throw Exception("Gagal upload foto profil")

                // 4. UPLOAD FOTO KTP[cite: 6]
                val ktpStream = ByteArrayOutputStream()
                ktpBitmap!!.compress(Bitmap.CompressFormat.JPEG, 70, ktpStream)
                val uploadKtpRes = employeeRepository.uploadKtpImage(userId, ktpStream.toByteArray())
                val ktpUrl = if (uploadKtpRes is ResultState.Success) uploadKtpRes.data else throw Exception("Gagal upload KTP")

                // 5. UPDATE KYC & PROFILE URL DI DATABASE[cite: 6]
                // TODO: Pastikan UpdateKycRequest sekarang menerima profile_url jika Anda menyatukannya
                val kycRequest = UpdateKycRequest(
                    ktp_image_url = ktpUrl,
                    face_embedding = profileEmbedding.joinToString(","),
                    ktp_match_score = score.toDouble(),
                    profile_url = profileUrl // Tambahkan ini di UpdateKycRequest Anda
                )
                val updateKycRes = employeeRepository.updateEmployeeKyc(userId, kycRequest)
                if (updateKycRes is ResultState.Error) throw Exception(updateKycRes.message)

                // JIKA SEMUA SUKSES, LANJUT KE STEP 4
                currentStep = 4

            } catch (e: Exception) {
                Log.e("KYC_DEBUG", "!!! EXCEPTION TERJADI !!!", e)
                errorMessage = e.message ?: "Terjadi kesalahan yang tidak diketahui"
            } finally {
                isLoading = false
            }
        }
    }
}