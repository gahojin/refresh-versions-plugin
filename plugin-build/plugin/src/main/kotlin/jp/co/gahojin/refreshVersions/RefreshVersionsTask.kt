/*
 * (C) 2025 GAHOJIN, Inc.
 */
package jp.co.gahojin.refreshVersions

import jp.co.gahojin.refreshVersions.dependency.MavenDependencyVersionsFetcher
import jp.co.gahojin.refreshVersions.ext.getDefaultVersionCatalog
import jp.co.gahojin.refreshVersions.ext.globalRepositories
import jp.co.gahojin.refreshVersions.ext.register
import jp.co.gahojin.refreshVersions.toml.TomlFile
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import org.gradle.api.DefaultTask
import org.gradle.api.InvalidUserCodeException
import org.gradle.api.Project
import org.gradle.api.artifacts.ConfigurationContainer
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.artifacts.repositories.ArtifactRepository
import org.gradle.api.artifacts.repositories.MavenArtifactRepository
import org.gradle.api.internal.artifacts.repositories.DefaultMavenArtifactRepository
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import java.io.File

abstract class RefreshVersionsTask : DefaultTask() {
    init {
        group = Constants.GROUP
    }

    @get:Internal
    abstract val versionsTomlFile: Property<File>

    @get:Internal
    abstract val sortSection: Property<Boolean>

    private val versionCatalog = project.getDefaultVersionCatalog()

    @TaskAction
    fun refreshVersions() {
        val versionCatalog = versionCatalog ?: run {
            logger.lifecycle("versionsCatalog disabled.")
            return
        }
        val file = requireNotNull(versionsTomlFile.orNull)
//        if (file.exists()) {
//            val toml = TomlFile.parseToml(file.readText())
//            logger.lifecycle("${toml.sections}")
//        }

        // configuration構文で定義した依存を抽出
        runBlocking {
            recordBuildscriptAndRegularDependencies(project)
                .forEach {
                    logger.lifecycle("${it.first} : ${it.second.joinToString()}")
                }
//            LookupVersionsCandidates()
//                .execute(repositories, versionCatalog.libraries(), emptySet())
        }
        logger.lifecycle("versions ${versionCatalog.versions().entries.joinToString { "${it.key}:${it.value}" }}")
        logger.lifecycle("libraries ${versionCatalog.libraries().joinToString { "${it.group}:${it.name}:${it.versionConstraint}, ${versionCatalog.versions().values.find {v -> it.versionConstraint == v }} " }}")
        logger.lifecycle("plugins ${versionCatalog.plugins().joinToString { "${it.pluginId}:${it.version}" }}")
    }

    fun recordBuildscriptAndRegularDependencies(rootProject: Project): List<Pair<Dependency, List<ArtifactRepository>>> {
        val allDependencies = mutableListOf<Pair<Dependency, List<ArtifactRepository>>>()
        val globalRepositories = ConfigHolder.settings.globalRepositories
        rootProject.allprojects {
            val repositories = it.repositories + globalRepositories
            try {
                it.afterEvaluate {
                    val dependencies = extractDependencies(it.configurations, repositories)
                    allDependencies.addAll(dependencies)
                }
            } catch (_: InvalidUserCodeException) {
                val dependencies = extractDependencies(it.configurations, repositories)
                allDependencies.addAll(dependencies)
            }
        }
        return allDependencies
    }

    private fun extractDependencies(
        configurations: ConfigurationContainer,
        repositories: List<ArtifactRepository>,
    ): Sequence<Pair<Dependency, List<ArtifactRepository>>> {
        // TODO 仮実装
        val mavenRepositories = repositories.filterIsInstance<DefaultMavenArtifactRepository>()
        return configurations
            .asSequence()
            .flatMap {
                it.dependencies.asSequence()
            }.mapNotNull { rawDependency ->
                // project(...)の依存は除外
                if (rawDependency is ProjectDependency) return@mapNotNull null
                // TODO リポジトリの制約を使用する

                rawDependency to mavenRepositories
            }
    }

    companion object {
        private const val TASK_NAME = "refreshVersions"

        fun register(project: Project, extensions: RefreshVersionsExtension) {
            project.tasks.register<RefreshVersionsTask>(TASK_NAME) {
                it.versionsTomlFile.set(extensions.getVersionsTomlFile())
                it.sortSection.set(extensions.sortSection)
            }
        }
    }
}
