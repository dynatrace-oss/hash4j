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

import static com.dynatrace.hash4j.hashing.HashUtil.mix;
import static com.dynatrace.hash4j.internal.UnsignedMultiplyUtil.unsignedMultiplyHigh;

import java.util.Objects;

final class WyhashFinal4 extends AbstractWyhashFinal {

  private final long secret0;

  // visible for testing
  WyhashFinal4(long seedForHash, long[] secret) {
    super(seedForHash ^ mix(seedForHash ^ secret[0], secret[1]), secret[1], secret[2], secret[3]);
    this.secret0 = secret[0];
  }

  static Hasher64 create() {
    return DEFAULT_HASHER_INSTANCE;
  }

  static Hasher64 create(long seedForHash) {
    return new WyhashFinal4(seedForHash, DEFAULT_SECRET);
  }

  static Hasher64 create(long seedForHash, long seedForSecret) {
    return new WyhashFinal4(seedForHash, makeSecret(seedForSecret));
  }

  private static final Hasher64 DEFAULT_HASHER_INSTANCE = create(0L);

  @Override
  protected long finish(long a, long b, long seed, long len) {
    a ^= secret1;
    b ^= seed;
    return mix((a * b) ^ secret0 ^ len, unsignedMultiplyHigh(a, b) ^ secret1);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (!(obj instanceof WyhashFinal4)) return false;
    WyhashFinal4 that = (WyhashFinal4) obj;
    return seed == that.seed
        && secret0 == that.secret0
        && secret1 == that.secret1
        && secret2 == that.secret2
        && secret3 == that.secret3;
  }

  @Override
  public int hashCode() {
    return Objects.hash(seed, secret0, secret1, secret2, secret3);
  }
}
