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

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import java.util.ArrayList;
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
    long tweak = (long) LONG_HANDLE.get(seedBytes, 0);
    long seed0 = (long) LONG_HANDLE.get(seedBytes, 8);
    long seed1 = (long) LONG_HANDLE.get(seedBytes, 16);

    long hash0 = PolymurHash2_0.create(tweak, seed0).hashBytesToLong(dataBytes);
    long hash1 = PolymurHash2_0.create(tweak, seed0, seed1).hashBytesToLong(dataBytes);

    LONG_HANDLE.set(hashBytes, 0, hash0);
    LONG_HANDLE.set(hashBytes, 8, hash1);
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
  protected List<ReferenceTestRecord64> getReferenceTestRecords() {
    List<ReferenceTestRecord64> referenceTestRecords = new ArrayList<>();
    for (PolymurHash2_0ReferenceData.ReferenceRecord r : PolymurHash2_0ReferenceData.get()) {
      referenceTestRecords.add(
          new ReferenceTestRecord64(
              Hashing.polymurHash2_0(r.getTweak(), r.getSeed0()), r.getData(), r.getHash0()));
      referenceTestRecords.add(
          new ReferenceTestRecord64(
              Hashing.polymurHash2_0(r.getTweak(), r.getSeed0(), r.getSeed1()),
              r.getData(),
              r.getHash1()));
    }
    return referenceTestRecords;
  }

  @Test
  void testPolymurPow37() {
    assertThat(PolymurHash2_0.calculatePolymurPow37())
        .isEqualTo(PolymurHash2_0.calculatePolymurPow37Reference());
  }
}
