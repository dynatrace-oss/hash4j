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

import static com.dynatrace.hash4j.hashing.HashMocks.createHasher64UsingDefaultImplementations;
import static com.dynatrace.hash4j.hashing.HashMocks.createHasher64WithFixedHash;
import static com.dynatrace.hash4j.internal.ByteArrayUtil.setLong;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

abstract class AbstractHasher64Test extends AbstractHasherTest {

  @Override
  protected abstract List<? extends Hasher64> createHashers();

  @Override
  protected HashStream createNonOptimizedHashStream(Hasher hasher) {
    return createHasher64UsingDefaultImplementations((Hasher64) hasher).hashStream();
  }

  @Test
  void testHashCompatibility() {

    long hash = 0x2a80de88db42361fL;

    Hasher64 hasher = createHasher64WithFixedHash(hash);

    byte[] b = {};
    String s = "";

    assertThat(hasher.hashBytesToLong(b)).isEqualTo(hash);
    assertThat(hasher.hashBytesToInt(b)).isEqualTo((int) hash);

    assertThat(hasher.hashBytesToLong(b, 0, 0)).isEqualTo(hash);
    assertThat(hasher.hashBytesToInt(b, 0, 0)).isEqualTo((int) hash);

    assertThat(hasher.hashBytesToLong(b, 0, 0, NativeByteArrayByteAccess.get())).isEqualTo(hash);
    assertThat(hasher.hashBytesToInt(b, 0, 0, NativeByteArrayByteAccess.get()))
        .isEqualTo((int) hash);

    assertThat(hasher.hashCharsToLong(s)).isEqualTo(hash);
    assertThat(hasher.hashCharsToInt(s)).isEqualTo((int) hash);

    assertThat(hasher.getHashBitSize()).isEqualTo(64);
  }

  @Override
  protected void getHashBytes(List<HashStream> hashStreams, byte[] hashBytes) {
    int off = 0;
    for (HashStream hashStream : hashStreams) {
      setLong(hashBytes, off, ((HashStream64) hashStream).getAsLong());
      off += 8;
    }
    Arrays.fill(hashBytes, off, hashBytes.length, (byte) 0);
  }
}
