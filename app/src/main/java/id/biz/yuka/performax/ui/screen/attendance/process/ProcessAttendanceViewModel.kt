package id.biz.yuka.performax.ui.screen.attendance.process

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.biz.yuka.libperformx.SupabaseConfig
import id.biz.yuka.libperformx.entity.TaskEntity
import id.biz.yuka.libperformx.repository.*
import id.biz.yuka.libperformx.request.ProcessAttendanceRequest
import id.biz.yuka.libperformx.utils.ResultState
import id.biz.yuka.performax.ui.components.MapMarker
import id.biz.yuka.performax.utils.FaceDetectionService
import id.biz.yuka.performax.utils.FaceRecognitionService
import io.github.jan.supabase.gotrue.auth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.async
import java.io.ByteArrayOutputStream
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject
import kotlin.math.*

@HiltViewModel
class ProcessAttendanceViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val scheduleRepository: ScheduleRepository,
    private val storeRepository: StoreRepository,
    private val attendanceRepository: AttendanceRepository,
    private val employeeRepository: EmployeeRepository,
    private val taskRepository: TaskRepository,
    private val faceDetectionService: FaceDetectionService,
    private val faceRecognitionService: FaceRecognitionService
) : ViewModel() {

    var requirePhoto by mutableStateOf(true)
        private set
    var allowAnyTime by mutableStateOf(false)
        private set
    var maxCheckInTime: String? = null
    var isLocationReady by mutableStateOf(false)
        private set
    private var locationReceivedAt = 0L

    fun submitWithoutPhoto() {
        if (requirePhoto || isUploadLoading || uploadSuccess || !isLocationReady) return
        if (android.os.SystemClock.elapsedRealtime() - locationReceivedAt > 30_000L) {
            isLocationReady = false
            globalErrorMessage = "Lokasi sudah kedaluwarsa. Tunggu pembaruan GPS."; return
        }
        uploadAttendanceAndProcess(null, false)
    }

    var overlayMessage by mutableStateOf("Memuat data jadwal...")
    var attendanceState by mutableStateOf("LOADING")
    var timeErrorMessage by mutableStateOf("")
    var globalErrorMessage by mutableStateOf<String?>(null)
    var currentAttendanceType by mutableStateOf("check_in")
    var isUploadLoading by mutableStateOf(false)
    var uploadSuccess by mutableStateOf(false)
    var isFaceTimeout by mutableStateOf(false)
    var pendingTasks by mutableStateOf<List<TaskEntity>>(emptyList())
    var completedTasks by mutableStateOf<List<TaskEntity>>(emptyList())
    var exceptionAccess by mutableStateOf(AttendanceExceptionAccess())
        private set
    val hasOffsiteAccess get() = exceptionAccess.allowsOffsite
    val hasShiftExceptionAccess get() = if (currentAttendanceType == "check_in")
        exceptionAccess.allowsLateArrival else exceptionAccess.allowsEarlyDeparture
    val hasPendingException get() = exceptionAccess.hasPendingFor(currentAttendanceType)
    var hasPendingRequestToday by mutableStateOf(false)

    // --- State Absen Manual ---
    var showManualFallback by mutableStateOf(false)
    var isManualMode by mutableStateOf(false)
    var recognitionFailCount by mutableIntStateOf(0)
    var showManualWarningDialog by mutableStateOf(false)
    var manualWarningCountdown by mutableIntStateOf(10)
    private var countdownJob: kotlinx.coroutines.Job? = null

    var employeeName by mutableStateOf("Memuat...")
    var storeName by mutableStateOf("Memuat...")
    var storeLocation by mutableStateOf<MapMarker?>(null)
    var userLocation by mutableStateOf<MapMarker?>(null)
    var activeScheduleId by mutableStateOf<String?>(null)
    var currentUserId by mutableStateOf<String?>(null)

    var expectedCheckOutTime by mutableStateOf("")
    var maxRadiusMeters = 0.0
    private var isVerificationStarted = false
    var livenessStep by mutableIntStateOf(0)
        private set

    // --- State AI & Wajah ---
    var savedFaceEmbedding: FloatArray? = null
    var thresholdScore: Double = 0.5
    var needsFaceFrames by mutableStateOf(false)
    val collectedEmbeddings = mutableListOf<FloatArray>()
    var lastCapturedFrame: Bitmap? = null // Frame terakhir untuk bukti
    var blinkCount = 0
    var wasBlinking = false
    @Volatile var isProcessingFrame = false

    private var attendanceLoadJob: kotlinx.coroutines.Job? = null

    init {
        loadAttendanceData()
    }

    fun clearGlobalError() {
        globalErrorMessage = null
    }

    fun retryLoadData() {
        globalErrorMessage = null
        attendanceState = "LOADING"
        loadAttendanceData()
    }

    fun loadAttendanceData() {
        attendanceLoadJob?.cancel()
        attendanceLoadJob = viewModelScope.launch {
            attendanceState = "LOADING"
            globalErrorMessage = null
            hasPendingRequestToday = false
            exceptionAccess = AttendanceExceptionAccess()
            isVerificationStarted = false
            isLocationReady = false

            val userId = SupabaseConfig.client.auth.currentUserOrNull()?.id
            if (userId == null) {
                attendanceState = "TIME_ERROR"
                timeErrorMessage = "Sesi login tidak valid! Silakan login kembali."
                globalErrorMessage = "Sesi login tidak terdeteksi. Silakan login kembali."
                return@launch
            }
            currentUserId = userId

            val today = LocalDate.now(ZoneId.of("Asia/Jakarta")).toString()
            // Independent reads share a round trip rather than blocking one another.
            val employeeRead = async { employeeRepository.getEmployeeById(userId) }
            val preferencesRead = async { employeeRepository.getAttendancePreferences(userId) }
            val manualRead = async { attendanceRepository.checkPendingManualAttendance(userId, today) }
            val extraRead = async { attendanceRepository.checkPendingExtraWorkRequest(userId, today) }
            val scheduleRead = async { scheduleRepository.getDailyAttendanceSchedule(userId, today) }

            when (val empRes = employeeRead.await()) {
                is ResultState.Success -> {
                    employeeName = empRes.data.fullName
                    val savedStr = empRes.data.faceEmbedding
                    if (!savedStr.isNullOrEmpty()) {
                        savedFaceEmbedding = savedStr.split(",").mapNotNull { it.toFloatOrNull() }.toFloatArray()
                    }
                    thresholdScore = empRes.data.ktpMatchScore ?: 0.5
                }
                is ResultState.Error -> {
                    employeeName = "Tanpa Nama"
                    Log.e("Absensi", "Gagal memuat profil karyawan: ${empRes.message}")
                }
            }

            when (val preferences = preferencesRead.await()) {
                is ResultState.Success -> {
                    requirePhoto = preferences.data.require_photo
                    allowAnyTime = preferences.data.allow_any_time
                    maxCheckInTime = preferences.data.max_check_in_time
                }
                is ResultState.Error -> {
                    attendanceState = "TIME_ERROR"
                    timeErrorMessage = "Gagal memuat aturan absensi: ${preferences.message}"
                    return@launch
                }
            }


            // 1. Cek apakah ada pengajuan Absen Manual status 'pending' hari ini
            try {
                val pendingManualRes = manualRead.await()
                if (pendingManualRes is ResultState.Success && pendingManualRes.data != null) {
                    hasPendingRequestToday = true
                    attendanceState = "TIME_ERROR"
                    timeErrorMessage = "Pengajuan Absen Manual Anda tanggal $today sedang dalam proses verifikasi Manager."
                    return@launch
                }
            } catch (e: Exception) {
                Log.e("Absensi", "Gagal cek pending manual attendance: ${e.message}")
            }

            // 2. Cek apakah ada pengajuan Masuk Kerja (Extra Work) status 'pending' hari ini
            try {
                val pendingExtraRes = extraRead.await()
                if (pendingExtraRes is ResultState.Success && pendingExtraRes.data != null) {
                    hasPendingRequestToday = true
                    attendanceState = "TIME_ERROR"
                    timeErrorMessage = "Pengajuan Masuk Kerja Anda tanggal $today sedang dalam proses verifikasi Manager."
                    return@launch
                }
            } catch (e: Exception) {
                Log.e("Absensi", "Gagal cek pending extra work request: ${e.message}")
            }

            when (val scheduleRes = scheduleRead.await()) {
                is ResultState.Success -> {
                    val schedule = scheduleRes.data
                    if (schedule == null) {
                        attendanceState = "TIME_ERROR"
                        timeErrorMessage = "Anda tidak memiliki jadwal shift hari ini."
                        return@launch
                    }
                    activeScheduleId = schedule.id

                    // Pending exceptions permit attendance; approval and deductions remain server-owned.
                    when (val access = attendanceRepository.getAttendanceExceptionAccess(userId, schedule.id)) {
                        is ResultState.Success -> exceptionAccess = access.data
                        is ResultState.Error -> {
                            attendanceState = "TIME_ERROR"
                            timeErrorMessage = "Gagal memuat pengajuan izin: ${access.message}"
                            return@launch
                        }
                    }

                    val storeDetail = when (val storeRes = storeRepository.getStoreDetail(schedule.storeId)) {
                        is ResultState.Success -> storeRes.data
                        else -> {
                            attendanceState = "TIME_ERROR"
                            timeErrorMessage = "Gagal memuat pengaturan toko dari server!"
                            globalErrorMessage = "Gagal menyambungkan ke server untuk memuat lokasi & data toko."
                            return@launch
                        }
                    }

                    // ✅ LANGSUNG PRE-POPULATE DATA TOKO SEBELUM PINDAH STATE
                    storeName = storeDetail.name
                    storeLocation = MapMarker(storeDetail.latitude, storeDetail.longitude, storeDetail.name)
                    maxRadiusMeters = (schedule.shifts?.attendanceRadius ?: 0).toDouble()

                    val currentTime = LocalTime.now(ZoneId.of("Asia/Jakarta"))
                    val startTime = parseTimeSafe(schedule.shifts?.startTime ?: "00:00:00")
                    val endTime = parseTimeSafe(schedule.shifts?.endTime ?: "00:00:00")

                    val openBefore = storeDetail.openBeforeMinutes.toLong()
                    val lateTolerance = storeDetail.lateToleranceMinutes.toLong()
                    val overtimeLimit = storeDetail.overtimeThresholdMinutes.toLong()
                    val closeTime = parseTimeSafe(storeDetail.closeTime.ifBlank { "23:59:00" })

                    val checkInStart = startTime.minusMinutes(openBefore)
                    val checkInEnd = maxCheckInTime?.let { parseTimeSafe(it) } ?: startTime.plusMinutes(lateTolerance)
                    val checkOutStart = endTime.minusMinutes(lateTolerance)
                    val checkOutMaxTime = endTime.plusMinutes(lateTolerance).plusMinutes(overtimeLimit)
                    val checkOutEnd = if (checkOutMaxTime.isBefore(closeTime)) checkOutMaxTime else closeTime

                    val checkAbsenRes = attendanceRepository.getAttendanceBySchedule(userId, schedule.id)
                    val attData = if (checkAbsenRes is ResultState.Success) checkAbsenRes.data else null
                    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

                    if (attData == null) {
                        if (id.biz.yuka.performax.utils.AttendancePolicy.canAttend(currentTime, checkInStart, checkInEnd, allowAnyTime) || (exceptionAccess.allowsLateArrival && currentTime.isAfter(checkInEnd))) {
                            currentAttendanceType = "check_in"
                            attendanceState = "CAMERA"
                            overlayMessage = "Mencari lokasi presisi GPS..."
                        } else {
                            attendanceState = "TIME_ERROR"
                            timeErrorMessage = "Waktu Absen Masuk tidak valid.\nBatas: ${checkInStart.format(timeFormatter)} s/d ${checkInEnd.format(timeFormatter)}"
                            return@launch
                        }
                    } else {
                        if (attData.checkOutTime != "--:--") {
                            attendanceState = "COMPLETED"
                            return@launch
                        } else {
                            if (currentTime.isBefore(checkOutStart) && !exceptionAccess.allowsEarlyDeparture && !allowAnyTime) {
                                expectedCheckOutTime = checkOutStart.format(timeFormatter)
                                attendanceState = "CHECKED_IN_ONLY"
                                return@launch
                            } else if (id.biz.yuka.performax.utils.AttendancePolicy.canAttend(currentTime, checkOutStart, checkOutEnd, allowAnyTime) || (exceptionAccess.allowsEarlyDeparture && currentTime.isBefore(checkOutStart))) {
                                currentAttendanceType = "check_out"
                                loadPendingTasks(userId)
                            } else {
                                attendanceState = "TIME_ERROR"
                                timeErrorMessage = "Waktu Absen Pulang telah lewat batas.\nBatas terakhir: ${checkOutEnd.format(timeFormatter)}"
                                return@launch
                            }
                        }
                    }
                }
                is ResultState.Error -> {
                    attendanceState = "TIME_ERROR"
                    timeErrorMessage = "Gagal menghubungi server!\n${scheduleRes.message}"
                    globalErrorMessage = "Gagal menyambungkan ke server: ${scheduleRes.message}"
                }
            }
        }
    }

    private suspend fun loadPendingTasks(userId: String) {
        val taskRes = taskRepository.getTasks(filterByUserId = userId, filterByOverdue = true)
        val overdueOrTodayTasks = if (taskRes is ResultState.Success) taskRes.data else emptyList()
        val (completed, pending) = overdueOrTodayTasks.partition { task ->
            task.assignees.find { it.userId == userId }?.status == "completed"
        }
        pendingTasks = pending
        completedTasks = completed
        if (pending.isNotEmpty() || completed.isNotEmpty()) {
            attendanceState = "TASK_REVIEW"
        } else {
            attendanceState = "CAMERA"
            overlayMessage = "Mencari lokasi presisi GPS..."
        }
    }

    fun proceedToCameraFromTaskReview() {
        if (storeLocation == null || storeName == "Memuat...") {
            globalErrorMessage = "Data toko belum siap. Memuat ulang data..."
            retryLoadData()
            return
        }
        attendanceState = "CAMERA"
        overlayMessage = "Mencari lokasi presisi GPS..."
    }

    private fun parseTimeSafe(timeStr: String): LocalTime = try {
        LocalTime.parse(timeStr.substring(0, 5))
    } catch (e: Exception) { LocalTime.MIDNIGHT }

    private fun isWithinWindow(current: LocalTime, start: LocalTime, end: LocalTime): Boolean = 
        if (start <= end) !current.isBefore(start) && !current.isAfter(end)
        else !current.isBefore(start) || !current.isAfter(end)

    fun updateLocation(lat: Double, lng: Double) {
        locationReceivedAt = android.os.SystemClock.elapsedRealtime()
        userLocation = MapMarker(lat, lng, "Posisi Anda")
        val targetStore = storeLocation
        if (targetStore == null || activeScheduleId == null || attendanceState != "CAMERA" || isUploadLoading || uploadSuccess) return

        val distance = calculateDistance(lat, lng, targetStore.lat, targetStore.lng)
        isLocationReady = distance <= maxRadiusMeters || hasOffsiteAccess
        if (!requirePhoto) {
            overlayMessage = if (isLocationReady) "Lokasi siap. Tekan tombol absen." else "Anda di luar radius (${distance.toInt()} m)."
            return
        }
        if (!isVerificationStarted || overlayMessage.contains("luar radius") || overlayMessage.contains("Mencari")) {
            if (distance <= maxRadiusMeters || hasOffsiteAccess) {
                isVerificationStarted = true
                if (hasOffsiteAccess && distance > maxRadiusMeters) {
                    overlayMessage = "Izin Absen Luar Radius Ditemukan\nMenyiapkan Kamera..."
                }
                startVerificationFlow()
            } else {
                overlayMessage = "Anda di luar radius!\nJarak: ${distance.toInt()}m (Maks: ${maxRadiusMeters.toInt()}m)"
            }
        }
    }

    private fun startVerificationFlow() {
        viewModelScope.launch {
            overlayMessage = "Lokasi Sesuai \nMenyiapkan Kamera..."
            delay(1500)
            livenessStep = 1
            isFaceTimeout = false
            blinkCount = 0
            needsFaceFrames = false
            overlayMessage = "Posisikan wajah Anda di kamera"
            delay(12000)
            if (livenessStep in 1..3 && !isManualMode) {
                isFaceTimeout = true
                needsFaceFrames = false
                showManualFallback = true
                overlayMessage = "Waktu habis! Verifikasi terlalu lama."
            }
        }
    }

    fun openManualWarningDialog() {
        showManualWarningDialog = true
        manualWarningCountdown = 10
        countdownJob?.cancel()
        countdownJob = viewModelScope.launch {
            while (manualWarningCountdown > 0) {
                delay(1000)
                manualWarningCountdown--
            }
        }
    }

    fun dismissManualWarningDialog() {
        showManualWarningDialog = false
        countdownJob?.cancel()
    }

    fun confirmManualWarningAndSwitch() {
        dismissManualWarningDialog()
        switchToManualMode()
    }

    fun switchToManualMode() {
        isManualMode = true
        needsFaceFrames = true
        livenessStep = 98
        isFaceTimeout = false
        overlayMessage = "Mode Manual Aktif\nPosisikan wajah & tekan tombol Kamera"
    }

    fun captureManualPhoto() {
        val bitmap = lastCapturedFrame
        if (bitmap != null) {
            livenessStep = 4
            overlayMessage = "Mengambil Foto Manual...\nMengunggah bukti..."
            val finalImage = applyWatermark(bitmap)
            uploadAttendanceAndProcess(finalImage, isManual = true)
        } else {
            triggerError("Gagal mengambil frame foto! Posisikan kamera dengan benar.")
        }
    }

    fun onFaceDataReceived(isFaceDetected: Boolean, isBlinking: Boolean, bitmap: Bitmap?) {
        if (isManualMode) {
            if (bitmap != null) {
                lastCapturedFrame = bitmap
            }
            return
        }

        if (livenessStep >= 4 || isFaceTimeout || isUploadLoading) return

        if (livenessStep == 1 && isFaceDetected) {
            isFaceTimeout = false
            livenessStep = 2
            blinkCount = 0
            wasBlinking = false
            overlayMessage = "Wajah terdeteksi! Silakan berkedip 2x"
        }
        else if (livenessStep == 2) {
            if (!isFaceDetected) {
                resetLiveness("Wajah hilang! Ulangi dari awal.")
                return
            }
            if (isBlinking && !wasBlinking) {
                blinkCount++
                wasBlinking = true
                if (blinkCount == 1) overlayMessage = "Bagus! Berkedip 1x lagi"
                else if (blinkCount >= 2) {
                    livenessStep = 3
                    collectedEmbeddings.clear()
                    needsFaceFrames = true
                    overlayMessage = "Tahan posisi...\nSedang memverifikasi wajah"
                }
            } else if (!isBlinking) wasBlinking = false
        }
        else if (livenessStep == 3) {
            if (!isFaceDetected) {
                resetLiveness("Wajah hilang! Ulangi dari awal.")
                return
            }
            if (bitmap != null && collectedEmbeddings.size < 10 && !isProcessingFrame) {
                isProcessingFrame = true
                lastCapturedFrame = bitmap // Simpan frame terakhir sebagai bukti
                viewModelScope.launch(Dispatchers.IO) {
                    try {
                        val cropped = faceDetectionService.getFaceCrop(bitmap)
                        if (cropped != null) {
                            val embedding = faceRecognitionService.getFaceEmbedding(cropped)
                            synchronized(collectedEmbeddings) { collectedEmbeddings.add(embedding) }
                        }
                    } catch (e: Exception) { e.printStackTrace() }
                    finally {
                        isProcessingFrame = false
                        if (collectedEmbeddings.size >= 10 && livenessStep == 3) verifyFaceRecognition()
                    }
                }
            }
        }
    }

    private fun verifyFaceRecognition() {
        livenessStep = 99 
        needsFaceFrames = false

        viewModelScope.launch(Dispatchers.Default) {
            if (savedFaceEmbedding == null) {
                launch(Dispatchers.Main) { 
                    showManualFallback = true
                    triggerError("Data biometrik tidak ditemukan!") 
                }
                return@launch
            }

            var totalScore = 0.0
            for (embedding in collectedEmbeddings) {
                totalScore += faceRecognitionService.calculateSimilarity(savedFaceEmbedding!!, embedding)
            }
            val avgScore = totalScore / collectedEmbeddings.size

            launch(Dispatchers.Main) {
                if (avgScore >= thresholdScore) {
                    recognitionFailCount = 0
                    livenessStep = 4
                    overlayMessage = "Wajah Terverifikasi!\nMengunggah bukti..."
                    val finalImage = lastCapturedFrame?.let { applyWatermark(it) }
                    if (finalImage != null) {
                        uploadAttendanceAndProcess(finalImage, isManual = false)
                    } else {
                        triggerError("Gagal memproses gambar bukti.")
                    }
                } else {
                    recognitionFailCount++
                    if (recognitionFailCount >= 3) {
                        showManualFallback = true
                        triggerError("Gagal cocok 3x! Silakan gunakan Absen Manual.")
                    } else {
                        triggerError("Wajah tidak cocok! Silakan coba lagi ($recognitionFailCount/3)")
                    }
                }
            }
        }
    }

    private fun applyWatermark(original: Bitmap): Bitmap {
        val result = original.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(result)
        val w = original.width.toFloat()
        val h = original.height.toFloat()

        val paint = Paint().apply {
            isAntiAlias = true
            color = Color.WHITE
            textAlign = Paint.Align.RIGHT
        }

        // Font sangat kecil & profesional
        val textSize = h * 0.02f 
        paint.textSize = textSize
        
        val lineSpacing = textSize * 1.2f
        val padding = 15f // Padding minimalis (sekitar 1-2dp)
        
        // Data Informasi
        val timeStr = LocalTime.now(ZoneId.of("Asia/Jakarta")).format(DateTimeFormatter.ofPattern("HH:mm:ss")) + " WIB"
        val dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
        val locStr = "${String.format(Locale.US, "%.6f", userLocation?.lat ?: 0.0)}, ${String.format(Locale.US, "%.6f", userLocation?.lng ?: 0.0)}"
        
        val lines = listOf(
            employeeName,
            storeName.uppercase(),
            "$dateStr | $timeStr",
            locStr,
            "VERIFIED BY PERFORMAX AI"
        )

        // 1. Hitung Ukuran Blok secara Presisi
        var maxW = 0f
        lines.forEach { 
            val width = paint.measureText(it)
            if (width > maxW) maxW = width
        }
        
        val blockW = maxW + (padding * 2)
        val blockH = (lines.size * lineSpacing) + (padding * 2)
        
        // Posisi menempel di pojok kanan bawah dengan sedikit margin dari tepi foto
        val rectLeft = w - blockW - padding
        val rectTop = h - blockH - padding
        val rectRight = w - padding
        val rectBottom = h - padding

        // 2. Draw Solid Background (Hitam Pekat 70%)
        val bgPaint = Paint().apply {
            color = Color.parseColor("#B3000000") 
            style = Paint.Style.FILL
        }
        canvas.drawRect(rectLeft, rectTop, rectRight, rectBottom, bgPaint)

        // 3. Draw Text Lines
        var currentY = rectBottom - padding - (textSize * 0.1f)
        lines.reversed().forEachIndexed { index, text ->
            paint.color = if (index == 0) Color.LTGRAY else Color.WHITE
            paint.alpha = if (index == 0) 150 else 255
            paint.textSize = if (index == 0) textSize * 0.8f else textSize
            
            canvas.drawText(text, rectRight - padding, currentY, paint)
            currentY -= lineSpacing
        }

        return result
    }

    private fun uploadAttendanceAndProcess(bitmap: Bitmap?, isManual: Boolean = false) {
        viewModelScope.launch {
            isUploadLoading = true
            try {
                val userId = currentUserId
                val scheduleId = activeScheduleId
                val userLoc = userLocation

                if (userId == null || scheduleId == null || userLoc == null) {
                    triggerError("Data absen tidak lengkap!")
                    globalErrorMessage = "Data lokasi atau jadwal belum lengkap. Silakan muat ulang."
                    return@launch
                }

                if (requirePhoto && bitmap == null) {
                    globalErrorMessage = "Foto wajib untuk karyawan ini."; return@launch
                }
                val uploadResult: ResultState<String> = if (bitmap != null) {
                    val imageBytes = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
                        val stream = ByteArrayOutputStream()
                        val largest = maxOf(bitmap.width, bitmap.height)
                        val scaled = if (largest > 1280) Bitmap.createScaledBitmap(bitmap,
                            (bitmap.width * 1280L / largest).toInt().coerceAtLeast(1),
                            (bitmap.height * 1280L / largest).toInt().coerceAtLeast(1), true) else bitmap
                        try { scaled.compress(Bitmap.CompressFormat.JPEG, 75, stream); stream.toByteArray() }
                        finally { if (scaled !== bitmap) scaled.recycle() }
                    }
                    attendanceRepository.uploadAttendanceImage(imageBytes, userId)
                } else ResultState.Success("")

                if (uploadResult is ResultState.Success) {
                    val request = ProcessAttendanceRequest(
                        p_user_id = userId,
                        p_schedule_id = scheduleId,
                        p_latitude = userLoc.lat,
                        p_longitude = userLoc.lng,
                        p_image_url = uploadResult.data,
                        p_attendance_type = currentAttendanceType,
                        p_is_manual_flag = isManual
                    )
                    val result = attendanceRepository.processAttendance(request)
                    if (result is ResultState.Success) {
                        uploadSuccess = true
                    } else {
                        val errMsg = (result as ResultState.Error).message
                        triggerError("Gagal Server: $errMsg")
                        globalErrorMessage = "Gagal memproses absen: $errMsg"
                    }
                } else {
                    val uploadErrMsg = (uploadResult as ResultState.Error).message
                    triggerError("Gagal Unggah: $uploadErrMsg")
                    globalErrorMessage = "Gagal mengunggah foto bukti: $uploadErrMsg"
                }
            } catch (e: Exception) {
                triggerError("Error Sistem: ${e.message}")
                globalErrorMessage = "Terjadi kesalahan sistem: ${e.message}"
            } finally {
                isUploadLoading = false
            }
        }
    }

    fun triggerError(errorMsg: String) {
        if (!requirePhoto) { overlayMessage = errorMsg; return }
        viewModelScope.launch {
            isUploadLoading = false
            livenessStep = 99
            overlayMessage = "❌ $errorMsg"
            delay(3500)
            livenessStep = 1
            resetFaceTimer()
        }
    }

    private fun resetLiveness(msg: String) {
        viewModelScope.launch {
            needsFaceFrames = false
            livenessStep = 1
            blinkCount = 0
            overlayMessage = msg
            resetFaceTimer()
        }
    }

    private fun resetFaceTimer() {
        viewModelScope.launch {
            isFaceTimeout = false
            delay(12000)
            if (livenessStep == 1 && !isManualMode) {
                isFaceTimeout = true
                showManualFallback = true
                overlayMessage = "Waktu habis! Wajah tidak ditemukan."
            }
        }
    }

    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371e3
        val p1 = lat1 * PI / 180; val p2 = lat2 * PI / 180
        val dp = (lat2 - lat1) * PI / 180; val dl = (lon2 - lon1) * PI / 180
        val a = sin(dp/2) * sin(dp/2) + cos(p1) * cos(p2) * sin(dl/2) * sin(dl/2)
        return r * (2 * atan2(sqrt(a), sqrt(1-a)))
    }
}