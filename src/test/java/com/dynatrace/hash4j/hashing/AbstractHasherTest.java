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
package com.dynatrace.hash4j.hashing;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.SplittableRandom;
import org.junit.jupiter.api.Test;

class AbstractHasherTest {

  @Test
  void testGetIntFromCharSequence() {
    int numIterations = 1000;
    SplittableRandom random = new SplittableRandom(0L);
    int len = 100;
    char[] chars = new char[1000];
    for (int i = 0; i < numIterations; ++i) {
      int k = random.nextInt();
      int off = random.nextInt(len - 1);
      chars[off] = (char) k;
      chars[off + 1] = (char) (k >>> 16);
      assertThat(AbstractHasher.getInt(new String(chars), off)).isEqualTo(k);
    }
  }

  @Test
  void testGetLongFromCharSequence() {
    int numIterations = 1000;
    SplittableRandom random = new SplittableRandom(0L);
    int len = 100;
    char[] chars = new char[1000];
    for (int i = 0; i < numIterations; ++i) {
      long k = random.nextLong();
      int off = random.nextInt(len - 3);
      chars[off] = (char) k;
      chars[off + 1] = (char) (k >>> 16);
      chars[off + 2] = (char) (k >>> 32);
      chars[off + 3] = (char) (k >>> 48);
      assertThat(AbstractHasher.getLong(new String(chars), off)).isEqualTo(k);
    }
  }
}
