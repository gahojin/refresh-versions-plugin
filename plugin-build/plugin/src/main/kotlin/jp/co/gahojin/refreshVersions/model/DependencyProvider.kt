/*
 * (C) 2025 GAHOJIN, Inc.
 */
package jp.co.gahojin.refreshVersions.model

interface DependencyProvider {
    fun getDependency(): Dependency
}
