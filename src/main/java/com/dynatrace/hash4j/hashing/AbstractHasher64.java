/*
 * Copyright 2022-2026 Dynatrace LLC
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

interface AbstractHasher64 extends AbstractHasher32, Hasher64 {

  @Override
  default <T> long hashToLong(T data, HashFunnel<T> funnel) {
    return hashStream().put(data, funnel).getAsLong();
  }

  @Override
  default long hashBytesToLong(byte[] input) {
    return hashBytesToLong(input, 0, input.length);
  }

  @Override
  default long hashBytesToLong(byte[] input, int off, int len) {
    return hashBytesToLong(input, off, len, ByteArrayByteAccess.get());
  }

  @Override
  default int hashBytesToInt(byte[] input, int off, int len) {
    return (int) hashBytesToLong(input, off, len);
  }

  @Override
  default <T> int hashBytesToInt(T input, long off, long len, ByteAccess<T> access) {
    return (int) hashBytesToLong(input, off, len, access);
  }

  @Override
  default int hashCharsToInt(CharSequence input) {
    return (int) hashCharsToLong(input);
  }

  @Override
  default int getHashBitSize() {
    return 64;
  }

  @Override
  default int hashIntToInt(int v) {
    return (int) hashIntToLong(v);
  }

  @Override
  default long hashIntToLong(int v) {
    return hashStream().putInt(v).getAsLong();
  }

  @Override
  default int hashIntIntToInt(int v1, int v2) {
    return (int) hashIntIntToLong(v1, v2);
  }

  @Override
  default long hashIntIntToLong(int v1, int v2) {
    return hashLongToLong((v1 & 0xFFFFFFFFL) | ((long) v2 << 32));
  }

  @Override
  default int hashIntIntIntToInt(int v1, int v2, int v3) {
    return (int) hashIntIntIntToLong(v1, v2, v3);
  }

  @Override
  default long hashIntIntIntToLong(int v1, int v2, int v3) {
    return hashStream().putInt(v1).putInt(v2).putInt(v3).getAsLong();
  }

  @Override
  default int hashIntLongToInt(int v1, long v2) {
    return (int) hashIntLongToLong(v1, v2);
  }

  @Override
  default long hashIntLongToLong(int v1, long v2) {
    return hashStream().putInt(v1).putLong(v2).getAsLong();
  }

  @Override
  default int hashLongToInt(long v) {
    return (int) hashLongToLong(v);
  }

  @Override
  default long hashLongToLong(long v) {
    return hashStream().putLong(v).getAsLong();
  }

  @Override
  default int hashLongLongToInt(long v1, long v2) {
    return (int) hashLongLongToLong(v1, v2);
  }

  @Override
  default long hashLongLongToLong(long v1, long v2) {
    return hashStream().putLong(v1).putLong(v2).getAsLong();
  }

  @Override
  default int hashLongLongLongToInt(long v1, long v2, long v3) {
    return (int) hashLongLongLongToLong(v1, v2, v3);
  }

  @Override
  default long hashLongLongLongToLong(long v1, long v2, long v3) {
    return hashStream().putLong(v1).putLong(v2).putLong(v3).getAsLong();
  }

  @Override
  default int hashLongIntToInt(long v1, int v2) {
    return (int) hashLongIntToLong(v1, v2);
  }

  @Override
  default long hashLongIntToLong(long v1, int v2) {
    return hashStream().putLong(v1).putInt(v2).getAsLong();
  }

  @Override
  default HashStream64 hashStreamFromState(byte[] state) {
    return hashStream().setState(state);
  }
}
