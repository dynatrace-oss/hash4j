/*
 * Copyright 2022-2025 Dynatrace LLC
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
package com.dynatrace.hash4j.random;

/** A provider for pseudo-random generators. */
public interface PseudoRandomGeneratorProvider {

  /**
   * Creates a new {@link PseudoRandomGenerator} instance.
   *
   * @return the new pseudo-random generator instance
   */
  PseudoRandomGenerator create();

  /**
   * Creates a new {@link PseudoRandomGenerator} instance and sets a seed.
   *
   * @param seed the seed value
   * @return the new pseudo-random generator instance
   */
  default PseudoRandomGenerator create(long seed) {
    return create().reset(seed);
  }

  /**
   * Returns a {@link PseudoRandomGeneratorProvider} based on the SplitMix64 algorithm.
   *
   * @return a {@link PseudoRandomGeneratorProvider}
   */
  static PseudoRandomGeneratorProvider splitMix64_V1() {
    return SplitMix64V1::new;
  }
}
