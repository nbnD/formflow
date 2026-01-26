package com.flutterjunction.formflow.core

/**
 * Stable identifier for a field within a form.
 *
 * Field keys must remain stable across recompositions and dynamic UI changes.
 * Use [key] for convenience.
 */
@JvmInline
public value class FieldKey public constructor(public val value: String) {
    init {
        require(value.isNotBlank()) { "FieldKey cannot be blank" }
    }

    override fun toString(): String = value
}

/**
 * Convenience factory for creating a [FieldKey].
 */
public fun key(value: String): FieldKey = FieldKey(value)
