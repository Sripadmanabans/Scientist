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

import com.adjectivemonk2.scientist.science
import com.varabyte.truthish.assertThat
import com.varabyte.truthish.assertThrows
import kotlin.test.Test

class ScienceTest {

  @Test
  fun science_EnabledExperiment() {
    var controlCalled = false
    var candidateCalled = false
    var beforeRunCalled = false
    var afterRunCalled = false
    var publishCalled = false

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
      beforeRun { beforeRunCalled = true }
      afterRun { _ -> afterRunCalled = true }
      publish { _ -> publishCalled = true }
    }

    assertThat(result).isEqualTo("control result")
    assertThat(controlCalled).isTrue()
    assertThat(candidateCalled).isTrue()
    assertThat(beforeRunCalled).isTrue()
    assertThat(afterRunCalled).isTrue()
    assertThat(publishCalled).isTrue()
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
