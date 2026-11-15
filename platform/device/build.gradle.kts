// Android adapters for device capabilities: permission and feature probes, settings screens.
plugins {
    alias(libs.plugins.goalocus.android.library)
    alias(libs.plugins.goalocus.hilt)
}

android {
    namespace = "dev.goalocus.platform.device"
}

dependencies {
    api(projects.core.domain)
    implementation(libs.androidx.core.ktx)
}
