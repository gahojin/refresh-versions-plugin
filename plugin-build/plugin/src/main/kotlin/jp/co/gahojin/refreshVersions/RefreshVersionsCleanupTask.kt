/*
 * (C) 2025 GAHOJIN, Inc.
 */
package jp.co.gahojin.refreshVersions

import jp.co.gahojin.refreshVersions.ext.register
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.TaskAction
import java.io.File

abstract class RefreshVersionsCleanupTask : DefaultTask() {
    init {
        group = Constants.GROUP
    }

    @InputFile
    var versionsTomlFile: File? = null
    @Input
    var sortSection: Boolean = false

    @TaskAction
    fun cleanUp() {
        val file = requireNotNull(versionsTomlFile)
        if (file.exists()) {
            val content = VersionCatalogCleaner.execute(file.readText(), sortSection)
            file.writeText(content)
        }
    }

    companion object {
        private const val TASK_NAME = "refreshVersionsCleanup"

        fun register(project: Project, extensions: RefreshVersionsExtension) {
            project.tasks.register<RefreshVersionsCleanupTask>(TASK_NAME) {
                it.versionsTomlFile = extensions.getVersionsTomlFile()
                it.sortSection = extensions.sortSection
            }
        }
    }
}
