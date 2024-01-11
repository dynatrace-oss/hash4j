/*
 * Copyright 2022-2024 Dynatrace LLC
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
package com.dynatrace.hash4j.hashing;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.SplittableRandom;
import org.junit.jupiter.api.Test;

class UnsignedMultiplyUtilTest {

  @Test
  void testUnsignedMultiply() {
    SplittableRandom random = new SplittableRandom(0L);
    int n = 10000;
    long checkSum = 0;
    for (int i = 0; i < n; ++i) {
      checkSum += UnsignedMultiplyUtil.unsignedMultiplyHigh(random.nextLong(), random.nextLong());
    }
    assertThat(checkSum).isEqualTo(0xab0a08649b745db7L);
  }
}
