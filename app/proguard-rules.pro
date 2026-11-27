# Keep stack trace line numbers for Play Console deobfuscation (upload mapping.txt)
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# kotlinx.serialization (defense-in-depth alongside bundled consumer rules)
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class io.github.mcello23.onda.data.**$$serializer { *; }
-keepclassmembers class io.github.mcello23.onda.data.** {
    *** Companion;
}
-keepclasseswithmembers class io.github.mcello23.onda.data.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# AndroidSVG uses reflection internally on SVG elements and enums
-keep class com.caverock.androidsvg.** { *; }
-dontwarn com.caverock.androidsvg.**

# OkHttp optional platform integrations
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

# Media3 / Guava annotations
-dontwarn com.google.errorprone.annotations.**
-dontwarn javax.annotation.Nullable
-dontwarn javax.annotation.CheckForNull
