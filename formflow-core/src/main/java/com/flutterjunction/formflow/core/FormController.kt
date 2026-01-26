package com.flutterjunction.formflow.core

import kotlinx.coroutines.flow.StateFlow

/**
 * Typed handle for interacting with a single field.
 *
 * This is a convenience API over [FormController] for field-level operations.
 */
public interface FieldHandle<T> {

    /** Field identifier. */
    public val key: FieldKey

    /** Reactive typed state for this field. */
    public val state: StateFlow<FieldState<T>>

    /**
     * Sets a new value for this field.
     *
     * @param clearServerErrors When true, clears any server errors for this field.
     */
    public fun set(value: T, clearServerErrors: Boolean = true)

    /** Marks the field as focused (used for UX metadata). */
    public fun focus()

    /** Marks the field as blurred and may trigger validation if configured. */
    public fun blur()

    /** Validates the field based on validators and trigger settings. */
    public fun validate(isSubmitAttempt: Boolean = false)

    /** Clears client and/or server errors for this field. */
    public fun clearErrors(clearClient: Boolean = true, clearServer: Boolean = true)

    /** Resets the field back to its initial value and clears errors/metadata. */
    public fun reset(toInitial: Boolean = true)
}

/**
 * Core API for managing form state, validation, and submission orchestration.
 *
 * `formflow-core` is UI-agnostic. UI layers (Compose) should observe [state] and
 * call these functions in response to user interactions.
 */
public interface FormController {

    /** Reactive form state (single source of truth). */
    public val state: StateFlow<FormState>

    /**
     * Registers a new field in the form.
     *
     * @param key Stable field identifier.
     * @param initial Initial value.
     * @param validateOn When validation should run for this field.
     * @param validators Synchronous validators for this field.
     */
    public fun <T> registerField(
        key: FieldKey,
        initial: T,
        validateOn: Set<ValidateOn> = setOf(ValidateOn.Blur, ValidateOn.Submit),
        validators: List<Validator<T>> = emptyList()
    ): FieldHandle<T>

    /**
     * Returns an existing field handle.
     *
     * @throws NoSuchElementException if the field was not registered
     */
    public fun <T> field(key: FieldKey): FieldHandle<T>

    /**
     * Sets a field value by key.
     *
     * @param clearServerErrors When true, clears server errors for this field.
     */
    public fun <T> setValue(key: FieldKey, value: T, clearServerErrors: Boolean = true)

    /** Marks a field as focused. */
    public fun focus(key: FieldKey)

    /** Marks a field as blurred and may trigger validation. */
    public fun blur(key: FieldKey)

    /**
     * Validates a single field.
     *
     * @param isSubmitAttempt True if validation is occurring as part of a submit attempt.
     */
    public fun validateField(key: FieldKey, isSubmitAttempt: Boolean = false)

    /**
     * Validates all fields.
     *
     * @return true if all fields are valid after validation.
     */
    public fun validateAll(isSubmitAttempt: Boolean = false): Boolean

    /**
     * Applies server errors to the form state.
     *
     * Field errors are stored per field; global errors are stored at form level.
     *
     * @param clearClientErrors If true, clears existing client errors before applying server errors.
     */
    public fun applyRemoteErrors(errors: List<RemoteError>, clearClientErrors: Boolean = false)

    /** Clears all stored server/global errors. */
    public fun clearRemoteErrors()

    /**
     * Performs a transactional submit:
     * - validates the form (submit-trigger)
     * - snapshots values
     * - runs [block]
     * - maps rejected errors back into form state
     *
     * @return the [SubmitResult] from the submit block.
     */
    public suspend fun <T> submit(
        policy: SubmitPolicy = SubmitPolicy.IgnoreIfSubmitting,
        block: suspend (ValuesSnapshot) -> SubmitResult<T>
    ): SubmitResult<T>
}
