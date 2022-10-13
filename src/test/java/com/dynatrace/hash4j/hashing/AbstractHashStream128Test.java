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

import static com.dynatrace.hash4j.testutils.TestUtils.byteArrayToCharSequence;
import static com.dynatrace.hash4j.testutils.TestUtils.hash128ToByteArray;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

abstract class AbstractHashStream128Test extends AbstractHashStreamTest {

  public static class ReferenceTestRecord128 extends ReferenceTestRecord<Hasher128> {

    private final byte[] expectedHash;

    public ReferenceTestRecord128(Hasher128 hashSupplier, byte[] input, byte[] expectedHash) {
      super(hashSupplier, input);
      this.expectedHash = Arrays.copyOf(expectedHash, expectedHash.length);
    }

    public byte[] getExpectedHash() {
      return expectedHash;
    }
  }

  protected abstract List<ReferenceTestRecord128> getReferenceTestRecords();

  @ParameterizedTest
  @MethodSource("getReferenceTestRecords")
  void testAgainstReference(AbstractHashStream128Test.ReferenceTestRecord128 r) {

    assertThat(hash128ToByteArray(r.getHasher().hashTo128Bits(r.getData(), BYTES_FUNNEL_1)))
        .isEqualTo(r.getExpectedHash());
    assertThat(hash128ToByteArray(r.getHasher().hashTo128Bits(r.getData(), BYTES_FUNNEL_2)))
        .isEqualTo(r.getExpectedHash());
    assertThat(hash128ToByteArray(r.getHasher().hashBytesTo128Bits(r.getData())))
        .isEqualTo(r.getExpectedHash());

    if (r.getData().length % 2 == 0) {
      CharSequence charSequence = byteArrayToCharSequence(r.getData());
      assertThat(hash128ToByteArray(r.getHasher().hashCharsTo128Bits(charSequence)))
          .isEqualTo(r.getExpectedHash());
      assertThat(hash128ToByteArray(r.getHasher().hashTo128Bits(charSequence, CHAR_FUNNEL)))
          .isEqualTo(r.getExpectedHash());
    }
  }
}
