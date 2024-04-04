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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

abstract class AbstractHasher32Test extends AbstractHasherTest {

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

  @Override
  protected HashStream createNonOptimizedHashStream(Hasher hasher) {

    HashStream hashStream = hasher.hashStream();
    HashStream32 hashStream32 = (HashStream32) hashStream;
    return new AbstractHashStream32() {
      @Override
      public int getAsInt() {
        return hashStream32.getAsInt();
      }

      @Override
      public HashStream32 putByte(byte v) {
        hashStream32.putByte(v);
        return this;
      }

      @Override
      public HashStream32 reset() {
        hashStream32.reset();
        return this;
      }

      @Override
      public HashStream32 copy() {
        return hashStream32.copy();
      }
    };
  }

  protected abstract List<? extends ReferenceTestRecord32> getReferenceTestRecords();

  @Override
  protected abstract List<? extends Hasher32> getHashers();

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

  @Test
  void testHashBytesToInt() {

    int hash = 0x6a6c9292;

    AbstractHasher32 hasher =
        new AbstractHasher32() {
          @Override
          public int hashBytesToInt(byte[] input, int off, int len) {
            return hash;
          }

          @Override
          public int hashCharsToInt(CharSequence input) {
            return hash;
          }

          @Override
          public HashStream32 hashStream() {
            return new AbstractHashStream32() {

              @Override
              public HashStream32 putByte(byte v) {
                return this;
              }

              @Override
              public HashStream32 reset() {
                return this;
              }

              @Override
              public HashStream32 copy() {
                throw new UnsupportedOperationException();
              }

              @Override
              public int getAsInt() {
                return hash;
              }

              @Override
              public int getHashBitSize() {
                return 32;
              }
            };
          }
        };

    byte[] b = {};
    String s = "";

    assertThat(hasher.hashBytesToInt(b)).isEqualTo(hash);
    assertThat(hasher.hashBytesToInt(b, 0, 0)).isEqualTo(hash);
    assertThat(hasher.hashCharsToInt(s)).isEqualTo(hash);
    assertThat(hasher.getHashBitSize()).isEqualTo(32);
  }
}
