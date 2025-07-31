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

import com.adjectivemonk2.scientist.Observation
import com.adjectivemonk2.scientist.Result
import com.varabyte.truthish.assertThat
import kotlin.test.Test

class ResultTest {

  private val raised = { _: String, _: Throwable -> }

  @Test
  fun test_create_onlyControl() {
    val control = Observation.create("control") { "Control Result" }
    val result = Result.create(control, emptyList(), raised, null, null, null)
    assertThat(result.control).isEqualTo(control)
    assertThat(result.candidates).isEmpty()
    assertThat(result.mismatched).isEmpty()
  }

  @Test
  fun test_create_control_candidate_nomatch() {
    val control = Observation.create("control") { "Control Result" }
    val candidate = Observation.create("candidate") { "Candidate Result" }
    val candidate2 = Observation.create("candidate2") { "Candidate2 Result" }
    val result = Result.create(control, listOf(candidate, candidate2), raised, null, null, null)
    assertThat(result.control).isEqualTo(control)
    assertThat(result.candidates).containsExactly(candidate, candidate2)
    assertThat(result.mismatched).containsExactly(candidate, candidate2)
  }

  @Test
  fun test_create_control_candidate_1_match() {
    val control = Observation.create("control") { "Control Result" }
    val candidate = Observation.create("candidate") { "Control Result" }
    val candidate2 = Observation.create("candidate2") { "Candidate2 Result" }
    val result = Result.create(control, listOf(candidate, candidate2), raised, null, null, null)
    assertThat(result.control).isEqualTo(control)
    assertThat(result.candidates).containsExactly(candidate, candidate2)
    assertThat(result.mismatched).containsExactly(candidate2)
  }

  @Test
  fun test_create_control_candidate_all_match() {
    val control = Observation.create("control") { "Control Result" }
    val candidate = Observation.create("candidate") { "Control Result" }
    val candidate2 = Observation.create("candidate2") { "Control Result" }
    val result = Result.create(control, listOf(candidate, candidate2), raised, null, null, null)
    assertThat(result.control).isEqualTo(control)
    assertThat(result.candidates).containsExactly(candidate, candidate2)
    assertThat(result.mismatched).isEmpty()
  }

  @Test
  fun test_create_control_candidates_ignore() {
    val control = Observation.create("control") { "Control Result" }
    val candidate = Observation.create("candidate") { "Candidate Result" }
    val candidate2 = Observation.create("candidate2") { "Candidate2 Result" }
    val ignore = { _: String?, b: String? -> b?.contains("2") ?: false }
    val result =
      Result.create(
        control = control,
        candidates = listOf(candidate, candidate2),
        raised = raised,
        compare = null,
        compareError = null,
        ignores = listOf(ignore),
      )
    assertThat(result.control).isEqualTo(control)
    assertThat(result.candidates).containsExactly(candidate, candidate2)
    assertThat(result.mismatched).containsExactly(candidate)
    assertThat(result.ignored).containsExactly(candidate2)
  }

  @Test
  fun test_create_control_candidates_ignore_throws() {
    val control = Observation.create("control") { "Control Result" }
    val candidate = Observation.create("candidate") { "Candidate Result" }
    val ignoreThrowable = RuntimeException("Ignore")
    val ignore = { _: String?, _: String? -> throw ignoreThrowable }
    var caughtOperation: String? = null
    var caughtException: Throwable? = null
    val result =
      Result.create(
        control = control,
        candidates = listOf(candidate),
        raised = { operation, throwable ->
          caughtOperation = operation
          caughtException = throwable
        },
        compare = null,
        compareError = null,
        ignores = listOf(ignore),
      )
    assertThat(result.control).isEqualTo(control)
    assertThat(result.candidates).containsExactly(candidate)
    assertThat(result.mismatched).containsExactly(candidate)
    assertThat(result.ignored).isEmpty()
    assertThat(caughtOperation).isEqualTo("ignore")
    assertThat(caughtException).isEqualTo(ignoreThrowable)
  }
}
