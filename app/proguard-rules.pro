# GuardianOS Shield - ProGuard rules optimizadas para bajo tamaño

# Mantener clases Room (KSP genera código diferente a kapt)
-keep class com.guardianos.shield.data.** { *; }
-keep class * implements androidx.room.RoomDatabase { *; }
-keep class androidx.room.** { *; }

# Mantener Compose
-keep class androidx.compose.runtime.** { *; }
-keep class kotlin.coroutines.** { *; }

# WebView necesario para SafeBrowser
-keepclassmembers class * extends android.webkit.WebViewClient {
    public *;
}
-keepclassmembers class * extends android.webkit.WebChromeClient {
    public *;
}

# Reducir agresivamente todo lo demás
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
}

# Eliminar metadata innecesaria
-dontwarn kotlin.reflect.jvm.internal.**
-dontwarn javax.annotation.**

# Optimizaciones específicas para bajo tamaño
-optimizationpasses 5
-allowaccessmodification
-repackageclasses ''
-flattenpackagehierarchy ''

# ============= SECURITY (EncryptedSharedPreferences) =============
-keep class androidx.security.crypto.** { *; }
-keep class com.google.crypto.tink.** { *; }
-dontwarn com.google.crypto.tink.**
-keep class com.guardianos.shield.security.SecurityHelper { *; }

# ============= VPN SERVICE (CRÍTICO) =============
-keep public class * extends android.net.VpnService {
    public <methods>;
}
-keep class com.guardianos.shield.service.DnsFilterService { *; }
-keep class com.guardianos.shield.service.AppMonitorService { *; }
-keep class com.guardianos.shield.service.LightweightMonitorService { *; }

# ============= DATASTORE =============
-keep class androidx.datastore.*.** { *; }
-keepclassmembers class * extends androidx.datastore.preferences.core.Preferences {
    <fields>;
}

# ============= GOOGLE PLAY BILLING =============
-keep class com.android.billingclient.** { *; }
-keep interface com.android.billingclient.** { *; }
-keepclassmembers class com.android.billingclient.** { *; }
-dontwarn com.android.billingclient.**

# ============= GSON & RETROFIT =============
-keepattributes Signature,*Annotation*
-keep class com.google.gson.** { *; }
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}
