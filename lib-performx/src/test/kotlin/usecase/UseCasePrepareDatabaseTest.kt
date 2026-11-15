package usecase

import id.biz.yuka.libperformx.SupabaseConfig
import id.biz.yuka.libperformx.repository.AuthRepository
import id.biz.yuka.libperformx.request.LoginRequest
import id.biz.yuka.libperformx.utils.ResultState
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestMethodOrder

/**
 * =========================================================================
 * KELAS: UseCasePrepareDatabaseTest (THE RESETTER)
 * =========================================================================
 * TUJUAN:
 * 1. Menyapu bersih (Factory Reset) seluruh data transaksi dan master di DB.
 * 2. Menanamkan (Seeding) kembali Master Data yang esensial (Role, Cuti, KPI).
 * =========================================================================
 */

// --- DATA CLASS UNTUK SEEDING ---
@Serializable
data class SeedLeaveType(val name: String, val description: String, val is_paid_leave: Boolean)

@Serializable
data class SeedKpiRule(val rule_code: String, val point_value: Int, val description: String)


@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class UseCasePrepareDatabaseTest {

    private val authRepository = AuthRepository()
    private var adminId: String = ""

    // UUID Dummy untuk mengelabui filter Supabase agar menghapus SEMUA baris
    private val dummyUuid = "00000000-0000-0000-0000-000000000000"

    @BeforeAll
    fun setup() = runBlocking {
        println("--- SETUP: Login Super Admin untuk Factory Reset ---")
        val loginResult = authRepository.login(LoginRequest("test@perfomx.com", "admin1234"))
        assertTrue(loginResult is ResultState.Success, "Admin gagal login!")
        adminId = SupabaseConfig.client.auth.currentUserOrNull()?.id ?: ""
    }

    @Test
    @Order(1)
    fun `test 1 - CLEAR SEMUA TABEL (Factory Reset)`() = runBlocking {
        println("\n==> 1. MEMULAI PENGHAPUSAN MASAL (FACTORY RESET) <==")
        val client = SupabaseConfig.client

        try {
            // 1. HAPUS TRANSAKSI TUGAS
            client.postgrest["task_comment_images"].delete { filter { neq("id", dummyUuid) } }
            client.postgrest["task_comments"].delete { filter { neq("id", dummyUuid) } }
            client.postgrest["task_images"].delete { filter { neq("id", dummyUuid) } }
            client.postgrest["task_has_labels"].delete { filter { neq("task_id", dummyUuid) } }
            client.postgrest["task_histories"].delete { filter { neq("id", dummyUuid) } }
            client.postgrest["task_assignments"].delete { filter { neq("id", dummyUuid) } }
            client.postgrest["tasks"].delete { filter { neq("id", dummyUuid) } }

            // 2. HAPUS TRANSAKSI ABSENSI & CUTI
            client.postgrest["attendances"].delete { filter { neq("id", dummyUuid) } }
            client.postgrest["offsite_attendance_requests"].delete { filter { neq("id", dummyUuid) } }
            client.postgrest["leave_requests"].delete { filter { neq("id", dummyUuid) } }

            // 3. HAPUS DATA GAJI & JADWAL
            client.postgrest["user_salary_deductions"].delete { filter { neq("id", dummyUuid) } }
            client.postgrest["user_salaries"].delete { filter { neq("id", dummyUuid) } }
            client.postgrest["user_schedules"].delete { filter { neq("id", dummyUuid) } }
            client.postgrest["shifts"].delete { filter { neq("id", dummyUuid) } }

            // 4. HAPUS DATA TOKO
            client.postgrest["attendance_settings"].delete { filter { neq("id", dummyUuid) } }
            client.postgrest["user_stores"].delete { filter { neq("id", dummyUuid) } }
            client.postgrest["stores"].delete { filter { neq("id", dummyUuid) } }

            // 5. HAPUS SEMUA PROFIL (KECUALI ADMIN PUSAT)
            client.postgrest["profiles"].delete { filter { neq("id", adminId) } }

            // 6. HAPUS MASTER DATA
            client.postgrest["leave_types"].delete { filter { neq("id", dummyUuid) } }
            client.postgrest["deduction_components"].delete { filter { neq("id", dummyUuid) } }
            client.postgrest["task_labels"].delete { filter { neq("id", dummyUuid) } }
            client.postgrest["kpi_point_rules"].delete { filter { neq("rule_code", "DUMMY") } }

            println("✅ Database berhasil disapu bersih! (Kecuali Profil Admin)")
        } catch (e: Exception) {
            println("🚨 Gagal Factory Reset: ${e.message}")
            throw e
        }
    }

    @Test
    @Order(2)
    fun `test 2 - SEEDER MASTER DATA (Suntik Data Standar)`() = runBlocking {
        println("\n==> 2. MEMULAI SEEDER MASTER DATA <==")
        val client = SupabaseConfig.client

        try {
            // A. SEEDER LEAVE TYPES
            val leaveTypes = listOf(
                SeedLeaveType("Sakit", "Wajib lampirkan surat dokter", true),
                SeedLeaveType("Cuti Tahunan", "Jatah cuti", true),
                SeedLeaveType("Izin Keperluan", "Izin tanpa dibayar", false)
            )
            client.postgrest["leave_types"].insert(leaveTypes)
            println("   - Master 'Leave Types' berhasil ditanam.")

            // B. SEEDER KPI POINT RULES
            val kpiRules = listOf(
                SeedKpiRule("IN_EARLY", 10, "Masuk Lebih Awal"),
                SeedKpiRule("IN_ON_TIME", 5, "Masuk Tepat Waktu"),
                SeedKpiRule("IN_LATE", -5, "Masuk Terlambat"),
                SeedKpiRule("OUT_EARLY", -5, "Pulang Lebih Awal"),
                SeedKpiRule("OUT_ON_TIME", 5, "Pulang Tepat Waktu"),
                SeedKpiRule("OUT_OVERTIME", 10, "Pulang Lembur")
            )
            client.postgrest["kpi_point_rules"].upsert(kpiRules)
            println("   - Master 'KPI Rules' berhasil ditanam.")

            println("✅ Seeder Master Data Selesai! Sistem siap digunakan untuk operasional.")
        } catch (e: Exception) {
            println("🚨 Gagal Seeder: ${e.message}")
            throw e
        }
    }
}