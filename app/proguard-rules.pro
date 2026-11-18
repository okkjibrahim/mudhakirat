# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# The web page calls these methods by name through the JS bridge (window.AndroidBridge.xxx()).
# If R8 renames or removes them, every call from index.html silently stops working.
-keepclassmembers class com.example.MainActivity$WebAppInterface {
    @android.webkit.JavascriptInterface <methods>;
}
-keepattributes JavascriptInterface

# System components started by the OS/Firebase by their class name (manifest, FCM, AlarmManager).
-keep class com.example.MainActivity { *; }
-keep class com.example.FcmService { *; }
-keep class com.example.AlarmReceiver { *; }
-keep class com.example.BootReceiver { *; }
-keep class com.example.AlarmScheduler { *; }
-keep class com.example.Notifier { *; }

# Firebase Messaging / AppCheck use reflection internally.
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**

# Keep stack traces readable if a crash report ever needs them.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
