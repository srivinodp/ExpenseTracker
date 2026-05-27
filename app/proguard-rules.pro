# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Preserve line number and source file attributes for readable production stack traces
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Safety rules for Room DB entity preservation
-keep @androidx.room.Entity class * { *; }