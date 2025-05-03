/*
 * Copyright 2024-2025 Dynatrace LLC
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

import static com.dynatrace.hash4j.testutils.TestUtils.byteArrayToHexString;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import java.util.function.Consumer;
import org.junit.jupiter.api.Test;

public class AbstractHashStream128Test extends AbstractHashStreamTest {

  @Override
  protected void assertBytes(Consumer<HashStream> c, String hexString) {
    TestHashStream128 hashStream = new TestHashStream128();
    c.accept(hashStream);
    assertThat(byteArrayToHexString(hashStream.getData())).isEqualTo(hexString);
  }

  private static final class TestHashStream128 implements AbstractHashStream128 {

    private final TestHashStream hashStream;

    public TestHashStream128(TestHashStream hashStream) {
      this.hashStream = hashStream;
    }

    public TestHashStream128() {
      this.hashStream = new TestHashStream();
    }

    @Override
    public HashValue128 get() {
      throw new UnsupportedOperationException();
    }

    @Override
    public TestHashStream128 putByte(byte v) {
      hashStream.putByte(v);
      return this;
    }

    @Override
    public TestHashStream128 reset() {
      hashStream.reset();
      return this;
    }

    @Override
    public TestHashStream128 copy() {
      return new TestHashStream128(hashStream.copy());
    }

    @Override
    public Hasher128 getHasher() {
      throw new UnsupportedOperationException();
    }

    public byte[] getData() {
      return hashStream.getData();
    }
  }

  @Test
  void testGetHashBitSize() {
    assertThat(new TestHashStream128().getHashBitSize()).isEqualTo(128);
  }
}
