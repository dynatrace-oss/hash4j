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

public class AbstractHasher64Test {

  @Test
  void testHashBytesToLong() {

    long hash = 0x2a80de88db42361fL;

    AbstractHasher64 hasher =
        new AbstractHasher64() {
          @Override
          public HashStream hashStream() {
            return new AbstractHashStream() {

              @Override
              public HashStream putByte(byte v) {
                return this;
              }

              @Override
              public long getAsLong() {
                return hash;
              }

              @Override
              public int getHashBitSize() {
                return 64;
              }
            };
          }
        };

    byte[] b = {};

    assertEquals(hash, hasher.hashBytesToLong(b));
    assertEquals(hash, hasher.hashBytesToLong(b, 0, 0));
  }
}
