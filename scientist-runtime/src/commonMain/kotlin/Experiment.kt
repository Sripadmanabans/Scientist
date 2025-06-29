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
  private val beforeRun: (() -> Unit)?,
  private val afterRun: ((result: Result<T>) -> Unit)?,
  private val publish: (result: Result<T>) -> Unit,
) {

  public fun run(name: String = "control"): T {
    val control = behaviors.getValue(name)
    if (!enabled) {
      return control()
    }
    beforeRun?.invoke()
    val result = generateResults(name, behaviors)
    afterRun?.invoke(result)
    publish(result)
    return result.control.answer.getOrThrow()
  }

  private fun generateResults(name: String, behaviors: Map<String, () -> T>): Result<T> {
    val observations =
      behaviors.keys.shuffled().map { key -> Observation.create(key, behaviors.getValue(key)) }
    val control = observations.first { it.name == name }
    return Result(control, observations - control)
  }

  public class Builder<T>() {
    private val behaviors = mutableMapOf<String, () -> T>()
    private var beforeRun: (() -> Unit)? = null
    private var afterRun: ((result: Result<T>) -> Unit)? = null
    private var publish: ((result: Result<T>) -> Unit) = { _ -> }

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

    public fun beforeRun(block: () -> Unit) {
      beforeRun = block
    }

    public fun afterRun(block: (result: Result<T>) -> Unit) {
      afterRun = block
    }

    public fun publish(block: (result: Result<T>) -> Unit) {
      publish = block
    }

    public fun build(): Experiment<T> {
      return Experiment(
        enabled = enabled,
        behaviors = behaviors.toMap(),
        beforeRun = beforeRun,
        afterRun = afterRun,
        publish = publish,
      )
    }
  }
}

public inline fun <T> Experiment(construct: Experiment.Builder<T>.() -> Unit): Experiment<T> {
  return Experiment.Builder<T>().apply(construct).build()
}

public inline fun <T> science(construct: Experiment.Builder<T>.() -> Unit): T {
  return Experiment.Builder<T>().apply(construct).build().run()
}
