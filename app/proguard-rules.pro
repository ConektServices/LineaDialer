# ── Linea ProGuard rules ────────────────────────────────────────────────────

# Compose
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# Room — keep entity + DAO classes
-keep class androidx.room.** { *; }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }
-keep @androidx.room.Database class * { *; }
-keepclassmembers class * {
    @androidx.room.* <methods>;
}

# Coil
-keep class coil.** { *; }
-dontwarn coil.**

# WorkManager
-keep class androidx.work.** { *; }
-keep class * extends androidx.work.Worker
-keep class * extends androidx.work.CoroutineWorker
-keepclassmembers class * extends androidx.work.CoroutineWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}

# Telecom
-keep class com.linea.dialer.telecom.** { *; }
-keep class android.telecom.** { *; }

# Data models + DB entities
-keep class com.linea.dialer.data.** { *; }

# ViewModels
-keep class com.linea.dialer.ui.viewmodel.** { *; }

# Navigation SafeArgs type info
-keep class androidx.navigation.** { *; }

# Accompanist Permissions
-keep class com.google.accompanist.permissions.** { *; }

# Kotlin coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembernames class kotlinx.** { volatile <fields>; }

# Keep Parcelable + Serializable
-keepclassmembers class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}
