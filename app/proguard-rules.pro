# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

# Keep data classes for serialization
-keepclassmembers class * {
    @kotlinx.serialization.SerialName <fields>;
}

# Keep Room entities
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# Supabase
-dontwarn io.github.jan.supabase.**
-keep class io.github.jan.supabase.** { *; }

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**

# Kotlinx Serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

# Keep Serializers
-keep,includedescriptorclasses class com.recall.app.**$$serializer { *; }
-keepclassmembers class com.recall.app.** {
    *** Companion;
}
-keepclasseswithmembers class com.recall.app.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Sentry
-keep class io.sentry.** { *; }
-dontwarn io.sentry.**

# PostHog
-keep class com.posthog.** { *; }
-dontwarn com.posthog.**
