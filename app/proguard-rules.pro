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

# Keep model classes
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# Keep Gson methods
-keep public class com.google.gson.** { *; }

-keep class com.example.randomcase.** { *; }
# Keep the service implementation
-keep class com.example.randomcase.CustomAccessibilityService { *; }

# Keep service-related annotations
-keepattributes *Annotation*

# Prevent Compose runtime issues
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

-keep class com.example.randomcase.MainActivity { *; }