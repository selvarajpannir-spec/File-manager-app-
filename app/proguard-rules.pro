# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

# PDFBox Android & BouncyCastle rules
-dontwarn com.tom_roush.**
-dontwarn org.bouncycastle.**
-dontwarn javax.xml.stream.**
-dontwarn java.awt.**
-keep class com.tom_roush.pdfbox.** { *; }
-keep class org.bouncycastle.** { *; }

# Room Database & SQLite Entity rules
-keep class com.example.data.** { *; }
-keep class androidx.room.** { *; }
-keepclassmembers class * extends androidx.room.RoomDatabase { *; }
-dontwarn androidx.room.paging.**

# App UI, Models & ViewModels
-keep class com.example.ui.FileManagerViewModel { *; }
-keep class com.example.util.** { *; }
-keep class com.example.service.** { *; }

# Coroutines & Lifecycle
-keepclassmembers class * extends androidx.lifecycle.ViewModel { *; }
