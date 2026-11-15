import id.biz.yuka.libperformx.SupabaseConfig
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

class SupabaseConnectionTest {

    @Test
    fun testPingSupabase() {
        runBlocking {
            println("Memulai ping ke Supabase...")

            try {
                val response = SupabaseConfig.client.postgrest["roles"]
                    .select()

                println("✅ KONEKSI BERHASIL!")
                println("Response JSON: ${response.data}")

            } catch (e: Exception) {
                println("❌ KONEKSI GAGAL: ${e.message}")
            }
        }
    }
}