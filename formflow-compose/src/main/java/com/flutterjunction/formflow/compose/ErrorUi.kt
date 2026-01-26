package com.flutterjunction.formflow.compose

import com.flutterjunction.formflow.core.FieldState

/**
 * Returns the most appropriate error message for UI display.
 *
 * Priority:
 * 1. Server error (authoritative)
 * 2. Client validation error
 *
 * Returns null if no error exists.
 */
public fun FieldState<*>.displayError(): String? {
    return serverErrors.firstOrNull()?.message
        ?: clientErrors.firstOrNull()?.message
}
