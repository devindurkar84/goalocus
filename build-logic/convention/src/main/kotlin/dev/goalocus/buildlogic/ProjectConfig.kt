package dev.goalocus.buildlogic

import com.android.build.api.dsl.CommonExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.kotlin.dsl.assign
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.getByType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinBaseExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension

/** SDK levels for every Android module. See architecture §2 (targetSdk 37, minSdk 31). */
internal object GoalocusSdk {
    const val COMPILE = 37
    const val TARGET = 37
    const val MIN = 31
}

private val JAVA_VERSION = JavaVersion.VERSION_17
private val JVM_TARGET = JvmTarget.JVM_17

internal val Project.libs: VersionCatalog
    get() = extensions.getByType<VersionCatalogsExtension>().named("libs")

internal fun Project.configureKotlinAndroid(android: CommonExtension) {
    android.apply {
        compileSdk = GoalocusSdk.COMPILE
        defaultConfig.apply {
            minSdk = GoalocusSdk.MIN
        }
        compileOptions.apply {
            sourceCompatibility = JAVA_VERSION
            targetCompatibility = JAVA_VERSION
        }
    }
    configureKotlin<KotlinAndroidProjectExtension>()
}

internal fun Project.configureKotlinJvm() {
    extensions.configure<JavaPluginExtension> {
        sourceCompatibility = JAVA_VERSION
        targetCompatibility = JAVA_VERSION
    }
    configureKotlin<KotlinJvmProjectExtension>()
}

internal fun Project.addUnitTestDependencies() {
    dependencies {
        "testImplementation"(libs.findLibrary("kotlin-test").get())
        // Explicit: the Kotlin plugin only infers the JUnit flavour (kotlin.test.Test) for JVM modules.
        "testImplementation"(libs.findLibrary("kotlin-test-junit").get())
        "testImplementation"(libs.findLibrary("junit").get())
    }
}

private inline fun <reified T : KotlinBaseExtension> Project.configureKotlin() = configure<T> {
    // Opt in with -PwarningsAsErrors=true (CI does) to keep the codebase warning-free.
    val warningsAsErrors = providers.gradleProperty("warningsAsErrors").map(String::toBoolean).orElse(false)
    val options = when (this) {
        is KotlinAndroidProjectExtension -> compilerOptions
        is KotlinJvmProjectExtension -> compilerOptions
        else -> error("Unsupported Kotlin extension ${T::class}")
    }
    options.jvmTarget = JVM_TARGET
    options.allWarningsAsErrors = warningsAsErrors
}
