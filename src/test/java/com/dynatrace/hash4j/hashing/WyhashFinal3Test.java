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

import com.dynatrace.hash4j.hashing.WyhashFinal3ReferenceData.ReferenceRecord;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class WyhashFinal3Test extends AbstractHashStream64Test {

  private static final List<Hasher64> HASHERS =
      Arrays.asList(
          Hashing.wyhashFinal3(),
          Hashing.wyhashFinal3(0xdfd1434b2173588fL),
          Hashing.wyhashFinal3(0xfa681c2ee9f17f88L, 0x3c88abf5128e96cbL));

  @Override
  protected List<Hasher64> getHashers() {
    return HASHERS;
  }

  @Override
  protected List<ReferenceTestRecord64> getReferenceTestRecords() {
    List<ReferenceTestRecord64> referenceTestRecords = new ArrayList<>();
    for (ReferenceRecord r : WyhashFinal3ReferenceData.get()) {
      referenceTestRecords.add(
          new ReferenceTestRecord64(Hashing.wyhashFinal3(), r.getData(), r.getHash0()));
      referenceTestRecords.add(
          new ReferenceTestRecord64(Hashing.wyhashFinal3(r.getSeed0()), r.getData(), r.getHash1()));
      referenceTestRecords.add(
          new ReferenceTestRecord64(
              Hashing.wyhashFinal3(0L, r.getSeed1()), r.getData(), r.getHash2()));
      referenceTestRecords.add(
          new ReferenceTestRecord64(
              Hashing.wyhashFinal3(r.getSeed0(), r.getSeed1()), r.getData(), r.getHash3()));
    }
    return referenceTestRecords;
  }
}
