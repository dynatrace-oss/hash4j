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

import static com.dynatrace.hash4j.hashing.HashMocks.createHasher128UsingDefaultImplementations;
import static com.dynatrace.hash4j.hashing.HashMocks.createHasher128WithFixedHash;
import static org.assertj.core.api.Assertions.assertThat;

import com.dynatrace.hash4j.testutils.TestUtils;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

abstract class AbstractHasher128Test extends AbstractHasherTest {

  @Override
  protected abstract List<? extends Hasher128> createHashers();

  @Override
  protected HashStream createNonOptimizedHashStream(Hasher hasher) {
    return createHasher128UsingDefaultImplementations((Hasher128) hasher).hashStream();
  }

  @Test
  void testHashBytesTo128Bits() {

    HashValue128 hash = new HashValue128(0x5cd2aeb8be6aa0bbL, 0x500e3ed0c42e364fL);

    Hasher128 hasher = createHasher128WithFixedHash(hash);

    byte[] b = {};
    String s = "";

    assertThat(hasher.hashBytesTo128Bits(b)).isEqualTo(hash);
    assertThat(hasher.hashBytesToLong(b)).isEqualTo(hash.getAsLong());
    assertThat(hasher.hashBytesToInt(b)).isEqualTo(hash.getAsInt());

    assertThat(hasher.hashBytesTo128Bits(b, 0, 0)).isEqualTo(hash);
    assertThat(hasher.hashBytesToLong(b, 0, 0)).isEqualTo(hash.getAsLong());
    assertThat(hasher.hashBytesToInt(b, 0, 0)).isEqualTo(hash.getAsInt());

    assertThat(hasher.hashCharsTo128Bits(s)).isEqualTo(hash);
    assertThat(hasher.hashCharsToLong(s)).isEqualTo(hash.getAsLong());
    assertThat(hasher.hashCharsToInt(s)).isEqualTo(hash.getAsInt());

    assertThat(hasher.getHashBitSize()).isEqualTo(128);
  }

  @ParameterizedTest
  @MethodSource("getHashers")
  void testGetCompatibility(Hasher128 hasher) {
    byte[] data = TestUtils.hexStringToByteArray("3011498ecb9ca21b2f6260617b55f3a7");
    HashStream128 intHashStream = hasher.hashStream();
    HashStream128 longHashStream = hasher.hashStream();
    HashStream128 hash128Calculator = hasher.hashStream();
    intHashStream.putBytes(data);
    longHashStream.putBytes(data);
    hash128Calculator.putBytes(data);
    int intHash = intHashStream.getAsInt();
    try {
      long longHash = longHashStream.getAsLong();
      assertThat((int) longHash).isEqualTo(intHash);
      HashValue128 hash128Hash = hash128Calculator.get();
      assertThat(hash128Hash.getAsLong()).isEqualTo(longHash);
    } catch (UnsupportedOperationException e) {
      // no compatibility check necessary, if 128-bit hash value is not supported
    }
  }

  @Override
  protected void getHashBytes(List<HashStream> hashStreams, byte[] hashBytes) {
    int off = 0;
    for (HashStream hashStream : hashStreams) {
      System.arraycopy(((HashStream128) hashStream).get().toByteArray(), 0, hashBytes, off, 16);
      off += 16;
    }
    Arrays.fill(hashBytes, off, hashBytes.length, (byte) 0);
  }
}
