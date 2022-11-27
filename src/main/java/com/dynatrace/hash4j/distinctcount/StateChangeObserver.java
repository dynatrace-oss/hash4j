/*
 * Copyright 2022 Dynatrace LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.dynatrace.hash4j.distinctcount;

/**
 * An observer of state changes of distinct counters such as {@link HyperLogLog} or {@link
 * UltraLogLog}.
 */
@FunctionalInterface
public interface StateChangeObserver {

  /**
   * This method is called whenever the internal state of the approximate distinct counter has
   * changed. After a state change, the probability of a next state change is usually smaller. The
   * positive decrement of this state change probability is passed as an argument.
   *
   * @param probabilityDecrement the positive probability decrement
   */
  void stateChanged(double probabilityDecrement);
}
