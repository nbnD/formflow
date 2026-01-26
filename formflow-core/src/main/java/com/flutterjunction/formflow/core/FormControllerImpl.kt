/**
 * Default internal implementation of [FormController].
 *
 * This class is intentionally **internal**:
 * consumers interact only with the [FormController] interface and factory.
 *
 * Responsibilities:
 * - Owns the single source of truth for [FormState]
 * - Manages field registration and lifecycle
 * - Orchestrates validation and submission flows
 * - Applies client and server errors deterministically
 *
 * Thread safety:
 * - Submission is serialized via [Mutex]
 * - State updates are emitted via [StateFlow]
 *
 * UI layers must not depend on this class directly.
 */


package com.flutterjunction.formflow.core

import InternalFormSync
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.sync.withLock


internal class FormControllerImpl(
    private val scope: CoroutineScope
) : FormController, InternalFormSync {


    private val submitGate = Mutex()
    private var activeSubmit: Deferred<SubmitResult<Any?>>? = null

    private var broadcastDeferred: CompletableDeferred<SubmitResult<Any?>>? = null
    private var broadcastQueuedBlock: (suspend (ValuesSnapshot) -> SubmitResult<Any?>)? = null
    private var broadcastMonitorRunning: Boolean = false




    private data class QueuedSubmit(
        val deferred: Deferred<SubmitResult<Any?>>,
        val block: suspend (ValuesSnapshot) -> SubmitResult<Any?>
    )

    private val mutex = Mutex()

    private val _state = MutableStateFlow(
        FormState(
            status = FormStatus.Idle,
            submittedOnce = false,
            globalErrors = emptyList(),
            fields = emptyMap()
        )
    )



    override val state: StateFlow<FormState> = _state.asStateFlow()

    // Field registry keeps validators + typed flows
    private val fields = LinkedHashMap<FieldKey, FieldRecord<*>>()

    /**
     * Registers a field with the form if not already registered.
     *
     * Registration is idempotent:
     * calling this multiple times with the same [key] returns
     * the existing [FieldHandle].
     *
     * @param key Stable identifier for the field.
     * @param initial Initial field value.
     * @param validateOn Validation trigger configuration.
     * @param validators Synchronous validators executed for this field.
     *
     * @return A typed [FieldHandle] bound to this form.
     */


    override fun <T> registerField(
        key: FieldKey,
        initial: T,
        validateOn: Set<ValidateOn>,
        validators: List<Validator<T>>
    ): FieldHandle<T> {
        if (fields.containsKey(key)) {
            @Suppress("UNCHECKED_CAST")
            return fields.getValue(key).handle as FieldHandle<T>
        }

        val typedState = FieldState(
            key = key,
            initial = initial,
            value = initial,
            touched = false,
            focusedOnce = false,
            clientErrors = emptyList(),
            serverErrors = emptyList(),
            validateOn = validateOn
        )

        val record = FieldRecord(
            key = key,
            initial = initial,
            validateOn = validateOn,
            validators = validators,
            state = MutableStateFlow(typedState),
            controller = this
        )

        fields[key] = record
        syncFieldToFormState(record)
        @Suppress("UNCHECKED_CAST")
        return record.handle as FieldHandle<T>
    }

    override fun <T> field(key: FieldKey): FieldHandle<T> {
        val record = fields[key] ?: throw NoSuchElementException("Field not registered: $key")
        @Suppress("UNCHECKED_CAST")
        return record.handle as FieldHandle<T>
    }

    override fun <T> setValue(key: FieldKey, value: T, clearServerErrors: Boolean) {
        val record = requireRecord<T>(key)

        val current = record.state.value
        val next = current.copy(
            value = value,
            // clear server errors when user edits (common UX)
            serverErrors = if (clearServerErrors) emptyList() else current.serverErrors
        )

        record.state.value = next
        syncFieldToFormState(record)

        // Validate on change if configured
        if (record.validateOn.contains(ValidateOn.Change)) {
            validateField(key, isSubmitAttempt = false)
        }
    }

    override fun focus(key: FieldKey) {
        val record = requireRecord<Any?>(key)
        val current = record.state.value
        if (!current.focusedOnce) {
            record.state.value = current.copy(focusedOnce = true)
            syncFieldToFormState(record)
        }
    }

    override fun blur(key: FieldKey) {
        val record = requireRecord<Any?>(key)
        val current = record.state.value
        val next = current.copy(touched = true)
        record.state.value = next
        syncFieldToFormState(record)

        if (record.validateOn.contains(ValidateOn.Blur)) {
            validateField(key, isSubmitAttempt = false)
        }
    }

    /**
     * Validates a single field according to its configured triggers.
     *
     * When [isSubmitAttempt] is true, validation is limited to fields
     * that include [ValidateOn.Submit].
     */

    override fun validateField(key: FieldKey, isSubmitAttempt: Boolean) {
        val record = fields[key] ?: return

        // Respect triggers:
        // - if submit attempt, only validate fields that include Submit
        // - otherwise validate only if field has Blur/Change configured OR caller explicitly asked (by calling validateField)
        val shouldValidate = if (isSubmitAttempt) {
            record.validateOn.contains(ValidateOn.Submit)
        } else {
            true
        }

        if (!shouldValidate) return

        val snapshot = currentSnapshot()
        record.validate(snapshot, isSubmitAttempt)
        syncFieldToFormState(record)
    }

    /**
     * Validates all registered fields.
     *
     * @param isSubmitAttempt When true, only submit-triggered validators run.
     * @return true if all fields are valid after validation.
     */

    override fun validateAll(isSubmitAttempt: Boolean): Boolean {
        val snapshot = currentSnapshot()

        // Update status briefly (optional, but nice)
        _state.value = _state.value.copy(status = FormStatus.Validating)

        for (record in fields.values) {
            val shouldValidate = if (isSubmitAttempt) {
                record.validateOn.contains(ValidateOn.Submit)
            } else {
                true
            }
            if (shouldValidate) {
                record.validate(snapshot, isSubmitAttempt)
            }
        }

        // Sync all fields into form state once
        syncAllFieldsToFormState()

        val ok = _state.value.fields.values.all { it.isValid }
        _state.value = _state.value.copy(status = FormStatus.Idle)
        return ok
    }

    override fun applyRemoteErrors(errors: List<RemoteError>, clearClientErrors: Boolean) {
        val global = mutableListOf<RemoteError.Global>()
        val perField = LinkedHashMap<FieldKey, MutableList<RemoteError.Field>>()

        for (e in errors) {
            when (e) {
                is RemoteError.Global -> global.add(e)
                is RemoteError.Field -> perField.getOrPut(e.key) { mutableListOf() }.add(e)
            }
        }

        // Apply globals
        _state.value = _state.value.copy(globalErrors = global.toList())

        // Apply field server errors
        for ((k, errs) in perField) {
            val record = fields[k] ?: continue
            record.updateErased { current ->
                current.copy(
                    clientErrors = if (clearClientErrors) emptyList() else current.clientErrors,
                    serverErrors = errs.toList()
                )
            }
        }

        syncAllFieldsToFormState()
    }


    override fun clearRemoteErrors() {
        // Clear globals
        _state.value = _state.value.copy(globalErrors = emptyList())

        // Clear all field server errors
        for (record in fields.values) {
            record.updateErased { current ->
                if (current.serverErrors.isEmpty()) current
                else current.copy(serverErrors = emptyList())
            }
        }

        syncAllFieldsToFormState()
    }

    /**
     * Executes a transactional form submission.
     *
     * Flow:
     * 1. Enforces submission policy (e.g. ignore if already submitting)
     * 2. Runs submit-triggered validation
     * 3. Captures an immutable snapshot of field values
     * 4. Executes the user-provided [block]
     * 5. Maps rejected errors back into form state
     *
     * Submission is serialized using a [Mutex] to prevent concurrent submits.
     */

    override suspend fun <T> submit(
        policy: SubmitPolicy,
        block: suspend (ValuesSnapshot) -> SubmitResult<T>
    ): SubmitResult<T> {

        @Suppress("UNCHECKED_CAST")
        val typedBlock = block as suspend (ValuesSnapshot) -> SubmitResult<Any?>

        val toAwait: Deferred<SubmitResult<Any?>> = submitGate.withLock {
            val current = activeSubmit

            // No active submit running
            if (current == null || current.isCompleted) {
                when (policy) {
                    SubmitPolicy.QueueLatestBroadcast -> {
                        val shared = CompletableDeferred<SubmitResult<Any?>>()
                        broadcastDeferred = shared
                        broadcastQueuedBlock = null
                        broadcastMonitorRunning = false

                        val started = startSubmit(typedBlock)
                        activeSubmit = started

                        ensureBroadcastMonitor()
                        return@withLock shared
                    }

                    else -> {
                        val started = startSubmit(typedBlock)
                        activeSubmit = started
                        return@withLock started
                    }
                }
            }

            // Active submit running
            when (policy) {

                SubmitPolicy.IgnoreIfSubmitting -> current

                SubmitPolicy.CancelPrevious -> {
                    current.cancel()
                    broadcastDeferred = null
                    broadcastQueuedBlock = null
                    broadcastMonitorRunning = false

                    val started = startSubmit(typedBlock)
                    activeSubmit = started
                    started
                }

                SubmitPolicy.QueueLatestDrop -> {
                    // Drop new intent; caller gets the in-flight result
                    current
                }

                SubmitPolicy.QueueLatestBroadcast -> {
                    val shared = broadcastDeferred ?: CompletableDeferred<SubmitResult<Any?>>().also {
                        broadcastDeferred = it
                    }

                    // Keep only the latest queued block
                    broadcastQueuedBlock = typedBlock

                    ensureBroadcastMonitor()
                    shared
                }
            }
        }

        @Suppress("UNCHECKED_CAST")
        return toAwait.await() as SubmitResult<T>
    }

    private fun ensureBroadcastMonitor() {
        if (broadcastMonitorRunning) return
        broadcastMonitorRunning = true

        val shared = broadcastDeferred ?: return
        val first = activeSubmit ?: return

        scope.async {
            // Wait first submit
            val firstResult = first.await()

            // If something was queued, run latest once and broadcast that result
            val queued = submitGate.withLock {
                val q = broadcastQueuedBlock
                broadcastQueuedBlock = null
                q
            }

            val finalResult =
                if (queued != null) startSubmit(queued).await()
                else firstResult

            // Complete shared result for ALL callers (including the first)
            shared.complete(finalResult)

            // Cleanup
            submitGate.withLock {
                broadcastDeferred = null
                broadcastQueuedBlock = null
                broadcastMonitorRunning = false
            }
        }
    }



    private fun startSubmit(
        block: suspend (ValuesSnapshot) -> SubmitResult<Any?>
    ): Deferred<SubmitResult<Any?>> =
        scope.async {

            _state.value = _state.value.copy(
                status = FormStatus.Submitting,
                submittedOnce = true
            )

            val valid = validateAll(isSubmitAttempt = true)
            if (!valid) {
                _state.value = _state.value.copy(status = FormStatus.Failure)
                return@async SubmitResult.Rejected(emptyList())
            }

            val snapshot = currentSnapshot()
            val result = block(snapshot)

            when (result) {
                is SubmitResult.Ok ->
                    _state.value = _state.value.copy(status = FormStatus.Success)

                is SubmitResult.Rejected -> {
                    applyRemoteErrors(result.errors, clearClientErrors = false)
                    _state.value = _state.value.copy(status = FormStatus.Failure)
                }
            }

            result
        }



    // ---------- helpers ----------

    private fun currentSnapshot(): ValuesSnapshot {
        val map = fields.values.associate { it.key to it.state.value.value as Any? }
        return ValuesSnapshotImpl(map)
    }

    private fun syncAllFieldsToFormState() {
        val newFields = fields.values.associate { rec ->
            rec.key to rec.state.value.toAnyFieldState()
        }
        _state.value = _state.value.copy(fields = newFields)
    }

    private fun syncFieldToFormState(record: FieldRecord<*>) {
        val current = _state.value.fields.toMutableMap()
        current[record.key] = record.state.value.toAnyFieldState()
        _state.value = _state.value.copy(fields = current)
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> requireRecord(key: FieldKey): FieldRecord<T> {
        val record = fields[key] ?: throw NoSuchElementException("Field not registered: $key")
        return record as FieldRecord<T>
    }

    override fun syncAll() {
        syncAllFieldsToFormState()
    }

    override fun syncField(key: FieldKey) {
        val record = fields[key] ?: return
        syncFieldToFormState(record)
    }

    private class ValuesSnapshotImpl(
        private val values: Map<FieldKey, Any?>
    ) : ValuesSnapshot {
        override fun <T> get(key: FieldKey): T {
            @Suppress("UNCHECKED_CAST")
            return values.getValue(key) as T
        }

        override fun asMap(): Map<FieldKey, Any?> = values
    }
}

/**
 * Internal container for a registered field.
 *
 * Holds:
 * - Typed state flow
 * - Validators
 * - Validation configuration
 * - Field-level handle
 *
 * This type is erased at the form level and must never be exposed publicly.
 */

internal class FieldRecord<T>(
    val key: FieldKey,
    val initial: T,
    val validateOn: Set<ValidateOn>,
    val validators: List<Validator<T>>,
    val state: MutableStateFlow<FieldState<T>>,
    controller: FormController
) {
    val handle: FieldHandle<T> = FieldHandleImpl(this, controller)

    fun validate(values: ValuesSnapshot, isSubmitAttempt: Boolean) {
        val ctx = ValidationContext(values = values, isSubmitAttempt = isSubmitAttempt)
        val errors = validators.flatMap { it.validate(state.value.value, ctx) }
        state.value = state.value.copy(clientErrors = errors)
    }

    /**
     * Updates field state in a type-erased manner.
     *
     * This exists to safely modify common properties (errors, metadata)
     * on [FieldState] instances stored behind star-projected generics.
     *
     * All unchecked casts are intentionally centralized here.
     */


    internal fun updateErased(
        transform: (FieldState<Any?>) -> FieldState<Any?>
    ) {
        @Suppress("UNCHECKED_CAST")
        val current = state.value as FieldState<Any?>
        val next = transform(current)

        @Suppress("UNCHECKED_CAST")
        state.value = next as FieldState<T>
    }

}

/**
 * Default implementation of [FieldHandle].
 *
 * Acts as a thin facade over [FormController] for field-level interactions.
 * All mutations ultimately delegate back to the controller.
 */


internal class FieldHandleImpl<T>(
    private val record: FieldRecord<T>,
    private val controller: FormController
) : FieldHandle<T> {

    override val key: FieldKey = record.key

    override val state: StateFlow<FieldState<T>> = record.state.asStateFlow()

    override fun set(value: T, clearServerErrors: Boolean) {
        controller.setValue(key, value, clearServerErrors)
    }

    override fun focus() {
        controller.focus(key)
    }

    override fun blur() {
        controller.blur(key)
    }

    override fun validate(isSubmitAttempt: Boolean) {
        controller.validateField(key, isSubmitAttempt)
    }

    /**
     * Clears client and/or server errors for this field.
     *
     * Note:
     * This triggers an immediate sync back into form state to keep
     * [FormState.fields] consistent with field-level flows.
     */

    override fun clearErrors(clearClient: Boolean, clearServer: Boolean) {
        val current = record.state.value
        record.state.value = current.copy(
            clientErrors = if (clearClient) emptyList() else current.clientErrors,
            serverErrors = if (clearServer) emptyList() else current.serverErrors
        )

        (controller as? InternalFormSync)?.syncField(key)
    }


    override fun reset(toInitial: Boolean) {
        val initialValue = if (toInitial) {
            record.initial
        } else {
            record.state.value.initial
        }

        record.state.value = record.state.value.copy(
            value = initialValue,
            touched = false,
            focusedOnce = false,
            clientErrors = emptyList(),
            serverErrors = emptyList()
        )

        (controller as? InternalFormSync)?.syncField(key)
    }

}

/**
 * Converts a typed [FieldState] into a type-erased representation
 * suitable for inclusion in [FormState].
 *
 * Used to expose form-level state without leaking generics.
 */

private fun FieldState<*>.toAnyFieldState(): AnyFieldState {
    return AnyFieldState(
        key = key,
        initial = initial,
        value = value,
        touched = touched,
        focusedOnce = focusedOnce,
        dirty = dirty,
        clientErrors = clientErrors,
        serverErrors = serverErrors,
        validateOn = validateOn
    )
}

