# Add project specific ProGuard rules here.
-keep public class * extends android.app.Service
-keep public class * extends android.view.inputmethod.InputMethodService
-keep class com.aikeyboard.** { *; }

# Keep OkHttp
-dontwarn okhttp3.**
-keep class okhttp3.** { *; }

# Keep Kotlin coroutines
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}
