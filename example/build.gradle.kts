plugins {
    id("java")
    alias(libs.plugins.detekt)
}

dependencies {
    implementation(platform(libs.kotlin.bom))
    implementation(libs.kotlin.stdlib)
    implementation(platform(libs.kotlinx.coroutines.bom))
    implementation(group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-core", version = "1.0")
    implementation(libs.zero.allocation.hashing)

    testImplementation(platform(libs.kotest.bom))
    testImplementation("io.mockk:mockk:1.13.16")
    testImplementation(libs.truth)
}
