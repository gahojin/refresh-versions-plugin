/*
 * (C) 2025 GAHOJIN, Inc.
 */
package jp.co.gahojin.refreshVersions

import okhttp3.ResponseBody

/**
 * HTTP通信エラー.
 */
@Suppress("MemberVisibilityCanBePrivate", "CanBeParameter")
class HttpException(
    val statusCode: Int,
    val body: ResponseBody,
) : RuntimeException("status: $statusCode, body: ${body.string()}")
