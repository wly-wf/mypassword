# SQLCipher
-keep class net.zetetic.database.** { *; }

# BouncyCastle
-keep class org.bouncycastle.** { *; }
-dontwarn org.bouncycastle.jcajce.provider.asymmetric.**
-dontwarn org.bouncycastle.jce.provider.**
-dontwarn javax.naming.**

# Security-crypto (Tink annotations)
-dontwarn com.google.errorprone.annotations.**
-dontwarn javax.annotation.**
-dontwarn javax.annotation.concurrent.**

# Room
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# Kotlin serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** { kotlinx.serialization.KSerializer serializer(...); }
-keep,includedescriptorclasses class com.mypassword.app.**$$serializer { *; }
-keepclassmembers class com.mypassword.app.** { *** Companion; }
-keepclasseswithmembers class com.mypassword.app.** { kotlinx.serialization.KSerializer serializer(...); }
