/*
 * Copyright 2022-2024 Dynatrace LLC
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

import java.util.Arrays;
import java.util.List;

class Komihash5_0Test extends AbstractHasher64Test {

  private static final List<Hasher64> HASHERS =
      Arrays.asList(Hashing.komihash5_0(), Hashing.komihash5_0(0x21a9c0c003cd280bL));

  @Override
  protected List<Hasher64> getHashers() {
    return HASHERS;
  }

  @Override
  protected String getChecksumResourceFileName() {
    return "Komihash 5.0.txt";
  }

  @Override
  protected void calculateHashForChecksum(byte[] seedBytes, byte[] hashBytes, byte[] dataBytes) {

    long seed = (long) LONG_HANDLE.get(seedBytes, 0);
    long hash0 = Hashing.komihash5_0().hashBytesToLong(dataBytes);
    long hash1 = Hashing.komihash5_0(seed).hashBytesToLong(dataBytes);

    LONG_HANDLE.set(hashBytes, 0, hash0);
    LONG_HANDLE.set(hashBytes, 8, hash1);
  }

  @Override
  protected void calculateHashForChecksum(byte[] seedBytes, byte[] hashBytes, CharSequence c) {

    long seed = (long) LONG_HANDLE.get(seedBytes, 0);
    long hash0 = Hashing.komihash5_0().hashCharsToLong(c);
    long hash1 = Hashing.komihash5_0(seed).hashCharsToLong(c);

    LONG_HANDLE.set(hashBytes, 0, hash0);
    LONG_HANDLE.set(hashBytes, 8, hash1);
  }

  @Override
  protected List<HashStream> getHashStreams(byte[] seedBytes) {
    long seed = (long) LONG_HANDLE.get(seedBytes, 0);
    return List.of(Hashing.komihash5_0().hashStream(), Hashing.komihash5_0(seed).hashStream());
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
