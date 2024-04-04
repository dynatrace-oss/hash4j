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

import org.junit.jupiter.api.Test;

class AbstractHashStreamCompatibilityTest {

  @Test
  void testHashCompatibility() {

    HashValue128 hash128 = new HashValue128(0x4cdfea92fccec3ffL, 0x85e6a3b83eb8873aL);
    AbstractHashStream128 calculator =
        new AbstractHashStream128() {

          @Override
          public HashStream128 putByte(byte v) {
            return this;
          }

          @Override
          public HashStream128 reset() {
            return this;
          }

          @Override
          public HashStream128 copy() {
            throw new UnsupportedOperationException();
          }

          @Override
          public int getHashBitSize() {
            return 128;
          }

          @Override
          public HashValue128 get() {
            return hash128;
          }
        };

    assertThat(calculator.getAsInt()).isEqualTo(hash128.getAsInt());
    assertThat(calculator.getAsLong()).isEqualTo(hash128.getAsLong());
    assertThat(calculator.get()).isEqualTo(hash128);
  }
}
