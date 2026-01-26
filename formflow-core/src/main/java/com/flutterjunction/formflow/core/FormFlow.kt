package com.flutterjunction.formflow.core


import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * Creates a default [FormController] implementation.
 *
 * @param scope Internal scope used for submit orchestration (cancel/queue policies).
 */
public fun createFormController(
    scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
): FormController = FormControllerImpl(scope)

