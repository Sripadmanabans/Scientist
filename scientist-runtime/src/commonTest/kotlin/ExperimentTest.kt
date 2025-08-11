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

import com.adjectivemonk2.scientist.BehaviorNotUniqueException
import com.adjectivemonk2.scientist.Experiment
import com.adjectivemonk2.scientist.Result
import com.adjectivemonk2.scientist.getOrThrow
import com.varabyte.truthish.assertThat
import com.varabyte.truthish.assertThrows
import kotlin.test.Test

class ExperimentTest {

  @Test
  fun experiment_builder_DefaultValues() {
    val builder = Experiment.Builder<String>()
    assertThat(builder.enabled).isFalse()
  }

  @Test
  fun experiment_builder_Control() {
    val experiment = Experiment { control { "control result" } }
    val result = experiment.run()
    assertThat(result).isEqualTo("control result")
  }

  @Test
  fun experiment_builder_DuplicateBehaviorName() {
    val exception =
      assertThrows<BehaviorNotUniqueException> {
        Experiment {
          control { "control result" }
          test("control") { "duplicate name" }
        }
      }

    assertThat(exception.message).isEqualTo("control is not unique")
  }

  @Test
  fun experiment_run_DisabledExperiment() {
    var controlCalled = false
    var candidateCalled = false
    var beforeRunCalled = false
    var afterRunCalled = false
    var publishCalled = false

    val experiment = Experiment {
      enabled = false
      control {
        controlCalled = true
        "control result"
      }
      test {
        candidateCalled = true
        "candidate result"
      }
      beforeRun { beforeRunCalled = true }
      afterRun { _ -> afterRunCalled = true }
      publish { _ -> publishCalled = true }
    }

    val result = experiment.run()
    assertThat(result).isEqualTo("control result")
    assertThat(controlCalled).isTrue()
    assertThat(candidateCalled).isFalse()
    assertThat(beforeRunCalled).isFalse()
    assertThat(afterRunCalled).isFalse()
    assertThat(publishCalled).isFalse()
  }

  @Test
  fun experiment_run_EnabledExperiment() {
    var controlCalled = false
    var candidateCalled = false

    val experiment = Experiment {
      enabled = true
      control {
        controlCalled = true
        "control result"
      }
      test {
        candidateCalled = true
        "candidate result"
      }
    }

    val result = experiment.run()
    assertThat(result).isEqualTo("control result")
    assertThat(controlCalled).isTrue()
    assertThat(candidateCalled).isTrue()
  }

  @Test
  fun experiment_run_WithSpecifiedBehaviorName() {
    val experiment = Experiment {
      control { "control result" }
      test("candidate") { "candidate result" }
    }

    val result = experiment.run("candidate")
    assertThat(result).isEqualTo("candidate result")
  }

  @Test
  fun experiment_run_WithNonExistentBehaviorName() {
    val experiment = Experiment { control { "control result" } }
    assertThrows<NoSuchElementException> { experiment.run("non-existent") }
  }

  @Test
  fun experiment_run_ControlThrowsException() {
    val exception = RuntimeException("control exception")
    val experiment = Experiment {
      control { throw exception }
      test { "candidate result" }
    }

    val actualException = assertThrows<RuntimeException> { experiment.run() }
    assertThat(actualException).isEqualTo(exception)
  }

  @Test
  fun experiment_callbacks_ExecutionOrder() {
    val executionOrder = mutableListOf<String>()

    val experiment = Experiment {
      enabled = true
      control { "control result" }
      test { "candidate result" }
      beforeRun { executionOrder.add("beforeRun") }
      afterRun { _ -> executionOrder.add("afterRun") }
      publish { _ -> executionOrder.add("publish") }
    }

    experiment.run()
    assertThat(executionOrder).containsExactly("beforeRun", "afterRun", "publish")
  }

  @Test
  fun experiment_callbacks_RaisedDefaultBehavior() {
    val publishException = RuntimeException("publish exception")

    val experiment = Experiment {
      enabled = true
      control { "control result" }
      test { "candidate result" }
      publish { _ -> throw publishException }
    }

    val exception = assertThrows<RuntimeException> { experiment.run() }
    assertThat(exception).isEqualTo(publishException)
  }

