plugins {
    id("java")
    alias(libs.plugins.dokka)
    alias(libs.plugins.detekt)
}

dependencies {
    implementation(platform(libs.kotlin.bom))
    implementation(libs.kotlin.stdlib)
    implementation(platform(libs.kotlinx.coroutines.bom))
    implementation(group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-core", version = "1.10.1")
    implementation(libs.zero.allocation.hashing)

    testImplementation(platform(libs.kotest.bom))
    testImplementation("io.mockk:mockk:1.13.17")
    testImplementation(libs.truth)
}
