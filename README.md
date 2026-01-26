# FormFlow

Predictable, explicit form state & submission for Jetpack Compose.

FormFlow helps you build **correct, user-friendly forms** without fighting
validation timing, server errors, focus management, or duplicate submits.

---

## Why FormFlow?

Jetpack Compose makes UI simple — **forms are still painful**.

Most real apps struggle with:

- ❌ Validation firing too early (errors on screen load)
- ❌ Blur vs submit validation inconsistencies
- ❌ Mapping backend errors back to fields
- ❌ Clearing server errors when users edit
- ❌ Preventing duplicate submits
- ❌ Focus & keyboard UX (Next / Done)

FormFlow solves these problems **explicitly**, without magic.

---

## What FormFlow gives you

- Typed field handles (`FieldHandle<T>`)
- Explicit validation triggers (`Change`, `Blur`, `Submit`)
- Clear separation of client vs server errors
- Submission orchestration with policies
- UI-agnostic core
- Thin, optional Jetpack Compose adapter
- Fully testable behaviour

No reflection.  
No code generation.  
No hidden lifecycle coupling.

---



## Example

```kotlin
val form = rememberFormController(scope)

val email = form.registerField(
    key = FieldKey("email"),
    initial = "",
    validators = listOf(
        Validator { v, _ ->
            if (v.isBlank()) listOf(ValidationError("Email required"))
            else emptyList()
        }
    )
)

val emailState by email.collectState()

TextField(
    value = emailState.value,
    onValueChange = email.onChange(),
    isError = emailState.hasError
)

```

## Modules

| Module           | Purpose                  |
| ---------------- | ------------------------ |
| formflow-core    | UI-agnostic engine       |
| formflow-compose | Compose bindings         |
| sample-compose   | Reference implementation |

- `:formflow-core` — pure Kotlin form engine (no Android/Compose)
- `:formflow-compose` — thin Jetpack Compose adapters
- `:sample-compose` - sample project


## Design principles

- No hidden state

- No reflection

- No code generation

- No lifecycle coupling

- Predictable, testable behaviour


## Why not ViewModel + mutableStateOf?

You can — until you need:

- consistent validation timing

- server error mapping

- duplicate submit protection

- focus-aware UX

- testable submission behaviour

FormFlow provides structure without forcing architecture.

## Status

🚧 Alpha

## Core behaviour is stable

- API may evolve based on feedback

- Compose ergonomics actively improving


