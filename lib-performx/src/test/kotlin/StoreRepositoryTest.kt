package id.biz.yuka.libperformx

import id.biz.yuka.libperformx.repository.StoreRepository
import id.biz.yuka.libperformx.request.CreateStoreRequest
import id.biz.yuka.libperformx.request.UpdateAttendanceSettingRequest
import id.biz.yuka.libperformx.request.UpdateStoreRequest
import id.biz.yuka.libperformx.utils.ResultState
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.providers.builtin.Email
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class StoreRepositoryTest {

    private val storeRepository = StoreRepository()

    @BeforeEach
    fun setup() = runBlocking {
        println("--- SETUP: Login sebagai Admin/Owner ---")
        try {
            SupabaseConfig.client.auth.signInWith(Email) {
                email = "test@perfomx.com" // Pastikan akun ini punya role Admin/Owner
                password = "admin1234"
            }
            println("✅ Login Admin Berhasil")
        } catch (e: Exception) {
            println("❌ Login Gagal: ${e.message}")
        }
    }

    @Test
    fun `test siklus hidup Toko (Create, Read, Update, Delete)`() = runBlocking {
        println("\n==> 1. TEST CREATE (Membuat Toko Baru via RPC) <==")

        val createRequest = CreateStoreRequest(
            p_name = "Toko Unit Test Yuka",
            p_address = "Jl. Koding No. 99, Semanu",
            p_phone_number = "081234567890",
            p_latitude = -7.9999,
            p_longitude = 110.8888,
            p_open_before_minutes = 30, // Buka 30 menit sebelum shift
            p_daily_close_time = "23:00:00"
        )

        val createResult = storeRepository.createStore(createRequest)
        var newStoreId = ""

        when (createResult) {
            is ResultState.Success -> {
                newStoreId = createResult.data
                println("✅ Toko berhasil dibuat! ID: $newStoreId")
                assertTrue(newStoreId.isNotBlank())
            }
            is ResultState.Error -> {
                println("❌ GAGAL CREATE: ${createResult.message}")
                assertTrue(false, "Test berhenti karena gagal membuat toko")
                return@runBlocking
            }
        }


        println("\n==> 2. TEST READ (Mengambil Detail via View) <==")
        val readResult = storeRepository.getStoreDetail(newStoreId)

        when (readResult) {
            is ResultState.Success -> {
                val toko = readResult.data
                println("✅ Detail Toko Terbaca!")
                println("Nama: ${toko.name}")
                println("Batas Awal Absen (Menit): ${toko.openBeforeMinutes}")
                println("Jam Tutup Harian: ${toko.closeTime}")
                assertTrue(toko.name == "Toko Unit Test Yuka")
            }
            is ResultState.Error -> {
                println("❌ GAGAL READ: ${readResult.message}")
                assertTrue(false)
            }
        }


        println("\n==> 3. TEST UPDATE (Mengubah Nama dan Setting Absen) <==")

        // Update Nama Toko
        val updateStoreRequest = UpdateStoreRequest(name = "Toko Unit Test Yuka (Updated)")
        val updateStoreResult = storeRepository.updateStore(newStoreId, updateStoreRequest)

        // Update Setting (Ubah jam tutup jadi jam 12 malam)
        val updateSettingRequest = UpdateAttendanceSettingRequest(daily_close_time = "00:00:00")
        val updateSettingResult = storeRepository.updateAttendanceSetting(newStoreId, updateSettingRequest)

        if (updateStoreResult is ResultState.Success && updateSettingResult is ResultState.Success) {
            println("✅ Update Toko & Setting Berhasil!")
            assertTrue(true)
        } else {
            println("❌ GAGAL UPDATE")
            assertTrue(false)
        }


        println("\n==> 4. TEST DELETE (Menghapus Setting lalu Toko) <==")
        val deleteResult = storeRepository.deleteStore(newStoreId)

        when (deleteResult) {
            is ResultState.Success -> {
                println("✅ Toko beserta pengaturannya BERHASIL DIHAPUS!")
                assertTrue(true)
            }
            is ResultState.Error -> {
                println("❌ GAGAL DELETE: ${deleteResult.message}")
                assertTrue(false)
            }
        }


        println("\n==> 5. VERIFIKASI DELETE (Pastikan toko benar-benar hilang) <==")
        val verifyResult = storeRepository.getStoreDetail(newStoreId)

        when (verifyResult) {
            is ResultState.Success -> {
                println("❌ GAGAL: Toko masih bisa ditemukan padahal sudah dihapus!")
                assertTrue(false)
            }
            is ResultState.Error -> {
                println("✅ VERIFIKASI BERHASIL: Toko sudah tidak ditemukan di database.")
                assertTrue(true)
            }
        }
    }
}