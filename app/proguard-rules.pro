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

# Aturan ini saja sudah cukup untuk Data Class (Sesuaikan dengan package Entitas Anda)
-keepclassmembers class id.biz.yuka.libperformx.entity.** {
    <fields>;
}

# Biarkan R8 bekerja, kita hanya matikan warningnya saja
-dontwarn java.lang.invoke.**
-dontwarn io.netty.**
-dontwarn io.ktor.**
-dontwarn java.nio.file.**
-dontwarn org.slf4j.**
-dontwarn org.codehaus.mojo.**
-dontwarn io.github.jan.supabase.**
-dontwarn kotlinx.serialization.**
-dontwarn kotlinx.coroutines.**
-dontwarn com.google.mlkit.**
-dontwarn com.google.android.gms.**
-dontwarn androidx.camera.**