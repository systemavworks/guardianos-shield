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
