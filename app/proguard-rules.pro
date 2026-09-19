# Add project specific ProGuard rules here.
# See https://developer.android.com/studio/build/shrink-code for more details.

# ---- HomeSajja release rules (R8) ----

# Firestore turns documents into these classes and back by reflection (field and enum names are the stored data),
# so shrinking or renaming them would silently break every screen.
-keep class com.homesajja.app.data.model.** { *; }
-keepclassmembers enum com.homesajja.app.data.model.** { *; }

# Keep what reflection-based libraries read from annotations and generics.
-keepattributes Signature, *Annotation*, EnclosingMethod, InnerClasses, RuntimeVisibleAnnotations

# The map library and its optional dependencies.
-dontwarn org.osmdroid.**
