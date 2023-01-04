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

import com.dynatrace.hash4j.hashing.Komihash4_3ReferenceData.ReferenceRecord;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

class Komihash4_3Test extends AbstractHasher64Test {

  private static final List<Hasher64> HASHERS =
      Arrays.asList(Hashing.komihash4_3(), Hashing.komihash4_3(0x1b5af6b8376953d2L));

  @Override
  protected List<Hasher64> getHashers() {
    return HASHERS;
  }

  @Override
  protected void calculateHashForChecksum(byte[] seedBytes, byte[] hashBytes, byte[] dataBytes) {

    long seed = (long) LONG_HANDLE.get(seedBytes, 0);
    long hash0 = Komihash4_3.create().hashBytesToLong(dataBytes);
    long hash1 = Komihash4_3.create(seed).hashBytesToLong(dataBytes);

    LONG_HANDLE.set(hashBytes, 0, hash0);
    LONG_HANDLE.set(hashBytes, 8, hash1);
  }

  @Override
  int getSeedSizeForChecksum() {
    return 8;
  }

  @Override
  int getHashSizeForChecksum() {
    return 16;
  }

  @Override
  String getExpectedChecksum() {
    return "b83dc90ff8c0ad72989f5150a6f7dba41adfe0a70b9112da93040f3882ce16f6";
  }

  @Override
  protected List<ReferenceTestRecord64> getReferenceTestRecords() {
    List<ReferenceTestRecord64> referenceTestRecords = new ArrayList<>();
    for (ReferenceRecord r : Komihash4_3ReferenceData.get()) {
      referenceTestRecords.add(
          new ReferenceTestRecord64(Hashing.komihash4_3(), r.getData(), r.getHash0()));
      referenceTestRecords.add(
          new ReferenceTestRecord64(Hashing.komihash4_3(r.getSeed()), r.getData(), r.getHash1()));
    }
    return referenceTestRecords;
  }
}
