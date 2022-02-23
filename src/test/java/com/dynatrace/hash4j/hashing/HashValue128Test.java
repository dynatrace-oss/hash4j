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
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.Test;

public class HashValue128Test {

  @Test
  public void testGetAsInt() {
    HashValue128 hash = new HashValue128(0x3b373969bb1aa907L, 0xf91ca468290066d3L);
    assertEquals(0x290066d3, hash.getAsInt());
  }

  @Test
  public void testGetLeastSignificantBits() {
    HashValue128 hash = new HashValue128(0x3b373969bb1aa907L, 0xf91ca468290066d3L);
    assertEquals(0xf91ca468290066d3L, hash.getLeastSignificantBits());
  }

  @Test
  public void testGetAsLong() {
    HashValue128 hash = new HashValue128(0x3b373969bb1aa907L, 0xf91ca468290066d3L);
    assertEquals(0xf91ca468290066d3L, hash.getAsLong());
  }

  @Test
  public void testHashCode() {
    HashValue128 hash = new HashValue128(0x3b373969bb1aa907L, 0xf91ca468290066d3L);
    assertEquals(0x290066d3, hash.hashCode());
  }

  @Test
  public void testGetMostSignificantBits() {
    HashValue128 hash = new HashValue128(0x3b373969bb1aa907L, 0xf91ca468290066d3L);
    assertEquals(0x3b373969bb1aa907L, hash.getMostSignificantBits());
  }

  @Test
  public void testEquals() {
    HashValue128 hash1a = new HashValue128(0x3b373969bb1aa907L, 0xf91ca468290066d3L);
    HashValue128 hash1b = new HashValue128(0x3b373969bb1aa907L, 0xf91ca468290066d3L);
    HashValue128 hash2 = new HashValue128(0x587b9c8a695ef518L, 0x9f3dc05b33bfb73eL);
    HashValue128 hash3 = new HashValue128(0x3b373969bb1aa907L, 0x9f3dc05b33bfb73eL);
    HashValue128 hash4 = new HashValue128(0x587b9c8a695ef518L, 0xf91ca468290066d3L);

    assertNotEquals(hash1a, null);
    assertEquals(hash1a, hash1b);
    assertEquals(hash1a, hash1a);
    assertNotEquals(hash1a, hash2);
    assertNotEquals(hash1a, hash3);
    assertNotEquals(hash1a, hash4);
    assertNotEquals(hash1a, new Object());
  }
}
