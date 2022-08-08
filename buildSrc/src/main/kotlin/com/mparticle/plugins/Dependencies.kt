package com.mparticle.plugins

object Versions {

    // Project
    const val kotlin = "1.7.10"

    const val compileSdkVersion = 31
    const val minSdkVersion = 16
    const val targetSdkVersion = 31
    const val applicationId = "com.mparticle.kits.appsflyer"
    const val applicationVersionCode = 1
    const val applicationVersionName = "1.0.0"

    // Android
    const val appcompat = "1.4.2"

    // Testing
    const val junit = "4.13.2"
    const val testRunner = "1.1.1"
    const val espressoCore = "3.1.1"
}

object Libraries {

    // Kotlin
    const val kotlinStdLib = "org.jetbrains.kotlin:kotlin-stdlib-jdk8:${Versions.kotlin}"

    // Android
    const val appcompat = "androidx.appcompat:appcompat:${Versions.appcompat}"

    // Testing
    const val junit = "junit:junit:${Versions.junit}"
    const val testRunner = "androidx.test:runner:${Versions.testRunner}"
    const val espressoCore = "androidx.test.espresso:espresso-core:${Versions.espressoCore}"
}
