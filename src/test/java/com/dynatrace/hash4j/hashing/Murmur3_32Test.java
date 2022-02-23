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

import java.util.ArrayList;
import java.util.List;

public class Murmur3_32Test extends AbstractHashSinkTest<Murmur3_32> {
  @Override
  protected Murmur3_32 createHashSink() {
    return new Murmur3_32(0x43a3fb15);
  }

  @Override
  protected List<Byte> getBytes(Murmur3_32 dataSink) {
    int x = dataSink.getAsInt();
    List<Byte> result = new ArrayList<>(4);
    for (int i = 24; i >= 0; i -= 8) {
      result.add((byte) ((x >>> i) & 0xFFL));
    }
    return result;
  }

  @Override
  protected boolean compareWithOriginalData() {
    return false;
  }
}
