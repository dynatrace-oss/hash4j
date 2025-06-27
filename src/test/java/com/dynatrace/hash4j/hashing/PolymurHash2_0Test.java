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

import static com.dynatrace.hash4j.hashing.PolymurHash2_0.equalsHelper;
import static com.dynatrace.hash4j.internal.ByteArrayUtil.getLong;
import static com.dynatrace.hash4j.internal.ByteArrayUtil.setLong;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

class PolymurHash2_0Test extends AbstractHasher64Test {

  private static final List<Hasher64> HASHERS =
      Arrays.asList(
          Hashing.polymurHash2_0(0xe5dbf194e07d973eL, 0x8d7c20f9caa22623L),
          Hashing.polymurHash2_0(0xb0bb8ad791e9abaaL, 0x9c12700e86b09cfdL, 0xcea48e71394caa79L));

  @Override
  protected List<Hasher64> getHashers() {
    return HASHERS;
  }

  @Override
  protected String getChecksumResourceFileName() {
    return "PolymurHash 2.0.txt";
  }

  @Override
  protected void calculateHashForChecksum(byte[] seedBytes, byte[] hashBytes, byte[] dataBytes) {
    long tweak = getLong(seedBytes, 0);
    long seed0 = getLong(seedBytes, 8);
    long seed1 = getLong(seedBytes, 16);

    long hash0 = Hashing.polymurHash2_0(tweak, seed0).hashBytesToLong(dataBytes);
    long hash1 = Hashing.polymurHash2_0(tweak, seed0, seed1).hashBytesToLong(dataBytes);

    setLong(hashBytes, 0, hash0);
    setLong(hashBytes, 8, hash1);
  }

  @Override
  protected void calculateHashForChecksum(byte[] seedBytes, byte[] hashBytes, CharSequence c) {
    long tweak = getLong(seedBytes, 0);
    long seed0 = getLong(seedBytes, 8);
    long seed1 = getLong(seedBytes, 16);

    long hash0 = Hashing.polymurHash2_0(tweak, seed0).hashCharsToLong(c);
    long hash1 = Hashing.polymurHash2_0(tweak, seed0, seed1).hashCharsToLong(c);

    setLong(hashBytes, 0, hash0);
    setLong(hashBytes, 8, hash1);
  }

  @Override
  protected List<HashStream> getHashStreams(byte[] seedBytes) {
    long tweak = getLong(seedBytes, 0);
    long seed0 = getLong(seedBytes, 8);
    long seed1 = getLong(seedBytes, 16);
    return List.of(
        Hashing.polymurHash2_0(tweak, seed0).hashStream(),
        Hashing.polymurHash2_0(tweak, seed0, seed1).hashStream());
  }

  @Override
  int getSeedSizeForChecksum() {
    return 24;
  }

  @Override
  int getHashSizeForChecksum() {
    return 16;
  }

  @Override
  protected int getBlockLengthInBytes() {
    return 49;
  }

  @Test
  void testPolymurPow37() {
    assertThat(PolymurHash2_0.calculatePolymurPow37())
        .isEqualTo(PolymurHash2_0.calculatePolymurPow37Reference());
  }

  private static byte[] B0 = {0};
  private static byte[] B1 = {1};

  @Test
  void testEqualsHelper() {
    assertThat(equalsHelper(0L, 0L, 0, 0, 0L, 0L, B0, B0)).isTrue();
    assertThat(equalsHelper(1L, 0L, 0, 0, 0L, 0L, B0, B0)).isFalse();
    assertThat(equalsHelper(0L, 0L, 1, 0, 0L, 0L, B0, B0)).isFalse();
    assertThat(equalsHelper(0L, 0L, 0, 0, 1L, 0L, B0, B0)).isFalse();
    assertThat(equalsHelper(0L, 0L, 1, 1, 0L, 0L, B1, B0)).isFalse();
  }
}
