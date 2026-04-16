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

group = "jp.co.gahojin.refreshVersions"
version = "0.9.0"

dependencies {
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.okhttp.core)
    implementation(libs.okhttp.coroutines)
    implementation(libs.okhttp.logging.interceptor)
    implementation(libs.okio)

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
        val javaVersion = JavaVersion.toVersion(libs.versions.java.get())
        sourceCompatibility = javaVersion
        targetCompatibility = javaVersion
    }
}

kotlin {
    compilerOptions {
        javaParameters = true
        jvmTarget = JvmTarget.fromTarget(libs.versions.java.get())
        apiVersion = KotlinVersion.KOTLIN_2_0
    }
}

tasks.withType<Detekt>().configureEach {
    jvmTarget = libs.versions.java.get()
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
    jvmTarget = libs.versions.java.get()
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

gradlePlugin {
    plugins {
        create(group.toString()) {
            id = group.toString()
            implementationClass = "jp.co.gahojin.refreshVersions.CorePlugin"
            version = version.toString()
        }
    }
}

mavenPublishing {
    configure(GradlePlugin(
        javadocJar = JavadocJar.Dokka("dokkaGeneratePublicationJavadoc"),
        sourcesJar = SourcesJar.Sources(),
    ))

    publishToMavenCentral()
    signAllPublications()

    coordinates(group.toString(), "${group}.gradle.plugin", version.toString())

    pom {
        name = "${group}.gradle.plugin"
        description = "Refresh Versions Plugin"
        url = "https://github.com/gahojin/refresh-versions-plugin/"
        licenses {
            license {
                name = "Apache-2.0"
                url = "https://www.apache.org/licenses/LICENSE-2.0.txt"
                distribution = "repo"
            }
        }
        developers {
            developer {
                id = "gahojin"
                name = "GAHOJIN, Inc."
                url = "https://github.com/gahojin/"
            }
        }
        scm {
            url = "https://github.com/refresh-versions-plugin/"
            connection = "scm:git:git://github.com/refresh-versions-plugin.git"
            developerConnection = "scm:git:ssh://git@github.com/refresh-versions-plugin.git"
        }
    }
}
