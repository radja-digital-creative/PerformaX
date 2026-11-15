package usecase

import id.biz.yuka.libperformx.SupabaseConfig
import id.biz.yuka.libperformx.repository.AuthRepository
import id.biz.yuka.libperformx.repository.ScheduleRepository
import id.biz.yuka.libperformx.repository.StoreRepository
import id.biz.yuka.libperformx.request.CreateScheduleRequest
import id.biz.yuka.libperformx.request.LoginRequest
import id.biz.yuka.libperformx.request.ShiftRequest
import id.biz.yuka.libperformx.response.UserProfileResponse
import id.biz.yuka.libperformx.utils.ResultState
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.runBlocking
import org.junit.internal.Checks.notNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestMethodOrder
import java.time.LocalDate
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UserStoreMapping(
    @SerialName("user_id")
    val user_id: String,

    @SerialName("store_id")
    val store_id: String
)

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class UseCaseScheduleManagementTest {

    private val authRepository = AuthRepository()
    private val scheduleRepository = ScheduleRepository()
    private val storeRepository = StoreRepository()

    private val todayStr = LocalDate.now().toString()

    @BeforeAll
    fun setupAll() = runBlocking {
        println("--- SETUP: Admin Login ---")
        authRepository.login(LoginRequest("test@perfomx.com", "admin1234"))
        println("✅ Setup Master Selesai.")
    }

    /**
     * FUNGSI 1: MENGATUR SHIFT UNTUK SEMUA TOKO
     */
    @Test
    @Order(1)
    fun `test 1 - Mengatur Master Shift Toko`() = runBlocking {
        println("\n==> TEST 1: MENGATUR MASTER SHIFT DI SEMUA TOKO <==")

        val stores = (storeRepository.getAllStores() as ResultState.Success).data

        stores.forEach { store ->
            println("📍 Memproses Toko: ${store.name}")

            val existingShiftsRes = scheduleRepository.getShifts(store.id)
            assertTrue(existingShiftsRes is ResultState.Success)

            val existingShift = (existingShiftsRes as ResultState.Success).data.find {
                it.shiftName == "Shift Pagi (Standard)"
            }

            if (existingShift == null) {
                val reqShift = ShiftRequest(
                    store_id = store.id,
                    shift_name = "Shift Pagi (Standard)",
                    start_time = "08:00:00",
                    end_time = "17:00:00",
                    attendance_radius = 50
                )
                val res = scheduleRepository.createShift(reqShift)
                assertTrue(res is ResultState.Success)
                println("   ✅ Master Shift 'Pagi' berhasil dibuat.")
            } else {
                println("   ℹ️ Master Shift 'Pagi' sudah ada.")
            }
        }
    }

    /**
     * FUNGSI 2: MENGATUR JADWAL SEMUA KARYAWAN BERDASARKAN PEMETAAN USER_STORES
     */
    @Test
    @Order(2)
    fun `test 2 - Menerbitkan Jadwal Harian Karyawan`() = runBlocking {
        println("\n==> TEST 2: MENERBITKAN JADWAL KERJA (ACUAN USER_STORES) <==")

        // 1. Ambil semua profil karyawan untuk keperluan filter Role dan Nama
        val allProfiles = SupabaseConfig.client.postgrest["profiles"]
            .select()
            .decodeList<UserProfileResponse>() // Pastikan class ini punya field role_id

        // Buat Map (Kamus) agar pencarian profil lebih cepat saat di dalam loop
        val profileMap = allProfiles.associateBy { it.user_id } // Sesuaikan dengan nama field ID Anda

        // 2. Ambil semua relasi dari tabel user_stores
        val userStores = SupabaseConfig.client.postgrest["user_stores"]
            .select()
            .decodeList<UserStoreMapping>() // Buat/Sesuaikan data class ini jika belum ada

        if (userStores.isEmpty()) {
            println("   ℹ️ Skip: Tabel user_stores kosong, tidak ada pemetaan karyawan.")
            return@runBlocking
        }

        // 3. Iterasi langsung berdasarkan pemetaan di user_stores
        userStores.forEach { mapping ->
            val empProfile = profileMap[mapping.user_id]

            println("\n🏢 Memproses Toko ID: ${mapping.store_id} | User: ${empProfile?.full_name}")

            // 4. Cari Shift Pagi di toko tempat user ini ditugaskan
            val shiftsRes = scheduleRepository.getShifts(mapping.store_id)
            if (shiftsRes !is ResultState.Success) {
                println("   ❌ Skip: Gagal mengambil data shift untuk toko ini.")
                return@forEach
            }

            val targetShift = shiftsRes.data.find { it.shiftName.contains("Pagi") }
            if (targetShift == null) {
                println("   ❌ Skip: Tidak ada shift pagi di toko ini.")
                return@forEach
            }

            // 5. Terbitkan Jadwal
            val reqSchedule = CreateScheduleRequest(
                user_id = mapping.user_id,
                store_id = mapping.store_id,
                shift_id = targetShift.id,
                work_date = todayStr,
                notes = "Jadwal otomatis via pemetaan user_stores"
            )

            val res = scheduleRepository.createSchedule(reqSchedule)

            if (res is ResultState.Success) {
                println("      ✅ Jadwal berhasil diterbitkan.")
            } else {
                val errorMsg = (res as ResultState.Error).message
                if (errorMsg.contains("duplicate key value")) {
                    println("      ℹ️ Skip: Jadwal sudah ada di database.")
                } else {
                    println("      ⚠️ Gagal: $errorMsg")
                }
            }
        }
    }
}