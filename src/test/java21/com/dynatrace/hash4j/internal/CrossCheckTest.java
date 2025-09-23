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
package com.dynatrace.hash4j.internal;

import static org.assertj.core.api.Assertions.assertThat;

import com.dynatrace.hash4j.hashing.Hashing;
import java.util.SplittableRandom;
import org.junit.jupiter.api.Test;

class CrossCheckTest {

  private static final int MAX_LENGTH = 2000;
  private static final int NUM_ITERATIONS = 5;

  @Test
  void testMurmur3_128JHashMurmur3() {
    SplittableRandom random = new SplittableRandom(0x60679c14de1c0954L);
    for (int len = 0; len < MAX_LENGTH; ++len) {
      byte[] b = new byte[len];
      for (int i = 0; i < NUM_ITERATIONS; ++i) {
        int seed = random.nextInt();
        random.nextBytes(b);

        assertThat(io.github.gbessonov.jhash.JHash.murmur3_128(b).getValueBytesLittleEndian())
            .isEqualTo(Hashing.murmur3_128().hashBytesTo128Bits(b).toByteArray());
        assertThat(
                io.github.gbessonov.jhash.JHash.newMurmur3_128()
                    .include(b)
                    .hash()
                    .getValueBytesLittleEndian())
            .isEqualTo(Hashing.murmur3_128().hashBytesTo128Bits(b).toByteArray());
        if (seed >= 0) {
          // not compatible with reference implementation for negative seeds
          assertThat(
                  io.github.gbessonov.jhash.JHash.murmur3_128(b, seed).getValueBytesLittleEndian())
              .isEqualTo(Hashing.murmur3_128(seed).hashBytesTo128Bits(b).toByteArray());
          assertThat(
                  io.github.gbessonov.jhash.JHash.newMurmur3_128(seed)
                      .include(b)
                      .hash()
                      .getValueBytesLittleEndian())
              .isEqualTo(Hashing.murmur3_128(seed).hashBytesTo128Bits(b).toByteArray());
        }
      }
    }
  }
}
