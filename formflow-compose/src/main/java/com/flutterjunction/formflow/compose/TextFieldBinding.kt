package com.flutterjunction.formflow.compose

import com.flutterjunction.formflow.core.FieldHandle

/**
 * Simple binding helper for text-like fields.
 *
 * Usage:
 * val state by email.collectState()
 * TextField(
 *   value = state.value,
 *   onValueChange = email.onChange(),
 *   isError = state.hasError
 * )
 */
public fun FieldHandle<String>.onChange(
    clearServerErrors: Boolean = true
): (String) -> Unit {
    return { newValue ->
        set(newValue, clearServerErrors)
    }
}
