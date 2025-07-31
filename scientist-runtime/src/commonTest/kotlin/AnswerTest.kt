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
import com.adjectivemonk2.scientist.getOrDefault
import com.adjectivemonk2.scientist.getOrElse
import com.adjectivemonk2.scientist.getOrThrow
import com.adjectivemonk2.scientist.runCatching
import com.varabyte.truthish.assertThat
import com.varabyte.truthish.assertThrows
import kotlin.test.Test

class AnswerTest {
  @Test
  fun successFactory() {
    val answer = Answer.success("test")
    assertThat(answer.isSuccess).isTrue()
    assertThat(answer.isFailure).isFalse()
    assertThat(answer.getOrNull()).isEqualTo("test")
    assertThat(answer.exceptionOrNull()).isNull()
    assertThat(answer.toString()).isEqualTo("Success(test)")
  }

  @Test
  fun failureFactory() {
    val exception = RuntimeException("test exception")
    val answer = Answer.failure<String>(exception)
    assertThat(answer.isSuccess).isFalse()
    assertThat(answer.isFailure).isTrue()
    assertThat(answer.getOrNull()).isNull()
    assertThat(answer.exceptionOrNull()).isEqualTo(exception)
    assertThat(answer.toString()).isEqualTo("Failure(java.lang.RuntimeException: test exception)")
  }

  @Test
  fun getOrThrow_Success() {
    val answer = Answer.success("test")
    assertThat(answer.getOrThrow()).isEqualTo("test")
  }

  @Test
  fun getOrThrow_Failure() {
    val exception = RuntimeException("test exception")
    val answer = Answer.failure<String>(exception)
    val actualException = assertThrows<RuntimeException> { answer.getOrThrow() }
    assertThat(actualException).isEqualTo(exception)
  }

  @Test
  fun getOrElse_Success() {
    val answer = Answer.success("test")
    val result = answer.getOrElse { "default" }
    assertThat(result).isEqualTo("test")
  }

  @Test
  fun getOrElse_Failure() {
    val exception = RuntimeException("test exception")
    val answer = Answer.failure<String>(exception)
    val result = answer.getOrElse { "default" }
    assertThat(result).isEqualTo("default")
  }

  @Test
  fun getOrDefault_Success() {
    val answer = Answer.success("test")
    val result = answer.getOrDefault("default")
    assertThat(result).isEqualTo("test")
  }

  @Test
  fun getOrDefault_Failure() {
    val exception = RuntimeException("test exception")
    val answer = Answer.failure<String>(exception)
    val result = answer.getOrDefault("defaultValue")
    assertThat(result).isEqualTo("defaultValue")
  }

  @Test
  fun runCatching_Success() {
    val answer = runCatching { "test" }
    assertThat(answer.isSuccess).isTrue()
    assertThat(answer.getOrNull()).isEqualTo("test")
  }

  @Test
  fun runCatching_Failure() {
    val answer = runCatching { throw RuntimeException("test exception") }
    assertThat(answer.isFailure).isTrue()
    assertThat(answer.exceptionOrNull()).isInstanceOf<RuntimeException>()
    assertThat(answer.exceptionOrNull()?.message).isEqualTo("test exception")
  }

  @Test
  fun runCatchingWithReceiver_Success() {
    val receiver = "test"
    val answer = receiver.runCatching { length }
    assertThat(answer.isSuccess).isTrue()
    assertThat(answer.getOrNull()).isEqualTo(4)
  }

  @Test
  fun runCatchingWithReceiver_Failure() {
    val receiver = "test"
    val answer = receiver.runCatching { throw RuntimeException("test exception") }
    assertThat(answer.isFailure).isTrue()
    assertThat(answer.exceptionOrNull()).isInstanceOf<RuntimeException>()
    assertThat(answer.exceptionOrNull()?.message).isEqualTo("test exception")
  }

  @Test
  fun failureEquality() {
    val exception1 = RuntimeException("test exception")
    val exception2 = RuntimeException("test exception")
    val failure1 = Answer.Failure(exception1)
    val failure2 = Answer.Failure(exception1)
    val failure3 = Answer.Failure(exception2)

    assertThat(failure1).isEqualTo(failure2)
    assertThat(failure1).isNotEqualTo(failure3)
    assertThat(failure1).isNotEqualTo("String")
    assertThat(failure1.hashCode()).isEqualTo(exception1.hashCode())
  }

  @Test
  fun answerToString() {
    val answer: Any = Answer.success("test")
    assertThat(answer.toString()).isEqualTo("Success(test)")
  }
}
