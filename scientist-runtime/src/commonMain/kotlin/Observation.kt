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
  internal companion object {
    fun <T> create(name: String, block: () -> T): Observation<T> {
      val (answer, duration) = measureTimedValue { runCatching { block() } }
      return Observation(name = name, answer = answer, duration = duration)
    }
  }
}
