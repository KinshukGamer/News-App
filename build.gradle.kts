// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
}
buildscript {
    dependencies {
        classpath("com.google.gms:google-services:4.3.15") // or latest
    }
}
//allprojects {             -because these is wrong
//    repositories {
//        google()
//        mavenCentral()
//    }
//}
