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
package com.dynatrace.hashlib.hashing;

import java.util.ArrayList;
import java.util.List;

public class Murmur3_128Test extends AbstractHash128SupplierTest<Murmur3_128> {

  @Override
  protected Murmur3_128 createHashSink() {
    return Murmur3_128.create(0xfc64a346);
  }

  @Override
  protected List<Byte> getBytes(Murmur3_128 dataSink) {
    HashValue128 x = dataSink.get();
    List<Byte> result = new ArrayList<>(16);
    for (int i = 56; i >= 0; i -= 8) {
      result.add((byte) ((x.getMostSignificantBits() >>> i) & 0xFFL));
    }
    for (int i = 56; i >= 0; i -= 8) {
      result.add((byte) ((x.getLeastSignificantBits() >>> i) & 0xFFL));
    }
    return result;
  }

  @Override
  protected boolean compareWithOriginalData() {
    return false;
  }
}
