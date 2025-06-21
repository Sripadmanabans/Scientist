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
import com.adjectivemonk2.scientist.science
import com.varabyte.truthish.assertThat
import com.varabyte.truthish.assertThrows
import kotlin.test.Test

class ExperimentTest {

  @Test
  fun builder_DefaultValues() {
    val builder = Experiment.Builder<String>()
    assertThat(builder.enabled).isFalse()
  }

  @Test
  fun builder_Control() {
    val experiment = Experiment { control { "control result" } }
    val result = experiment.run()
    assertThat(result).isEqualTo("control result")
  }

  @Test
  fun builder_Test() {
    val experiment = Experiment {
      control { "control result" }
      test("candidate") { "candidate result" }
    }
    val result = experiment.run()
    assertThat(result).isEqualTo("control result")
  }

  @Test
  fun builder_DuplicateBehaviorName() {
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
  fun run_DisabledExperiment() {
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
  fun run_EnabledExperiment() {
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
  fun run_WithSpecifiedBehaviorName() {
    val experiment = Experiment {
      control { "control result" }
      test("candidate") { "candidate result" }
    }

    val result = experiment.run("candidate")

    assertThat(result).isEqualTo("candidate result")
  }

  @Test
  fun run_WithNonExistentBehaviorName() {
    val experiment = Experiment { control { "control result" } }

    assertThrows<NoSuchElementException> { experiment.run("non-existent") }
  }

  @Test
  fun run_ControlThrowsException() {
    val exception = RuntimeException("control exception")
    val experiment = Experiment {
      control { throw exception }
      test { "candidate result" }
    }

    assertThrows<RuntimeException> { experiment.run() }
  }

  @Test
  fun science_Function() {
    val result = science {
      control { "control result" }
      test { "candidate result" }
    }

    assertThat(result).isEqualTo("control result")
  }
}
