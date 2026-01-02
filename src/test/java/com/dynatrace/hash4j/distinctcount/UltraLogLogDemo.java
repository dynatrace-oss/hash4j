/*
 * Copyright 2022-2026 Dynatrace LLC
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

import static org.assertj.core.api.Assertions.assertThat;

import com.dynatrace.hash4j.hashing.Hasher64;
import com.dynatrace.hash4j.hashing.Hashing;
import org.assertj.core.data.Percentage;
import org.junit.jupiter.api.Test;

class UltraLogLogDemo {

  @Test
  void demoBasicUsage() {

    Hasher64 hasher = Hashing.komihash5_0();

    UltraLogLog sketch = UltraLogLog.create(12);

    sketch.add(hasher.hashCharsToLong("foo"));
    sketch.add(hasher.hashCharsToLong("bar"));
    sketch.add(hasher.hashCharsToLong("foo"));

    double distinctCountEstimate = sketch.getDistinctCountEstimate();

    assertThat(distinctCountEstimate).isCloseTo(2., Percentage.withPercentage(10));
  }

  @Test
  void demoMerging() {

    Hasher64 hasher = Hashing.komihash5_0();

    UltraLogLog sketch1 =
        UltraLogLog.create(12)
            .add(hasher.hashCharsToLong("foo"))
            .add(hasher.hashCharsToLong("bar"));
    UltraLogLog sketch2 = UltraLogLog.create(14).add(hasher.hashCharsToLong("foo"));

    UltraLogLog sketchMerged = UltraLogLog.merge(sketch1, sketch2);

    double distinctCountEstimate = sketchMerged.getDistinctCountEstimate();

    assertThat(distinctCountEstimate).isCloseTo(2., Percentage.withPercentage(10));
  }

  @Test
  void demoMartingaleEstimation() {

    Hasher64 hasher = Hashing.komihash5_0();

    UltraLogLog sketch = UltraLogLog.create(12);
    MartingaleEstimator martingaleEstimator = new MartingaleEstimator();

    sketch.add(hasher.hashCharsToLong("foo"), martingaleEstimator);
    assertThat(martingaleEstimator.getDistinctCountEstimate())
        .isCloseTo(1., Percentage.withPercentage(10));

    sketch.add(hasher.hashCharsToLong("bar"), martingaleEstimator);
    assertThat(martingaleEstimator.getDistinctCountEstimate())
        .isCloseTo(2., Percentage.withPercentage(10));

    sketch.add(hasher.hashCharsToLong("foo"), martingaleEstimator);
    assertThat(martingaleEstimator.getDistinctCountEstimate())
        .isCloseTo(2., Percentage.withPercentage(10));
  }
}
