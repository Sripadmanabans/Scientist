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
import com.adjectivemonk2.scientist.science
import com.varabyte.truthish.assertThat
import com.varabyte.truthish.assertThrows
import kotlin.test.Test

class ExperimentTest {

  // ===== EXPERIMENT: BUILDER TESTS =====

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
  fun experiment_builder_Test() {
    val experiment = Experiment {
      control { "control result" }
      test("candidate") { "candidate result" }
    }
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

  // ===== EXPERIMENT: RUN TESTS =====

  @Test
  fun experiment_run_DisabledExperiment() {
    var controlCalled = false
    var candidateCalled = false

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
    }

    val result = experiment.run()

    assertThat(result).isEqualTo("control result")
    assertThat(controlCalled).isTrue()
    assertThat(candidateCalled).isFalse()
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

    assertThrows<RuntimeException> { experiment.run() }
  }

  // ===== EXPERIMENT: CALLBACK TESTS =====

  @Test
  fun experiment_callbacks_BeforeRun() {
    var beforeRunCalled = false

    val experiment = Experiment {
      enabled = true
      control { "control result" }
      test { "candidate result" }
      beforeRun { beforeRunCalled = true }
    }

    experiment.run()

    assertThat(beforeRunCalled).isTrue()
  }

  @Test
  fun experiment_callbacks_AfterRun() {
    var afterRunCalled = false
    var result: Result<String>? = null

    val experiment = Experiment {
      enabled = true
      control { "control result" }
      test { "candidate result" }
      afterRun {
        afterRunCalled = true
        result = it
      }
    }

    experiment.run()

    assertThat(afterRunCalled).isTrue()
    assertThat(result).isNotNull()
    assertThat(result!!.candidates).isNotEmpty()
  }

  @Test
  fun experiment_callbacks_Publish() {
    var publishCalled = false
    var result: Result<String>? = null

    val experiment = Experiment {
      enabled = true
      control { "control result" }
      test { "candidate result" }
      publish {
        publishCalled = true
        result = it
      }
    }

    experiment.run()

    assertThat(publishCalled).isTrue()
    assertThat(result).isNotNull()
    assertThat(result!!.candidates).isNotEmpty()
  }

  @Test
  fun experiment_callbacks_ExecutionOrder() {
    // Use a simple list to track the order of execution
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
    assertThat(exception).isSameAs(publishException)
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
  fun experiment_callbacks_NotCalledWhenDisabled() {
    var beforeRunCalled = false
    var afterRunCalled = false
    var publishCalled = false

    val experiment = Experiment {
      enabled = false
      control { "control result" }
      test { "candidate result" }
      beforeRun { beforeRunCalled = true }
      afterRun { _ -> afterRunCalled = true }
      publish { _ -> publishCalled = true }
    }

    experiment.run()

    assertThat(beforeRunCalled).isFalse()
    assertThat(afterRunCalled).isFalse()
    assertThat(publishCalled).isFalse()
  }

  // ===== SCIENCE FUNCTION TESTS =====

  @Test
  fun science_BasicFunction() {
    val result = science {
      control { "control result" }
      test { "candidate result" }
    }

    assertThat(result).isEqualTo("control result")
  }

  @Test
  fun science_EnabledExperiment() {
    var controlCalled = false
    var candidateCalled = false

    val result = science {
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

    assertThat(result).isEqualTo("control result")
    assertThat(controlCalled).isTrue()
    assertThat(candidateCalled).isTrue()
  }

  @Test
  fun science_ControlThrowsException() {
    val exception = RuntimeException("control exception")

    assertThrows<RuntimeException> {
      science {
        control { throw exception }
        test { "candidate result" }
      }
    }
  }

  @Test
  fun science_WithCallbacks() {
    var beforeRunCalled = false
    var afterRunCalled = false
    var publishCalled = false

    val result = science {
      enabled = true
      control { "control result" }
      test { "candidate result" }
      beforeRun { beforeRunCalled = true }
      afterRun { _ -> afterRunCalled = true }
      publish { _ -> publishCalled = true }
    }

    assertThat(result).isEqualTo("control result")
    assertThat(beforeRunCalled).isTrue()
    assertThat(afterRunCalled).isTrue()
    assertThat(publishCalled).isTrue()
  }

  @Test
  fun science_WithRaisedCallback() {
    val publishException = RuntimeException("publish exception")
    var raisedCalled = false

    val result = science {
      enabled = true
      control { "control result" }
      test { "candidate result" }
      publish { _ -> throw publishException }
      raised { _, _ -> raisedCalled = true }
    }

    assertThat(result).isEqualTo("control result")
    assertThat(raisedCalled).isTrue()
  }

  @Test
  fun science_DisabledExperiment() {
    var controlCalled = false
    var candidateCalled = false

    val result = science {
      enabled = false
      control {
        controlCalled = true
        "control result"
      }
      test {
        candidateCalled = true
        "candidate result"
      }
    }

    assertThat(result).isEqualTo("control result")
    assertThat(controlCalled).isTrue()
    assertThat(candidateCalled).isFalse()
  }
}
