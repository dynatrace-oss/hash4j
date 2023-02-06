/*
 * Copyright 2022-2023 Dynatrace LLC
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

import com.dynatrace.hash4j.hashing.WyhashFinal4ReferenceData.ReferenceRecord;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

class WyhashFinal4Test extends AbstractHasher64Test {

  private static final List<Hasher64> HASHERS =
      Arrays.asList(
          Hashing.wyhashFinal4(),
          Hashing.wyhashFinal4(0xdfd1434b2173588fL),
          Hashing.wyhashFinal4(0xfa681c2ee9f17f88L, 0x3c88abf5128e96cbL));

  @Override
  protected List<Hasher64> getHashers() {
    return HASHERS;
  }

  @Override
  protected void calculateHashForChecksum(byte[] seedBytes, byte[] hashBytes, byte[] dataBytes) {
    long seed0 = (long) LONG_HANDLE.get(seedBytes, 0);
    long seed1 = (long) LONG_HANDLE.get(seedBytes, 8);
    long rand = (long) LONG_HANDLE.get(seedBytes, 16);

    long hash0 = WyhashFinal4.create().hashBytesToLong(dataBytes);
    long hash1 = WyhashFinal4.create(seed0).hashBytesToLong(dataBytes);
    long hash2 = 0;
    long hash3 = 0;
    if ((rand & 0x3fL) == 0) {
      hash2 = WyhashFinal4.create(0L, seed1).hashBytesToLong(dataBytes);
      hash3 = WyhashFinal4.create(seed0, seed1).hashBytesToLong(dataBytes);
    }

    LONG_HANDLE.set(hashBytes, 0, hash0);
    LONG_HANDLE.set(hashBytes, 8, hash1);
    LONG_HANDLE.set(hashBytes, 16, hash2);
    LONG_HANDLE.set(hashBytes, 24, hash3);
  }

  @Override
  int getSeedSizeForChecksum() {
    return 24;
  }

  @Override
  int getHashSizeForChecksum() {
    return 32;
  }

  @Override
  String getExpectedChecksum() {
    return "aa315273c7f0e1ba2aa9cdfbbeaf35a4c33d0a29ee5e58895c42bf76b01270a9";
  }

  @Override
  protected List<ReferenceTestRecord64> getReferenceTestRecords() {
    List<ReferenceTestRecord64> referenceTestRecords = new ArrayList<>();
    for (ReferenceRecord r : WyhashFinal4ReferenceData.get()) {
      referenceTestRecords.add(
          new ReferenceTestRecord64(Hashing.wyhashFinal4(), r.getData(), r.getHash0()));
      referenceTestRecords.add(
          new ReferenceTestRecord64(Hashing.wyhashFinal4(r.getSeed0()), r.getData(), r.getHash1()));
      referenceTestRecords.add(
          new ReferenceTestRecord64(
              Hashing.wyhashFinal4(0L, r.getSeed1()), r.getData(), r.getHash2()));
      referenceTestRecords.add(
          new ReferenceTestRecord64(
              Hashing.wyhashFinal4(r.getSeed0(), r.getSeed1()), r.getData(), r.getHash3()));
    }
    return referenceTestRecords;
  }
}
