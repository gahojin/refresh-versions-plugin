/*
 * (C) 2025 GAHOJIN, Inc.
 */
package jp.co.gahojin.refreshVersions

import jp.co.gahojin.refreshVersions.extension.getSettingsFile
import jp.co.gahojin.refreshVersions.extension.register
import jp.co.gahojin.refreshVersions.internal.SettingsCleaner
import jp.co.gahojin.refreshVersions.internal.VersionCatalogCleaner
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.provider.Property
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import java.io.File

abstract class RefreshVersionsCleanupTask : DefaultTask() {
    init {
        group = Constants.GROUP
    }

    @get:InputFile
    abstract val versionsTomlFile: Property<File>

    @get:Internal
    abstract val sortSection: Property<Boolean>

    @get:InputFile
    @get:Optional
    abstract val rootSettingsFile: Property<File>

    @get:InputFile
    @get:Optional
    abstract val buildSrcSettingsFile: Property<File>

    @TaskAction
    fun cleanUp() {
        VersionCatalogCleaner.execute(
            file = versionsTomlFile.get(),
            sortSection = sortSection.getOrElse(false),
        )

        // settings.gradle(.kts)ファイルを更新
        SettingsCleaner.execute(
            files = listOf(rootSettingsFile, buildSrcSettingsFile).mapNotNull { it.orNull },
        )
    }

    companion object {
        private const val TASK_NAME = "refreshVersionsCleanup"

        @JvmStatic
        fun register(project: Project, extensions: RefreshVersionsExtension) {
            project.tasks.register<RefreshVersionsCleanupTask>(TASK_NAME) {
                it.versionsTomlFile.set(extensions.getVersionsTomlFile())
                it.sortSection.set(extensions.sortSection)
                it.rootSettingsFile.set(project.getSettingsFile())
                it.buildSrcSettingsFile.set(project.getSettingsFile("buildSrc/"))
            }
        }
    }
}
