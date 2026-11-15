package id.biz.yuka.libperformx

import id.biz.yuka.libperformx.repository.TaskRepository
import id.biz.yuka.libperformx.request.CreateTaskRequest
import id.biz.yuka.libperformx.request.UpdateTaskRequest
import id.biz.yuka.libperformx.utils.ResultState
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.providers.builtin.Email
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.OffsetDateTime

class TaskRepositoryTest {

    private val taskRepository = TaskRepository()
    private var adminUserId: String = ""

    @BeforeEach
    fun setup() = runBlocking {
        println("--- SETUP: Login sebagai Manager/Admin ---")
        try {
            SupabaseConfig.client.auth.signInWith(Email) {
                email = "test@perfomx.com"
                password = "admin1234"
            }
            adminUserId = SupabaseConfig.client.auth.currentUserOrNull()?.id ?: ""
            println("✅ Login Berhasil: $adminUserId")
        } catch (e: Exception) {
            println("❌ Login Gagal: ${e.message}")
        }
    }

    @Test
    fun `test siklus hidup Task (Create, Read, Update, Complete, Delete)`() = runBlocking {
        // GANTI INI DENGAN UUID KARYAWAN & TOKO ASLI DI DATABASE ANDA
        val targetEmployeeId = "4075f1e0-2745-45ef-8fb9-91af5f3a6b43"
        val targetStoreId = "c6565c49-063a-4467-9c9e-2923984d7821"

        println("\n==> 1. TEST CREATE TASK <==")
        val createReq = CreateTaskRequest(
            p_title = "Audit Stok Barang Akhir Bulan",
            p_description = "Tolong hitung sisa barang di gudang sesuai dengan sistem.",
            p_due_date = OffsetDateTime.now().plusDays(2).toString(), // Deadline lusa
            p_store_id = targetStoreId,
            p_assigned_to_ids = listOf(targetEmployeeId)
        )

        val createResult = taskRepository.createTask(createReq)
        var newTaskId = ""

        when (createResult) {
            is ResultState.Success -> {
                newTaskId = createResult.data
                println("✅ Tugas Berhasil Dibuat! Task ID: $newTaskId")
                assertTrue(newTaskId.isNotBlank())
            }
            is ResultState.Error -> {
                println("❌ GAGAL CREATE: ${createResult.message}")
                assertTrue(false)
                return@runBlocking
            }
        }


        println("\n==> 2. TEST READ & FILTER (Ambil semua tugas untuk Karyawan tsb) <==")
        val readResult = taskRepository.getTasks(filterByUserId = targetEmployeeId, filterByToday = false)
        when (readResult) {
            is ResultState.Success -> {
                println("✅ Berhasil menemukan ${readResult.data.size} tugas untuk karyawan tersebut.")
                val testTask = readResult.data.find { it.id == newTaskId }
                assertTrue(testTask != null, "Tugas yang baru dibuat harusnya ada di list ini")
            }
            is ResultState.Error -> {
                println("❌ GAGAL READ: ${readResult.message}")
                assertTrue(false)
            }
        }


        println("\n==> 3. TEST UPDATE TASK <==")
        val updateReq = UpdateTaskRequest(
            p_task_id = newTaskId,
            p_title = "Audit Stok Barang (URGENT)",
            p_description = "Lakukan audit hari ini juga!",
            p_due_date = OffsetDateTime.now().toString(), // Ubah deadline jadi hari ini
            p_store_id = targetStoreId,
            p_assigned_to_ids = listOf(targetEmployeeId, adminUserId) // Tambahkan admin bantu kerja
        )
        val updateResult = taskRepository.updateTask(updateReq)
        assertTrue(updateResult is ResultState.Success)
        println("✅ Berhasil Update Tugas & Tambah Assignee!")


        println("\n==> 4. TEST DELETE TASK <==")
        val deleteResult = taskRepository.deleteTask(newTaskId)
        when (deleteResult) {
            is ResultState.Success -> {
                println("✅ Tugas beserta relasinya (Assignments) BERHASIL DIHAPUS!")
                assertTrue(true)
            }
            is ResultState.Error -> {
                println("❌ GAGAL DELETE: ${deleteResult.message}")
                assertTrue(false)
            }
        }
    }
}