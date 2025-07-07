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

package com.adjectivemonk2.scientist

import kotlin.time.Duration
import kotlin.time.measureTimedValue

public class Observation<T>(
  public val name: String,
  public val answer: Answer<T>,
  public val duration: Duration,
) {

  internal fun equals(
    other: Observation<T>?,
    compare: ((T, T) -> Boolean)?,
    compareError: ((Throwable, Throwable) -> Boolean)?,
  ): Boolean {
    if (this === other) return true
    if (other == null) return false

    val otherAnswer = other.answer
    return if (answer.isSuccess && otherAnswer.isSuccess) {
      val thisValue = answer.getOrThrow()
      val otherValue = otherAnswer.getOrThrow()
      compare?.invoke(thisValue, otherValue) ?: (thisValue == otherValue)
    } else if (answer.isFailure && otherAnswer.isFailure) {
      val thisError = answer.exceptionOrNull()!!
      val otherError = otherAnswer.exceptionOrNull()!!
      compareError?.invoke(thisError, otherError) ?: (thisError == otherError)
    } else {
      false
    }
  }

  internal companion object {
    fun <T> create(name: String, block: () -> T): Observation<T> {
      val (answer, duration) = measureTimedValue { runCatching { block() } }
      return Observation(name = name, answer = answer, duration = duration)
    }
  }
}