  @Test
  fun experiment_callbacks_RaisedCustomImplementation() {
    val publishException = RuntimeException("publish exception")
    var raisedCalled = false
    var capturedOperation: String? = null
    var capturedThrowable: Throwable? = null

    val experiment = Experiment {
      enabled = true
      control { "control result" }
      test { "candidate result" }
      publish { _ -> throw publishException }
      raised { operation, throwable ->
        raisedCalled = true
        capturedOperation = operation
        capturedThrowable = throwable
      }
    }

    val result = experiment.run()
    assertThat(result).isEqualTo("control result")
    assertThat(raisedCalled).isTrue()
    assertThat(capturedOperation).isEqualTo("publish")
    assertThat(capturedThrowable).isSameAs(publishException)
  }

  @Test
  fun experiment_compare_DefaultBehavior() {
    var result: Result<String>? = null

    val experiment = Experiment {
      enabled = true
      control { "result" }
      test { "result" }
      publish { result = it }
    }

    experiment.run()
    assertThat(result).isNotNull()
    assertThat(result!!.mismatched).isEmpty()
  }

  @Test
  fun experiment_compare_CustomComparison() {
    var result: Result<String>? = null

    val experiment = Experiment {
      enabled = true
      control { "size" }
      test { "test" }
      compare { control, candidate -> control.length == candidate.length }
      publish { result = it }
    }

    experiment.run()
    assertThat(result).isNotNull()
    assertThat(result!!.mismatched).isEmpty()
  }

  @Test
  fun experiment_compareError_DefaultBehavior() {
    var result: Result<String>? = null
    val controlException = RuntimeException("control error")
    val candidateException = RuntimeException("candidate error")

    val experiment = Experiment {
      enabled = true
      control { throw controlException }
      test { throw candidateException }
      publish { result = it }
    }

    try {
      experiment.run()
    } catch (_: RuntimeException) {
      // We expect an exception to be raised, but we handle it in the raised block
    }

    assertThat(result).isNotNull()
    assertThat(result!!.mismatched).isNotEmpty()
    assertThat(result.mismatched.size).isEqualTo(1)
  }

  @Test
  fun experiment_compareError_CustomComparison() {
    var result: Result<String>? = null
    val controlException = RuntimeException("control error")
    val candidateException = RuntimeException("candidate error")

    val experiment = Experiment {
      enabled = true
      control { throw controlException }
      test { throw candidateException }
      compareError { control, candidate -> control.javaClass == candidate.javaClass }
      publish { result = it }
    }

    try {
      experiment.run()
    } catch (_: RuntimeException) {
      // We expect an exception to be raised, but we handle it in the raised block
    }

    assertThat(result).isNotNull()
    assertThat(result!!.mismatched).isEmpty()
  }

  @Test
  fun experiment_with_ignores() {
    var result: Result<String>? = null
    val experiment = Experiment {
      enabled = true
      control { "control result" }
      test("test1") { "candidate" }
      test("test2") { "result" }
      test("test3") { "mismatched" }
      ignore { _, candidate -> candidate.answer.getOrThrow().contains("result") }
      ignore { _, candidate -> candidate.answer.getOrThrow().contains("candidate") }
      publish { result = it }
    }
    val experimentValue = experiment.run()
    assertThat(experimentValue).isEqualTo("control result")
    assertThat(result).isNotNull()
    assertThat(result!!.mismatched).hasSize(3)
    assertThat(result.ignored).hasSize(2)
  }

  @Test
  fun experiment_with_ignore_throws() {
    val ignoreException = RuntimeException("ignore")
    var caughtThrowable: Throwable? = null
    var caughtOperation: String? = null
    val experiment = Experiment {
      enabled = true
      control { "control result" }
      test { "candidate result" }
      ignore { _, _ -> throw ignoreException }
      raised { operation, throwable ->
        caughtOperation = operation
        caughtThrowable = throwable
      }
    }
    val result = experiment.run()
    assertThat(result).isEqualTo("control result")
    assertThat(caughtThrowable).isEqualTo(ignoreException)
    assertThat(caughtOperation).isEqualTo("ignore")
  }

  @Test
  fun experiment_with_default_compare_control_candidate_equal() {
    var result: Result<String>? = null
    val experiment = Experiment {
      enabled = true
      control { "result" }
      test { "result" }
      publish { result = it }
    }
    experiment.run()
    assertThat(result).isNotNull()
    assertThat(result!!.mismatched).isEmpty()
  }

