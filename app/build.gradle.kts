plugins {
    alias(libs.plugins.goalocus.android.application)
    alias(libs.plugins.goalocus.android.compose)
    alias(libs.plugins.goalocus.hilt)
    alias(libs.plugins.goalocus.content.pack)
}

android {
    namespace = "dev.goalocus"

    defaultConfig {
        applicationId = "dev.goalocus"
        versionCode = 1
        versionName = "0.1.0"
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
        }
        release {
            // Personal sideloaded build for now (product spec §1). Shrinking comes with Play Store prep.
            isMinifyEnabled = false
        }
    }
}

dependencies {
    implementation(projects.core.domain)
    implementation(projects.core.content)
    implementation(projects.core.designsystem)
    implementation(projects.platform.device)

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.hilt.lifecycle.viewmodel.compose)
    implementation(libs.kotlinx.coroutines.android)

    testImplementation(libs.kotlinx.coroutines.test)
}
