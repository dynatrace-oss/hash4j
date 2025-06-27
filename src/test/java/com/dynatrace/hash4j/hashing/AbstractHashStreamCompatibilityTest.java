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
package com.dynatrace.hash4j.hashing;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class AbstractHashStreamCompatibilityTest {

  @Test
  void testHashCompatibility() {
    byte[] dummyByteArray = {};
    String dummyChars = "";

    HashValue128 hash128 = new HashValue128(0x4cdfea92fccec3ffL, 0x85e6a3b83eb8873aL);

    Hasher128 hasher = HashMocks.createHasher128WithFixedHash(hash128);
    HashStream128 hashStream = hasher.hashStream();

    assertThat(hashStream.getAsInt()).isEqualTo(hash128.getAsInt());
    assertThat(hashStream.getAsLong()).isEqualTo(hash128.getAsLong());
    assertThat(hashStream.get()).isEqualTo(hash128);

    assertThat(hasher.hashBytesToInt(dummyByteArray)).isEqualTo(hash128.getAsInt());
    assertThat(hasher.hashBytesToLong(dummyByteArray)).isEqualTo(hash128.getAsLong());
    assertThat(hasher.hashBytesTo128Bits(dummyByteArray)).isEqualTo(hash128);

    assertThat(hasher.hashBytesToInt(dummyByteArray, 0, 0)).isEqualTo(hash128.getAsInt());
    assertThat(hasher.hashBytesToLong(dummyByteArray, 0, 0)).isEqualTo(hash128.getAsLong());
    assertThat(hasher.hashBytesTo128Bits(dummyByteArray, 0, 0)).isEqualTo(hash128);

    assertThat(hasher.hashCharsToInt(dummyChars)).isEqualTo(hash128.getAsInt());
    assertThat(hasher.hashCharsToLong(dummyChars)).isEqualTo(hash128.getAsLong());
    assertThat(hasher.hashCharsTo128Bits(dummyChars)).isEqualTo(hash128);
  }
}
