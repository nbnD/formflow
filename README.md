# FormFlow

**FormFlow** is a **deterministic, coroutine-first form state engine** for Kotlin, designed to handle real-world forms: validation, server errors, concurrent submits, and UI focus/blur — **without turning your ViewModel into spaghetti**.

It works on **pure Kotlin core**, with a **Jetpack Compose adapter** on top.

---

## Why FormFlow?

Most form implementations break down when you need to handle:

- Client + server validation together
- Concurrent submits (double-clicks, retries)
- “Latest submit wins” vs “queue everything”
- Clearing errors at the *right* time (not too early, not too late)
- Focus / blur driven validation

FormFlow solves these **explicitly**, not accidentally.

---

## Key Ideas

### 1. Fields are first-class
Each field has:
- value
- client errors
- server errors
- touched / focused state

No magic strings. No implicit coupling.

---

### 2. Client vs Server errors are separate
FormFlow **never mixes them**.

```kotlin
FieldState(
  clientErrors = listOf(...),
  serverErrors = listOf(...)
)
```
## API stability

- `formflow-core` is the stable contract.
- Implementation types are internal.
- Until 1.0, breaking changes may happen between minor versions.
