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
  fun experiment_enabled_callbacks_ExecutionOrder() {
    val executionOrder = mutableListOf<String>()
    val experiment = Experiment {
      enabled = true
      control { "control result" }
      test { "candidate result" }
      beforeRun { executionOrder.add("beforeRun") }
      afterRun { _ -> executionOrder.add("afterRun") }
      publish { _ -> executionOrder.add("publish") }
    }
    val result = experiment.run()
    assertThat(result).isEqualTo("control result")
    assertThat(executionOrder).containsExactly("beforeRun", "afterRun", "publish")
  }

  @Test
  fun experiment_disabled_callbacks_ExecutionOrder() {
    val executionOrder = mutableListOf<String>()
    val experiment = Experiment {
      enabled = false
      control { "control result" }
      test { "candidate result" }
      beforeRun { executionOrder.add("beforeRun") }
      afterRun { _ -> executionOrder.add("afterRun") }
      publish { _ -> executionOrder.add("publish") }
    }
    val result = experiment.run()
    assertThat(result).isEqualTo("control result")
    assertThat(executionOrder).isEmpty()
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
    val experiment = Experiment {
      enabled = true
      control { "control result" }
      test { "candidate result" }
      publish { _ -> throw publishException }
      raised { operation, throwable ->
        raisedCalled = true
        assertThat(operation).isEqualTo("publish")
        assertThat(throwable).isEqualTo(publishException)
      }
    }
    val result = experiment.run()
    assertThat(result).isEqualTo("control result")
    assertThat(raisedCalled).isTrue()
  }

  @Test
  fun experiment_compare_DefaultBehavior() {
    val experiment = Experiment {
      enabled = true
      control { "result" }
      test { "result" }
      publish { result -> assertThat(result.mismatched).isEmpty() }
    }
    val result = experiment.run()
    assertThat(result).isEqualTo("result")
  }

  @Test
  fun experiment_compare_CustomComparison() {
    val experiment = Experiment {
      enabled = true
      control { "size" }
      test { "test" }
      compare { control, candidate -> control.length == candidate.length }
      publish { result -> assertThat(result.mismatched).isEmpty() }
    }
    val result = experiment.run()
    assertThat(result).isEqualTo("size")
  }

  @Test
  fun experiment_compareError_DefaultBehavior() {
    val controlException = RuntimeException("control error")
    val candidateException = RuntimeException("candidate error")
    val experiment = Experiment {
      enabled = true
      control { throw controlException }
      test { throw candidateException }
      publish { result -> assertThat(result.mismatched.size).isEqualTo(1) }
    }

    try {
      experiment.run()
    } catch (_: RuntimeException) {
      // We expect an exception to be raised, but we handle it in the raised block
    }
  }

  @Test
  fun experiment_compareError_CustomComparison() {
    val controlException = RuntimeException("control error")
    val candidateException = RuntimeException("candidate error")
    val experiment = Experiment {
      enabled = true
      control { throw controlException }
      test { throw candidateException }
      compareError { control, candidate -> control.javaClass == candidate.javaClass }
      publish { result -> assertThat(result.mismatched).isEmpty() }
    }

    try {
      experiment.run()
    } catch (_: RuntimeException) {
      // We expect an exception to be raised, but we handle it in the raised block
    }
  }

  @Test
  fun experiment_with_ignores() {
    val experiment = Experiment {
      enabled = true
      control { "control result" }
      test("test1") { "candidate" }
      test("test2") { "result" }
      test("test3") { "mismatched" }
      ignore { _, candidate -> candidate.answer.getOrThrow().contains("result") }
      ignore { _, candidate -> candidate.answer.getOrThrow().contains("candidate") }
      publish { result ->
        assertThat(result.mismatched).hasSize(3)
        assertThat(result.ignored).hasSize(2)
      }
    }
    val result = experiment.run()
    assertThat(result).isEqualTo("control result")
  }

  @Test
  fun experiment_with_ignore_throws() {
    val ignoreException = RuntimeException("ignore")
    val experiment = Experiment {
      enabled = true
      control { "control result" }
      test { "candidate result" }
      ignore { _, _ -> throw ignoreException }
      raised { operation, throwable ->
        assertThat(operation).isEqualTo("ignore")
        assertThat(throwable).isEqualTo(ignoreException)
      }
    }
    val result = experiment.run()
    assertThat(result).isEqualTo("control result")
  }

  @Test
  fun experiment_with_default_compare_control_candidate_equal() {
    val experiment = Experiment {
      enabled = true
      control { "result" }
      test { "result" }
      publish { result -> assertThat(result.mismatched).isEmpty() }
    }
    val result = experiment.run()
    assertThat(result).isEqualTo("result")
  }

  @Test
  fun experiment_with_custom_compare_control_candidate_equal() {
    val experiment = Experiment {
      enabled = true
      control { "benz" }
      test { "audi" }
      compare { control, candidate -> control.length == candidate.length }
      publish { result -> assertThat(result.mismatched).isEmpty() }
    }
    val result = experiment.run()
    assertThat(result).isEqualTo("benz")
  }

  @Test
  fun experiment_with_default_compare_control_candidate_not_equal() {
    val experiment = Experiment {
      enabled = true
      control { "control result" }
      test { "candidate result" }
      publish { result -> assertThat(result.mismatched).isNotEmpty() }
    }
    val result = experiment.run()
    assertThat(result).isEqualTo("control result")
  }

  @Test
  fun experiment_with_custom_compare_control_candidate_not_equal() {
    val experiment = Experiment {
      enabled = true
      control { "control result" }
      test { "candidate result" }
      compare { control, candidate -> control.length == candidate.length }
      publish { result -> assertThat(result.mismatched).isNotEmpty() }
    }
    val result = experiment.run()
    assertThat(result).isEqualTo("control result")
  }

  @Test
  fun experiment_with_default_compare_error_control_candidate_same_throws() {
    val controlException = RuntimeException("control error")
    val experiment = Experiment {
      enabled = true
      control { throw controlException }
      test { throw controlException }
      publish { result -> assertThat(result.mismatched).isEmpty() }
    }

    val exception = assertThrows<RuntimeException> { experiment.run() }
    assertThat(exception).isEqualTo(controlException)
  }

  @Test
  fun experiment_with_custom_compare_error_control_candidate_same_throws() {
    val controlException = RuntimeException("control error")
    val candidateException = RuntimeException("control error")
    val experiment = Experiment {
      enabled = true
      control { throw controlException }
      test { throw candidateException }
      compareError { control, candidate -> control.message == candidate.message }
      publish { result -> assertThat(result.mismatched).isEmpty() }
    }

    val exception = assertThrows<RuntimeException> { experiment.run() }
    assertThat(exception).isEqualTo(controlException)
  }

  @Test
  fun experiment_with_default_compare_error_control_candidate_different_throws() {
    val controlException = RuntimeException("control error")
    val candidateException = RuntimeException("candidate error")
    val experiment = Experiment {
      enabled = true
      control { throw controlException }
      test { throw candidateException }
      publish { result -> assertThat(result.mismatched).isNotEmpty() }
    }

    val exception = assertThrows<RuntimeException> { experiment.run() }
    assertThat(exception).isEqualTo(controlException)
  }

  @Test
  fun experiment_with_custom_compare_error_control_candidate_different_throws() {
    val controlException = RuntimeException("control error")
    val candidateException = RuntimeException("candidate error")
    val experiment = Experiment {
      enabled = true
      control { throw controlException }
      test { throw candidateException }
      compareError { control, candidate -> control.message == candidate.message }
      publish { result -> assertThat(result.mismatched).isNotEmpty() }
    }

    val exception = assertThrows<RuntimeException> { experiment.run() }
    assertThat(exception).isEqualTo(controlException)
  }

  @Test
  fun experiment_with_control_success_candidate_throws() {
    val candidateException = RuntimeException("candidate error")
    val experiment = Experiment {
      enabled = true
      control { "control result" }
      test { throw candidateException }
      publish { result -> assertThat(result.mismatched).isNotEmpty() }
    }

    val runValue = experiment.run()
    assertThat(runValue).isEqualTo("control result")
  }

  @Test
  fun experiment_with_control_throws_candidate_success() {
    val controlException = RuntimeException("control error")
    val experiment = Experiment {
      enabled = true
      control { throw controlException }
      test { "candidate result" }
      publish { result -> assertThat(result.mismatched).isNotEmpty() }
    }

    val exception = assertThrows<RuntimeException> { experiment.run() }
    assertThat(exception).isEqualTo(controlException)
  }

  @Test
  fun experiment_run_if_not_called_when_only_control() {
    var runIfCalled = false
    var controlCalled = false
    val experiment = Experiment {
      enabled = true
      runIf {
        runIfCalled = true
        true
      }
      control {
        controlCalled = true
        "control result"
      }
    }

    experiment.run()
    assertThat(controlCalled).isTrue()
    assertThat(runIfCalled).isFalse()
  }

  @Test
  fun experiment_run_if_and_candidate_not_called_when_not_enabled() {
    var runIfCalled = false
    var controlCalled = false
    var candidateCalled = false
    val experiment = Experiment {
      enabled = false
      runIf {
        runIfCalled = true
        true
      }
      control {
        controlCalled = true
        "control result"
      }
      test {
        candidateCalled = true
        "candidate result"
      }
    }

    experiment.run()
    assertThat(controlCalled).isTrue()
    assertThat(candidateCalled).isFalse()
    assertThat(runIfCalled).isFalse()
  }

  @Test
  fun experiment_run_if_called_when_enabled_and_more_than_1_behavior_candidate_not_called() {
    var runIfCalled = false
    var controlCalled = false
    var candidateCalled = false
    val experiment = Experiment {
      enabled = true
      runIf {
        runIfCalled = true
        false
      }
      control {
        controlCalled = true
        "control result"
      }
      test {
        candidateCalled = true
        "candidate result"
      }
    }

    experiment.run()
    assertThat(controlCalled).isTrue()
    assertThat(candidateCalled).isFalse()
    assertThat(runIfCalled).isTrue()
  }

  @Test
  fun experiment_run_if_called_when_enabled_and_more_than_1_behavior_candidate_called() {
    var runIfCalled = false
    var controlCalled = false
    var candidateCalled = false
    val experiment = Experiment {
      enabled = true
      runIf {
        runIfCalled = true
        true
      }
      control {
        controlCalled = true
        "control result"
      }
      test {
        candidateCalled = true
        "candidate result"
      }
    }

    experiment.run()
    assertThat(controlCalled).isTrue()
    assertThat(candidateCalled).isTrue()
    assertThat(runIfCalled).isTrue()
  }

  @Test
  fun experiment_run_if_throws_when_enabled_and_more_than_1_behavior_candidate_not_called() {
    val runIfException = RuntimeException("runIf exception")
    var runIfCalled = false
    var controlCalled = false
    var candidateCalled = false
    var raisedCalled = false
    val experiment = Experiment {
      enabled = true
      runIf {
        runIfCalled = true
        throw runIfException
      }
      control {
        controlCalled = true
        "control result"
      }
      test {
        candidateCalled = true
        "candidate result"
      }
      raised { operation, throwable ->
        raisedCalled = true
        assertThat(operation).isEqualTo("runIf")
        assertThat(throwable).isEqualTo(runIfException)
      }
    }

    experiment.run()
    assertThat(controlCalled).isTrue()
    assertThat(candidateCalled).isFalse()
    assertThat(runIfCalled).isTrue()
    assertThat(raisedCalled).isTrue()
  }
}
