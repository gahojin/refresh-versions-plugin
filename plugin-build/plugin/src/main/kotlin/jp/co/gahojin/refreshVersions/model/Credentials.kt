/*
 * (C) 2025 GAHOJIN, Inc.
 */
package jp.co.gahojin.refreshVersions.model

import java.nio.charset.Charset

/**
 * 認証情報.
 */
interface Credentials {
    fun asAuthorization(): String
}

/**
 * パスワード認証情報.
 */
internal data class PasswordCredentials(
    val username: String,
    val password: String,
    val charset: Charset = Charsets.UTF_8,
) : Credentials {
    override fun asAuthorization(): String {
        return okhttp3.Credentials.basic(
            username = username,
            password = password,
            charset = charset,
        )
    }
}
