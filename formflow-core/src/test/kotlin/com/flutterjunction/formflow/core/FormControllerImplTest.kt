package com.flutterjunction.formflow.core

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

public class FormControllerImplTest {

    @Test
    public fun `registerField is idempotent and sets initial state`() = runTest {
        val c = createFormController()

        val h1 = c.registerField(key("email"), initial = "")
        val h2 = c.registerField(key("email"), initial = "SHOULD_BE_IGNORED")

        // same handle instance (idempotent)
        assertTrue(h1 === h2)

        val f = c.state.value.fields.getValue(key("email"))
        assertEquals("", f.value)
        assertFalse(f.touched)
        assertFalse(f.dirty)
        assertTrue(f.isValid)
    }

    @Test
    public fun `setValue updates value and dirty`() = runTest {
        val c = createFormController()
        c.registerField(key("email"), initial = "")

        c.setValue(key("email"), "a@b.com")

        val f = c.state.value.fields.getValue(key("email"))
        assertEquals("a@b.com", f.value)
        assertTrue(f.dirty)
    }

    @Test
    public fun `blur marks touched`() = runTest {
        val c = createFormController()
        c.registerField(key("email"), initial = "")

        c.blur(key("email"))

        val f = c.state.value.fields.getValue(key("email"))
        assertTrue(f.touched)
    }

    @Test
    public fun `validateAll with submitAttempt validates only Submit-triggered fields`() = runTest {
        val c = createFormController()

        c.registerField(
            key = key("email"),
            initial = "",
            validateOn = setOf(ValidateOn.Submit),
            validators = listOf(Validator<String> { v, _ ->
                if (v.isBlank()) listOf(ValidationError("required")) else emptyList()
            })
        )

        // Not validated yet
        assertTrue(c.state.value.fields.getValue(key("email")).clientErrors.isEmpty())

        val ok = c.validateAll(isSubmitAttempt = true)
        assertFalse(ok)
        assertEquals("required", c.state.value.fields.getValue(key("email")).clientErrors.first().message)
    }

    @Test
    public fun `submit rejects when invalid and marks submittedOnce`() = runTest {
        val c = createFormController()

        c.registerField(
            key = key("email"),
            initial = "",
            validators = listOf(Validator<String> { v, _ ->
                if (v.isBlank()) listOf(ValidationError("required")) else emptyList()
            })
        )

        val result = c.submit { SubmitResult.Ok(Unit) }

        assertTrue(result is SubmitResult.Rejected)
        assertTrue(c.state.value.submittedOnce)
        assertEquals("required", c.state.value.fields.getValue(key("email")).clientErrors.first().message)
        assertTrue(c.state.value.status is FormStatus.Failure)
    }

    @Test
    public fun `remote field error is applied and cleared on change`() = runTest {
        val c = createFormController()
        c.registerField(key("email"), initial = "")

        c.applyRemoteErrors(listOf(RemoteError.Field(key("email"), "already exists")))
        assertEquals(1, c.state.value.fields.getValue(key("email")).serverErrors.size)

        c.setValue(key("email"), "new@x.com", clearServerErrors = true)
        assertEquals(0, c.state.value.fields.getValue(key("email")).serverErrors.size)
    }

    @Test
    public fun `submit snapshot sees value at submit time`() = runTest {
        val c = createFormController()
        c.registerField(key("email"), initial = "")
        c.setValue(key("email"), "first@x.com")

        val result = c.submit { snap ->
            val v = snap.get<String>(key("email"))
            assertEquals("first@x.com", v)
            SubmitResult.Ok(Unit)
        }

        assertTrue(result is SubmitResult.Ok)
    }

    @Test
    public fun `clearErrors clears selected error types and syncs state`() = runTest {
        val c = createFormController()

        c.registerField(
            key = key("email"),
            initial = "",
            validators = listOf(
                Validator { _, _ -> listOf(ValidationError("client")) }
            )
        )

        // trigger client error
        c.validateAll()
        c.applyRemoteErrors(listOf(RemoteError.Field(key("email"), "server")))

        val h = c.field<String>(key("email"))
        h.clearErrors(clearClient = true, clearServer = false)

        val f = c.state.value.fields.getValue(key("email"))
        assertTrue(f.clientErrors.isEmpty())
        assertEquals(1, f.serverErrors.size)
    }
    @Test
    public fun `reset restores initial value and clears metadata`() = runTest {
        val c = createFormController()
        val h = c.registerField(key("email"), initial = "")

        h.set("abc@x.com")
        h.blur()
        h.validate()

        h.reset()

        val f = c.state.value.fields.getValue(key("email"))
        assertEquals("", f.value)
        assertFalse(f.touched)
        assertFalse(f.focusedOnce)
        assertTrue(f.clientErrors.isEmpty())
        assertTrue(f.serverErrors.isEmpty())
    }
    @Test
    public fun `reset false restores to current initial snapshot`() = runTest {
        val c = createFormController()
        val h = c.registerField(key("email"), initial = "")

        // Change from initial
        h.set("abc@x.com")

        // "reset(false)" should restore to the field's current initial snapshot.
        // In v1, initial snapshot does NOT change when user edits, so this equals the original initial: ""
        h.reset(toInitial = false)

        val f1 = c.state.value.fields.getValue(key("email"))
        assertEquals("", f1.value)

        // Confirm it behaves consistently after another edit
        h.set("second@x.com")
        h.reset(toInitial = false)

        val f2 = c.state.value.fields.getValue(key("email"))
        assertEquals("", f2.value)
    }

}
