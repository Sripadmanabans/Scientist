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

package com.adjectivemonk2.scientist

public class Experiment<T>
private constructor(
  private val enabled: Boolean,
  private val behaviors: Map<String, () -> T>,
  private val runIf: (() -> Boolean)?,
  private val beforeRun: (() -> Unit)?,
  private val afterRun: ((result: Result<T>) -> Unit)?,
  private val publish: (result: Result<T>) -> Unit,
  internal val raised: (operation: String, throwable: Throwable) -> Unit,
  internal val compare: ((control: T, candidate: T) -> Boolean)?,
  internal val compareError: ((control: Throwable, candidate: Throwable) -> Boolean)?,
  internal val ignores: List<(control: Observation<T>, candidate: Observation<T>) -> Boolean>?,
) {

  public fun run(name: String = "control"): T {
    val control = behaviors.getValue(name)
    if (!shouldRunExperiment()) {
      return control()
    }
    beforeRun?.invoke()
    val result = generateResults(name, behaviors)
    afterRun?.invoke(result)
    try {
      publish(result)
    } catch (throwable: Throwable) {
      raised("publish", throwable)
    }
    return result.control.answer.getOrThrow()
  }

  private fun shouldRunExperiment(): Boolean {
    return behaviors.size > 1 && enabled && runIfAllowed()
  }

  private fun runIfAllowed(): Boolean {
    return try {
      runIf?.invoke() ?: true
    } catch (throwable: Throwable) {
      raised("runIf", throwable)
      false
    }
  }

  private fun generateResults(name: String, behaviors: Map<String, () -> T>): Result<T> {
    val observations =
      behaviors.keys.shuffled().map { key ->
        Observation.create(key, this, behaviors.getValue(key))
      }
    val control = observations.first { it.name == name }
    val candidates = observations - control
    val mismatched = candidates.filterNotTo(mutableSetOf()) { candidate -> control == candidate }
    val ignored =
      if (ignores != null) {
        mismatched.filterTo(mutableSetOf()) { candidate ->
          ignores.any { ignore ->
            try {
              ignore(control, candidate)
            } catch (throwable: Throwable) {
              raised("ignore", throwable)
              false
            }
          }
        }
      } else {
        emptySet()
      }
    return Result(control, observations - control, mismatched, ignored)
  }

  public class Builder<T>() {
    private val behaviors = mutableMapOf<String, () -> T>()
    private var runIf: (() -> Boolean)? = null
    private var beforeRun: (() -> Unit)? = null
    private var afterRun: ((result: Result<T>) -> Unit)? = null
    private var publish: ((result: Result<T>) -> Unit) = { _ -> }
    private var raised: (operation: String, throwable: Throwable) -> Unit = { _, throwable ->
      throw throwable
    }

    private var compare: ((control: T, candidate: T) -> Boolean)? = null
    private var compareError: ((control: Throwable, candidate: Throwable) -> Boolean)? = null

    private var ignores:
      MutableList<(control: Observation<T>, candidate: Observation<T>) -> Boolean>? =
      null

    public var enabled: Boolean = false

    public fun control(block: () -> T) {
      test("control", block)
    }

    public fun test(name: String = "candidate", block: () -> T) {
      if (behaviors.containsKey(name)) {
        throw BehaviorNotUniqueException(name)
      }
      behaviors[name] = block
    }

    public fun runIf(predicate: () -> Boolean) {
      runIf = predicate
    }

    public fun beforeRun(block: () -> Unit) {
      beforeRun = block
    }

    public fun afterRun(block: (result: Result<T>) -> Unit) {
      afterRun = block
    }

    public fun publish(block: (result: Result<T>) -> Unit) {
      publish = block
    }

    public fun raised(block: (operation: String, throwable: Throwable) -> Unit) {
      raised = block
    }

    public fun compare(block: (control: T, candidate: T) -> Boolean) {
      compare = block
    }

    public fun compareError(block: (control: Throwable, candidate: Throwable) -> Boolean) {
      compareError = block
    }

    public fun ignore(block: (control: Observation<T>, candidate: Observation<T>) -> Boolean) {
      if (ignores == null) {
        ignores = mutableListOf()
      }
      ignores!!.add(block)
    }

    public fun build(): Experiment<T> {
      return Experiment(
        enabled = enabled,
        behaviors = behaviors.toMap(),
        runIf = runIf,
        beforeRun = beforeRun,
        afterRun = afterRun,
        publish = publish,
        raised = raised,
        compare = compare,
        compareError = compareError,
        ignores = ignores?.toList(),
      )
    }
  }
}

public inline fun <T> Experiment(construct: Experiment.Builder<T>.() -> Unit): Experiment<T> {
  return Experiment.Builder<T>().apply(construct).build()
}
