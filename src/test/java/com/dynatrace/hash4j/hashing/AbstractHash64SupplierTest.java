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

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public abstract class AbstractHash64SupplierTest<T extends Hash64Supplier>
    extends AbstractHashSinkTest<T> {

  @Test
  public void testLongIntCompatibility() {
    byte[] data = TestUtils.hexStringToByteArray("3011498ecb9ca21b2f6260617b55f3a7");
    Hash64Supplier longSink = createHashSink();
    Hash32Supplier intSink = createHashSink();
    longSink.putBytes(data);
    intSink.putBytes(data);
    long longHash = longSink.getAsLong();
    int intHash = intSink.getAsInt();
    assertEquals(intHash, (int) longHash);
  }
}
