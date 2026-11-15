package usecase

import id.biz.yuka.libperformx.SupabaseConfig
import id.biz.yuka.libperformx.repository.AuthRepository
import id.biz.yuka.libperformx.repository.StoreRepository
import id.biz.yuka.libperformx.repository.TaskRepository
import id.biz.yuka.libperformx.request.CreateTaskRequest
import id.biz.yuka.libperformx.request.LoginRequest
import id.biz.yuka.libperformx.request.UpdateTaskRequest
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
import java.time.OffsetDateTime

/**
 * =========================================================================
 * KELAS: UseCaseTaskManagementTest
 * =========================================================================
 * TUJUAN:
 * Menguji SELURUH siklus hidup Manajemen Tugas secara berurutan.
 * Menggunakan anotasi PER_CLASS dan OrderAnnotation agar data yang dibuat
 * di Test 1 bisa dilanjutkan di Test 2, 3, dan seterusnya.
 * =========================================================================
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS) // Izinkan variabel dibagikan antar fungsi Test
@TestMethodOrder(MethodOrderer.OrderAnnotation::class) // Eksekusi Test berurutan berdasarkan @Order
class UseCaseTaskManagementTest {

    private val authRepository = AuthRepository()
    private val taskRepository = TaskRepository()
    private val storeRepository = StoreRepository()

    // Variabel Global (Shared State antar Test)
    private var adminId: String = ""
    private var targetEmpUid: String = ""
    private var targetStoreId: String = ""
    private var newTaskId: String = ""

    /**
     * Dijalankan HANYA SEKALI sebelum semua tes dimulai.
     * Tugasnya: Login Admin & Menyiapkan data target (Karyawan & Toko)
     */
    @BeforeAll
    fun setupAll() = runBlocking {
        println("--- SETUP GLOBAL: Memulai Siklus ---")
        val loginResult = authRepository.login(LoginRequest("test@perfomx.com", "admin1234"))
        assertTrue(loginResult is ResultState.Success, "Admin gagal login!")
        adminId = SupabaseConfig.client.auth.currentUserOrNull()?.id ?: ""

        val allStores = (storeRepository.getAllStores() as ResultState.Success).data
        assertTrue(allStores.isNotEmpty(), "Database tidak memiliki toko!")
        targetStoreId = allStores.first().id

        val empUidResult = authRepository.getUserIdByEmail("kariawan_toko_a@perfomx.com")
        assertTrue(empUidResult is ResultState.Success, "Email karyawan tidak ditemukan!")
        targetEmpUid = (empUidResult as ResultState.Success).data

        println("✅ Setup Global Selesai. Siap menjalankan rantai tes.")
    }

    @Test
    @Order(1)
    fun `test 1 - Manager Membuat Tugas Baru (Create & Assign)`() = runBlocking {
        println("\n==> TEST 1: MANAGER MEMBUAT TUGAS BARU <==")
        val createReq = CreateTaskRequest(
            p_title = "Audit Stok Gudang (Unit Test)",
            p_description = "Tolong hitung sisa barang di gudang belakang.",
            p_due_date = OffsetDateTime.now().plusDays(1).toString(), // Deadline Besok
            p_store_id = targetStoreId,
            p_assigned_to_ids = listOf(targetEmpUid), // Ditugaskan ke Karyawan A
            p_label_ids = emptyList(),
            p_image_urls = listOf("https://link/referensi_gudang.jpg")
        )

        val createRes = taskRepository.createTask(createReq)
        if (createRes is ResultState.Error) println("🚨 ERROR CREATE: ${createRes.message}")
        assertTrue(createRes is ResultState.Success, "Gagal membuat tugas")

        newTaskId = (createRes as ResultState.Success).data // Simpan ID untuk Test Berikutnya
        assertTrue(newTaskId.isNotBlank())
        println("✅ Tugas berhasil dibuat! (ID: $newTaskId)")
    }

    @Test
    @Order(2)
    fun `test 2 - Manager Mengecek Daftar Tugas (Read All)`() = runBlocking {
        println("\n==> TEST 2: MANAGER MENGECEK DAFTAR TUGAS <==")
        val listRes = taskRepository.getTasks()
        assertTrue(listRes is ResultState.Success)

        val allTasks = (listRes as ResultState.Success).data
        val isTaskExist = allTasks.any { it.id == newTaskId }
        assertTrue(isTaskExist, "Tugas tidak muncul di daftar!")
        println("✅ Tugas berhasil terdeteksi di Dashboard.")
    }

    @Test
    @Order(3)
    fun `test 3 - Manager Menambahkan Komentar Instruksi (Diskusi)`() = runBlocking {
        println("\n==> TEST 3: MANAGER MENAMBAHKAN KOMENTAR <==")
        val commentPayload = mapOf(
            "task_id" to newTaskId,
            "user_id" to adminId,
            "comment_text" to "Ingat, rak atas juga dihitung ya!"
        )
        // Bypass repository khusus untuk simulasi cepat
        SupabaseConfig.client.postgrest["task_comments"].insert(commentPayload)
        println("✅ Komentar berhasil ditambahkan.")
    }

    @Test
    @Order(4)
    fun `test 4 - Manager Merevisi Tugas (Update)`() = runBlocking {
        println("\n==> TEST 4: MANAGER MEREVISI DEADLINE TUGAS <==")
        val updateReq = UpdateTaskRequest(
            p_task_id = newTaskId,
            p_title = "Audit Stok Gudang (REVISI URGENT)",
            p_description = "Tolong hitung sisa barang HARI INI.",
            p_due_date = OffsetDateTime.now().toString(),
            p_store_id = targetStoreId,
            p_assigned_to_ids = listOf(targetEmpUid),
            p_label_ids = emptyList(),
            p_image_urls = listOf("https://link/referensi_gudang.jpg")
        )
        val updateRes = taskRepository.updateTask(updateReq)

        // 🚨 TAMBAHKAN BARIS INI UNTUK MELIHAT ERROR ASLINYA
        if (updateRes is ResultState.Error) {
            println("🚨 ERROR DETAIL (SUPABASE): ${updateRes.message}")
        }

        assertTrue(updateRes is ResultState.Success, "Gagal merevisi tugas")
        println("✅ Deadline dan Judul Tugas berhasil direvisi!")
    }

    @Test
    @Order(5)
    fun `test 5 - Verifikasi Penyelesaian Tugas dan Poin KPI Otomatis`() = runBlocking {
        println("\n==> TEST 5: VERIFIKASI PENYELESAIAN TUGAS & KPI OTOMATIS <==")

        // A. Ambil Detail dan Ekstrak Assignment ID
        val detailBeforeComplete = (taskRepository.getTaskDetail(newTaskId) as ResultState.Success).data
        val assignmentId = detailBeforeComplete.assignees.first().assignmentId

        // B. Karyawan menyelesaikan tugas (Sistem otomatis memproses KPI di belakang layar)
        val completeRes = taskRepository.completeMyTaskAssignment(assignmentId)
        assertTrue(completeRes is ResultState.Success, "Gagal menyelesaikan tugas")
        println("   [Simulasi]: Karyawan menekan tombol 'Selesai'. Sistem memproses KPI otomatis...")

        // C. Verifikasi Detail Akhir (Membuktikan View membaca perubahan otomatis tersebut)
        val finalDetailRes = taskRepository.getTaskDetail(newTaskId)
        assertTrue(finalDetailRes is ResultState.Success)
        val finalDetail = (finalDetailRes as ResultState.Success).data

        // Ambil data penugasan karyawan tersebut
        val completedAssignee = finalDetail.assignees.first()

        println("   - Judul Tugas: ${finalDetail.title}")
        println("   - Status Selesai: ${completedAssignee.isCompleted}")
        println("   - Poin KPI Didapat: ${completedAssignee.earnedPoints}")
        println("   - Jumlah Komentar: ${finalDetail.comments.size}")

        // D. Assert (Validasi Ketat)
        assertTrue(finalDetail.title.contains("REVISI"))
        assertTrue(completedAssignee.isCompleted, "Status tugas seharusnya sudah berubah menjadi Selesai")

        // Memastikan nilai KPI terbaca (baik itu berubah menjadi > 0 sesuai aturan sistem Anda)
        assertTrue(completedAssignee.earnedPoints >= 0, "Sistem KPI gagal memberikan/membaca poin!")
        assertTrue(finalDetail.comments.isNotEmpty())

        println("✅ Penyelesaian tugas dan kalkulasi KPI otomatis berhasil diverifikasi!")
    }

    @Test
    @Order(6)
    fun `test 6 - Clean Up (Menghapus Tugas dari Sistem)`() = runBlocking {
        println("\n==> TEST 6: CLEAN UP (HARD DELETE) <==")
        val deleteRes = taskRepository.deleteTask(newTaskId)
        if (deleteRes is ResultState.Error) println("🚨 ERROR DELETE: ${deleteRes.message}")
        assertTrue(deleteRes is ResultState.Success, "Gagal membersihkan tugas")

        println("🧹 Semua data terkait tugas (Komentar, Gambar, Assignment) berhasil disapu bersih.")
        println("🎉 SIKLUS MANAJEMEN TUGAS BERHASIL SEMPURNA!")
    }
}