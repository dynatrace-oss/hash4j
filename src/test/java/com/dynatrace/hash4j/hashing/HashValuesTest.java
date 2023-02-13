/*
 * Copyright 2023 Dynatrace LLC
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

class HashValuesTest {

  @Test
  void testToHexStringAndToByteArray() {

    HashValue128 hashValue128 = new HashValue128(0xf0e1d2c3b4a59687L, 0x78695a4b3c2d1e0fL);
    long hashValue64 = 0x78695a4b3c2d1e0fL;
    int hashValue32 = 0x3c2d1e0f;

    assertThat(hashValue128.getAsLong()).isEqualTo(hashValue64);
    assertThat(hashValue128.getAsInt()).isEqualTo(hashValue32);
    assertThat((int) hashValue64).isEqualTo(hashValue32);

    assertThat(HashValues.toHexString(hashValue128)).isEqualTo("f0e1d2c3b4a5968778695a4b3c2d1e0f");
    assertThat(HashValues.toHexString(hashValue64)).isEqualTo("78695a4b3c2d1e0f");
    assertThat(HashValues.toHexString(hashValue32)).isEqualTo("3c2d1e0f");

    assertThat(Long.toHexString(hashValue64)).isEqualTo("78695a4b3c2d1e0f");
    assertThat(Long.toHexString(hashValue32)).isEqualTo("3c2d1e0f");

    assertThat(HashValues.toByteArray(hashValue128))
        .containsExactly(
            0x0f, 0x1e, 0x2d, 0x3c, 0x4b, 0x5a, 0x69, 0x78, 0x87, 0x96, 0xa5, 0xb4, 0xc3, 0xd2,
            0xe1, 0xf0);
    assertThat(HashValues.toByteArray(hashValue64))
        .containsExactly(0x0f, 0x1e, 0x2d, 0x3c, 0x4b, 0x5a, 0x69, 0x78);
    assertThat(HashValues.toByteArray(hashValue32)).containsExactly(0x0f, 0x1e, 0x2d, 0x3c);
  }
}
