plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
}

kotlin { jvmToolchain(17) }

dependencies {
    implementation(libs.kotlinx.coroutines.core)      // ensureActive() in generator
    implementation(libs.kotlinx.serialization.json)
    testImplementation(libs.kotlin.test)
    testImplementation(libs.kotlinx.coroutines.test)
}

tasks.test {
    useJUnitPlatform()
    testLogging { showStandardStreams = true }
}
