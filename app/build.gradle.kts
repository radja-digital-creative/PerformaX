plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.kotlin.serialization)
    id("com.google.gms.google-services")
}

android {
    namespace = "id.biz.yuka.performax"
    compileSdk = 36

    defaultConfig {
        applicationId = "id.biz.yuka.performax"
        minSdk = 29
        targetSdk = 35
        versionCode = 722
        versionName = "7.2.2-pxn"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        splits {
            abi {
                isEnable = true
                reset()
                include("arm64-v8a")
                isUniversalApk = false
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "/META-INF/LICENSE.md"
            excludes += "/META-INF/LICENSE-notice.md"
            pickFirst("META-INF/LICENSE.md")
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(platform(libs.firebase.bom))
    
    // Icons
    implementation(libs.androidx.compose.material.icons.extended)
    
    // Coil for Image Loading
    implementation(libs.coil.compose)
    
    // Navigation
    implementation(libs.androidx.navigation.compose)
    
    // Splash Screen
    implementation(libs.androidx.core.splashscreen)
    
    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.common)
    implementation(libs.androidx.hilt.work)
    ksp(libs.androidx.hilt.compiler)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.hilt.navigation.compose)
    
    // DataStore (Modern Shared Preferences)
    implementation(libs.androidx.datastore.preferences)

    // WebView Support for Leaflet
    implementation(libs.androidx.webkit)

    // CameraX
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)

    //Firebase
    implementation(libs.firebase.messaging)

    // (Nantinya kita tambahkan ML Kit Face Detection di sini)
     implementation(libs.face.detection)

    // GPS Location (Penting jika Leaflet digunakan untuk deteksi lokasi user)
    implementation(libs.play.services.location)

    //TensorFlow Lite
    implementation(libs.tensorflow.lite)
    implementation(libs.tensorflow.lite.support)


    // Project modules
    implementation(project(":lib-performx"))

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}