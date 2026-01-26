package com.flutterjunction.formflow.core

/**
 * Defines when validation should be executed for a field.
 *
 * - [Change] validate as the value changes (can be noisy; use carefully)
 * - [Blur] validate when user leaves the field
 * - [Submit] validate on submit attempt
 */
public enum class ValidateOn { Change, Blur, Submit }

/**
 * Context passed to validators.
 *
 * Provides access to the full values snapshot so validators can implement
 * cross-field rules (e.g., confirmPassword matches password).
 *
 * @param values Snapshot of all field values at the time validation runs.
 * @param isSubmitAttempt True when validation is running as part of submission.
 */
public data class ValidationContext(
    public val values: ValuesSnapshot,
    public val isSubmitAttempt: Boolean
)

/**
 * Synchronous validator for a field value.
 *
 * v1 supports only synchronous validation. Async validation can be introduced in v2
 * without breaking this contract by adding a separate async validator type.
 */
public fun interface Validator<T> {

    /**
     * Validates [value] using the given [ctx].
     *
     * Return an empty list if valid; otherwise return one or more [ValidationError]s.
     */
    public fun validate(value: T, ctx: ValidationContext): List<ValidationError>
}

/**
 * Read-only snapshot of current form values.
 *
 * Snapshots are used for:
 * - cross-field validation
 * - submission (ensuring stable values during async calls)
 */
public interface ValuesSnapshot {

    /**
     * Reads a typed value for a field.
     *
     * @throws ClassCastException if the stored value is not of type [T]
     * @throws NoSuchElementException if the key is missing
     */
    public fun <T> get(key: FieldKey): T

    /**
     * Returns all values as a map (type-erased).
     */
    public fun asMap(): Map<FieldKey, Any?>
}
