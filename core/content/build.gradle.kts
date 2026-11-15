// Pure Kotlin: parses the compiled content pack and models the topic graph.
plugins {
    alias(libs.plugins.goalocus.jvm.library)
    alias(libs.plugins.kotlin.serialization)
}

dependencies {
    api(projects.core.domain)
    // api: StreamContentSource takes a CoroutineDispatcher in its public constructor.
    api(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.json)

    testImplementation(libs.kotlinx.coroutines.test)
}
