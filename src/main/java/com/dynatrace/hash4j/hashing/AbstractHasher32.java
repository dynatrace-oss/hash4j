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

interface AbstractHasher32 extends Hasher32, Hasher {

  @Override
  default <T> int hashToInt(T data, HashFunnel<T> funnel) {
    return hashStream().put(data, funnel).getAsInt();
  }

  @Override
  default int hashBytesToInt(byte[] input) {
    return hashBytesToInt(input, 0, input.length);
  }

  @Override
  default int getHashBitSize() {
    return 32;
  }

  @Override
  default int hashLongLongToInt(long v1, long v2) {
    return hashStream().putLong(v1).putLong(v2).getAsInt();
  }

  @Override
  default int hashLongLongLongToInt(long v1, long v2, long v3) {
    return hashStream().putLong(v1).putLong(v2).putLong(v3).getAsInt();
  }

  @Override
  default int hashLongIntToInt(long v1, int v2) {
    return hashStream().putLong(v1).putInt(v2).getAsInt();
  }

  @Override
  default int hashIntLongToInt(int v1, long v2) {
    return hashStream().putInt(v1).putLong(v2).getAsInt();
  }

  @Override
  default int hashIntIntIntToInt(int v1, int v2, int v3) {
    return hashStream().putInt(v1).putInt(v2).putInt(v3).getAsInt();
  }
}
