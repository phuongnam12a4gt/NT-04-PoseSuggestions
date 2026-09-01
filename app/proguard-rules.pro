# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in D:\Android\Sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.

# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep rules here:

# Kotlin Serialization
-keepattributes *Annotation*, InnerClasses
-keepclassmembers class * {
    @kotlinx.serialization.Serializable *;
}
-keepclassmembers class * {
    @kotlinx.serialization.SerialName *;
}

# ML Kit Pose Detection
-keep class com.google.mlkit.** { *; }
-keep class com.google.android.gms.internal.mlkit_vision_pose.** { *; }
-keep class com.google.android.gms.internal.mlkit_vision_subject_segmentation.** { *; }

# Keep data models
-keep class com.ppnnttt.posesuggestions.LandmarkTemplate { *; }
-keep class com.ppnnttt.posesuggestions.PoseTemplate { *; }
-keep class com.ppnnttt.posesuggestions.RecommendationMetadata { *; }
-keep class com.ppnnttt.posesuggestions.PoseTemplatesConfig { *; }
-keep class com.ppnnttt.posesuggestions.PoseRecording { *; }
-keep class com.ppnnttt.posesuggestions.PoseFrame { *; }

# WorkManager
-keep class androidx.work.impl.** { *; }
-dontwarn androidx.work.impl.**
-keep class androidx.work.impl.WorkDatabase_Impl { *; }
-keep class androidx.work.impl.background.systemjob.SystemJobService { *; }
-keep class * extends androidx.work.ListenableWorker { *; }

# Room
-keep class * extends androidx.room.RoomDatabase
-keep class androidx.room.MultiInstanceInvalidationService { *; }

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
#-rename sourcefileattribute SourceFile
