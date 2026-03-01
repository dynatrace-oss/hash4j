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

import static com.dynatrace.hash4j.internal.ByteArrayUtil.getInt;
import static com.dynatrace.hash4j.internal.ByteArrayUtil.setInt;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.params.provider.Arguments;

class XXH32Test extends AbstractHasher32Test {

  @Override
  protected List<Hasher32> createHashers() {
    return Arrays.asList(Hashing.xxh32(), Hashing.xxh32(0x43a3fb15));
  }

  @Override
  protected String getChecksumResourceFileName() {
    return "XXH32.txt";
  }

  @Override
  protected void calculateHashForChecksum(byte[] seedBytes, byte[] hashBytes, byte[] dataBytes) {
    int seed = getInt(seedBytes, 0);
    int hash0 = Hashing.xxh32().hashBytesToInt(dataBytes);
    int hash1 = Hashing.xxh32(seed).hashBytesToInt(dataBytes);
    setInt(hashBytes, 0, hash0);
    setInt(hashBytes, 4, hash1);
  }

  @Override
  protected void calculateHashForChecksum(
      byte[] seedBytes,
      byte[] hashBytes,
      Object o,
      long off,
      long len,
      ByteAccess<Object> byteAccess) {
    int seed = getInt(seedBytes, 0);
    int hash0 = Hashing.xxh32().hashBytesToInt(o, off, len, byteAccess);
    int hash1 = Hashing.xxh32(seed).hashBytesToInt(o, off, len, byteAccess);
    setInt(hashBytes, 0, hash0);
    setInt(hashBytes, 4, hash1);
  }

  @Override
  protected void calculateHashForChecksum(byte[] seedBytes, byte[] hashBytes, CharSequence c) {
    int seed = getInt(seedBytes, 0);
    int hash0 = Hashing.xxh32().hashCharsToInt(c);
    int hash1 = Hashing.xxh32(seed).hashCharsToInt(c);
    setInt(hashBytes, 0, hash0);
    setInt(hashBytes, 4, hash1);
  }

  @Override
  protected List<HashStream> getHashStreams(byte[] seedBytes) {
    int seed = getInt(seedBytes, 0);
    return List.of(Hashing.xxh32().hashStream(), Hashing.xxh32(seed).hashStream());
  }

  @Override
  int getSeedSizeForChecksum() {
    return 4;
  }

  @Override
  int getHashSizeForChecksum() {
    return 8;
  }

  @Override
  protected int getBlockLengthInBytes() {
    return 16;
  }

  @Override
  protected byte getLatestStreamSerialVersion() {
    return 0;
  }

  @Override
  protected Stream<Arguments> getLegalStateCases() {
    List<Arguments> arguments = new ArrayList<>();
    for (Hasher hasher : getHashers()) {
      // offset=0, no large flag, totalLen=0: 6 bytes
      arguments.add(arguments(hasher, "000000000000"));
      // offset=1, no large flag, totalLen=1: 6+1=7 bytes
      arguments.add(arguments(hasher, "000101000000FF"));
      // offset=0x0F, large flag, totalLen=0x1F: 6+16+15=37 bytes
      arguments.add(
          arguments(
              hasher,
              "008F1F000000AAAAAAAABBBBBBBBCCCCCCCCDDDDDDDDEEEEEEEEEEEEEEEEEEEEEEEEEEEEEE"));
    }
    return arguments.stream();
  }

  @Override
  protected Stream<Arguments> getIllegalStateCases() {
    List<Arguments> arguments = new ArrayList<>();
    for (Hasher hasher : getHashers()) {
      arguments.add(arguments(hasher, ""));
      arguments.add(arguments(hasher, "00"));
      // wrong version
      arguments.add(arguments(hasher, "010000000000"));
      // reserved bits set
      arguments.add(arguments(hasher, "001000000000"));
      // offset=1 but no buffer byte provided
      arguments.add(arguments(hasher, "000100000000"));
      // large flag set but no acc bytes
      arguments.add(arguments(hasher, "008000000000"));
    }
    return arguments.stream();
  }
}
