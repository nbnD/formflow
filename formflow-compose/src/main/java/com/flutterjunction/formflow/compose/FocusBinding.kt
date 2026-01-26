package com.flutterjunction.formflow.compose


import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import com.flutterjunction.formflow.core.FieldHandle

/**
 * Wires Compose focus events to FormFlow field lifecycle.
 *
 * - calls focus() when field gains focus
 * - calls blur() when field loses focus
 */
public fun Modifier.formFieldFocus(
    field: FieldHandle<*>
): Modifier {
    return this.onFocusChanged { state ->
        if (state.isFocused) {
            field.focus()
        } else if (!state.isFocused && state.hasFocus.not()) {
            field.blur()
        }
    }
}
