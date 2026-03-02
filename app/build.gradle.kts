// app/build.gradle.kts
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
}

// Leer credenciales de firma desde local.properties (nunca en git)
val localProperties = Properties().apply {
    val localFile = rootProject.file("local.properties")
    if (localFile.exists()) localFile.inputStream().use { load(it) }
}

android {
    namespace = "com.guardianos.shield"
    compileSdk = 35

    // ── Dimensiones de flavors ────────────────────────────────────────────────
    // "idioma": langEs (español) y langEn (inglés) → APKs/AABs completamente
    // independientes, cada uno con sus strings y applicationId propio.
    //
    // Comandos de build:
    //   ./gradlew assembleLangEsDebug        → APK debug   (ES)
    //   ./gradlew assembleLangEnDebug        → APK debug   (EN)
    //   ./gradlew bundleLangEsRelease        → AAB release (ES) → Play Store
    //   ./gradlew bundleLangEnRelease        → AAB release (EN) → Play Store
    //   ./gradlew assembleLangEsRelease      → APK release (ES)
    //   ./gradlew assembleLangEnRelease      → APK release (EN)
    flavorDimensions += "idioma"

    defaultConfig {
        // applicationId base — los flavors lo sobreescriben/añaden
        minSdk = 31  // Android 12 (Snow Cone) — mínimo para FOREGROUND_SERVICE_SPECIAL_USE y APIs de accesibilidad estables
        targetSdk = 35 // Android 15
        versionCode = 4
        versionName = "1.1.0"

        // Optimización RAM para tu Aspire E5-571G
        vectorDrawables {
            useSupportLibrary = true
        }

    }

    productFlavors {
        // ── Versión en Español ───────────────────────────────────────────────
        create("langEs") {
            dimension  = "idioma"
            applicationId          = "com.guardianos.shield"
            versionNameSuffix      = "-es"
            // BuildConfig accessible desde Kotlin: BuildConfig.FLAVOR ("langEs")
            // Strings específicos ES vienen de src/main/res/values/strings.xml (base)
        }

        // ── Versión en Inglés ────────────────────────────────────────────────
        create("langEn") {
            dimension  = "idioma"
            applicationId          = "com.guardianos.shield.en"
            versionNameSuffix      = "-en"
            // Strings EN vienen de src/langEn/res/values/strings.xml (override de main)
        }
    }

    signingConfigs {
        create("release") {
            val keystoreFile = localProperties["KEYSTORE_FILE"] as? String
            val keystorePassword = localProperties["KEYSTORE_PASSWORD"] as? String
            val keyAlias = localProperties["KEY_ALIAS"] as? String
            val keyPassword = localProperties["KEY_PASSWORD"] as? String
            if (keystoreFile != null && keystorePassword != null &&
                keyAlias != null && keyPassword != null) {
                storeFile     = file(keystoreFile)
                storePassword = keystorePassword
                this.keyAlias      = keyAlias
                this.keyPassword   = keyPassword
                // Firma v3 (Android 9+): permite rotación de clave sin cambiar certificado
                // Firma v4 (Android 11+): mejora compatibilidad con ADB incremental
                enableV1Signing = false  // no necesario con minSdk=31
                enableV2Signing = true
                enableV3Signing = true
                enableV4Signing = true
            }
        }
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
            // ✅ Testing premium sin compra real — solo en debug, R8 elimina esto en release
            buildConfigField("boolean", "FORCE_PREMIUM", "true")
        }
        release {
            isDebuggable = false
            isMinifyEnabled = true // Activado para Play Store y F-Droid
            isShrinkResources = true // Activado para Play Store y F-Droid
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
            buildConfigField("boolean", "FORCE_PREMIUM", "false")
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
        noCompress += listOf("dat", "bin", "txt") // Evitar compresión innecesaria
    }

    // ── Nombrar APKs por flavor correctamente ────────────────────────────────
    // setProperty("archivesBaseName") no funciona por flavor en Kotlin DSL;
    // se usa applicationVariants para renombrar los outputs.
    applicationVariants.all {
        val variant = this
        val flavorSuffix = if (variant.flavorName.contains("langEn")) "en" else "es"
        outputs.all {
            val output = this as? com.android.build.gradle.internal.api.BaseVariantOutputImpl
            output?.outputFileName =
                "guardianos-shield-v1.1.0-${flavorSuffix}-${variant.buildType.name}.apk"
        }
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
    
    // Security - EncryptedSharedPreferences
    implementation("androidx.security:security-crypto:1.1.0-alpha06")
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    // Google Play Billing Library para compras in-app (v7+ requerido por Play Console desde ago 2025)
    // Nota: billing 8.x requiere Kotlin 2.1+; usar 7.1.1 hasta migrar Kotlin
    implementation("com.android.billingclient:billing-ktx:7.1.1")

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

