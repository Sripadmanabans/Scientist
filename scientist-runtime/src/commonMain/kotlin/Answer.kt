/*
 * Copyright (C) 2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

@file:OptIn(ExperimentalContracts::class)

package com.adjectivemonk2.scientist

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.getOrElse
import kotlin.getOrThrow

/* Note: This class is a copy of Result from kotlin, this was made because result clashes with the value that you would
 * expect an experiment to pass.
 */
/**
 * A discriminated union that encapsulates a successful outcome with a value of type [T] or a
 * failure with an arbitrary [Throwable] exception.
 */
@JvmInline
public value class Answer<out T>
@PublishedApi
internal constructor(@PublishedApi internal val value: Any?) {

  /**
   * Returns `true` if this instance represents a successful outcome. In this case [isFailure]
   * returns `false`.
   */
  public val isSuccess: Boolean
    get() = value !is Failure

  /**
   * Returns `true` if this instance represents a failed outcome. In this case [isSuccess] returns
   * `false`.
   */
  public val isFailure: Boolean
    get() = value is Failure

  /**
   * Returns the encapsulated value if this instance represents [success][Answer.isSuccess] or
   * `null` if it is [failure][Answer.isFailure].
   *
   * This function is a shorthand for `getOrElse { null }` (see [getOrElse]) or `fold(onSuccess = {
   * it }, onFailure = { null })` (see [fold]).
   */
  @Suppress("NOTHING_TO_INLINE")
  public inline fun getOrNull(): T? {
    @Suppress("UNCHECKED_CAST")
    return when {
      isFailure -> null
      else -> value as T
    }
  }

  /**
   * Returns the encapsulated [Throwable] exception if this instance represents [failure][isFailure]
   * or `null` if it is [success][isSuccess].
   *
   * This function is a shorthand for `fold(onSuccess = { null }, onFailure = { it })` (see [fold]).
   */
  public fun exceptionOrNull(): Throwable? {
    return when (value) {
      is Failure -> value.exception
      else -> null
    }
  }

  /**
   * Returns a string `Success(v)` if this instance represents [success][Answer.isSuccess] where `v`
   * is a string representation of the value or a string `Failure(x)` if it is [failure][isFailure]
   * where `x` is a string representation of the exception.
   */
  public override fun toString(): String {
    return when (value) {
      is Failure -> value.toString() // "Failure($exception)"
      else -> "Success($value)"
    }
  }

  public companion object {
    /** Returns an instance that encapsulates the given [value] as successful value. */
    @Suppress("NOTHING_TO_INLINE")
    @JvmName("success")
    public inline fun <T> success(value: T): Answer<T> = Answer(value)

    /** Returns an instance that encapsulates the given [Throwable] [exception] as failure. */
    @Suppress("NOTHING_TO_INLINE")
    @JvmName("failure")
    public inline fun <T> failure(exception: Throwable): Answer<T> =
      Answer(createFailure(exception))
  }

  internal class Failure(@JvmField val exception: Throwable) {
    override fun equals(other: Any?): Boolean = other is Failure && exception == other.exception

    override fun hashCode(): Int = exception.hashCode()

    override fun toString(): String = "Failure($exception)"
  }
}

/**
 * Creates an instance of internal marker [Answer.Failure] class to make sure that this class is not
 * exposed in ABI.
 */
@PublishedApi internal fun createFailure(exception: Throwable): Any = Answer.Failure(exception)

/**
 * Throws exception if the answer is failure. This internal function minimizes inlined bytecode for
 * [getOrThrow] and makes sure that in the future we can add some exception-augmenting logic here
 * (if needed).
 */
@PublishedApi
@SinceKotlin("1.3")
internal fun Answer<*>.throwOnFailure() {
  if (value is Answer.Failure) throw value.exception
}

/**
 * Calls the specified function [block] with `this` value as its receiver and returns its
 * encapsulated answer if invocation was successful, catching any [Throwable] exception that was
 * thrown from the [block] function execution and encapsulating it as a failure.
 */
public inline fun <T, R> T.runCatching(block: T.() -> R): Answer<R> {
  return try {
    Answer.success(block())
  } catch (e: Throwable) {
    Answer.failure(e)
  }
}

// -- extensions ---

/**
 * Returns the encapsulated value if this instance represents [success][Answer.isSuccess] or throws
 * the encapsulated [Throwable] exception if it is [failure][Answer.isFailure].
 *
 * This function is a shorthand for `getOrElse { throw it }` (see [getOrElse]).
 */
@Suppress("NOTHING_TO_INLINE")
public inline fun <T> Answer<T>.getOrThrow(): T {
  throwOnFailure()
  @Suppress("UNCHECKED_CAST")
  return value as T
}

/**
 * Returns the encapsulated value if this instance represents [success][Answer.isSuccess] or the
 * result of [onFailure] function for the encapsulated [Throwable] exception if it is
 * [failure][Answer.isFailure].
 *
 * Note, that this function rethrows any [Throwable] exception thrown by [onFailure] function.
 *
 * This function is a shorthand for `fold(onSuccess = { it }, onFailure = onFailure)` (see [fold]).
 */
public inline fun <R, T : R> Answer<T>.getOrElse(onFailure: (exception: Throwable) -> R): R {
  contract { callsInPlace(onFailure, InvocationKind.AT_MOST_ONCE) }
  @Suppress("UNCHECKED_CAST")
  return when (val exception = exceptionOrNull()) {
    null -> value as T
    else -> onFailure(exception)
  }
}

/**
 * Returns the encapsulated value if this instance represents [success][Answer.isSuccess] or the
 * [defaultValue] if it is [failure][Answer.isFailure].
 *
 * This function is a shorthand for `getOrElse { defaultValue }` (see [getOrElse]).
 */
@Suppress("NOTHING_TO_INLINE")
public inline fun <R, T : R> Answer<T>.getOrDefault(defaultValue: R): R {
  if (isFailure) return defaultValue
  @Suppress("UNCHECKED_CAST")
  return value as T
}
