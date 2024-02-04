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

public class FarmHashUoTest extends AbstractHasher64Test {

  private static final List<Hasher64> HASHERS =
      Arrays.asList(
          Hashing.farmHashUo(),
          Hashing.farmHashUo(0x1b5af6b8376953d2L),
          Hashing.farmHashUo(0x1b0fd3d0cecf44bbL, 0x1d195f7db40c4a96L));

  @Override
  protected List<Hasher64> getHashers() {
    return HASHERS;
  }

  @Override
  protected void calculateHashForChecksum(byte[] seedBytes, byte[] hashBytes, byte[] dataBytes) {
    long seed = (long) LONG_HANDLE.get(seedBytes, 0);
    long seed0 = (long) LONG_HANDLE.get(seedBytes, 8);
    long seed1 = (long) LONG_HANDLE.get(seedBytes, 16);

    long hash0 = FarmHashUo.create().hashBytesToLong(dataBytes);
    long hash1 = FarmHashUo.create(seed).hashBytesToLong(dataBytes);
    long hash2 = FarmHashUo.create(seed0, seed1).hashBytesToLong(dataBytes);

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
  String getExpectedChecksum() {
    return "50f5d48e00fda3fadcacaa5fec1944d90c33539e19ca2dfcb6a97173a3754682";
  }

  @Override
  protected List<ReferenceTestRecord64> getReferenceTestRecords() {

    List<ReferenceTestRecord64> referenceTestRecords = new ArrayList<>();
    for (FarmHashUoReferenceData.ReferenceRecord r : FarmHashUoReferenceData.get()) {
      referenceTestRecords.add(
          new ReferenceTestRecord64(Hashing.farmHashUo(), r.getData(), r.getHash0()));
      referenceTestRecords.add(
          new ReferenceTestRecord64(Hashing.farmHashUo(r.getSeed0()), r.getData(), r.getHash1()));
      referenceTestRecords.add(
          new ReferenceTestRecord64(
              Hashing.farmHashUo(r.getSeed0(), r.getSeed1()), r.getData(), r.getHash2()));
    }
    return referenceTestRecords;
  }
}
