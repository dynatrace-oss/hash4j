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

class HashValue128Test {

  @Test
  void testGetAsInt() {
    HashValue128 hash = new HashValue128(0x3b373969bb1aa907L, 0xf91ca468290066d3L);
    assertThat(hash.getAsInt()).isEqualTo(0x290066d3);
  }

  @Test
  void testGetLeastSignificantBits() {
    HashValue128 hash = new HashValue128(0x3b373969bb1aa907L, 0xf91ca468290066d3L);
    assertThat(hash.getLeastSignificantBits()).isEqualTo(0xf91ca468290066d3L);
  }

  @Test
  void testGetAsLong() {
    HashValue128 hash = new HashValue128(0x3b373969bb1aa907L, 0xf91ca468290066d3L);
    assertThat(hash.getAsLong()).isEqualTo(0xf91ca468290066d3L);
  }

  @Test
  void testHashCode() {
    HashValue128 hash = new HashValue128(0x3b373969bb1aa907L, 0xf91ca468290066d3L);
    assertThat(hash.hashCode()).isEqualTo(0x290066d3);
  }

  @Test
  void testGetMostSignificantBits() {
    HashValue128 hash = new HashValue128(0x3b373969bb1aa907L, 0xf91ca468290066d3L);
    assertThat(hash.getMostSignificantBits()).isEqualTo(0x3b373969bb1aa907L);
  }

  @Test
  void testEquals() {
    HashValue128 hash1a = new HashValue128(0x3b373969bb1aa907L, 0xf91ca468290066d3L);
    HashValue128 hash1b = new HashValue128(0x3b373969bb1aa907L, 0xf91ca468290066d3L);
    HashValue128 hash2 = new HashValue128(0x587b9c8a695ef518L, 0x9f3dc05b33bfb73eL);
    HashValue128 hash3 = new HashValue128(0x3b373969bb1aa907L, 0x9f3dc05b33bfb73eL);
    HashValue128 hash4 = new HashValue128(0x587b9c8a695ef518L, 0xf91ca468290066d3L);

    assertThat(hash1a.equals(null)).isFalse();
    assertThat(hash1a.equals(hash1b)).isTrue();
    assertThat(hash1a.equals(hash1a)).isTrue();
    assertThat(hash1a.equals(hash2)).isFalse();
    assertThat(hash1a.equals(hash3)).isFalse();
    assertThat(hash1a.equals(hash4)).isFalse();
    assertThat(hash1a.equals((new Object()))).isFalse();
  }
}
