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
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

public abstract class AbstractHashStream32Test extends AbstractHashStreamTest {

  public static class ReferenceTestRecord32 extends ReferenceTestRecord<Hasher32> {

    private final int expectedHash;

    public ReferenceTestRecord32(Hasher32 hashSupplier, byte[] input, int expectedHash) {
      super(hashSupplier, input);
      this.expectedHash = expectedHash;
    }

    public int getExpectedHash() {
      return expectedHash;
    }
  }

  protected abstract List<ReferenceTestRecord32> getReferenceTestRecords();

  @ParameterizedTest
  @MethodSource("getReferenceTestRecords")
  void testAgainstReference(ReferenceTestRecord32 r) {

    assertThat(r.getHasher().hashToInt(r.getData(), BYTES_FUNNEL_1)).isEqualTo(r.getExpectedHash());
    assertThat(r.getHasher().hashToInt(r.getData(), BYTES_FUNNEL_2)).isEqualTo(r.getExpectedHash());
    assertThat(r.getHasher().hashBytesToInt(r.getData())).isEqualTo(r.getExpectedHash());

    if (r.getData().length % 2 == 0) {
      CharSequence charSequence = byteArrayToCharSequence(r.getData());
      assertThat(r.getHasher().hashCharsToInt(charSequence)).isEqualTo(r.getExpectedHash());
      assertThat(r.getHasher().hashToInt(charSequence, CHAR_FUNNEL)).isEqualTo(r.getExpectedHash());
    }
  }
}
