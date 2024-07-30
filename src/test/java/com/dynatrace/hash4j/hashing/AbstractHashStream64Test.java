/*
 * Copyright 2024 Dynatrace LLC
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

public class AbstractHashStream64Test extends AbstractHashStreamTest {

  @Override
  protected void assertBytes(Consumer<HashStream> c, String hexString) {
    TestHashStream64 hashStream = new TestHashStream64();
    c.accept(hashStream);
    assertThat(byteArrayToHexString(hashStream.getData())).isEqualTo(hexString);
  }

  private static final class TestHashStream64 extends AbstractHashStream64 {

    private final TestHashStream hashStream;

    public TestHashStream64(TestHashStream hashStream) {
      this.hashStream = hashStream;
    }

    public TestHashStream64() {
      this.hashStream = new TestHashStream();
    }

    @Override
    public long getAsLong() {
      throw new UnsupportedOperationException();
    }

    @Override
    public TestHashStream64 putByte(byte v) {
      hashStream.putByte(v);
      return this;
    }

    @Override
    public TestHashStream64 reset() {
      hashStream.reset();
      return this;
    }

    @Override
    public TestHashStream64 copy() {
      return new TestHashStream64(hashStream.copy());
    }

    public byte[] getData() {
      return hashStream.getData();
    }
  }

  @Test
  void testGetHashBitSize() {
    assertThat(new TestHashStream64().getHashBitSize()).isEqualTo(64);
  }
}
