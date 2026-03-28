# PixelProKeyboard ProGuard Rules
# Production-ready configuration for AI Voice Keyboard

# Keep all Android Services
-keep public class * extends android.app.Service
-keep public class * extends android.view.inputmethod.InputMethodService

# Keep all app classes
-keep class com.aikeyboard.** { *; }
-keepclassmembers class com.aikeyboard.** { *; }

# Keep OkHttp and Okio
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }

# Keep Kotlin coroutines
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# Keep Kotlin metadata for reflection
-keep class kotlin.Metadata { *; }
-keep class kotlin.** { *; }
-dontwarn kotlin.**

# Keep BuildConfig for API key access
-keep class com.aikeyboard.BuildConfig { *; }

# Optimization settings
-optimizationpasses 5
-dontusemixedcaseclassnames
-dontskipnonpubliclibraryclasses
-verbose

# Optimization settings for release
-optimizations !code/simplification/arithmetic,!field/*,!class/merging/*,!code/allocation/variable

# Allow optimization
-allowaccessmodification
