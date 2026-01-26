package com.flutterjunction.formflow.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.flutterjunction.formflow.core.FormController
import com.flutterjunction.formflow.core.createFormController
import kotlinx.coroutines.CoroutineScope

/**
 * Creates and remembers a [FormController] for Compose.
 *
 * The controller is created once per composition and survives recomposition.
 * The caller should provide a stable [CoroutineScope] (e.g. viewModelScope).
 */
@Composable
public fun rememberFormController(
    scope: CoroutineScope
): FormController {
    return remember(scope) {
        createFormController(scope)
    }
}

