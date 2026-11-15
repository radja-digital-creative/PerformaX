package usecase

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * =========================================================================
 * KELAS: MasterHRISRunnerUseCaseTest
 * =========================================================================
 * TUJUAN:
 * Mengeksekusi seluruh Use Case secara otomatis (10 Tahap).
 * Alur Logis: Reset DB -> Seed Master -> Bikin Banyak Toko & List -> Bikin Karyawan -> Operasional.
 * =========================================================================
 */
class MasterHRISRunnerUseCaseTest {

    data class TestReport(
        val id: Int,
        val name: String,
        val isSuccess: Boolean,
        val errorMsg: String?
    )

    @Test
    fun `Jalankan Seluruh Skenario Utama HRIS`() = runBlocking {
        val reports = mutableListOf<TestReport>()

        println("===================================================================")
        println("🚀 MEMULAI MASTER RUNNER - PERFORM X HRIS (10 USE CASE)")
        println("===================================================================")

        // --- 1. USE CASE PREPARE DATABASE (RESET & SEEDER) ---
        reports.add(runUseCase(1, "Persiapan Database (Factory Reset & Seeder)") {
            val test = UseCasePrepareDatabaseTest()
            test.setup()
            test.`test 1 - CLEAR SEMUA TABEL (Factory Reset)`()
            test.`test 2 - SEEDER MASTER DATA (Suntik Data Standar)`()
        })

        // --- 2. USE CASE STORE MANAGEMENT (MEMBANGUN BANYAK TOKO) ---
        reports.add(runUseCase(2, "Manajemen Master Toko & Aturan Absensi") {
            val test = UseCaseStoreManagementTest()
            test.setupAll()
            test.`test 1 - Membuat Toko Pusat`()
            test.`test 2 - Membuat Toko Cabang`()
            test.`test 3 - Mengedit Aturan Absensi Toko Cabang`()
            test.`test 4 - Melihat Daftar Seluruh Toko`()
        })

        // --- 3. NEW: MASTER SHIFT (SETELAH TOKO TERBUAT) ---
        reports.add(runUseCase(3, "Manajemen Master Shift Toko") {
            val test = UseCaseScheduleManagementTest()
            test.setupAll()
            test.`test 1 - Mengatur Master Shift Toko`()
        })

        // --- 4. USE CASE REGISTRASI (MEMASUKKAN KARYAWAN KE TOKO) ---
        reports.add(runUseCase(4, "Registrasi Master & Karyawan") {
            val test = UseCaseRegistrationTest()
            test.setupAll()
            test.`test 1 - Pendaftaran Admin (Tanpa Assign Toko)`()
            test.`test 2 - Pendaftaran Manager (Dengan Assign Toko)`()
            test.`test 3 - Pendaftaran Karyawan Biasa`()
        })

        // --- 5. NEW: JADWAL KARYAWAN (SETELAH KARYAWAN TERBUAT) ---
        reports.add(runUseCase(5, "Penerbitan Jadwal Kerja Karyawan") {
            val test = UseCaseScheduleManagementTest()
            test.setupAll()
            test.`test 1 - Mengatur Master Shift Toko`()
            test.`test 2 - Menerbitkan Jadwal Harian Karyawan`()
        })

        // --- 6. USE CASE EMPLOYEE MANAGEMENT (FULL SIKLUS) ---
        reports.add(runUseCase(6, "Siklus Hidup Manajemen Karyawan (Randomized)") {
            val test = UseCaseEmployeeManagementTest()

            test.setupAll() // Login Admin
            test.`test 1 - Registrasi Karyawan Baru (Create)`()
            test.`test 2 - Membaca Detail Salah Satu Karyawan Secara Acak (Read)`()
            test.`test 3 - Update Data Karyawan Secara Acak (Update)`()
            test.`test 4 - Memberhentikan Karyawan Secara Acak (Soft Delete)`()
        })

        // --- 7. USE CASE EMPLOYEE ATTENDANCE (OPERASIONAL KARYAWAN) ---
        reports.add(runUseCase(7, "Operasional Absensi & Izin Offsite (Karyawan)") {
            val test = UseCaseEmployeeAttendanceTest()
            test.setupAll()
            test.`test 1 - Karyawan Melihat Jadwal Absen Hari Ini`()
            test.`test 2 - GAGAL Check-In karena berada di Luar Radius (Fake GPS)`()
            test.`test 3 - HRD Menyetujui Izin Offsite Karyawan`()
            test.`test 4 - SUKSES Check-In dari Luar Kota berkat Izin Offsite`()
            test.`test 5 - SUKSES Check-Out dan Mendapatkan KPI`()
        })

        // --- 8. USE CASE ATTENDANCE MANAGEMENT (ADMIN SIDAK) ---
        reports.add(runUseCase(8, "Monitoring & Anulir Absensi (Admin)") {
            val test = UseCaseAttendanceManagementTest()

            // 1. Panggil Setup Global (Sekarang namanya setupAll)
            test.setupAll()

            // 2. Eksekusi ke-5 fase secara berurutan
            test.`test 1 - Persiapan Data Absensi (Injeksi Dummy Fake GPS)`()
            test.`test 2 - HRD Membaca Laporan Absensi Harian`()
            test.`test 3 - HRD Menganulir Absensi (Intervensi)`()
            test.`test 4 - Memastikan Status Anulir Tersimpan`()
        })

        // --- 9. USE CASE TASK MANAGEMENT ---
        reports.add(runUseCase(9, "Manajemen Tugas & Poin KPI") {
            val test = UseCaseTaskManagementTest()
            test.setupAll()
            test.`test 1 - Manager Membuat Tugas Baru (Create & Assign)`()
            test.`test 2 - Manager Mengecek Daftar Tugas (Read All)`()
            test.`test 3 - Manager Menambahkan Komentar Instruksi (Diskusi)`()
            test.`test 4 - Manager Merevisi Tugas (Update)`()
            test.`test 5 - Verifikasi Penyelesaian Tugas dan Poin KPI Otomatis`()
        })

        // --- 10. USE CASE LEAVE MANAGEMENT ---
        reports.add(runUseCase(10, "Manajemen Pengajuan Izin / Cuti") {
            val test = UseCaseLeaveManagementTest()
            test.setupAll()
            test.`test 1 - Karyawan Mengajukan Izin Sakit`()
            test.`test 2 - Manager Melihat Daftar Pending`()
            test.`test 3 - Manager Menyetujui Izin`()
        })

        // --- 11. USE CASE PAYROLL MANAGEMENT ---
        reports.add(runUseCase(11, "Kalkulasi Penggajian & Potongan") {
            val test = UseCasePayrollManagementTest()
            test.setupAll()
            test.`test 1 - Admin Membuat Master Data Potongan (BPJS)`()
            test.`test 2 - Admin Melihat Daftar Gaji & Menambahkan Potongan ke Karyawan`()
            test.`test 3 - Karyawan Login dan Melihat Slip Gaji Miliknya Sendiri`()
        })

        // --- 12. USE CASE DASHBOARD ---
        reports.add(runUseCase(12, "Render Dashboard Multi-Role") {
            val test = UseCaseDashboardTest()
            test.setupAll()
            test.`test 1 - Load Dashboard untuk OWNER (Global View)`()
            test.`test 2 - Load Dashboard untuk MANAGER (Branch View & Personal Attendance)`()
            test.`test 3 - Load Dashboard untuk KARYAWAN (Personal View)`()
        })

        // =========================================================
        // PRINT SUMMARY REPORT
        // =========================================================
        println("\n\n")
        println("===================================================================")
        println("🎯 RANGKUMAN HASIL UJI COBA KESELURUHAN (TEST REPORT)")
        println("===================================================================")
        var totalSuccess = 0
        reports.forEach { report ->
            if (report.isSuccess) {
                println("✅ [USE CASE ${report.id}] ${report.name} -> SUKSES")
                totalSuccess++
            } else {
                println("❌ [USE CASE ${report.id}] ${report.name} -> GAGAL")
                println("   └─ Detail Error: ${report.errorMsg}")
            }
        }
        println("-------------------------------------------------------------------")
        println("📊 TOTAL BERHASIL : $totalSuccess / ${reports.size}")
        println("===================================================================")

        val isAllSuccess = reports.all { it.isSuccess }
        assertTrue(isAllSuccess, "Ada Use Case yang gagal, periksa log rangkuman di atas!")
    }

    private suspend fun runUseCase(id: Int, name: String, block: suspend () -> Unit): TestReport {
        println("\n\n▶️ MEMULAI USE CASE $id: $name ...")
        return try {
            block()
            println("⏳ Selesai. Menunggu 5 detik agar sinkronisasi Database stabil...")
            delay(5000)
            TestReport(id, name, true, null)
        } catch (e: Throwable) {
            val cleanError = e.message ?: e.localizedMessage ?: e.toString()
            println("\n🛑 ERROR TERDETEKSI PADA USE CASE $id:")
            println("   $cleanError")
            println("   (Sistem mengabaikan error ini dan melanjutkan ke Use Case berikutnya...)")
            println("⏳ Menunggu 5 detik sebelum lanjut...")
            delay(5000)
            TestReport(id, name, false, cleanError)
        }
    }
}