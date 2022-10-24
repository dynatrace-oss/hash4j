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

import org.junit.jupiter.api.Test;

class AbstractHasher128Test {

  @Test
  void testHashBytesTo128Bits() {

    HashValue128 hash = new HashValue128(0x5cd2aeb8be6aa0bbL, 0x500e3ed0c42e364fL);

    AbstractHasher128 hasher =
        new AbstractHasher128() {
          @Override
          public HashValue128 hashBytesTo128Bits(byte[] input, int off, int len) {
            return hash;
          }

          @Override
          public HashValue128 hashCharsTo128Bits(CharSequence input) {
            return hash;
          }

          @Override
          public HashStream hashStream() {
            return new AbstractHashStream() {

              @Override
              public HashStream putByte(byte v) {
                return this;
              }

              @Override
              public HashStream reset() {
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
    String s = "";

    assertThat(hasher.hashBytesTo128Bits(b)).isEqualTo(hash);
    assertThat(hasher.hashBytesToLong(b)).isEqualTo(hash.getAsLong());
    assertThat(hasher.hashBytesToInt(b)).isEqualTo(hash.getAsInt());

    assertThat(hasher.hashBytesTo128Bits(b, 0, 0)).isEqualTo(hash);
    assertThat(hasher.hashBytesToLong(b, 0, 0)).isEqualTo(hash.getAsLong());
    assertThat(hasher.hashBytesToInt(b, 0, 0)).isEqualTo(hash.getAsInt());

    assertThat(hasher.hashCharsTo128Bits(s)).isEqualTo(hash);
    assertThat(hasher.hashCharsToLong(s)).isEqualTo(hash.getAsLong());
    assertThat(hasher.hashCharsToInt(s)).isEqualTo(hash.getAsInt());
  }
}
