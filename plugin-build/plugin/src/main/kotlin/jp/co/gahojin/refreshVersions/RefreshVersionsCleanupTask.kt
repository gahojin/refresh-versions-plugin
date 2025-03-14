/*
 * (C) 2025 GAHOJIN, Inc.
 */
package jp.co.gahojin.refreshVersions

import jp.co.gahojin.refreshVersions.extension.register
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.provider.Property
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import java.io.File

abstract class RefreshVersionsCleanupTask : DefaultTask() {
    init {
        group = Constants.GROUP
    }

    @get:Internal
    abstract val versionsTomlFile: Property<File>

    @get:Internal
    abstract val sortSection: Property<Boolean>

    @TaskAction
    fun cleanUp() {
        val file = requireNotNull(versionsTomlFile.orNull)
        if (file.exists()) {
            val content = VersionCatalogCleaner.execute(file.readText(), sortSection.getOrElse(false))
            file.writeText(content)
        }
    }

    companion object {
        private const val TASK_NAME = "refreshVersionsCleanup"

        fun register(project: Project, extensions: RefreshVersionsExtension) {
            project.tasks.register<RefreshVersionsCleanupTask>(TASK_NAME) {
                it.versionsTomlFile.set(extensions.getVersionsTomlFile())
                it.sortSection.set(extensions.sortSection)
            }
        }
    }
}
