package com.mparticle.plugins

import com.android.build.gradle.BaseExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

class KitPlugin : Plugin<Project> {

    private val Project.android: BaseExtension
        get() = extensions.findByName("android") as? BaseExtension
            ?: error("Not an Android module: $name")

    override fun apply(project: Project) =
        with(project) {
            applyPlugins()
            androidConfig()
            dependenciesConfig()
        }

    private fun Project.applyPlugins() {
        plugins.run {
            apply("com.android.library")
            apply("kotlin-android")
            apply("kotlin-android-extensions")
        }
    }

    private fun Project.androidConfig() {
        android.run {
            compileSdkVersion(31)
            defaultConfig {
                minSdk = 16
                targetSdk = 31
                versionCode = 1
                versionName = "1.0"
                testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
            }
            buildTypes {
                getByName("debug") {
                    isMinifyEnabled = false
                }
            }
        }
    }

    private fun Project.dependenciesConfig() {
        dependencies {
            "api"("com.mparticle:android-kit-base:5.44.0")
            "testImplementation"("junit:junit:4.13.2")
            "testImplementation"("org.mockito:mockito-core:1.10.19")
            "testImplementation"("androidx.annotation:annotation:[1.0.0,)")
            "compileOnly"("androidx.annotation:annotation:[1.0.0,)")
        }
    }
}
