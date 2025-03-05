/*
 * (C) 2025 GAHOJIN, Inc.
 */
package jp.co.gahojin.refreshVersions

import okhttp3.ResponseBody

/**
 * HTTP通信エラー.
 */
class HttpException(
    val statusCode: Int,
    val body: ResponseBody,
) : RuntimeException()
