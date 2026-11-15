package usecase

import id.biz.yuka.libperformx.repository.AuthRepository
import id.biz.yuka.libperformx.repository.StoreRepository
import id.biz.yuka.libperformx.request.CreateStoreRequest
import id.biz.yuka.libperformx.request.LoginRequest
import id.biz.yuka.libperformx.request.UpdateAttendanceSettingRequest
import id.biz.yuka.libperformx.utils.ResultState
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestMethodOrder

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class UseCaseStoreManagementTest {

    private val authRepository = AuthRepository()
    private val storeRepository = StoreRepository()

    private var tokoCabangId: String = ""

    @BeforeAll
    fun setupAll() = runBlocking {
        println("--- SETUP: Owner Login ---")
        val loginResult = authRepository.login(LoginRequest("test@perfomx.com", "admin1234"))
        assertTrue(loginResult is ResultState.Success, "Owner gagal login!")
        println("✅ Owner berhasil login. Siap membangun jaringan toko.")
    }

    @Test
    @Order(1)
    fun `test 1 - Membuat Toko Pusat`() = runBlocking {
        println("\n==> TEST 1: MEMBANGUN TOKO PUSAT <==")
        val reqPusat = CreateStoreRequest(
            p_name = "Toko Pusat (Headquarter)",
            p_address = "Jl. Jendral Sudirman No. 1",
            p_daily_close_time = "23:00:00",
            p_latitude = -7.7926,
            p_longitude = 110.3658
        )
        val res = storeRepository.createStore(reqPusat)
        assertTrue(res is ResultState.Success, "Gagal membuat Toko Pusat")
        println("✅ Toko Pusat berhasil dibangun!")
    }

    @Test
    @Order(2)
    fun `test 2 - Membuat Toko Cabang`() = runBlocking {
        println("\n==> TEST 2: MEMBANGUN TOKO CABANG <==")
        val reqCabang = CreateStoreRequest(
            p_name = "Toko Cabang Malioboro",
            p_address = "Jl. Malioboro No. 99",
            p_daily_close_time = "22:00:00",
            p_latitude = -7.7926,
            p_longitude = 110.3658
        )
        val res = storeRepository.createStore(reqCabang)
        assertTrue(res is ResultState.Success, "Gagal membuat Toko Cabang")
        tokoCabangId = (res as ResultState.Success).data
        println("✅ Toko Cabang berhasil dibangun! (ID: $tokoCabangId)")
    }

    @Test
    @Order(3)
    fun `test 3 - Mengedit Aturan Absensi Toko Cabang`() = runBlocking {
        println("\n==> TEST 3: UPDATE ATURAN ABSENSI TOKO CABANG <==")
        val updateSettingReq = UpdateAttendanceSettingRequest(
            daily_close_time = "23:30:00",
            late_tolerance_minutes = 30 // Ubah toleransi jadi 30 menit
        )
        val updateSettingRes = storeRepository.updateAttendanceSetting(tokoCabangId, updateSettingReq)
        assertTrue(updateSettingRes is ResultState.Success, "Gagal update tabel attendance_settings")
        println("✅ Aturan absensi Toko Cabang berhasil diperbarui.")
    }

    @Test
    @Order(4)
    fun `test 4 - Melihat Daftar Seluruh Toko`() = runBlocking {
        println("\n==> TEST 4: MELIHAT DAFTAR TOKO <==")
        val listResult = storeRepository.getAllStores()
        assertTrue(listResult is ResultState.Success, "Gagal mengambil daftar toko")

        val stores = (listResult as ResultState.Success).data
        assertTrue(stores.size >= 2, "Jumlah toko harus minimal 2!")

        println("✅ Berhasil memuat ${stores.size} Toko ke dalam Sistem:")
        stores.forEachIndexed { index, store ->
            println("   ${index + 1}. [${store.id}] ${store.name}")
        }
    }
}