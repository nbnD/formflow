
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
This lets you:
- clear client errors on typing
- keep server errors until the user actually fixes the field

### 3. Submit is a policy, not a hack

You choose how submits behave:

- Reject concurrent submits
- Queue latest
- Broadcast final result
- Drop intermediate submits

This avoids race conditions and UI bugs.

---

4. Core is UI-agnostic
- formflow-core → pure Kotlin
- formflow-compose → thin Compose bindings

You can reuse the core in:
- Compose
- Desktop
- Tests
- Future UI frameworks


## Modules

| Module           | Purpose                  |
| ---------------- | ------------------------ |
| formflow-core    | UI-agnostic engine       |
| formflow-compose | Compose bindings         |
| sample-compose   | Reference implementation |

- `:formflow-core` — pure Kotlin form engine (no Android/Compose)
- `:formflow-compose` — thin Jetpack Compose adapters
- `:sample-compose` - sample project

### Quick Start (Compose)
#### 1. Create a controller
   ```kotlin
@Composable
fun LoginScreen(scope: CoroutineScope) {
    val form = rememberFormController(scope)

```
### 2. Register Fields
``` kotlin
val email = remember {
    form.registerField(
        key = FieldKey("email"),
        initial = "",
        validators = listOf(
            Validator<String> { v, _ ->
                if (v.isBlank()) listOf(ValidationError("Email required"))
                else emptyList()
            }
        )
    )
}
```

### 3. Bind to UI
```kotlin
val emailState by email.collectState()

TextField(
    value = emailState.value,
    onValueChange = email.onChange(),
    isError = emailState.hasError
)
```
### 4. Submit
```kotlin
scope.launch {
    form.submit { values ->
        // values is an immutable snapshot
        SubmitResult.Ok(Unit)
    }
}
```
### Error Display Helper (Optional)
```koltin
fun FieldState<*>.displayError(): String? =
    serverErrors.firstOrNull()?.message
        ?: clientErrors.firstOrNull()?.message
```
This ensures server errors always win.

### Sample App Behavior

The sample login screen demonstrates:
- Errors shown on first submit
- Errors cleared on focus / typing
- Password validation (min length)
- Server error (“email already exists”)
- Successful submit → welcome screen

**This behavior is intentional, not accidental.**

### Status

⚠️ Pre-1.0

APIs are stabilizing but may change.

What is stable:
- Core concepts
- Error model
- Submit policies

---

### Roadmap

- API freeze (core)
- XML / View binding adapter
- Docs site
- Kotlin Multiplatform support
- 1.0 release

### Philosophy

FormFlow is built on one rule:
- Forms are state machines, not event soup.

If you agree with that — this library is for you.
