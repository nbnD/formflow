package com.flutterjunction.formflow.core

/**
 * Client-side validation error produced by a [Validator].
 *
 * @param message Human-readable error message intended for display.
 * @param code Optional machine-readable identifier (useful for analytics or mapping).
 */
public data class ValidationError(
    public val message: String,
    public val code: String? = null
)

/**
 * Server-side error returned from a backend/API.
 *
 * This is applied to form state via [FormController.applyRemoteErrors].
 * Field errors should be rendered inline on the corresponding field;
 * global errors are non-field specific and usually displayed as a banner/snackbar.
 */
public sealed class RemoteError {

    /**
     * Error specific to a single field.
     */
    public data class Field(
        public val key: FieldKey,
        public val message: String,
        public val code: String? = null
    ) : RemoteError()

    /**
     * Error not tied to a specific field (e.g., "Service unavailable").
     */
    public data class Global(
        public val message: String,
        public val code: String? = null
    ) : RemoteError()
}
