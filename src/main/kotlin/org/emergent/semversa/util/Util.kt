package org.emergent.semversa.util

import java.io.Serializable
import java.util.*
import java.util.function.Predicate
import java.util.function.Supplier

object Util {

    fun toShortHash(hash: String): String {
        return hash.substring(0, hash.length.coerceAtMost(8))
    }

    fun isBlank(value: String?): Boolean {
        return value.isNullOrBlank()
    }

    fun isNotBlank(value: String?): Boolean {
        return !isBlank(value)
    }

    fun isEmpty(value: String?): Boolean {
        return value.isNullOrEmpty()
    }

    fun isNotEmpty(value: String?): Boolean {
        return !isEmpty(value)
    }

    fun <T> mustPass(value: T, condition: Predicate<T>, message: Supplier<String>): T {
        if (!condition.test(value)) {
            throw IllegalArgumentException(message.get())
        }
        return value
    }

    fun assertNotNegative(value: Int?): Int {
        return assertNotNegative(value, "Number $value")
    }

    fun assertNotNegative(value: Int?, label: String): Int {
        return mustPass(value ?: 0, { it >= 0 }) {
            "$label must be a non-negative integer"
        }
    }

    fun check(condition: Boolean) {
        if (!condition) {
            throw IllegalStateException()
        }
    }

    fun <T> memoize(delegate: Supplier<T>): Supplier<T> {
        return if (delegate is MemoizingSupplier<*>) {
            delegate
        } else {
            MemoizingSupplier(Objects.requireNonNull(delegate))
        }
    }

    private class MemoizingSupplier<T>(
        private val delegate: Supplier<T>
    ) : Supplier<T>, Serializable {

        @Transient
        @Volatile
        private var initialized = false

        @Transient
        private var value: T? = null

        override fun get(): T {
            // A 2-field variant of double-checked locking.
            if (!initialized) {
                synchronized(this) {
                    if (!initialized) {
                        val t = delegate.get()
                        value = t
                        initialized = true
                        return t
                    }
                }
            }
            return value!!
        }

        override fun toString(): String {
            return "${this::class.simpleName}($delegate)"
        }

        companion object {
            const val serialVersionUID: Long = 0
        }
    }
}