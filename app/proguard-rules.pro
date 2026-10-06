# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

# Room Database ProGuard Rules
-keepclassmembers class * extends androidx.room.RoomDatabase {
    <init>();
}
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**
-keep class com.example.data.local.entity.** { *; }
-keep class com.example.data.local.dao.** { *; }
-keep class com.example.domain.model.** { *; }
-keep class com.example.data.local.converter.** { *; }
