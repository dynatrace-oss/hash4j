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
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.provider.Arguments;

class FarmHashUoTest extends AbstractHasher64Test {

  @Override
  protected List<Hasher64> createHashers() {
    return Arrays.asList(
        Hashing.farmHashUo(),
        Hashing.farmHashUo(0x1b5af6b8376953d2L),
        Hashing.farmHashUo(0x1b0fd3d0cecf44bbL, 0x1d195f7db40c4a96L));
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

  @Override
  protected byte getLatestStreamSerialVersion() {
    return 0;
  }

  @Test
  void testEqualsAdditionalCases() {
    assertThat(FarmHashUo.create(0)).isNotEqualTo(FarmHashUo.create(1));
    assertThat(FarmHashUo.create(0, 0)).isNotEqualTo(FarmHashUo.create(1, 0));
    assertThat(FarmHashUo.create(0, 0)).isNotEqualTo(FarmHashUo.create(0, 1));
  }

  @Override
  protected Stream<Arguments> getLegalStateCases() {
    List<Arguments> arguments = new ArrayList<>();
    for (Hasher hasher : getHashers()) {
      arguments.add(arguments(hasher, "0080"));
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
      arguments.add(arguments(hasher, "007f"));
      arguments.add(arguments(hasher, "0000"));
    }
    return arguments.stream();
  }
}
