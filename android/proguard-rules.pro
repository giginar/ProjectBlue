# libGDX invokes native methods through JNI. Keep their names and declaring classes.
-keepclasseswithmembernames,includedescriptorclasses class * {
    native <methods>;
}
# Application code uses explicit JSON parsing, with no reflective model deserialization.
# Google SDK consumer rules are supplied by their AARs; do not keep whole packages.
-assumenosideeffects class android.util.Log {
    public static *** v(...);
    public static *** d(...);
    public static *** i(...);
}
