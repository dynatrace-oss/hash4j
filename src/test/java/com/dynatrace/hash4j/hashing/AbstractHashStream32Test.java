/*
 * Copyright 2024-2026 Dynatrace LLC
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

import com.dynatrace.hash4j.hashing.HashMocks.TestHashStream32;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;

class AbstractHashStream32Test extends AbstractHashStreamTest {

  @Override
  protected void assertBytes(Consumer<HashStream> c, String hexString) {
    TestHashStream32 hashStream = new TestHashStream32();
    c.accept(hashStream);
    assertThat(byteArrayToHexString(hashStream.getData())).isEqualTo(hexString);
  }

  @Test
  void testGetHashBitSize() {
    assertThat(new TestHashStream32().getHashBitSize()).isEqualTo(32);
  }
}
