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
   * Returns a {@link Hasher64} implementing the 64-bit Komihash (version 4.3) algorithm using a
   * seed value of zero.
   *
   * <p>This implementation is compatible with the C++ reference implementation of {@code komihash}
   * defined in <a
   * href="https://github.com/avaneev/komihash/blob/e107760596dc5e883e26a58f81a5fd653061bd5a/komihash.h">komihash.h</a>
   * on an Intel x86 architecture. Furthermore, it is compatible with Komihash versions 4.5 and 4.7.
   *
   * <p>This function is superseded by {@link #komihash5_0} and should only be used if compatibility
   * is needed.
   *
   * @return a hasher instance
   */
  public static Hasher64 komihash4_3() {
    return Komihash4_3.create();
  }

  /**
   * Returns a {@link Hasher64} implementing the 64-bit Komihash (version 4.3) algorithm using the
   * given seed value.
   *
   * <p>This implementation is compatible with the C++ reference implementation of {@code komihash}
   * defined in <a
   * href="https://github.com/avaneev/komihash/blob/e107760596dc5e883e26a58f81a5fd653061bd5a/komihash.h">komihash.h</a>
   * on an Intel x86 architecture. Furthermore, it is compatible with Komihash versions 4.5 and 4.7.
   *
   * <p>This function is superseded by {@link #komihash5_0} and should only be used if compatibility
   * is needed.
   *
   * @param seed a 64-bit seed
   * @return a hasher instance
   */
  public static Hasher64 komihash4_3(long seed) {
    return Komihash4_3.create(seed);
  }

  /**
   * Returns a {@link Hasher64} implementing the 64-bit Komihash (version 5.0) algorithm using a
   * seed value of zero.
   *
   * <p>This implementation is compatible with the C++ reference implementation of {@code komihash}
   * defined in <a
   * href="https://github.com/avaneev/komihash/blob/3f5ff057be1f4738e21b2d225c9d34cc089524bd/komihash.h">komihash.h</a>
   * on an Intel x86 architecture.
   *
   * <p>This implementation is also compatible with Komihash versions 5.10 and 5.28.
   *
   * @return a hasher instance
   */
  public static Hasher64 komihash5_0() {
    return Komihash5_0.create();
  }

  /**
   * Returns a {@link Hasher64} implementing the 64-bit Komihash (version 5.0) algorithm using the
   * given seed value.
   *
   * <p>This implementation is compatible with the C++ reference implementation of {@code komihash}
   * defined in <a
   * href="https://github.com/avaneev/komihash/blob/3f5ff057be1f4738e21b2d225c9d34cc089524bd/komihash.h">komihash.h</a>
   * on an Intel x86 architecture.
   *
   * <p>This implementation is also compatible with Komihash versions 5.10 and 5.28.
   *
   * @param seed a 64-bit seed
   * @return a hasher instance
   */
  public static Hasher64 komihash5_0(long seed) {
    return Komihash5_0.create(seed);
  }

  /**
   * Returns a {@link Hasher64} implementing the 64-bit PolymurHash (version 2.0) algorithm using
   * the given tweak and seed value.
   *
   * <p>This implementation is compatible with the C++ reference implementation of {@code
   * polymur_hash} defined in <a
   * href="https://github.com/orlp/polymur-hash/blob/c6cc6884459560443e696604e9db3b6bb61a9bfa/polymur-hash.h">polymur-hash.h</a>
   * on an Intel x86 architecture.
   *
   * @param tweak a 64-bit tweak
   * @param seed a 64-bit seed
   * @return a hasher instance
   */
  public static Hasher64 polymurHash2_0(long tweak, long seed) {
    return PolymurHash2_0.create(tweak, seed);
  }

  /**
   * Returns a {@link Hasher64} implementing the 64-bit PolymurHash (version 2.0) algorithm using
   * the given tweak and seed values.
   *
   * <p>This implementation is compatible with the C++ reference implementation of {@code
   * polymur_hash} defined in <a
   * href="https://github.com/orlp/polymur-hash/blob/c6cc6884459560443e696604e9db3b6bb61a9bfa/polymur-hash.h">polymur-hash.h</a>
   * on an Intel x86 architecture.
   *
   * @param tweak a 64-bit tweak
   * @param kSeed a 64-bit kSeed
   * @param sSeed a 64-bit sSeed
   * @return a hasher instance
   */
  public static Hasher64 polymurHash2_0(long tweak, long kSeed, long sSeed) {
    return PolymurHash2_0.create(tweak, kSeed, sSeed);
  }

  /**
   * Returns a {@link Hasher64} implementing the 64-bit Wyhash (version final 3) algorithm using a
   * seed value of zero and the default secret.
   *
   * <p>This implementation is compatible with the C++ reference implementation of {@code wyhash}
   * defined in <a
   * href="https://github.com/wangyi-fudan/wyhash/blob/991aa3dab624e50b066f7a02ccc9f6935cc740ec/wyhash.h">wyhash.h</a>
   * on an Intel x86 architecture.
   *
   * <p>This function is superseded by {@link #wyhashFinal4} and should only be used if
   * compatibility is needed.
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
   * <p>This function is superseded by {@link #wyhashFinal4} and should only be used if
   * compatibility is needed.
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
   * <p>This function is superseded by {@link #wyhashFinal4} and should only be used if
   * compatibility is needed.
   *
   * @param seed a 64-bit seed
   * @param seedForSecret a 64-bit seed for secret generation
   * @return a hasher instance
   */
  public static Hasher64 wyhashFinal3(long seed, long seedForSecret) {
    return WyhashFinal3.create(seed, seedForSecret);
  }

  /**
   * Returns a {@link Hasher64} implementing the 64-bit Wyhash (version final 4) algorithm using a
   * seed value of zero and the default secret.
   *
   * <p>This implementation is compatible with the C++ reference implementation of {@code wyhash}
   * defined in <a
   * href="https://github.com/wangyi-fudan/wyhash/blob/ea3b25e1aef55d90f707c3a292eeb9162e2615d8/wyhash.h">wyhash.h</a>
   * on an Intel x86 architecture.
   *
   * @return a hasher instance
   */
  public static Hasher64 wyhashFinal4() {
    return WyhashFinal4.create();
  }

  /**
   * Returns a {@link Hasher64} implementing the 64-bit Wyhash (version final 4) algorithm using the
   * given seed value and the default secret.
   *
   * <p>This implementation is compatible with the C++ reference implementation of {@code wyhash}
   * defined in <a
   * href="https://github.com/wangyi-fudan/wyhash/blob/ea3b25e1aef55d90f707c3a292eeb9162e2615d8/wyhash.h">wyhash.h</a>
   * on an Intel x86 architecture.
   *
   * @param seed a 64-bit seed
   * @return a hasher instance
   */
  public static Hasher64 wyhashFinal4(long seed) {
    return WyhashFinal4.create(seed);
  }

  /**
   * Returns a {@link Hasher64} implementing the 64-bit Wyhash (version final 4) algorithm using the
   * given seed values.
   *
   * <p>This implementation is compatible with the C++ reference implementation of {@code wyhash}
   * and {@code make_secret} defined in <a
   * href="https://github.com/wangyi-fudan/wyhash/blob/ea3b25e1aef55d90f707c3a292eeb9162e2615d8/wyhash.h">wyhash.h</a>
   * on an Intel x86 architecture.
   *
   * @param seed a 64-bit seed
   * @param seedForSecret a 64-bit seed for secret generation
   * @return a hasher instance
   */
  public static Hasher64 wyhashFinal4(long seed, long seedForSecret) {
    return WyhashFinal4.create(seed, seedForSecret);
  }

  /**
   * Returns a {@link Hasher64} implementing the 64-bit FarmHashNa algorithm with default seed.
   *
   * <p>This implementation is compatible with the C++ reference implementation of {@code
   * farmhashna::Hash64} defined in <a
   * href="https://github.com/google/farmhash/blob/0d859a811870d10f53a594927d0d0b97573ad06d/src/farmhash.cc">farmhash.cc</a>
   * on an Intel x86 architecture.
   *
   * @return a hasher instance
   */
  public static Hasher64 farmHashNa() {
    return FarmHashNa.create();
  }

  /**
   * Returns a {@link Hasher64} implementing the 64-bit FarmHashNa algorithm using the given seed
   * value.
   *
   * <p>This implementation is compatible with the C++ reference implementation of {@code
   * farmhashna::Hash64WithSeed} defined in <a
   * href="https://github.com/google/farmhash/blob/0d859a811870d10f53a594927d0d0b97573ad06d/src/farmhash.cc">farmhash.cc</a>
   * on an Intel x86 architecture.
   *
   * @param seed the seed
   * @return a hasher instance
   */
  public static Hasher64 farmHashNa(long seed) {
    return FarmHashNa.create(seed);
  }

  /**
   * Returns a {@link Hasher64} implementing the 64-bit FarmHashNa algorithm using the given seed
   * values.
   *
   * <p>This implementation is compatible with the C++ reference implementation of {@code
   * farmhashna::Hash64WithSeeds} defined in <a
   * href="https://github.com/google/farmhash/blob/0d859a811870d10f53a594927d0d0b97573ad06d/src/farmhash.cc">farmhash.cc</a>
   * on an Intel x86 architecture.
   *
   * @param seed0 the first seed value
   * @param seed1 the second seed value
   * @return a hasher instance
   */
  public static Hasher64 farmHashNa(long seed0, long seed1) {
    return FarmHashNa.create(seed0, seed1);
  }

  /**
   * Returns a {@link Hasher64} implementing the 64-bit FarmHashUo algorithm with default seed.
   *
   * <p>This implementation is compatible with the C++ reference implementation of {@code
   * farmhashuo::Hash64} defined in <a
   * href="https://github.com/google/farmhash/blob/0d859a811870d10f53a594927d0d0b97573ad06d/src/farmhash.cc">farmhash.cc</a>
   * on an Intel x86 architecture.
   *
   * @return a hasher instance
   */
  public static Hasher64 farmHashUo() {
    return FarmHashUo.create();
  }

  /**
   * Returns a {@link Hasher64} implementing the 64-bit FarmHashUo algorithm using the given seed
   * value.
   *
   * <p>This implementation is compatible with the C++ reference implementation of {@code
   * farmhashuo::Hash64WithSeed} defined in <a
   * href="https://github.com/google/farmhash/blob/0d859a811870d10f53a594927d0d0b97573ad06d/src/farmhash.cc">farmhash.cc</a>
   * on an Intel x86 architecture.
   *
   * @param seed the seed
   * @return a hasher instance
   */
  public static Hasher64 farmHashUo(long seed) {
    return FarmHashUo.create(seed);
  }

  /**
   * Returns a {@link Hasher64} implementing the 64-bit FarmHashUo algorithm using the given seed
   * values.
   *
   * <p>This implementation is compatible with the C++ reference implementation of {@code
   * farmhashuo::Hash64WithSeeds} defined in <a
   * href="https://github.com/google/farmhash/blob/0d859a811870d10f53a594927d0d0b97573ad06d/src/farmhash.cc">farmhash.cc</a>
   * on an Intel x86 architecture.
   *
   * @param seed0 the first seed value
   * @param seed1 the second seed value
   * @return a hasher instance
   */
  public static Hasher64 farmHashUo(long seed0, long seed1) {
    return FarmHashUo.create(seed0, seed1);
  }

  /**
   * Returns a {@link Hasher64} implementing the 64-bit XXH3 algorithm.
   *
   * <p>This implementation is compatible with the C++ reference implementation of {@code
   * XXH3_64bits} defined in <a
   * href="https://github.com/Cyan4973/xxHash/blob/v0.8.3/xxhash.h">xxhash.h</a> on an Intel x86
   * architecture.
   *
   * @return a hasher instance
   */
  public static Hasher64 xxh3_64() {
    return XXH3_64.create();
  }

  /**
   * Returns a {@link Hasher64} implementing the 64-bit XXH3 algorithm using the given seed value.
   *
   * <p>This implementation is compatible with the C++ reference implementation of {@code
   * XXH3_64bits_withSeed} defined in <a
   * href="https://github.com/Cyan4973/xxHash/blob/v0.8.3/xxhash.h">xxhash.h</a> on an Intel x86
   * architecture.
   *
   * @param seed the seed value
   * @return a hasher instance
   */
  public static Hasher64 xxh3_64(long seed) {
    return XXH3_64.create(seed);
  }

  /**
   * Returns a {@link Hasher128} implementing the 128-bit XXH3 algorithm.
   *
   * <p>This implementation is compatible with the C++ reference implementation of {@code
   * XXH3_128bits} defined in <a
   * href="https://github.com/Cyan4973/xxHash/blob/v0.8.3/xxhash.h">xxhash.h</a> on an Intel x86
   * architecture.
   *
   * @return a hasher instance
   */
  public static Hasher128 xxh3_128() {
    return XXH3_128.create();
  }

  /**
   * Returns a {@link Hasher128} implementing the 128-bit XXH3 algorithm using the given seed value.
   *
   * <p>This implementation is compatible with the C++ reference implementation of {@code
   * XXH3_128bits_withSeed} defined in <a
   * href="https://github.com/Cyan4973/xxHash/blob/v0.8.3/xxhash.h">xxhash.h</a> on an Intel x86
   * architecture.
   *
   * @param seed the seed value
   * @return a hasher instance
   */
  public static Hasher128 xxh3_128(long seed) {
    return XXH3_128.create(seed);
  }

  /**
   * Returns a {@link Hasher32} implementing the 32-bit XXH32 algorithm using a seed value of zero.
   *
   * <p>This implementation is compatible with the C reference implementation of {@code XXH32}
   * defined in <a href="https://github.com/Cyan4973/xxHash/blob/v0.8.3/xxhash.h">xxhash.h</a> on an
   * Intel x86 architecture.
   *
   * @return a hasher instance
   */
  public static Hasher32 xxh32() {
    return XXH32.create();
  }

  /**
   * Returns a {@link Hasher32} implementing the 32-bit XXH32 algorithm using the given seed value.
   *
   * <p>This implementation is compatible with the C reference implementation of {@code XXH32}
   * defined in <a href="https://github.com/Cyan4973/xxHash/blob/v0.8.3/xxhash.h">xxhash.h</a> on an
   * Intel x86 architecture.
   *
   * @param seed a 32-bit seed
   * @return a hasher instance
   */
  public static Hasher32 xxh32(int seed) {
    return XXH32.create(seed);
  }

  /**
   * Returns a {@link Hasher64} implementing the 64-bit XXH64 algorithm using a seed value of zero.
   *
   * <p>This implementation is compatible with the C reference implementation of {@code XXH64}
   * defined in <a href="https://github.com/Cyan4973/xxHash/blob/v0.8.3/xxhash.h">xxhash.h</a> on an
   * Intel x86 architecture.
   *
   * @return a hasher instance
   */
  public static Hasher64 xxh64() {
    return XXH64.create();
  }

  /**
   * Returns a {@link Hasher64} implementing the 64-bit XXH64 algorithm using the given seed value.
   *
   * <p>This implementation is compatible with the C reference implementation of {@code XXH64}
   * defined in <a href="https://github.com/Cyan4973/xxHash/blob/v0.8.3/xxhash.h">xxhash.h</a> on an
   * Intel x86 architecture.
   *
   * @param seed a 64-bit seed
   * @return a hasher instance
   */
  public static Hasher64 xxh64(long seed) {
    return XXH64.create(seed);
  }

  /**
   * Returns a {@link Hasher64} implementing the 64-bit Rapidhash v3 (latest v3 release) algorithm
   * using a seed value of zero and the default secret.
   *
   * <p>IMPORTANT: Rapidhash v3 was re-released with a breaking change (see <a
   * href="https://github.com/dynatrace-oss/hash4j/issues/555">details</a>). This method implements
   * the latest v3 release and is not compatible with the first v3 release for input lengths of 5,
   * 6, and 7 bytes. However, it is compatible with both v3 releases for any other input lengths. If
   * you need compatibility with the first v3 release (because you also expect input lengths smaller
   * than 8 bytes) please see {@link #rapidhashV3Legacy()}. This function is not compatible with
   * {@code Hashing.rapidhash3()} that was available in Hash4j versions prior to v0.30.0.
   *
   * <p>This implementation is compatible with the C++ reference implementation of {@code rapidhash}
   * revision bc4b4baa defined in <a
   * href="https://github.com/Nicoshev/rapidhash/blob/bc4b4baa48a15ff52ff4725e1ccdcda62815221c/rapidhash.h#L506">rapidhash.h</a>
   * on an Intel x86 architecture.
   *
   * @return a hasher instance
   */
  public static Hasher64 rapidhashV3() {
    return RapidhashV3.create();
  }

  /**
   * Returns a {@link Hasher64} implementing the 64-bit Rapidhash v3 (latest v3 release) algorithm
   * using the given seed value and the default secret.
   *
   * <p>IMPORTANT: Rapidhash v3 was re-released with a breaking change (see <a
   * href="https://github.com/dynatrace-oss/hash4j/issues/555">details</a>). This method implements
   * the latest v3 release and is not compatible with the first v3 release for input lengths of 5,
   * 6, and 7 bytes. However, it is compatible with both v3 releases for any other input lengths. If
   * you need compatibility with the first v3 release (because you also expect input lengths smaller
   * than 8 bytes) please see {@link #rapidhashV3Legacy(long)}. This function is not compatible with
   * {@code Hashing.rapidhash3(long)} that was available in Hash4j versions prior to v0.30.0.
   *
   * <p>This implementation is compatible with the C++ reference implementation of {@code
   * rapidhash_withSeed} revision bc4b4baa defined in <a
   * href="https://github.com/Nicoshev/rapidhash/blob/bc4b4baa48a15ff52ff4725e1ccdcda62815221c/rapidhash.h#L492">rapidhash.h</a>
   * on an Intel x86 architecture.
   *
   * @param seed a 64-bit seed
   * @return a hasher instance
   */
  public static Hasher64 rapidhashV3(long seed) {
    return RapidhashV3.create(seed);
  }

  /**
   * Returns a {@link Hasher64} implementing the 64-bit Rapidhash v3 (first v3 release) algorithm
   * using a seed value of zero and the default secret.
   *
   * <p>IMPORTANT: Rapidhash v3 was re-released with a breaking change (see <a
   * href="https://github.com/dynatrace-oss/hash4j/issues/555">details</a>). This method implements
   * the first v3 release and is not compatible with the latest v3 release for input lengths of 5,
   * 6, and 7 bytes. However, it is compatible with both v3 releases for any other input lengths.
   * This method is not recommended for use and will likely be removed in a future release. If you
   * cannot switch to {@link #rapidhashV3()}, because you expect input lengths smaller than 8 bytes,
   * please consider moving the corresponding code to your own code base. This method corresponds to
   * {@code Hashing.rapidhash3()} that was available in Hash4j versions prior to v0.30.0.
   *
   * <p>This implementation is compatible with the C++ reference implementation of {@code rapidhash}
   * revision bbaf1a70 defined in <a
   * href="https://github.com/Nicoshev/rapidhash/blob/bbaf1a70775b785f11dab29dc7d9bd717b4eb6a6/rapidhash.h#L506">rapidhash.h</a>
   * on an Intel x86 architecture.
   *
   * @return a hasher instance
   */
  @Deprecated
  public static Hasher64 rapidhashV3Legacy() {
    return RapidhashV3Legacy.create();
  }

  /**
   * Returns a {@link Hasher64} implementing the 64-bit Rapidhash v3 (first v3 release) algorithm
   * using the given seed value and the default secret.
   *
   * <p>IMPORTANT: Rapidhash v3 was re-released with a breaking change (see <a
   * href="https://github.com/dynatrace-oss/hash4j/issues/555">details</a>). This method implements
   * the first v3 release and is not compatible with the latest v3 release for input lengths of 5,
   * 6, and 7 bytes. However, it is compatible with both v3 releases for any other input lengths.
   * This method is not recommended for use and will likely be removed in a future release. If you
   * cannot switch to {@link #rapidhashV3(long)}, because you expect input lengths smaller than 8
   * bytes, please consider moving the corresponding code to your own code base. This method
   * corresponds to {@code Hashing.rapidhash3(long)} that was available in Hash4j versions prior to
   * v0.30.0.
   *
   * <p>This implementation is compatible with the C++ reference implementation of {@code
   * rapidhash_withSeed} revision bbaf1a70 defined in <a
   * href="https://github.com/Nicoshev/rapidhash/blob/bbaf1a70775b785f11dab29dc7d9bd717b4eb6a6/rapidhash.h#L492">rapidhash.h</a>
   * on an Intel x86 architecture.
   *
   * @param seed a 64-bit seed
   * @return a hasher instance
   */
  @Deprecated
  public static Hasher64 rapidhashV3Legacy(long seed) {
    return RapidhashV3Legacy.create(seed);
  }

  /**
   * Returns a {@link Hasher64} implementing the 64-bit ChibiHash v2 algorithm using the given seed
   * value.
   *
   * <p>This implementation is compatible with the C++ reference implementation of {@code
   * chibihash64} defined in <a
   * href="https://github.com/N-R-K/ChibiHash/blob/0ae6a3cc36f40ba1073d29c91496caa6510dc2ef/chibihash64.h#L27">chibihash64.h</a>
   * on an Intel x86 architecture.
   *
   * @param seed a 64-bit seed
   * @return a hasher instance
   */
  public static Hasher64 chibiHash2(long seed) {
    return ChibiHash2.create(seed);
  }

  /**
   * Returns a {@link Hasher64} implementing the 64-bit MetroHash algorithm using a seed value of
   * zero.
   *
   * <p>This implementation is compatible with the C++ reference implementation of {@code
   * MetroHash64::Hash} defined in <a
   * href="https://github.com/jandrewrogers/MetroHash/blob/master/src/metrohash64.cpp">metrohash64.cpp</a>
   * on an Intel x86 architecture.
   *
   * @return a hasher instance
   */
  public static Hasher64 metroHash64() {
    return MetroHash64.create();
  }

  /**
   * Returns a {@link Hasher64} implementing the 64-bit MetroHash algorithm using the given seed
   * value.
   *
   * <p>This implementation is compatible with the C++ reference implementation of {@code
   * MetroHash64::Hash} defined in <a
   * href="https://github.com/jandrewrogers/MetroHash/blob/master/src/metrohash64.cpp">metrohash64.cpp</a>
   * on an Intel x86 architecture.
   *
   * @param seed a 64-bit seed
   * @return a hasher instance
   */
  public static Hasher64 metroHash64(long seed) {
    return MetroHash64.create(seed);
  }

  /**
   * Returns a {@link Hasher128} implementing the 128-bit MetroHash algorithm using a seed value of
   * zero.
   *
   * <p>This implementation is compatible with the C++ reference implementation of {@code
   * MetroHash128::Hash} defined in <a
   * href="https://github.com/jandrewrogers/MetroHash/blob/master/src/metrohash128.cpp">metrohash128.cpp</a>
   * on an Intel x86 architecture.
   *
   * @return a hasher instance
   */
  public static Hasher128 metroHash128() {
    return MetroHash128.create();
  }

  /**
   * Returns a {@link Hasher128} implementing the 128-bit MetroHash algorithm using the given seed
   * value.
   *
   * <p>This implementation is compatible with the C++ reference implementation of {@code
   * MetroHash128::Hash} defined in <a
   * href="https://github.com/jandrewrogers/MetroHash/blob/master/src/metrohash128.cpp">metrohash128.cpp</a>
   * on an Intel x86 architecture.
   *
   * @param seed a 64-bit seed
   * @return a hasher instance
   */
  public static Hasher128 metroHash128(long seed) {
    return MetroHash128.create(seed);
  }
}
