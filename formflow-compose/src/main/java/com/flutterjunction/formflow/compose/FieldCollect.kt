package com.flutterjunction.formflow.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import com.flutterjunction.formflow.core.FieldHandle
import com.flutterjunction.formflow.core.FieldState

/**
 * Collects a [FieldHandle]'s state as Compose [State].
 *
 * This is a thin wrapper over StateFlow → Compose state.
 */
@Composable
public fun <T> FieldHandle<T>.collectState(): State<FieldState<T>> {
    return state.collectAsState()
}
