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

import static com.dynatrace.hash4j.internal.ByteArrayUtil.getLong;
import static com.dynatrace.hash4j.internal.ByteArrayUtil.setLong;

import java.util.Arrays;
import java.util.List;

class Komihash4_3Test extends AbstractHasher64Test {

  private static final List<Hasher64> HASHERS =
      Arrays.asList(Hashing.komihash4_3(), Hashing.komihash4_3(0x1b5af6b8376953d2L));

  @Override
  protected List<Hasher64> getHashers() {
    return HASHERS;
  }

  @Override
  protected String getChecksumResourceFileName() {
    return "Komihash 4.3.txt";
  }

  @Override
  protected void calculateHashForChecksum(byte[] seedBytes, byte[] hashBytes, byte[] dataBytes) {

    long seed = getLong(seedBytes, 0);
    long hash0 = Hashing.komihash4_3().hashBytesToLong(dataBytes);
    long hash1 = Hashing.komihash4_3(seed).hashBytesToLong(dataBytes);

    setLong(hashBytes, 0, hash0);
    setLong(hashBytes, 8, hash1);
  }

  @Override
  protected void calculateHashForChecksum(byte[] seedBytes, byte[] hashBytes, CharSequence c) {

    long seed = getLong(seedBytes, 0);
    long hash0 = Hashing.komihash4_3().hashCharsToLong(c);
    long hash1 = Hashing.komihash4_3(seed).hashCharsToLong(c);

    setLong(hashBytes, 0, hash0);
    setLong(hashBytes, 8, hash1);
  }

  @Override
  protected List<HashStream> getHashStreams(byte[] seedBytes) {
    long seed = getLong(seedBytes, 0);
    return List.of(Hashing.komihash4_3().hashStream(), Hashing.komihash4_3(seed).hashStream());
  }

  @Override
  int getSeedSizeForChecksum() {
    return 8;
  }

  @Override
  int getHashSizeForChecksum() {
    return 16;
  }

  @Override
  protected int getBlockLengthInBytes() {
    return 64;
  }
}
