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

import static com.dynatrace.hash4j.internal.ByteArrayUtil.getLong;
import static com.dynatrace.hash4j.internal.ByteArrayUtil.setLong;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

class FarmHashUoTest extends AbstractHasher64Test {

  private static final List<Hasher64> HASHERS =
      Arrays.asList(
          Hashing.farmHashUo(),
          Hashing.farmHashUo(0x1b5af6b8376953d2L),
          Hashing.farmHashUo(0x1b0fd3d0cecf44bbL, 0x1d195f7db40c4a96L));

  @Override
  protected List<Hasher64> getHashers() {
    return HASHERS;
  }

  @Override
  protected String getChecksumResourceFileName() {
    return "FarmHash UO.txt";
  }

  @Override
  protected void calculateHashForChecksum(byte[] seedBytes, byte[] hashBytes, byte[] dataBytes) {
    long seed = getLong(seedBytes, 0);
    long seed0 = getLong(seedBytes, 8);
    long seed1 = getLong(seedBytes, 16);

    long hash0 = Hashing.farmHashUo().hashBytesToLong(dataBytes);
    long hash1 = Hashing.farmHashUo(seed).hashBytesToLong(dataBytes);
    long hash2 = Hashing.farmHashUo(seed0, seed1).hashBytesToLong(dataBytes);

    setLong(hashBytes, 0, hash0);
    setLong(hashBytes, 8, hash1);
    setLong(hashBytes, 16, hash2);
  }

  @Override
  protected void calculateHashForChecksum(byte[] seedBytes, byte[] hashBytes, CharSequence c) {
    long seed = getLong(seedBytes, 0);
    long seed0 = getLong(seedBytes, 8);
    long seed1 = getLong(seedBytes, 16);

    long hash0 = Hashing.farmHashUo().hashCharsToLong(c);
    long hash1 = Hashing.farmHashUo(seed).hashCharsToLong(c);
    long hash2 = Hashing.farmHashUo(seed0, seed1).hashCharsToLong(c);

    setLong(hashBytes, 0, hash0);
    setLong(hashBytes, 8, hash1);
    setLong(hashBytes, 16, hash2);
  }

  @Override
  protected List<HashStream> getHashStreams(byte[] seedBytes) {
    long seed = getLong(seedBytes, 0);
    long seed0 = getLong(seedBytes, 8);
    long seed1 = getLong(seedBytes, 16);

    return List.of(
        Hashing.farmHashUo().hashStream(),
        Hashing.farmHashUo(seed).hashStream(),
        Hashing.farmHashUo(seed0, seed1).hashStream());
  }

  @Override
  int getSeedSizeForChecksum() {
    return 24;
  }

  @Override
  int getHashSizeForChecksum() {
    return 24;
  }

  @Override
  protected int getBlockLengthInBytes() {
    return 64;
  }

  private static byte[] B0A = {
    -39, -52, -97, 103, 44, -99, -51, 76, 46, 125, -67, 95, 30, 70, -78, -128, 72, 66, -110, -112,
    28, -109, 38, 118, 118, 102, 110, -61, -4, 8, -90, 46, -36, -53, -17, -97, -72, 113, -128, -3,
    -117, -114, -50, 113, -118, -5, -52, 25, -118, -70, -82, 62, -122, -99, 9, 1, 30, -123, 127,
    -108, 75, 78, 106, 116, -65, 76, -121, 17, -101, 125, -38, -48
  };
  private static byte[] B0B = {
    39, 52, 97, -103, -44, 99, 51, -76, 46, 125, -67, 95, 30, 70, -78, -128, 72, 66, -110, -112, 28,
    -109, 38, 118, 118, 102, 110, -61, -4, 8, -90, 46, -36, -53, -17, -97, -72, 113, -128, -3, -117,
    -114, -50, 113, -118, -5, -52, 25, -118, -70, -82, 62, -122, -99, 9, 1, 30, -123, 127, -108, 75,
    78, 106, 116, -65, 76, -121, 17, -101, 125, -38, -48
  };
  private static byte[] B1 = {
    -39, -52, -97, 103, 44, -99, -51, 76, -46, 125, -67, 95, 30, 70, -78, -128, 72, 66, -110, -112,
    28, -109, 38, 118, 118, 102, 110, -61, -4, 8, -90, 46, -36, -53, -17, -97, -72, 113, -128, -3,
    -117, -114, -50, 113, -118, -5, -52, 25, -118, -70, -82, 62, -122, -99, 9, 1, 30, -123, 127,
    -108, 75, 78, 106, 116, -65, 76, -121, 17, -101, 125, -38, -48
  };

  @Test
  void testEqualsHelper() {
    assertThat(
            FarmHashUo.equalsHelper(
                0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, false, false, 0, 0,
                B0A, B0B))
        .isTrue();
    assertThat(
            FarmHashUo.equalsHelper(
                1L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, false, false, 0, 0,
                B0A, B0B))
        .isFalse();
    assertThat(
            FarmHashUo.equalsHelper(
                0L, 0L, 1L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, false, false, 0, 0,
                B0A, B0B))
        .isFalse();
    assertThat(
            FarmHashUo.equalsHelper(
                0L, 0L, 0L, 0L, 1L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, false, false, 0, 0,
                B0A, B0B))
        .isFalse();
    assertThat(
            FarmHashUo.equalsHelper(
                0L, 0L, 0L, 0L, 0L, 0L, 1L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, false, false, 0, 0,
                B0A, B0B))
        .isFalse();
    assertThat(
            FarmHashUo.equalsHelper(
                0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 1L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, false, false, 0, 0,
                B0A, B0B))
        .isFalse();
    assertThat(
            FarmHashUo.equalsHelper(
                0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 1L, 0L, 0L, 0L, 0L, 0L, false, false, 0, 0,
                B0A, B0B))
        .isFalse();
    assertThat(
            FarmHashUo.equalsHelper(
                0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 1L, 0L, 0L, 0L, false, false, 0, 0,
                B0A, B0B))
        .isFalse();
    assertThat(
            FarmHashUo.equalsHelper(
                0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 1L, 0L, false, false, 0, 0,
                B0A, B0B))
        .isFalse();
    assertThat(
            FarmHashUo.equalsHelper(
                0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, true, false, 0, 0,
                B0A, B0B))
        .isFalse();
    assertThat(
            FarmHashUo.equalsHelper(
                0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, false, false, 1, 0,
                B0A, B0B))
        .isFalse();
    assertThat(
            FarmHashUo.equalsHelper(
                0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, false, false, 1, 1,
                B0A, B1))
        .isFalse();
  }
}
