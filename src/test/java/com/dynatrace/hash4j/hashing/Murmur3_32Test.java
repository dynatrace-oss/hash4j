/*
 * Copyright 2022-2024 Dynatrace LLC
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

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.LongStream;
import org.junit.jupiter.api.Test;

class Murmur3_32Test extends AbstractHasher32Test {

  private static final List<Hasher32> HASHERS =
      Arrays.asList(Hashing.murmur3_32(), Hashing.murmur3_32(0x43a3fb15));

  @Override
  protected List<Hasher32> getHashers() {
    return HASHERS;
  }

  @Override
  protected String getChecksumResourceFileName() {
    return "Murmur3 32.txt";
  }

  @Override
  protected void calculateHashForChecksum(byte[] seedBytes, byte[] hashBytes, byte[] dataBytes) {
    int seed = (int) INT_HANDLE.get(seedBytes, 0);
    int hash0 = Hashing.murmur3_32().hashBytesToInt(dataBytes);
    int hash1 = Hashing.murmur3_32(seed).hashBytesToInt(dataBytes);
    INT_HANDLE.set(hashBytes, 0, hash0);
    INT_HANDLE.set(hashBytes, 4, hash1);
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
  protected List<ReferenceTestRecord32> getReferenceTestRecords() {
    List<ReferenceTestRecord32> referenceTestRecords = new ArrayList<>();
    for (Murmur3_32ReferenceData.ReferenceRecord r : Murmur3_32ReferenceData.get()) {
      referenceTestRecords.add(
          new ReferenceTestRecord32(Hashing.murmur3_32(), r.getData(), r.getHash0()));
      referenceTestRecords.add(
          new ReferenceTestRecord32(Hashing.murmur3_32(r.getSeed()), r.getData(), r.getHash1()));
    }
    return referenceTestRecords;
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
    HashStream32 stream = Hashing.murmur3_32().hashStream();
    LongStream.range(0, len).forEach(i -> stream.putByte((byte) (i & 0xFF)));
    assertThat(stream.getAsInt()).isEqualTo(0x0038818d);
  }
}
