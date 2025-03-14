plugins {
    id("java")
    alias(libs.plugins.dokka)
}

dependencies {
    implementation(platform(libs.kotlin.bom))
    implementation(libs.kotlin.stdlib)
    implementation(platform(libs.kotlinx.coroutines.bom))

    testImplementation(platform(libs.kotest.bom))
    testImplementation(libs.mockk)
    testImplementation(libs.truth)
}
