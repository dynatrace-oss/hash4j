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

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class AbstractHashStreamCompatibilityTest {

  @Test
  void testHashCompatibility() {

    HashValue128 hash128 = new HashValue128(0x4cdfea92fccec3ffL, 0x85e6a3b83eb8873aL);
    AbstractHashStream calculator =
        new AbstractHashStream() {

          @Override
          public HashStream putByte(byte v) {
            return this;
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

    assertEquals(hash128.getAsInt(), calculator.getAsInt());
    assertEquals(hash128.getAsLong(), calculator.getAsLong());
    assertEquals(hash128, calculator.get());
  }
}
