# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in /sdk/tools/proguard/proguard-android.txt

# Keep OkHttp
-dontwarn okhttp3.**
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }

# Keep Okio
-dontwarn okio.**
-keep class okio.** { *; }

# Keep Gson
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn sun.misc.**
-keep class com.google.gson.stream.** { *; }
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# Keep only necessary public APIs from app package
# Don't keep API keys - let them be obfuscated
-keep class com.aikeyboard.keyboard.** { public *; }
-keep class com.aikeyboard.settings.** { public *; }
-keep class com.aikeyboard.voice.GeminiVoiceClient { public *; }
-keep class com.aikeyboard.translation.ZAiClient { public *; }
-keep class com.aikeyboard.ui.theme.** { *; }

# Obfuscate the Application class but keep it for manifest
-keep class com.aikeyboard.AiKeyboardApp { public <init>(); }

# Keep Kotlin coroutines
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# Keep Compose
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# Remove logging in release
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
    public static int i(...);
}
