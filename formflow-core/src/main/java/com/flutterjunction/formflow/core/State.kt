package com.flutterjunction.formflow.core

/**
 * Represents the high-level lifecycle state of a form.
 *
 * This is intentionally coarse-grained so UI layers can react predictably
 * without inspecting internal field details.
 */
public sealed class FormStatus {

    /** Initial idle state before any validation or submission. */
    public data object Idle : FormStatus()

    /** Client-side validation is currently running. */
    public data object Validating : FormStatus()

    /** A submit operation is currently running. */
    public data object Submitting : FormStatus()

    /** Submission completed successfully. */
    public data object Success : FormStatus()

    /** Submission failed (validation, server errors, or exceptions). */
    public data object Failure : FormStatus()
}

/**
 * Immutable state holder for a single field.
 *
 * @param T the type of the field value
 */
public data class FieldState<T>(
    /** Unique identifier for this field. */
    public val key: FieldKey,
    /** Initial value used to compute dirty state. */
    public val initial: T,
    /** Current value of the field. */
    public val value: T,
    /** Whether user has blurred this field at least once. */
    public val touched: Boolean,
    /** Whether the field has ever received focus. */
    public val focusedOnce: Boolean,
    /** Client-side validation errors. */
    public val clientErrors: List<ValidationError>,
    /** Server-side errors mapped to this field. */
    public val serverErrors: List<RemoteError.Field>,
    /** When validation should run for this field. */
    public val validateOn: Set<ValidateOn>
) {

    /** True if the current value differs from the initial value. */
    public val dirty: Boolean
        get() = value != initial

    /** True if any client or server errors exist. */
    public val hasError: Boolean
        get() = clientErrors.isNotEmpty() || serverErrors.isNotEmpty()

    /** True if the field contains no errors. */
    public val isValid: Boolean
        get() = !hasError
}

/**
 * Aggregate immutable state of the entire form.
 *
 * This is the single source of truth that UI layers observe.
 */
public data class FormState(
    /** Current lifecycle status of the form. */
    public val status: FormStatus,
    /** True once a submit attempt has occurred. */
    public val submittedOnce: Boolean,
    /** Global errors not tied to a specific field. */
    public val globalErrors: List<RemoteError.Global>,
    /** All registered fields. */
    public val fields: Map<FieldKey, AnyFieldState>
) {

    /** True if any field in the form is dirty. */
    public val isDirty: Boolean
        get() = fields.values.any { it.dirty }

    /** True if all fields are valid. */
    public val isValid: Boolean
        get() = fields.values.all { it.isValid }

    /** True if submission is allowed (not already submitting). */
    public val canSubmit: Boolean
        get() = status !is FormStatus.Submitting
}

/**
 * Type-erased field state used in [FormState] to avoid leaking generics.
 *
 * This allows [FormState] to hold a map of fields regardless of their value types,
 * while still exposing derived flags needed by UI.
 */
public data class AnyFieldState(
    public val key: FieldKey,
    public val initial: Any?,
    public val value: Any?,
    public val touched: Boolean,
    public val focusedOnce: Boolean,
    public val dirty: Boolean,
    public val clientErrors: List<ValidationError>,
    public val serverErrors: List<RemoteError.Field>,
    public val validateOn: Set<ValidateOn>
) {

    /** True if any errors exist. */
    public val hasError: Boolean
        get() = clientErrors.isNotEmpty() || serverErrors.isNotEmpty()

    /** True if the field contains no errors. */
    public val isValid: Boolean
        get() = !hasError
}
