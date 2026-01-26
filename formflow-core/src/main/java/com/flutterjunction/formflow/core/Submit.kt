package com.flutterjunction.formflow.core

/**
 * Controls behavior when submit is triggered while another submit is already running.
 */
/**
 * Controls how concurrent submit calls are handled.
 */
public enum class SubmitPolicy {

    /**
     * If a submit is already running, ignore new submit calls
     * and return the in-flight result.
     */
    IgnoreIfSubmitting,

    /**
     * Cancel any in-flight submit and start the new one immediately.
     */
    CancelPrevious,

    /**
     * While a submit is running, keep only the latest queued submit.
     * Older queued callers are dropped and may be cancelled.
     */
    QueueLatestDrop,

    /**
     * While a submit is running, keep the latest queued submit.
     * When it runs, its result is delivered to ALL queued callers.
     */
    QueueLatestBroadcast
}

/**
 * Result of a form submission.
 *
 * The library does not dictate networking; consumers provide the submit block.
 */
public sealed class SubmitResult<out T> {

    /**
     * Submission succeeded.
     */
    public data class Ok<T>(public val data: T) : SubmitResult<T>()

    /**
     * Submission was rejected by server validation rules.
     *
     * These errors should be applied to form state via [FormController.applyRemoteErrors].
     */
    public data class Rejected(public val errors: List<RemoteError>) : SubmitResult<Nothing>()
}
