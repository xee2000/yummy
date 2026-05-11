# ─── React Native ───────────────────────────────────────────────────────────
-keep class com.facebook.react.** { *; }
-keep class com.facebook.hermes.** { *; }
-keep class com.facebook.jni.** { *; }
-dontwarn com.facebook.react.**
-dontwarn com.facebook.hermes.**

# ─── 앱 패키지 전체 보존 ─────────────────────────────────────────────────────
-keep class com.pms_parkin_mobile.** { *; }
-keep class com.woorisystem.** { *; }

# ─── Retrofit / OkHttp ───────────────────────────────────────────────────────
-keepattributes Signature
-keepattributes *Annotation*
-keep class retrofit2.** { *; }
-keep interface retrofit2.** { *; }
-dontwarn retrofit2.**
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }
-dontwarn okhttp3.**
-dontwarn okio.**

# ─── Gson ────────────────────────────────────────────────────────────────────
-keep class com.google.gson.** { *; }
-keepclassmembers class * {
    @com.google.gson.annotations.Expose <fields>;
    @com.google.gson.annotations.SerializedName <fields>;
}

# ─── Kotlin ──────────────────────────────────────────────────────────────────
-keep class kotlin.** { *; }
-keep class kotlinx.coroutines.** { *; }
-dontwarn kotlin.**
-dontwarn kotlinx.coroutines.**

# ─── Beacon Library ──────────────────────────────────────────────────────────
-keep class org.altbeacon.** { *; }
-dontwarn org.altbeacon.**

# ─── Timber ──────────────────────────────────────────────────────────────────
-dontwarn org.slf4j.**
-dontwarn com.jakewharton.timber.**

# ─── 스택트레이스 라인 번호 보존 (크래시 분석용) ──────────────────────────────
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
