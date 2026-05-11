# Keep Hilt-generated classes
-keep class dagger.hilt.android.internal.managers.** { *; }
-keep class * extends androidx.lifecycle.ViewModel { *; }

# kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-keepclasseswithmembers class **$$serializer { *; }
-keepclassmembers class * {
    @kotlinx.serialization.Serializable <methods>;
}

# Keep capability registry data classes (parsed at runtime from JSON)
-keep class com.chargepilot.core.model.** { *; }
-keep class com.chargepilot.core.capability.** { *; }
