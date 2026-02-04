import com.vanniktech.maven.publish.GradlePlugin
import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.SourcesJar
import dev.detekt.gradle.Detekt
import dev.detekt.gradle.DetektCreateBaselineTask
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

plugins {
    alias(libs.plugins.detekt)
    alias(libs.plugins.dokka)
    alias(libs.plugins.dokka.javadoc)
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlinx.kover)
    alias(libs.plugins.maven.publish)
    id("java-gradle-plugin")
    id("signing")
}

group = Maven.PLUGIN_ID
version = Maven.VERSION

dependencies {
    implementation(platform(libs.kotlin.bom))
    implementation(libs.kotlin.stdlib)
    implementation(platform(libs.kotlinx.coroutines.bom))
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.okhttp.core)
    implementation(libs.okhttp.coroutines)
    implementation(libs.okhttp.logging.interceptor)
    implementation(libs.okio)

    testImplementation(platform(libs.kotlinx.coroutines.bom))
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(platform(libs.kotest.bom))
    testImplementation(libs.kotest.runner.junit5)
    testImplementation(libs.kotest.assertions.core)
    testImplementation(libs.kotest.property)
    testImplementation(libs.mockk)
}

detekt {
    parallel = true
    buildUponDefaultConfig = true
    allRules = false
    autoCorrect = true
    config.from(rootDir.resolve("../config/detekt.yml"))
}

java {
    toolchain {
        sourceCompatibility = Build.jvmTarget
        targetCompatibility = Build.jvmTarget
    }
}

kotlin {
    compilerOptions {
        javaParameters = true
        jvmTarget = JvmTarget.fromTarget(Build.jvmTarget.toString())
        apiVersion = KotlinVersion.KOTLIN_2_0
    }
}

tasks.withType<Detekt>().configureEach {
    jvmTarget = Build.jvmTarget.toString()
    reports {
        html.required = false
        checkstyle.required = false
        sarif.required = true
        markdown.required = true
    }
    exclude("build/")
    exclude("resources/")
}

tasks.withType<DetektCreateBaselineTask>().configureEach {
    jvmTarget = Build.jvmTarget.toString()
    exclude("build/")
    exclude("resources/")
}

tasks.withType<Test>().configureEach {
    systemProperties = System.getProperties().asIterable().associate { it.key.toString() to it.value }
    useJUnitPlatform()
    testLogging {
        showStandardStreams = true
        events("passed", "skipped", "failed")
    }
    jvmArgs("-XX:+EnableDynamicAgentLoading")
}

signing {
    useGpgCmd()
    sign(publishing.publications)
}

gradlePlugin {
    plugins {
        create(Maven.PLUGIN_ID) {
            id = Maven.PLUGIN_ID
            implementationClass = Maven.IMPLEMENTATION_CLASS
            version = Maven.VERSION
        }
    }
}

mavenPublishing {
    configure(GradlePlugin(
        javadocJar = JavadocJar.Dokka("dokkaGeneratePublicationJavadoc"),
        sourcesJar = SourcesJar.Sources(),
    ))

    publishToMavenCentral()

    coordinates(Maven.PLUGIN_ID, Maven.ARTIFACT_ID, Maven.VERSION)

    pom {
        name = Maven.ARTIFACT_ID
        description = Maven.DESCRIPTION
        url = "https://github.com/${Maven.GITHUB_REPOSITORY}/"
        licenses {
            license {
                name = Maven.LICENSE_NAME
                url = Maven.LICENSE_URL
                distribution = Maven.LICENSE_DIST
            }
        }
        developers {
            developer {
                id = Maven.DEVELOPER_ID
                name = Maven.DEVELOPER_NAME
                url = Maven.DEVELOPER_URL
            }
        }
        scm {
            url = "https://github.com/${Maven.GITHUB_REPOSITORY}/"
            connection = "scm:git:git://github.com/${Maven.GITHUB_REPOSITORY}.git"
            developerConnection = "scm:git:ssh://git@github.com/${Maven.GITHUB_REPOSITORY}.git"
        }
    }
}
