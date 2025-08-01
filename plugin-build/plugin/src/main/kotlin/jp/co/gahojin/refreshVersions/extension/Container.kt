/*
 * (C) 2025 GAHOJIN, Inc.
 */
package jp.co.gahojin.refreshVersions.extension

import org.gradle.api.Action
import org.gradle.api.Task
import org.gradle.api.plugins.ExtensionContainer
import org.gradle.api.tasks.TaskContainer
import org.gradle.api.tasks.TaskProvider

inline fun <reified T : Task> TaskContainer.register(name: String, configurationAction: Action<in T>): TaskProvider<T> {
    return register(name, T::class.java, configurationAction)
}

inline fun <reified T : Any> ExtensionContainer.create(name: String): T {
    return create(name, T::class.java)
}

inline fun <reified T : Any> ExtensionContainer.findByType(): T? {
    return findByType(T::class.java)
}
