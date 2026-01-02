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

import static com.dynatrace.hash4j.internal.ByteArrayUtil.getLong;
import static com.dynatrace.hash4j.internal.ByteArrayUtil.setLong;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.provider.Arguments;

class PolymurHash2_0Test extends AbstractHasher64Test {

  @Override
  protected List<Hasher64> createHashers() {
    return Arrays.asList(
        Hashing.polymurHash2_0(0xe5dbf194e07d973eL, 0x8d7c20f9caa22623L),
        Hashing.polymurHash2_0(0xb0bb8ad791e9abaaL, 0x9c12700e86b09cfdL, 0xcea48e71394caa79L));
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
  protected void calculateHashForChecksum(
      byte[] seedBytes,
      byte[] hashBytes,
      Object o,
      long off,
      long len,
      ByteAccess<Object> byteAccess) {
    long tweak = getLong(seedBytes, 0);
    long seed0 = getLong(seedBytes, 8);
    long seed1 = getLong(seedBytes, 16);

    long hash0 = Hashing.polymurHash2_0(tweak, seed0).hashBytesToLong(o, off, len, byteAccess);
    long hash1 =
        Hashing.polymurHash2_0(tweak, seed0, seed1).hashBytesToLong(o, off, len, byteAccess);

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

  @Override
  protected byte getLatestStreamSerialVersion() {
    return 0;
  }

  @Test
  void testPolymurPow37() {
    assertThat(PolymurHash2_0.calculatePolymurPow37())
        .isEqualTo(PolymurHash2_0.calculatePolymurPow37Reference());
  }

  @Test
  void testEqualsAdditionalCases() {
    assertThat(PolymurHash2_0.create(0, 0, 0)).isNotEqualTo(PolymurHash2_0.create(1, 0, 0));
    assertThat(PolymurHash2_0.create(0, 0, 0)).isNotEqualTo(PolymurHash2_0.create(0, 1, 0));
    assertThat(PolymurHash2_0.create(0, 0, 0)).isNotEqualTo(PolymurHash2_0.create(0, 0, 1));
  }

  @Override
  protected Stream<Arguments> getLegalStateCases() {
    List<Arguments> arguments = new ArrayList<>();
    for (Hasher hasher : getHashers()) {
      arguments.add(arguments(hasher, "0081000000000000000000"));
    }
    return arguments.stream();
  }

  @Override
  protected Stream<Arguments> getIllegalStateCases() {
    List<Arguments> arguments = new ArrayList<>();
    for (Hasher hasher : getHashers()) {
      arguments.add(arguments(hasher, ""));
      arguments.add(arguments(hasher, "00"));
      arguments.add(arguments(hasher, "01"));
      arguments.add(arguments(hasher, "0080"));
      arguments.add(arguments(hasher, "003F"));
      arguments.add(arguments(hasher, "0081"));
      arguments.add(arguments(hasher, "00810000000000000000"));
      arguments.add(arguments(hasher, "008100000000000000000000"));
      arguments.add(arguments(hasher, "0001"));
      arguments.add(arguments(hasher, "00010000"));
    }
    return arguments.stream();
  }
}