  @Test
  fun experiment_with_custom_compare_control_candidate_equal() {
    var result: Result<String>? = null
    val experiment = Experiment {
      enabled = true
      control { "benz" }
      test { "audi" }
      compare { control, candidate -> control.length == candidate.length }
      publish { result = it }
    }
    experiment.run()
    assertThat(result).isNotNull()
    assertThat(result!!.mismatched).isEmpty()
  }

  @Test
  fun experiment_with_default_compare_control_candidate_not_equal() {
    var result: Result<String>? = null
    val experiment = Experiment {
      enabled = true
      control { "control result" }
      test { "candidate result" }
      publish { result = it }
    }
    experiment.run()
    assertThat(result).isNotNull()
    assertThat(result!!.mismatched).isNotEmpty()
  }

  @Test
  fun experiment_with_custom_compare_control_candidate_not_equal() {
    var result: Result<String>? = null
    val experiment = Experiment {
      enabled = true
      control { "control result" }
      test { "candidate result" }
      compare { control, candidate -> control.length == candidate.length }
      publish { result = it }
    }
    experiment.run()
    assertThat(result).isNotNull()
    assertThat(result!!.mismatched).isNotEmpty()
  }

  @Test
  fun experiment_with_default_compare_error_control_candidate_same_throws() {
    val controlException = RuntimeException("control error")
    var result: Result<String>? = null
    val experiment = Experiment {
      enabled = true
      control { throw controlException }
      test { throw controlException }
      publish { result = it }
    }

    assertThrows<RuntimeException> { experiment.run() }
    assertThat(result).isNotNull()
    assertThat(result!!.mismatched).isEmpty()
  }

  @Test
  fun experiment_with_custom_compare_error_control_candidate_same_throws() {
    val controlException = RuntimeException("control error")
    val candidateException = RuntimeException("control error")
    var result: Result<String>? = null
    val experiment = Experiment {
      enabled = true
      control { throw controlException }
      test { throw candidateException }
      compareError { control, candidate -> control.message == candidate.message }
      publish { result = it }
    }

    assertThrows<RuntimeException> { experiment.run() }
    assertThat(result).isNotNull()
    assertThat(result!!.mismatched).isEmpty()
  }

  @Test
  fun experiment_with_default_compare_error_control_candidate_different_throws() {
    val controlException = RuntimeException("control error")
    val candidateException = RuntimeException("candidate error")
    var result: Result<String>? = null
    val experiment = Experiment {
      enabled = true
      control { throw controlException }
      test { throw candidateException }
      publish { result = it }
    }

    assertThrows<RuntimeException> { experiment.run() }
    assertThat(result).isNotNull()
    assertThat(result!!.mismatched).isNotEmpty()
  }

  @Test
  fun experiment_with_custom_compare_error_control_candidate_different_throws() {
    val controlException = RuntimeException("control error")
    val candidateException = RuntimeException("candidate error")
    var result: Result<String>? = null
    val experiment = Experiment {
      enabled = true
      control { throw controlException }
      test { throw candidateException }
      compareError { control, candidate -> control.message == candidate.message }
      publish { result = it }
    }

    assertThrows<RuntimeException> { experiment.run() }
    assertThat(result).isNotNull()
    assertThat(result!!.mismatched).isNotEmpty()
  }

  @Test
  fun experiment_with_control_success_candidate_throws() {
    val candidateException = RuntimeException("candidate error")
    var result: Result<String>? = null
    val experiment = Experiment {
      enabled = true
      control { "control result" }
      test { throw candidateException }
      publish { result = it }
    }

    val runValue = experiment.run()
    assertThat(runValue).isEqualTo("control result")
    assertThat(result).isNotNull()
    assertThat(result!!.mismatched).isNotEmpty()
  }

  @Test
  fun experiment_with_control_throws_candidate_success() {
    val controlException = RuntimeException("control error")
    var result: Result<String>? = null
    val experiment = Experiment {
      enabled = true
      control { throw controlException }
      test { "candidate result" }
      publish { result = it }
    }

    val exception = assertThrows<RuntimeException> { experiment.run() }
    assertThat(exception).isEqualTo(controlException)
    assertThat(result).isNotNull()
    assertThat(result!!.mismatched).isNotEmpty()
  }
}
