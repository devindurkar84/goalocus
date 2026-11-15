// Theme, colours, type and shared composables. No feature logic lives here.
plugins {
    alias(libs.plugins.goalocus.android.library)
    alias(libs.plugins.goalocus.android.compose)
}

android {
    namespace = "dev.goalocus.core.designsystem"
}

dependencies {
    api(libs.androidx.compose.material3)
    api(libs.androidx.compose.ui)
}
