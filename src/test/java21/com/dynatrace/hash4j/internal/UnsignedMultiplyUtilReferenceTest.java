/*
 * Copyright 2025-2026 Dynatrace LLC
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
package com.dynatrace.hash4j.internal;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import java.util.SplittableRandom;
import org.junit.jupiter.api.Test;

class UnsignedMultiplyUtilReferenceTest {

  @Test
  void testAgainstJava() {

    SplittableRandom random = new SplittableRandom(0x75ec0a7f2d98ba4fL);
    int numIterations = 50;
    for (int i = 0; i < numIterations; ++i) {
      long a = random.nextLong();
      long b = random.nextLong();
      assertThat(UnsignedMultiplyUtil.unsignedMultiplyHigh(a, b))
          .isEqualTo(Math.unsignedMultiplyHigh(a, b));
    }
  }
}
