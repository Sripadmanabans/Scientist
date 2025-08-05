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

import com.adjectivemonk2.scientist.Experiment
import com.adjectivemonk2.scientist.Result
import com.varabyte.truthish.assertThat
import kotlin.test.Test

class ObservationTest {

  @Test
  fun testObservation_equals_with_different_class() {
    var result: Result<String>? = null
    val experiment = Experiment {
      enabled = true
      control { "control result" }
      test { "candidate result" }
      publish { result = it }
    }
    experiment.run()
    assertThat(result).isNotNull()
    assertThat(result!!.control).isNotEqualTo("candidate result")
  }

  @Test
  fun testObservation_equals_with_null() {
    var result: Result<String>? = null
    val experiment = Experiment {
      enabled = true
      control { "control result" }
      test { "candidate result" }
      publish { result = it }
    }
    experiment.run()
    assertThat(result).isNotNull()
    assertThat(result!!.control).isNotEqualTo(null)
  }
}
