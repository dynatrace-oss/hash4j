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
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.params.provider.Arguments;

class Komihash5_0Test extends AbstractHasher64Test {

  @Override
  protected List<Hasher64> createHashers() {
    return Arrays.asList(Hashing.komihash5_0(), Hashing.komihash5_0(0x21a9c0c003cd280bL));
  }

  @Override
  protected String getChecksumResourceFileName() {
    return "Komihash 5.0.txt";
  }

  @Override
  protected void calculateHashForChecksum(byte[] seedBytes, byte[] hashBytes, byte[] dataBytes) {

    long seed = getLong(seedBytes, 0);
    long hash0 = Hashing.komihash5_0().hashBytesToLong(dataBytes);
    long hash1 = Hashing.komihash5_0(seed).hashBytesToLong(dataBytes);

    setLong(hashBytes, 0, hash0);
    setLong(hashBytes, 8, hash1);
  }

  @Override
  protected void calculateHashForChecksum(byte[] seedBytes, byte[] hashBytes, CharSequence c) {

    long seed = getLong(seedBytes, 0);
    long hash0 = Hashing.komihash5_0().hashCharsToLong(c);
    long hash1 = Hashing.komihash5_0(seed).hashCharsToLong(c);

    setLong(hashBytes, 0, hash0);
    setLong(hashBytes, 8, hash1);
  }

  @Override
  protected List<HashStream> getHashStreams(byte[] seedBytes) {
    long seed = getLong(seedBytes, 0);
    return List.of(Hashing.komihash5_0().hashStream(), Hashing.komihash5_0(seed).hashStream());
  }

  @Override
  int getSeedSizeForChecksum() {
    return 8;
  }

  @Override
  int getHashSizeForChecksum() {
    return 16;
  }

  @Override
  protected int getBlockLengthInBytes() {
    return 64;
  }

  @Override
  protected byte getLatestStreamSerialVersion() {
    return 0;
  }

  @Override
  protected Stream<Arguments> getLegalStateCases() {
    List<Arguments> arguments = new ArrayList<>();
    for (Hasher hasher : getHashers()) {
      arguments.add(arguments(hasher, "0000"));
      arguments.add(arguments(hasher, "0001FF"));
      arguments.add(
          arguments(
              hasher,
              "0080AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));
      arguments.add(
          arguments(
              hasher,
              "0081AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));
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
      arguments.add(arguments(hasher, "01FF"));
      arguments.add(arguments(hasher, "010000"));
      arguments.add(
          arguments(
              hasher,
              "0080AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));
      arguments.add(
          arguments(
              hasher,
              "0080AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));
    }
    return arguments.stream();
  }
}
