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

public class Result<T>
private constructor(
  public val control: Observation<T>,
  public val candidates: List<Observation<T>>,
  public val mismatched: List<Observation<T>>,
  public val ignored: List<Observation<T>>,
) {

  internal companion object {
    fun <T> create(
      control: Observation<T>,
      candidates: List<Observation<T>>,
      raised: (operation: String, throwable: Throwable) -> Unit,
      compare: ((T, T) -> Boolean)?,
      compareError: ((Throwable, Throwable) -> Boolean)?,
      ignores: List<(T?, T?) -> Boolean>?,
    ): Result<T> {
      val mismatched = candidates.filterNot { control.equals(it, compare, compareError) }

      val ignored =
        mismatched.filter { candidate ->
          ignores?.any { ignore ->
            try {
              ignore(control.answer.getOrThrow(), candidate.answer.getOrThrow())
            } catch (throwable: Throwable) {
              raised("ignore", throwable)
              false
            }
          } ?: false
        }

      return Result(control, candidates, mismatched - ignored, ignored)
    }
  }
}
