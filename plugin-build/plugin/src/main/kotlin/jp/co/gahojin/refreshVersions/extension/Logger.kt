/*
 * (C) 2025 GAHOJIN, Inc.
 */
package jp.co.gahojin.refreshVersions.extension

import org.gradle.api.logging.Logger

inline fun Logger.lifecycle(message: () -> String) {
    if (isLifecycleEnabled) {
        lifecycle(message())
    }
}

inline fun Logger.debug(message: () -> String) {
    if (isDebugEnabled) {
        debug(message())
    }
}

inline fun Logger.info(message: () -> String) {
    if (isInfoEnabled) {
        info(message())
    }
}

inline fun Logger.warn(message: () -> String) {
    if (isWarnEnabled) {
        warn(message())
    }
}

inline fun Logger.error(message: () -> String) {
    if (isErrorEnabled) {
        error(message())
    }
}
