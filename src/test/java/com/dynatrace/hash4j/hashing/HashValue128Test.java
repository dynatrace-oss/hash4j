/*
 * Copyright 2022-2026 Dynatrace LLC
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
    assertThat(hash1a.equals(new Object())).isFalse();
  }

  @Test
  void testToString() {
    assertThat(new HashValue128(0x0001020304050607L, 0x08090a0b0c0d0e0fL))
        .hasToString("0x000102030405060708090a0b0c0d0e0f");
    assertThat(new HashValue128(0x3b373969bb1aa907L, 0xf91ca468290066d3L))
        .hasToString("0x3b373969bb1aa907f91ca468290066d3");
  }

  @Test
  void testToByteArray() {
    assertThat(new HashValue128(0x0011223344556677L, 0x8899aabbccddeeffL).toByteArray())
        .isEqualTo(
            new byte[] {
              (byte) 0xff,
              (byte) 0xee,
              (byte) 0xdd,
              (byte) 0xcc,
              (byte) 0xbb,
              (byte) 0xaa,
              (byte) 0x99,
              (byte) 0x88,
              (byte) 0x77,
              (byte) 0x66,
              (byte) 0x55,
              (byte) 0x44,
              (byte) 0x33,
              (byte) 0x22,
              (byte) 0x11,
              (byte) 0x00
            });
    assertThat(new HashValue128(0xaf6cbc3f12e21e38L, 0x2d5c760d5dd733c4L).toByteArray())
        .isEqualTo(
            new byte[] {
              (byte) 0xc4,
              (byte) 0x33,
              (byte) 0xd7,
              (byte) 0x5d,
              (byte) 0x0d,
              (byte) 0x76,
              (byte) 0x5c,
              (byte) 0x2d,
              (byte) 0x38,
              (byte) 0x1e,
              (byte) 0xe2,
              (byte) 0x12,
              (byte) 0x3f,
              (byte) 0xbc,
              (byte) 0x6c,
              (byte) 0xaf
            });
  }
}
