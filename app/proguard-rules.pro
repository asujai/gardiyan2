# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# Room Database Proguard Rules
-keep class * extends androidx.room.RoomDatabase
-keep class * implements androidx.room.RoomDatabase$Callback
-keep class * extends androidx.room.migration.Migration
-keepclassmembers class * extends androidx.room.RoomDatabase {
    void <init>(...);
}
-dontwarn androidx.room.limits.Limit
-dontwarn androidx.room.RxRoom
-dontwarn androidx.room.GuavaRoom

# Kotlin Coroutines Proguard Rules
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembernames class kotlinx.coroutines.android.HandlerContext$ScheduledRunnable {
    *** run();
}
-dontwarn kotlinx.coroutines.**

# Strip noisy debug/info logs from minified release builds while keeping warnings/errors.
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
    public static int i(...);
}

# Jetpack Compose Proguard Rules
-keepclassmembers class * extends androidx.compose.ui.platform.AbstractComposeView {
    void <init>(...);
}
-keep class androidx.compose.ui.platform.ComposeView { *; }
-keep class * implements androidx.compose.runtime.snapshots.SnapshotStateObserver { *; }
-dontwarn androidx.compose.**
