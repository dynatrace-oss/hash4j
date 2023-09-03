/*
 * Copyright 2022-2023 Dynatrace LLC
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

abstract class AbstractHasher64 extends AbstractHasher32 implements Hasher64 {

  @Override
  public <T> long hashToLong(T data, HashFunnel<T> funnel) {
    return hashStream().put(data, funnel).getAsLong();
  }

  @Override
  public long hashBytesToLong(byte[] input) {
    return hashBytesToLong(input, 0, input.length);
  }

  @Override
  public int hashBytesToInt(byte[] input, int off, int len) {
    return (int) hashBytesToLong(input, off, len);
  }

  @Override
  public int hashCharsToInt(CharSequence input) {
    return (int) hashCharsToLong(input);
  }

  @Override
  public int getHashBitSize() {
    return 64;
  }

  @Override
  public long hashLongLongToLong(long v1, long v2) {
    return hashStream().putLong(v1).putLong(v2).getAsLong();
  }

  @Override
  public long hashLongLongLongToLong(long v1, long v2, long v3) {
    return hashStream().putLong(v1).putLong(v2).putLong(v3).getAsLong();
  }
}
