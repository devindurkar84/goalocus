package dev.goalocus.buildlogic

import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.CommonExtension
import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.getByType

/** `goalocus.android.application` — the app module. Kotlin is built into AGP 9; no kotlin-android plugin. */
class AndroidApplicationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            apply(plugin = "com.android.application")
            extensions.configure<ApplicationExtension> {
                configureKotlinAndroid(this)
                defaultConfig.targetSdk = GoalocusSdk.TARGET
                testOptions.animationsDisabled = true
            }
            addUnitTestDependencies()
        }
    }
}

/** `goalocus.android.library` — Android library modules (designsystem, platform adapters). */
class AndroidLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            apply(plugin = "com.android.library")
            extensions.configure<LibraryExtension> {
                configureKotlinAndroid(this)
                testOptions.targetSdk = GoalocusSdk.TARGET
                lint.targetSdk = GoalocusSdk.TARGET
            }
            addUnitTestDependencies()
        }
    }
}

/** `goalocus.android.compose` — adds Compose to an Android app or library module. Apply after the Android plugin. */
class AndroidComposeConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.withPlugin("com.android.base") {
                apply(plugin = "org.jetbrains.kotlin.plugin.compose")
                extensions.getByType<CommonExtension>().buildFeatures.compose = true
                dependencies {
                    val bom = libs.findLibrary("androidx-compose-bom").get()
                    "implementation"(platform(bom))
                    "implementation"(libs.findLibrary("androidx-compose-ui-tooling-preview").get())
                    "debugImplementation"(libs.findLibrary("androidx-compose-ui-tooling").get())
                }
            }
        }
    }
}

/** `goalocus.jvm.library` — pure Kotlin modules (domain, content). No Android dependency allowed. */
class JvmLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            apply(plugin = "org.jetbrains.kotlin.jvm")
            configureKotlinJvm()
            addUnitTestDependencies()
        }
    }
}

/** `goalocus.hilt` — Hilt with KSP for Android modules, plain hilt-core for JVM modules. */
class HiltConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            apply(plugin = "com.google.devtools.ksp")
            dependencies {
                "ksp"(libs.findLibrary("hilt-compiler").get())
                "ksp"(libs.findLibrary("kotlin-metadata").get())
            }
            pluginManager.withPlugin("org.jetbrains.kotlin.jvm") {
                dependencies { "implementation"(libs.findLibrary("hilt-core").get()) }
            }
            pluginManager.withPlugin("com.android.base") {
                apply(plugin = "dagger.hilt.android.plugin")
                dependencies { "implementation"(libs.findLibrary("hilt-android").get()) }
            }
        }
    }
}
