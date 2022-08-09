
plugins {
    id("com.android.library")
    id("mparticle-kit-plugin")
    kotlin("android")
}

repositories {
    google()
    mavenCentral()
}

android {
    compileSdk = 31
    defaultConfig {
        minSdk = 16
    }
}

dependencies {
    api("com.appsflyer:af-android-sdk:6.8.0")
}