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

import static com.dynatrace.hash4j.helper.ByteArrayUtil.getLong;
import static com.dynatrace.hash4j.helper.ByteArrayUtil.setLong;

import java.util.Arrays;
import java.util.List;

class WyhashFinal3Test extends AbstractHasher64Test {

  private static final List<Hasher64> HASHERS =
      Arrays.asList(
          Hashing.wyhashFinal3(),
          Hashing.wyhashFinal3(0xdfd1434b2173588fL),
          Hashing.wyhashFinal3(0xfa681c2ee9f17f88L, 0x3c88abf5128e96cbL));

  @Override
  protected List<Hasher64> getHashers() {
    return HASHERS;
  }

  @Override
  protected String getChecksumResourceFileName() {
    return "Wyhash final 3.txt";
  }

  @Override
  protected void calculateHashForChecksum(byte[] seedBytes, byte[] hashBytes, byte[] dataBytes) {
    long seed0 = getLong(seedBytes, 0);
    long seed1 = getLong(seedBytes, 8);
    long rand = getLong(seedBytes, 16);

    long hash0 = Hashing.wyhashFinal3().hashBytesToLong(dataBytes);
    long hash1 = Hashing.wyhashFinal3(seed0).hashBytesToLong(dataBytes);
    long hash2 = 0;
    long hash3 = 0;
    if ((rand & 0x3fL) == 0) {
      hash2 = Hashing.wyhashFinal3(0L, seed1).hashBytesToLong(dataBytes);
      hash3 = Hashing.wyhashFinal3(seed0, seed1).hashBytesToLong(dataBytes);
    }

    setLong(hashBytes, 0, hash0);
    setLong(hashBytes, 8, hash1);
    setLong(hashBytes, 16, hash2);
    setLong(hashBytes, 24, hash3);
  }

  @Override
  protected void calculateHashForChecksum(byte[] seedBytes, byte[] hashBytes, CharSequence c) {
    long seed0 = getLong(seedBytes, 0);
    long seed1 = getLong(seedBytes, 8);
    long rand = getLong(seedBytes, 16);

    long hash0 = Hashing.wyhashFinal3().hashCharsToLong(c);
    long hash1 = Hashing.wyhashFinal3(seed0).hashCharsToLong(c);
    long hash2 = 0;
    long hash3 = 0;
    if ((rand & 0x3fL) == 0) {
      hash2 = Hashing.wyhashFinal3(0L, seed1).hashCharsToLong(c);
      hash3 = Hashing.wyhashFinal3(seed0, seed1).hashCharsToLong(c);
    }

    setLong(hashBytes, 0, hash0);
    setLong(hashBytes, 8, hash1);
    setLong(hashBytes, 16, hash2);
    setLong(hashBytes, 24, hash3);
  }

  @Override
  protected List<HashStream> getHashStreams(byte[] seedBytes) {
    long seed0 = getLong(seedBytes, 0);
    long seed1 = getLong(seedBytes, 8);
    long rand = getLong(seedBytes, 16);
    if ((rand & 0x3fL) == 0) {
      return List.of(
          Hashing.wyhashFinal3().hashStream(),
          Hashing.wyhashFinal3(seed0).hashStream(),
          Hashing.wyhashFinal3(0L, seed1).hashStream(),
          Hashing.wyhashFinal3(seed0, seed1).hashStream());
    } else {
      return List.of(Hashing.wyhashFinal3().hashStream(), Hashing.wyhashFinal3(seed0).hashStream());
    }
  }

  @Override
  int getSeedSizeForChecksum() {
    return 24;
  }

  @Override
  int getHashSizeForChecksum() {
    return 32;
  }

  @Override
  protected int getBlockLengthInBytes() {
    return 48;
  }
}
