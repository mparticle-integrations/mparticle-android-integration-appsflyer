buildscript {
    val kotlinVersion = "1.7.10"
    if (!project.hasProperty("version") || project.version == "unspecified") {
        project.version = '+'
    }
    repositories {
        google()
        mavenLocal()
        mavenCentral()
    }
    dependencies {
        classpath("com.android.tools.build:gradle:7.2.2")
        classpath("com.mparticle:android-kit-plugin:5.44.0")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.7.10")
    }
}

tasks.register("clean", Delete::class) {
    delete(rootProject.buildDir)
}