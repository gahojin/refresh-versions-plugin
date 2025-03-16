/*
 * (C) 2025 GAHOJIN, Inc.
 */
package jp.co.gahojin.refreshVersions.model

/**
 * 認証情報.
 */
interface Credentials

/**
 * パスワード認証情報.
 */
data class PasswordCredentials(
    val username: String,
    val password: String,
) : Credentials
