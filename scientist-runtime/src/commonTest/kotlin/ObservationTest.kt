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
import kotlin.time.Duration
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

  // ===== EQUALS METHOD TESTS =====

  @Test
  fun equals_SameReference_ReturnsTrue() {
    val observation = Observation.create("test") { "result" }

    val result = observation.equals(observation, null, null)

    assertThat(result).isTrue()
  }

  @Test
  fun equals_NullComparison_ReturnsFalse() {
    val observation = Observation.create("test") { "result" }

    val result = observation.equals(null, null, null)

    assertThat(result).isFalse()
  }

  @Test
  fun equals_BothSuccess_SameValue_ReturnsTrue() {
    val observation1 = Observation.create("test1") { "result" }
    val observation2 = Observation.create("test2") { "result" }

    val result = observation1.equals(observation2, null, null)

    assertThat(result).isTrue()
  }

  @Test
  fun equals_BothSuccess_DifferentValue_ReturnsFalse() {
    val observation1 = Observation.create("test1") { "result1" }
    val observation2 = Observation.create("test2") { "result2" }

    val result = observation1.equals(observation2, null, null)

    assertThat(result).isFalse()
  }

  @Test
  fun equals_BothSuccess_DifferentValue_CustomComparison_ReturnsTrue() {
    val observation1 = Observation.create("test1") { "result1" }
    val observation2 = Observation.create("test2") { "result2" }

    // The compare function returns a boolean indicating whether the values are equal
    val result = observation1.equals(observation2, { _, _ -> true }, null)

    // Since the custom comparison returns true, the result should be true
    assertThat(result).isTrue()
  }

  @Test
  fun equals_BothFailure_SameException_ReturnsTrue() {
    val exception = RuntimeException("test exception")
    val observation1 = Observation.create("test1") { throw exception }
    val observation2 = Observation.create("test2") { throw exception }

    val result = observation1.equals(observation2, null, null)

    assertThat(result).isTrue()
  }

  @Test
  fun equals_BothFailure_DifferentException_ReturnsFalse() {
    val exception1 = RuntimeException("test exception 1")
    val exception2 = RuntimeException("test exception 2")
    val observation1 = Observation.create("test1") { throw exception1 }
    val observation2 = Observation.create("test2") { throw exception2 }

    val result = observation1.equals(observation2, null, null)

    assertThat(result).isFalse()
  }

  @Test
  fun equals_BothFailure_DifferentException_CustomComparison_ReturnsTrue() {
    val exception1 = RuntimeException("test exception 1")
    val exception2 = RuntimeException("test exception 2")
    val observation1 = Observation.create("test1") { throw exception1 }
    val observation2 = Observation.create("test2") { throw exception2 }

    // The compareError function returns a boolean indicating whether the exceptions are equal
    val result = observation1.equals(observation2, null) { _, _ -> true }

    // Since the custom comparison returns true, the result should be true
    assertThat(result).isTrue()
  }

  @Test
  fun equals_OneSuccess_OneFailure_ReturnsFalse() {
    val exception = RuntimeException("test exception")

    // Create observations with explicit type parameter
    val observation1: Observation<String> = Observation.create("test1") { "result" }

    // Create a failure observation with the same type parameter
    val observation2: Observation<String> =
      Observation(name = "test2", answer = Answer.failure(exception), duration = Duration.ZERO)

    val result1 = observation1.equals(observation2, null, null)
    val result2 = observation2.equals(observation1, null, null)

    assertThat(result1).isFalse()
    assertThat(result2).isFalse()
  }

  @Test
  fun equals_BothSuccess_OneNullValue_CustomComparison_HandlesNull() {
    val observation1: Observation<String?> = Observation.create("test1") { "result" }
    val observation2: Observation<String?> = Observation.create("test2") { null }

    // Test with a custom comparison that handles null values
    val result = observation1.equals(observation2, { a, b -> a == b }, null)

    assertThat(result).isFalse()
  }

  @Test
  fun equals_BothFailure_OneNullError_ReturnsFalse() {
    val exception = RuntimeException("test exception")

    // Create a failure observation with a real exception
    val observation1: Observation<String> = Observation.create("test1") { throw exception }

    // Test the branch where one has error and one doesn't
    val observation2: Observation<String> = Observation.create("test2") { "success" }
    val result = observation1.equals(observation2, null, null)

    assertThat(result).isFalse()
  }

  @Test
  fun equals_BothSuccess_WithNullValues_ReturnsTrue() {
    val observation1: Observation<String?> = Observation.create("test1") { null }
    val observation2: Observation<String?> = Observation.create("test2") { null }

    val result = observation1.equals(observation2, null, null)

    assertThat(result).isTrue()
  }

  @Test
  fun equals_BothSuccess_WithNullValues_CustomComparison_ReturnsCustomResult() {
    val observation1: Observation<String?> = Observation.create("test1") { null }
    val observation2: Observation<String?> = Observation.create("test2") { null }

    // Custom comparison that returns false for null values
    val result = observation1.equals(observation2, { _, _ -> false }, null)

    assertThat(result).isFalse()
  }

  @Test
  fun equals_MixedNullAndNonNullValues_ReturnsFalse() {
    val observation1: Observation<String?> = Observation.create("test1") { "result" }
    val observation2: Observation<String?> = Observation.create("test2") { null }

    val result1 = observation1.equals(observation2, null, null)
    val result2 = observation2.equals(observation1, null, null)

    assertThat(result1).isFalse()
    assertThat(result2).isFalse()
  }
}
