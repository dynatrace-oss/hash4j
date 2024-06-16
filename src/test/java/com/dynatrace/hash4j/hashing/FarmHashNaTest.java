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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class FarmHashNaTest extends AbstractHasher64Test {

  private static final List<Hasher64> HASHERS =
      Arrays.asList(
          Hashing.farmHashNa(),
          Hashing.farmHashNa(0x1b5af6b8376953d2L),
          Hashing.farmHashNa(0x1b0fd3d0cecf44bbL, 0x1d195f7db40c4a96L));

  @Override
  protected List<Hasher64> getHashers() {
    return HASHERS;
  }

  @Override
  protected String getChecksumResourceFileName() {
    return "FarmHash NA.txt";
  }

  @Override
  protected void calculateHashForChecksum(byte[] seedBytes, byte[] hashBytes, byte[] dataBytes) {
    long seed = (long) LONG_HANDLE.get(seedBytes, 0);
    long seed0 = (long) LONG_HANDLE.get(seedBytes, 8);
    long seed1 = (long) LONG_HANDLE.get(seedBytes, 16);

    long hash0 = FarmHashNa.create().hashBytesToLong(dataBytes);
    long hash1 = FarmHashNa.create(seed).hashBytesToLong(dataBytes);
    long hash2 = FarmHashNa.create(seed0, seed1).hashBytesToLong(dataBytes);

    LONG_HANDLE.set(hashBytes, 0, hash0);
    LONG_HANDLE.set(hashBytes, 8, hash1);
    LONG_HANDLE.set(hashBytes, 16, hash2);
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
  protected List<ReferenceTestRecord64> getReferenceTestRecords() {

    List<ReferenceTestRecord64> referenceTestRecords = new ArrayList<>();
    for (FarmHashNaReferenceData.ReferenceRecord r : FarmHashNaReferenceData.get()) {
      referenceTestRecords.add(
          new ReferenceTestRecord64(Hashing.farmHashNa(), r.getData(), r.getHash0()));
      referenceTestRecords.add(
          new ReferenceTestRecord64(Hashing.farmHashNa(r.getSeed0()), r.getData(), r.getHash1()));
      referenceTestRecords.add(
          new ReferenceTestRecord64(
              Hashing.farmHashNa(r.getSeed0(), r.getSeed1()), r.getData(), r.getHash2()));
    }
    return referenceTestRecords;
  }
}
