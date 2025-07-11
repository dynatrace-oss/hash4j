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

import static com.dynatrace.hash4j.internal.ByteArrayUtil.getInt;
import static com.dynatrace.hash4j.internal.ByteArrayUtil.setLong;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import com.dynatrace.hash4j.testutils.TestUtils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.LongStream;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.provider.Arguments;

class Murmur3_128Test extends AbstractHasher128Test {

  @Override
  protected List<Hasher128> createHashers() {
    return Arrays.asList(Hashing.murmur3_128(), Hashing.murmur3_128(0xfc64a346));
  }

  @Override
  protected String getChecksumResourceFileName() {
    return "Murmur3 128.txt";
  }

  @Override
  protected void calculateHashForChecksum(byte[] seedBytes, byte[] hashBytes, byte[] dataBytes) {
    int seed = getInt(seedBytes, 0);

    HashValue128 hash0 = Hashing.murmur3_128().hashBytesTo128Bits(dataBytes);
    HashValue128 hash1 = Hashing.murmur3_128(seed).hashBytesTo128Bits(dataBytes);

    setLong(hashBytes, 0, hash0.getLeastSignificantBits());
    setLong(hashBytes, 8, hash0.getMostSignificantBits());
    setLong(hashBytes, 16, hash1.getLeastSignificantBits());
    setLong(hashBytes, 24, hash1.getMostSignificantBits());
  }

  @Override
  protected void calculateHashForChecksum(byte[] seedBytes, byte[] hashBytes, CharSequence c) {
    int seed = getInt(seedBytes, 0);

    HashValue128 hash0 = Hashing.murmur3_128().hashCharsTo128Bits(c);
    HashValue128 hash1 = Hashing.murmur3_128(seed).hashCharsTo128Bits(c);

    setLong(hashBytes, 0, hash0.getLeastSignificantBits());
    setLong(hashBytes, 8, hash0.getMostSignificantBits());
    setLong(hashBytes, 16, hash1.getLeastSignificantBits());
    setLong(hashBytes, 24, hash1.getMostSignificantBits());
  }

  @Override
  protected List<HashStream> getHashStreams(byte[] seedBytes) {
    int seed = getInt(seedBytes, 0);
    return List.of(Hashing.murmur3_128().hashStream(), Hashing.murmur3_128(seed).hashStream());
  }

  @Override
  int getSeedSizeForChecksum() {
    return 4;
  }

  @Override
  int getHashSizeForChecksum() {
    return 32;
  }

  @Override
  protected int getBlockLengthInBytes() {
    return 16;
  }

  @Override
  protected byte getLatestStreamSerialVersion() {
    return 0;
  }

  /**
   * The C reference implementation does not define the hash value computation of byte sequences
   * longer than {@link Integer#MAX_VALUE} as it uses a native unsigned {@code int} for the length
   * of the byte array. This test verifies that a predefined byte sequence longer than {@link
   * Integer#MAX_VALUE} always results in the same hash value.
   */
  @Test
  void testLongInput() {
    long len = 1L + Integer.MAX_VALUE;
    HashStream128 stream = Hashing.murmur3_128().hashStream();
    LongStream.range(0, len).forEach(i -> stream.putByte((byte) (i & 0xFF)));
    byte[] hashValueBytes = stream.get().toByteArray();
    byte[] expected = TestUtils.hexStringToByteArray("4b32a2e0240ee13e2b5a84668f916ce2");
    assertThat(hashValueBytes).isEqualTo(expected);
  }

  @Override
  protected Stream<Arguments> getLegalStateCases() {
    List<Arguments> arguments = new ArrayList<>();
    for (Hasher hasher : getHashers()) {
      arguments.add(arguments(hasher, "000000000000000000"));
      arguments.add(arguments(hasher, "000100000000000000AA"));
      arguments.add(arguments(hasher, "001000000000000000AAAAAAAAAAAAAAAABBBBBBBBBBBBBBBB"));
      arguments.add(arguments(hasher, "00F0FFFFFFFFFFFFFF11111111111111111111111111111111"));
      arguments.add(arguments(hasher, "00F1FFFFFFFFFFFFFF1111111111111111111111111111111122"));
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
      arguments.add(arguments(hasher, "0000000000000000"));
      arguments.add(arguments(hasher, "000000000000000000AA"));
      arguments.add(arguments(hasher, "000100000000000000"));
      arguments.add(arguments(hasher, "000100000000000000AABB"));
      arguments.add(arguments(hasher, "001000000000000000AAAAAAAAAAAAAAAABBBBBBBBBBBBBB"));
      arguments.add(arguments(hasher, "001000000000000000AAAAAAAAAAAAAAAABBBBBBBBBBBBBBBBCC"));
      arguments.add(arguments(hasher, "00F0FFFFFFFFFFFFFF111111111111111111111111111111"));
      arguments.add(arguments(hasher, "00F0FFFFFFFFFFFFFF1111111111111111111111111111111133"));
      arguments.add(arguments(hasher, "00F1FFFFFFFFFFFFFF11111111111111111111111111111111"));
      arguments.add(arguments(hasher, "00F1FFFFFFFFFFFFFF111111111111111111111111111111112233"));
    }
    return arguments.stream();
  }
}
