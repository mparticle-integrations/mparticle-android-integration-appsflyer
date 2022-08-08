import com.mparticle.plugins.KitPlugin
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("com.android.library")
    id("mparticle-kit-plugin")
    kotlin("android")
}

repositories {
    google()
    mavenCentral()
}

//apply(plugin = "com.mparticle.kit")

//android {
//    compileSdk = 31
//    defaultConfig {
//        minSdk = 16
//    }
//}

dependencies {
    //api("com.mparticle:android-kit-base:5.44.0")
    api("com.appsflyer:af-android-sdk:6.8.0")
}