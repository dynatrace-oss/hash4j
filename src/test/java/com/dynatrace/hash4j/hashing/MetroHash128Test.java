/*
 * Copyright 2026 Dynatrace LLC
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

class MetroHash128Test extends AbstractHasher128Test {

  @Override
  protected List<Hasher128> createHashers() {
    return Arrays.asList(Hashing.metroHash128(), Hashing.metroHash128(0x18a689a5e4dd1b9eL));
  }

  @Override
  protected String getChecksumResourceFileName() {
    return "MetroHash 128.txt";
  }

  @Override
  protected void calculateHashForChecksum(byte[] seedBytes, byte[] hashBytes, byte[] dataBytes) {
    long seed = getLong(seedBytes, 0);

    HashValue128 hash0 = Hashing.metroHash128().hashBytesTo128Bits(dataBytes);
    HashValue128 hash1 = Hashing.metroHash128(seed).hashBytesTo128Bits(dataBytes);

    setLong(hashBytes, 0, hash0.getLeastSignificantBits());
    setLong(hashBytes, 8, hash0.getMostSignificantBits());
    setLong(hashBytes, 16, hash1.getLeastSignificantBits());
    setLong(hashBytes, 24, hash1.getMostSignificantBits());
  }

  @Override
  protected void calculateHashForChecksum(
      byte[] seedBytes,
      byte[] hashBytes,
      Object o,
      long off,
      long len,
      ByteAccess<Object> byteAccess) {
    long seed = getLong(seedBytes, 0);

    HashValue128 hash0 = Hashing.metroHash128().hashBytesTo128Bits(o, off, len, byteAccess);
    HashValue128 hash1 = Hashing.metroHash128(seed).hashBytesTo128Bits(o, off, len, byteAccess);

    setLong(hashBytes, 0, hash0.getLeastSignificantBits());
    setLong(hashBytes, 8, hash0.getMostSignificantBits());
    setLong(hashBytes, 16, hash1.getLeastSignificantBits());
    setLong(hashBytes, 24, hash1.getMostSignificantBits());
  }

  @Override
  protected void calculateHashForChecksum(byte[] seedBytes, byte[] hashBytes, CharSequence c) {
    long seed = getLong(seedBytes, 0);

    HashValue128 hash0 = Hashing.metroHash128().hashCharsTo128Bits(c);
    HashValue128 hash1 = Hashing.metroHash128(seed).hashCharsTo128Bits(c);

    setLong(hashBytes, 0, hash0.getLeastSignificantBits());
    setLong(hashBytes, 8, hash0.getMostSignificantBits());
    setLong(hashBytes, 16, hash1.getLeastSignificantBits());
    setLong(hashBytes, 24, hash1.getMostSignificantBits());
  }

  @Override
  protected List<HashStream> getHashStreams(byte[] seedBytes) {
    long seed = getLong(seedBytes, 0);
    return List.of(Hashing.metroHash128().hashStream(), Hashing.metroHash128(seed).hashStream());
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
    return 32;
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
          arguments(hasher, "001FAAAAAAAAAAAAAAAABBBBBBBBBBBBBBBBCCCCCCCCCCCCCCCCDDDDDDDDDDDDDD"));
      arguments.add(
          arguments(
              hasher, "0080AAAAAAAAAAAAAAAABBBBBBBBBBBBBBBBCCCCCCCCCCCCCCCCDDDDDDDDDDDDDDDD"));
      arguments.add(
          arguments(
              hasher,
              "009FAAAAAAAAAAAAAAAABBBBBBBBBBBBBBBBCCCCCCCCCCCCCCCCDDDDDDDDDDDDDDDDEEEEEEEEEEEEEEEEFFFFFFFFFFFFFFFFAAAAAAAAAAAAAAAABBBBBBBBBBBBBB"));
    }
    return arguments.stream();
  }

  @Override
  protected Stream<Arguments> getIllegalStateCases() {
    List<Arguments> arguments = new ArrayList<>();
    for (Hasher hasher : getHashers()) {
      arguments.add(arguments(hasher, ""));
      arguments.add(arguments(hasher, "00"));
      arguments.add(arguments(hasher, "0100"));
      arguments.add(arguments(hasher, "0020"));
      arguments.add(arguments(hasher, "0040"));
      arguments.add(arguments(hasher, "0001"));
      arguments.add(arguments(hasher, "0001FFFF"));
      arguments.add(arguments(hasher, "0080"));
      arguments.add(arguments(hasher, "0080AAAAAAAAAAAAAAAA"));
      arguments.add(
          arguments(
              hasher, "0080AAAAAAAAAAAAAAAABBBBBBBBBBBBBBBBCCCCCCCCCCCCCCCCDDDDDDDDDDDDDDDDFF"));
    }
    return arguments.stream();
  }
}
