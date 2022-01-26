/*
 * Copyright 2022 Dynatrace LLC
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
package com.dynatrace.hashlib.hashing;

/** Various implementations of hash functions. */
public final class Hashing {

  private Hashing() {}

  @FunctionalInterface
  private interface ExtendedHasher32 extends Hasher32 {

    Hash32Supplier create();

    @Override
    default <T> int hashToInt(T data, HashFunnel<T> funnel) {
      Hash32Supplier sink = create();
      funnel.put(data, sink);
      return sink.getAsInt();
    }
  }

  @FunctionalInterface
  private interface ExtendedHasher64 extends Hasher64, ExtendedHasher32 {

    @Override
    Hash64Supplier create();

    @Override
    default <T> long hashToLong(T data, HashFunnel<T> funnel) {
      Hash64Supplier sink = create();
      funnel.put(data, sink);
      return sink.getAsLong();
    }
  }

  @FunctionalInterface
  private interface ExtendedHasher128 extends Hasher128, ExtendedHasher64 {

    @Override
    Hash128Supplier create();

    @Override
    default <T> HashValue128 hashTo128Bits(T data, HashFunnel<T> funnel) {
      Hash128Supplier sink = create();
      funnel.put(data, sink);
      return sink.get();
    }
  }

  private static final Hasher128 MURMUR3_128 = (ExtendedHasher128) () -> Murmur3_128.create(0);

  /**
   * Returns a {@link Hasher128} implementing the 128-bit Murmur3 algorithm (little-endian) using a
   * seed value of zero.
   *
   * @return a hasher instance
   */
  public static Hasher128 murmur3_128() {
    return MURMUR3_128;
  }

  /**
   * Returns a {@link Hasher128} implementing the 128-bit Murmur3 algorithm (little-endian) using
   * the given seed value.
   *
   * @param seed a 128-bit seed
   * @return a hasher instance
   */
  public static Hasher128 murmur3_128(int seed) {
    return (ExtendedHasher128) () -> Murmur3_128.create(seed);
  }

  static Hasher128 murmur3_128withSeedBug(int seed) {
    return (ExtendedHasher128) () -> Murmur3_128.createWithSeedBug(seed);
  }
}
