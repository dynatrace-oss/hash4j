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

import static com.dynatrace.hash4j.hashing.HashUtil.mix;

import java.util.Objects;

final class WyhashFinal3 extends AbstractWyhashFinal {

  // visible for testing
  WyhashFinal3(long seedForHash, long[] secret) {
    super(seedForHash ^ secret[0], secret[1], secret[2], secret[3]);
  }

  @Override
  protected long finish(long a, long b, long seed, long len) {
    return mix(secret1 ^ len, mix(a ^ secret1, b ^ seed));
  }

  static Hasher64 create() {
    return DEFAULT_HASHER_INSTANCE;
  }

  static Hasher64 create(long seedForHash) {
    return new WyhashFinal3(seedForHash, DEFAULT_SECRET);
  }

  static Hasher64 create(long seedForHash, long seedForSecret) {
    return new WyhashFinal3(seedForHash, makeSecret(seedForSecret));
  }

  private static final Hasher64 DEFAULT_HASHER_INSTANCE = create(0L);

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (!(obj instanceof WyhashFinal3)) return false;
    WyhashFinal3 that = (WyhashFinal3) obj;
    return seed == that.seed
        && secret1 == that.secret1
        && secret2 == that.secret2
        && secret3 == that.secret3;
  }

  @Override
  public int hashCode() {
    return Objects.hash(seed, secret1, secret2, secret3);
  }
}
