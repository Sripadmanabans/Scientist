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

import com.adjectivemonk2.scientist.Answer
import com.adjectivemonk2.scientist.Observation
import com.varabyte.truthish.assertThat
import kotlin.test.Test
import kotlin.time.DurationUnit
import kotlin.time.toDuration

class ObservationTest {

  @Test
  fun create_Success() {
    val name = "test"
    val value = "result"
    val observation = Observation.create(name) { value }

    assertThat(observation.name).isEqualTo(name)
    assertThat(observation.answer.isSuccess).isTrue()
    assertThat(observation.answer.getOrNull()).isEqualTo(value)
    assertThat(observation.duration.isNegative()).isFalse()
  }

  @Test
  fun create_Failure() {
    val name = "test"
    val exception = RuntimeException("test exception")
    val observation = Observation.create(name) { throw exception }

    assertThat(observation.name).isEqualTo(name)
    assertThat(observation.answer.isFailure).isTrue()
    assertThat(observation.answer.exceptionOrNull()).isEqualTo(exception)
    assertThat(observation.duration.isNegative()).isFalse()
  }

  @Test
  fun constructor_InitializesProperties() {
    val name = "test"
    val answer = Answer.success("result")
    val duration = 100L.toDuration(DurationUnit.NANOSECONDS)
    val observation = Observation(name, answer, duration)

    assertThat(observation.name).isEqualTo(name)
    assertThat(observation.answer).isEqualTo(answer)
    assertThat(observation.duration).isEqualTo(duration)
  }

  @Test
  fun create_MeasuresDuration() {
    val observation =
      Observation.create("test") {
        Thread.sleep(10) // Small delay to ensure measurable duration
        "result"
      }

    assertThat(observation.duration.inWholeNanoseconds > 0).isTrue()
  }
}
