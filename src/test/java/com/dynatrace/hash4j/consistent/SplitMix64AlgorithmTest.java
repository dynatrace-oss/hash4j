/*
 * Copyright 2025 Dynatrace LLC
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
package com.dynatrace.hash4j.consistent;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.SplittableRandom;
import org.hipparchus.random.RandomGenerator;
import org.hipparchus.random.Well1024a;
import org.junit.jupiter.api.Test;

class SplitMix64AlgorithmTest {

  @Test
  void testConsistency() {

    RandomGenerator rng = new Well1024a(0xd65757fcf698d8a6L);

    int numIterations = 100;
    int numValuesPerIterations = 10;

    for (int i = 0; i < numIterations; ++i) {
      long seed = rng.nextLong();
      SplittableRandom splittableRandom = new SplittableRandom(seed);
      long state = SplitMix64Algorithm.get().initState(seed);
      {
        long actual = SplitMix64Algorithm.get().nextLong(state);
        long expected = splittableRandom.nextLong();
        assertThat(actual).isEqualTo(expected);
      }
      for (int j = 1; j < numValuesPerIterations; ++j) {
        state = SplitMix64Algorithm.get().updateState(state);
        long actual = SplitMix64Algorithm.get().nextLong(state);
        long expected = splittableRandom.nextLong();
        assertThat(actual).isEqualTo(expected);
      }
    }
  }
}
