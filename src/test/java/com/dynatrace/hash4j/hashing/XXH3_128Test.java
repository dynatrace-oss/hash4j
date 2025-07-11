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

class XXH3_128Test extends AbstractHasher128Test {

  @Override
  protected List<Hasher128> createHashers() {
    return Arrays.asList(Hashing.xxh3_128(), Hashing.xxh3_128(0x84bef0228911ff91L));
  }

  @Override
  protected String getChecksumResourceFileName() {
    return "XXH3_128.txt";
  }

  @Override
  protected void calculateHashForChecksum(byte[] seedBytes, byte[] hashBytes, byte[] dataBytes) {
    long seed = getLong(seedBytes, 0);

    HashValue128 hash0 = Hashing.xxh3_128().hashBytesTo128Bits(dataBytes);
    HashValue128 hash1 = Hashing.xxh3_128(seed).hashBytesTo128Bits(dataBytes);

    setLong(hashBytes, 0, hash0.getLeastSignificantBits());
    setLong(hashBytes, 8, hash0.getMostSignificantBits());
    setLong(hashBytes, 16, hash1.getLeastSignificantBits());
    setLong(hashBytes, 24, hash1.getMostSignificantBits());
  }

  @Override
  protected void calculateHashForChecksum(byte[] seedBytes, byte[] hashBytes, CharSequence c) {
    long seed = getLong(seedBytes, 0);

    HashValue128 hash0 = Hashing.xxh3_128().hashCharsTo128Bits(c);
    HashValue128 hash1 = Hashing.xxh3_128(seed).hashCharsTo128Bits(c);

    setLong(hashBytes, 0, hash0.getLeastSignificantBits());
    setLong(hashBytes, 8, hash0.getMostSignificantBits());
    setLong(hashBytes, 16, hash1.getLeastSignificantBits());
    setLong(hashBytes, 24, hash1.getMostSignificantBits());
  }

  @Override
  protected List<HashStream> getHashStreams(byte[] seedBytes) {
    long seed = getLong(seedBytes, 0);
    return List.of(Hashing.xxh3_128().hashStream(), Hashing.xxh3_128(seed).hashStream());
  }

  @Override
  int getSeedSizeForChecksum() {
    return 8;
  }

  @Override
  int getHashSizeForChecksum() {
    return 32;
  }

  @Override
  protected int getBlockLengthInBytes() {
    return 1024;
  }

  @Override
  protected byte getLatestStreamSerialVersion() {
    return 0;
  }

  @Override
  protected Stream<Arguments> getLegalStateCases() {
    List<Arguments> arguments = new ArrayList<>();
    for (Hasher hasher : getHashers()) {
      arguments.add(arguments(hasher, "000000000000000000"));
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
      arguments.add(arguments(hasher, "00000000000000000000"));
    }
    return arguments.stream();
  }
}
