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

import static com.dynatrace.hash4j.testutils.TestUtils.hash128ToByteArray;
import static org.assertj.core.api.Assertions.assertThat;

import com.dynatrace.hash4j.testutils.TestUtils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.LongStream;
import org.junit.jupiter.api.Test;

class Murmur3_128Test extends AbstractHasher128Test {

  private static final List<Hasher128> HASHERS =
      Arrays.asList(Hashing.murmur3_128(), Hashing.murmur3_128(0xfc64a346));

  @Override
  protected List<Hasher128> getHashers() {
    return HASHERS;
  }

  @Override
  protected void calculateHashForChecksum(byte[] seedBytes, byte[] hashBytes, byte[] dataBytes) {
    int seed = (int) INT_HANDLE.get(seedBytes, 0);

    HashValue128 hash0 = Hashing.murmur3_128().hashBytesTo128Bits(dataBytes);
    HashValue128 hash1 = Hashing.murmur3_128(seed).hashBytesTo128Bits(dataBytes);

    LONG_HANDLE.set(hashBytes, 0, hash0.getLeastSignificantBits());
    LONG_HANDLE.set(hashBytes, 8, hash0.getMostSignificantBits());
    LONG_HANDLE.set(hashBytes, 16, hash1.getLeastSignificantBits());
    LONG_HANDLE.set(hashBytes, 24, hash1.getMostSignificantBits());
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
  String getExpectedChecksum() {
    return "483c1ed6d0936ba877b9e9e063b27774b6b1f481ae6fae439e19120619cc2d1a";
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
    byte[] hashValueBytes = hash128ToByteArray(stream.get());
    byte[] expected = TestUtils.hexStringToByteArray("4b32a2e0240ee13e2b5a84668f916ce2");
    assertThat(hashValueBytes).isEqualTo(expected);
  }

  @Override
  protected List<ReferenceTestRecord128> getReferenceTestRecords() {
    List<ReferenceTestRecord128> referenceTestRecords = new ArrayList<>();
    for (Murmur3_128ReferenceData.ReferenceRecord r : Murmur3_128ReferenceData.get()) {
      referenceTestRecords.add(
          new ReferenceTestRecord128(Hashing.murmur3_128(), r.getData(), r.getHash0()));
      referenceTestRecords.add(
          new ReferenceTestRecord128(Hashing.murmur3_128(r.getSeed()), r.getData(), r.getHash1()));
    }
    return referenceTestRecords;
  }
}
