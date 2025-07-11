/*
 * Copyright 2022-2025 Dynatrace LLC
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

import static com.dynatrace.hash4j.hashing.HashMocks.createHasher32UsingDefaultImplementations;
import static com.dynatrace.hash4j.hashing.HashMocks.createHasher32WithFixedHash;
import static com.dynatrace.hash4j.internal.ByteArrayUtil.setInt;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

abstract class AbstractHasher32Test extends AbstractHasherTest {

  @Override
  protected HashStream createNonOptimizedHashStream(Hasher hasher) {
    return createHasher32UsingDefaultImplementations((Hasher32) hasher).hashStream();
  }

  @Override
  protected abstract List<? extends Hasher32> createHashers();

  @Test
  void testHashBytesToInt() {

    int hash = 0x6a6c9292;

    Hasher32 hasher = createHasher32WithFixedHash(hash);

    byte[] b = {};
    String s = "";

    assertThat(hasher.hashBytesToInt(b)).isEqualTo(hash);
    assertThat(hasher.hashBytesToInt(b, 0, 0)).isEqualTo(hash);
    assertThat(hasher.hashCharsToInt(s)).isEqualTo(hash);
    assertThat(hasher.getHashBitSize()).isEqualTo(32);
  }

  @Override
  protected void getHashBytes(List<HashStream> hashStreams, byte[] hashBytes) {
    int off = 0;
    for (HashStream hashStream : hashStreams) {
      setInt(hashBytes, off, ((HashStream32) hashStream).getAsInt());
      off += 4;
    }
    Arrays.fill(hashBytes, off, hashBytes.length, (byte) 0);
  }
}
