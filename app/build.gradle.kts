// app/build.gradle.kts
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.guardianos.shield"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.guardianos.shield"
        minSdk = 24  // Android 7.0 (Nougat) - mínimo realista para apps de seguridad modernas
        targetSdk = 34 // Android 14
        versionCode = 1
        versionName = "1.0.0"

        // Optimización RAM para tu Aspire E5-571G
        vectorDrawables {
            useSupportLibrary = true
        }

        // Reducir tamaño del APK
        resConfigs("en", "es") // Solo inglés/español
        setProperty("archivesBaseName", "guardianos-shield-v$versionName")
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
            isDebuggable = true
            isMinifyEnabled = false
            isShrinkResources = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        release {
            isDebuggable = false
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("debug") // Para testing inicial
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
        // ✅ Flags seguros para tu hardware limitado (SIN -Xuse-k2 ni -Xmax-memory)
        freeCompilerArgs = listOf(
            "-Xbackend-threads=2", // Limitar hilos del compilador (tu CPU dual-core)
            "-opt-in=kotlin.RequiresOptIn"
        )
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.11" // Compatible con Kotlin 1.9.23
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "/META-INF/INDEX.LIST"
            excludes += "/META-INF/DEPENDENCIES"
            // Reducir tamaño eliminando recursos innecesarios
            excludes += "/META-INF/*.kotlin_module"
            excludes += "/META-INF/*.version"
        }
    }

    // Optimización crítica para tu hardware limitado
    androidResources {
        noCompress("dat", "bin", "txt") // Evitar compresión innecesaria
    }
}

dependencies {
    // Core AndroidX
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.work.runtime)
    implementation(libs.androidx.compose.material.icons.extended)
    
    // Lifecycle + ViewModel con Compose
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.service)

    // Compose BOM (Bill of Materials) - gestión automática de versiones
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.junit4)

    // Navigation Compose
    implementation(libs.androidx.navigation.compose)

    // Room Database (con KSP - NO kapt)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler) // ✅ KSP en lugar de kapt

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.core)

    // DataStore (preferencias modernas)
    implementation(libs.androidx.datastore.preferences)

    // Networking (para futuras actualizaciones de blocklists)
    implementation(libs.square.retrofit)
    implementation(libs.square.retrofit.gson)
    implementation(libs.square.okhttp)
    implementation(libs.square.okhttp.logging)
    implementation(libs.google.gson)

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.test.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
}

// ✅ Tarea optimizada para tu hardware (SIN flags inválidos)
tasks.register<Exec>("assembleDebugOptimized") {
    description = "Compilar debug optimizado para hardware limitado"
    commandLine = listOf(
        "./gradlew",
        ":app:assembleDebug",
        "--no-daemon",
        "--max-workers=2", // Limitar workers para tu CPU
        "--console=plain"
    )
}
