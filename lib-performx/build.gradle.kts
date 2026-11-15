plugins {
    id("com.android.library") // ✅ UBAH: Sekarang menjadi Android Library
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "id.biz.yuka.libperformx" // Wajib untuk Android Library
    compileSdk = 36

    defaultConfig {
        minSdk = 29
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    // ✅ PENTING: Memastikan Unit Test (12/12) Anda tetap bisa berjalan!
    testOptions {
        unitTests.all {
            it.useJUnitPlatform()
        }
    }
}

dependencies {
    // Membagikan Supabase ke modul :app
    api(platform(libs.supabase.bom))
    api(libs.supabase.postgrest)
    api(libs.supabase.gotrue)
    api(libs.supabase.storage)
    api(libs.supabase.realtime)
    api(libs.supabase.functions)

    implementation(libs.ktor.client.cio)
    implementation(libs.kotlinx.serialization.json)

    // Testing
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.kotlinx.coroutines.test)
    implementation(kotlin("test"))
}