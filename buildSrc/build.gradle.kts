import org.gradle.kotlin.dsl.`kotlin-dsl`
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    `kotlin-dsl`
    `kotlin-dsl-precompiled-script-plugins`
}

gradlePlugin {
    plugins {
        register("mparticle-kit-plugin") {
            id = "mparticle-kit-plugin"
            implementationClass = "com.mparticle.plugins.KitPlugin"
        }
    }
}

repositories {
    google()
    mavenLocal()
    mavenCentral()
}

dependencies {
    implementation("com.android.tools.build:gradle:7.2.2")
//    implementation("com.android.tools.build:gradle-api:7.2.2")
//    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.7.0")
//    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:1.5.31")
}