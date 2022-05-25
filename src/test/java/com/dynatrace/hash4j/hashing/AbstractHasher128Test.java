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

public class AbstractHasher128Test {

  @Test
  void testHashBytesTo128Bits() {

    HashValue128 hash = new HashValue128(0x5cd2aeb8be6aa0bbL, 0x500e3ed0c42e364fL);

    AbstractHasher128 hasher =
        new AbstractHasher128() {
          @Override
          public HashStream hashStream() {
            return new AbstractHashStream() {

              @Override
              public HashStream putByte(byte v) {
                return this;
              }

              @Override
              public HashValue128 get() {
                return hash;
              }

              @Override
              public int getHashBitSize() {
                return 128;
              }
            };
          }
        };

    byte[] b = {};

    assertEquals(hash, hasher.hashBytesTo128Bits(b));
    assertEquals(hash, hasher.hashBytesTo128Bits(b, 0, 0));
  }
}
