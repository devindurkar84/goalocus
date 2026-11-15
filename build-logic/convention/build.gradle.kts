import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    `kotlin-dsl`
}

group = "dev.goalocus.buildlogic"

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_17
    }
}

dependencies {
    compileOnly(libs.android.gradlePlugin)
    compileOnly(libs.compose.gradlePlugin)
    compileOnly(libs.kotlin.gradlePlugin)
    compileOnly(libs.ksp.gradlePlugin)
}

tasks {
    validatePlugins {
        enableStricterValidation = true
        failOnWarning = true
    }
}

gradlePlugin {
    plugins {
        register("androidApplication") {
            id = libs.plugins.goalocus.android.application.get().pluginId
            implementationClass = "dev.goalocus.buildlogic.AndroidApplicationConventionPlugin"
        }
        register("androidLibrary") {
            id = libs.plugins.goalocus.android.library.get().pluginId
            implementationClass = "dev.goalocus.buildlogic.AndroidLibraryConventionPlugin"
        }
        register("androidCompose") {
            id = libs.plugins.goalocus.android.compose.get().pluginId
            implementationClass = "dev.goalocus.buildlogic.AndroidComposeConventionPlugin"
        }
        register("jvmLibrary") {
            id = libs.plugins.goalocus.jvm.library.get().pluginId
            implementationClass = "dev.goalocus.buildlogic.JvmLibraryConventionPlugin"
        }
        register("hilt") {
            id = libs.plugins.goalocus.hilt.get().pluginId
            implementationClass = "dev.goalocus.buildlogic.HiltConventionPlugin"
        }
        register("contentPack") {
            id = libs.plugins.goalocus.content.pack.get().pluginId
            implementationClass = "dev.goalocus.buildlogic.ContentPackConventionPlugin"
        }
    }
}
