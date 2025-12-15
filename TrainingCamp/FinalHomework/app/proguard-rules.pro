# 基础混淆配置（配合 proguard-android-optimize.txt）

# Room：保留注解与生成的 Schema 访问
-keep class androidx.room.** { *; }
-keepclassmembers class * extends androidx.room.RoomDatabase {
    *;
}
-dontwarn androidx.room.**

# Gson：反射模型保持字段名
-keep class com.google.gson.** { *; }
-dontwarn com.google.gson.**

# PDFBox
-dontwarn org.bouncycastle.**
-dontwarn com.tom_roush.pdfbox.**
-keep class com.tom_roush.pdfbox.** { *; }
-keep class org.apache.commons.logging.** { *; }

# Markwon & CommonMark
-dontwarn org.commonmark.**
-dontwarn io.noties.markwon.**

# Glide
-keep class com.bumptech.glide.** { *; }
-keep public class * implements com.bumptech.glide.module.GlideModule
-dontwarn com.bumptech.glide.**

# TensorFlow Lite
-dontwarn org.tensorflow.**
-keep class org.tensorflow.** { *; }

# OkHttp/Okio
-dontwarn okhttp3.**
-dontwarn okio.**

# 保留方法参数名（便于反射使用）
-keepattributes Signature,RuntimeVisibleAnnotations,RuntimeVisibleParameterAnnotations,RuntimeInvisibleAnnotations,RuntimeInvisibleParameterAnnotations,EnclosingMethod,InnerClasses,SourceFile,LineNumberTable