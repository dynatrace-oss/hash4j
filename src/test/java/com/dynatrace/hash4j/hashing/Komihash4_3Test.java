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

import com.dynatrace.hash4j.hashing.Komihash4_3ReferenceData.ReferenceRecord;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Komihash4_3Test extends AbstractHashCalculator64Test {

  private static final List<Hasher64> HASHERS =
      Arrays.asList(Hashing.komihash4_3(), Hashing.komihash4_3(0x1b5af6b8376953d2L));

  @Override
  protected List<Hasher64> getHashers() {
    return HASHERS;
  }

  @Override
  protected List<ReferenceTestRecord64> getReferenceTestRecords() {
    List<ReferenceTestRecord64> referenceTestRecords = new ArrayList<>();
    for (ReferenceRecord r : Komihash4_3ReferenceData.get()) {
      referenceTestRecords.add(
          new ReferenceTestRecord64(Komihash4_3.create(), r.getData(), r.getHash0()));
      referenceTestRecords.add(
          new ReferenceTestRecord64(Komihash4_3.create(r.getSeed()), r.getData(), r.getHash1()));
    }
    return referenceTestRecords;
  }
}
