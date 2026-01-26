package com.flutterjunction.formflow.core

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

public class SubmitPolicyTest {

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    public fun `IgnoreIfSubmitting returns in-flight result`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val scope = TestScope(dispatcher)

        val c = createFormController(scope)
        c.registerField(key("email"), initial = "ok")

        val a = scope.async {
            c.submit(SubmitPolicy.IgnoreIfSubmitting) {
                delay(100)
                SubmitResult.Ok("A")
            }
        }

        val b = scope.async {
            c.submit(SubmitPolicy.IgnoreIfSubmitting) {
                delay(10)
                SubmitResult.Ok("B")
            }
        }

        advanceUntilIdle()

        val ra = a.await() as SubmitResult.Ok
        val rb = b.await() as SubmitResult.Ok
        assertEquals("A", ra.data)
        assertEquals("A", rb.data)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    public fun `CancelPrevious makes the second result win`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val scope = TestScope(dispatcher)

        val c = createFormController(scope)
        c.registerField(key("email"), initial = "ok")

        val a = scope.async {
            c.submit(SubmitPolicy.CancelPrevious) {
                delay(100)
                SubmitResult.Ok("FIRST")
            }
        }

        val b = scope.async {
            c.submit(SubmitPolicy.CancelPrevious) {
                delay(10)
                SubmitResult.Ok("SECOND")
            }
        }

        advanceUntilIdle()

        val rb = b.await() as SubmitResult.Ok
        assertEquals("SECOND", rb.data)

        // A may be cancelled depending on timing; cancellation is acceptable.
        // If it completes, it should not override state or "win".
        // We don't assert A's result to avoid flakiness across dispatchers.
        assertTrue(c.state.value.status is FormStatus.Success)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    public fun `QueueLatestDrop runs latest intent but earlier caller gets in-flight result`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val scope = TestScope(dispatcher)

        val c = createFormController(scope)
        c.registerField(key("email"), initial = "ok")

        val a = scope.async {
            c.submit(SubmitPolicy.QueueLatestDrop) {
                delay(100)
                SubmitResult.Ok("A")
            }
        }

        // queued while A is running (drop behavior)
        val b = scope.async {
            c.submit(SubmitPolicy.QueueLatestDrop) {
                delay(10)
                SubmitResult.Ok("B")
            }
        }

        advanceUntilIdle()

        val ra = a.await() as SubmitResult.Ok
        val rb = b.await() as SubmitResult.Ok

        // In a drop-style queue, caller B does NOT necessarily get B's result.
        // A common and safe contract: B receives A's in-flight result.
        assertEquals("A", ra.data)
        assertEquals("A", rb.data)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    public fun `QueueLatestBroadcast delivers final queued result to all queued callers`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val scope = TestScope(dispatcher)

        val c = createFormController(scope)
        c.registerField(key("email"), initial = "ok")

        val a = scope.async {
            c.submit(SubmitPolicy.QueueLatestBroadcast) {
                delay(100)
                SubmitResult.Ok("A")
            }
        }

        val b = scope.async {
            c.submit(SubmitPolicy.QueueLatestBroadcast) {
                delay(10)
                SubmitResult.Ok("B")
            }
        }

        val c2 = scope.async {
            c.submit(SubmitPolicy.QueueLatestBroadcast) {
                delay(1)
                SubmitResult.Ok("C")
            }
        }

        advanceUntilIdle()

        val ra = a.await() as SubmitResult.Ok
        val rb = b.await() as SubmitResult.Ok
        val rc = c2.await() as SubmitResult.Ok

        // Everyone should receive the latest queued result ("C")
        assertEquals("C", ra.data)
        assertEquals("C", rb.data)
        assertEquals("C", rc.data)
    }
}
