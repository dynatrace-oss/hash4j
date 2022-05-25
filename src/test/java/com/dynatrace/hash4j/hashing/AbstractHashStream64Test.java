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

import static com.dynatrace.hash4j.hashing.TestUtils.byteArrayToCharSequence;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

public abstract class AbstractHashStream64Test extends AbstractHashStreamTest {

  public static class ReferenceTestRecord64
      extends AbstractHashStreamTest.ReferenceTestRecord<Hasher64> {

    private final long expectedHash;

    public ReferenceTestRecord64(Hasher64 hashSupplier, byte[] input, long expectedHash) {
      super(hashSupplier, input);
      this.expectedHash = expectedHash;
    }

    public long getExpectedHash() {
      return expectedHash;
    }
  }

  protected abstract List<ReferenceTestRecord64> getReferenceTestRecords();

  @ParameterizedTest
  @MethodSource("getReferenceTestRecords")
  void testAgainstReference(ReferenceTestRecord64 r) {

    assertEquals(r.getExpectedHash(), r.getHasher().hashToLong(r.getData(), BYTES_FUNNEL_1));
    assertEquals(r.getExpectedHash(), r.getHasher().hashToLong(r.getData(), BYTES_FUNNEL_2));
    assertEquals(r.getExpectedHash(), r.getHasher().hashBytesToLong(r.getData()));

    if (r.getData().length % 2 == 0) {
      CharSequence charSequence = byteArrayToCharSequence(r.getData());
      assertEquals(r.getExpectedHash(), r.getHasher().hashToLong(charSequence, CHAR_FUNNEL));
    }
  }
}
