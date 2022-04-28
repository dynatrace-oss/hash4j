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
package com.dynatrace.hash4j.hashing;

/** Various implementations of hash functions. */
public final class Hashing {

  private Hashing() {}

  /**
   * Returns a {@link Hasher32} implementing the 32-bit Murmur3 algorithm (little-endian) using a
   * seed value of zero.
   *
   * <p>This implementation is compatible with the C++ reference implementation of {@code
   * MurmurHash3_x86_32} defined in <a
   * href="https://github.com/aappleby/smhasher/blob/61a0530f28277f2e850bfc39600ce61d02b518de/src/MurmurHash3.cpp">MurmurHash3.cpp</a>
   * on an Intel x86 architecture.
   *
   * @return a hasher instance
   */
  public static Hasher32 murmur3_32() {
    return Murmur3_32.create();
  }

  /**
   * Returns a {@link Hasher32} implementing the 32-bit Murmur3 algorithm (little-endian) using the
   * given seed value.
   *
   * <p>This implementation is compatible with the C++ reference implementation of {@code
   * MurmurHash3_x86_32} defined in <a
   * href="https://github.com/aappleby/smhasher/blob/61a0530f28277f2e850bfc39600ce61d02b518de/src/MurmurHash3.cpp">MurmurHash3.cpp</a>
   * on an Intel x86 architecture.
   *
   * @param seed a 32-bit seed
   * @return a hasher instance
   */
  public static Hasher32 murmur3_32(int seed) {
    return Murmur3_32.create(seed);
  }

  /**
   * Returns a {@link Hasher128} implementing the 128-bit Murmur3 algorithm (little-endian) using a
   * seed value of zero.
   *
   * <p>This implementation is compatible with the C++ reference implementation of {@code
   * MurmurHash3_x64_128} defined in <a
   * href="https://github.com/aappleby/smhasher/blob/61a0530f28277f2e850bfc39600ce61d02b518de/src/MurmurHash3.cpp">MurmurHash3.cpp</a>
   * on an Intel x86 architecture.
   *
   * @return a hasher instance
   */
  public static Hasher128 murmur3_128() {
    return Murmur3_128.create();
  }

  /**
   * Returns a {@link Hasher128} implementing the 128-bit Murmur3 algorithm (little-endian) using
   * the given seed value.
   *
   * <p>This implementation is compatible with the C++ reference implementation of {@code
   * MurmurHash3_x64_128} defined in <a
   * href="https://github.com/aappleby/smhasher/blob/61a0530f28277f2e850bfc39600ce61d02b518de/src/MurmurHash3.cpp">MurmurHash3.cpp</a>
   * on an Intel x86 architecture.
   *
   * @param seed a 32-bit seed
   * @return a hasher instance
   */
  public static Hasher128 murmur3_128(int seed) {
    return Murmur3_128.create(seed);
  }

  /**
   * Returns a {@link Hasher128} implementing the 128-bit Murmur3 algorithm (little-endian) using a
   * seed value of zero and the default secret.
   *
   * <p>This implementation is compatible with the C++ reference implementation of {@code wyhash}
   * defined in <a
   * href="https://github.com/wangyi-fudan/wyhash/blob/991aa3dab624e50b066f7a02ccc9f6935cc740ec/wyhash.h">wyhash.h</a>
   * on an Intel x86 architecture.
   *
   * @return a hasher instance
   */
  public static Hasher64 wyhashFinal3() {
    return WyhashFinal3.create();
  }

  /**
   * Returns a {@link Hasher64} implementing the 64-bit Wyhash (version final 3) algorithm using the
   * given seed value and the default secret.
   *
   * <p>This implementation is compatible with the C++ reference implementation of {@code wyhash}
   * defined in <a
   * href="https://github.com/wangyi-fudan/wyhash/blob/991aa3dab624e50b066f7a02ccc9f6935cc740ec/wyhash.h">wyhash.h</a>
   * on an Intel x86 architecture.
   *
   * @param seed a 64-bit seed
   * @return a hasher instance
   */
  public static Hasher64 wyhashFinal3(long seed) {
    return WyhashFinal3.create(seed);
  }

  /**
   * Returns a {@link Hasher64} implementing the 64-bit Wyhash (version final 3) algorithm using the
   * given seed values.
   *
   * <p>This implementation is compatible with the C++ reference implementation of {@code wyhash}
   * and {@code make_secret} defined in <a
   * href="https://github.com/wangyi-fudan/wyhash/blob/991aa3dab624e50b066f7a02ccc9f6935cc740ec/wyhash.h">wyhash.h</a>
   * on an Intel x86 architecture.
   *
   * @param seed a 64-bit seed
   * @param seedForSecret a 64-bit seed for secret generation
   * @return a hasher instance
   */
  public static Hasher64 wyhashFinal3(long seed, long seedForSecret) {
    return WyhashFinal3.create(seed, seedForSecret);
  }
}
