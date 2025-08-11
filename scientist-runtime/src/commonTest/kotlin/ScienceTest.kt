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
import kotlin.test.Test

class ScienceTest {

  @Test
  fun science_without_name_EnabledExperiment() {
    var controlCalled = false
    var candidateCalled = false
    val callOrder = mutableListOf<String>()

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
      beforeRun { callOrder.add("before") }
      afterRun { callOrder.add("after") }
      publish { callOrder.add("publish") }
    }

    assertThat(result).isEqualTo("control result")
    assertThat(controlCalled).isTrue()
    assertThat(candidateCalled).isTrue()
    assertThat(callOrder).containsExactly("before", "after", "publish")
  }

  @Test
  fun science_without_name_DisabledExperiment() {
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

  @Test
  fun science_name_EnabledExperiment() {
    var test1Called = false
    var test2Called = false
    val callOrder = mutableListOf<String>()

    val result =
      science("Test1") {
        enabled = true
        test("Test1") {
          test1Called = true
          "test1 result"
        }
        test("Test2") {
          test2Called = true
          "test2 result"
        }
        beforeRun { callOrder.add("before") }
        afterRun { callOrder.add("after") }
        publish { callOrder.add("publish") }
      }

    assertThat(result).isEqualTo("test1 result")
    assertThat(test1Called).isTrue()
    assertThat(test2Called).isTrue()
    assertThat(callOrder).containsExactly("before", "after", "publish")
  }

  @Test
  fun science_with_name_DisabledExperiment() {
    var test1Called = false
    var test2Called = false

    val result =
      science("Test2") {
        enabled = false
        test("Test1") {
          test1Called = true
          "test1 result"
        }
        test("Test2") {
          test2Called = true
          "test2 result"
        }
      }

    assertThat(result).isEqualTo("test2 result")
    assertThat(test1Called).isFalse()
    assertThat(test2Called).isTrue()
  }
}
