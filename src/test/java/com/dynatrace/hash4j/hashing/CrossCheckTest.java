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

import static com.dynatrace.hash4j.testutils.TestUtils.*;
import static org.assertj.core.api.Assertions.assertThat;

import com.appmattus.crypto.Algorithm;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.SplittableRandom;
import net.openhft.hashing.LongHashFunction;
import net.openhft.hashing.LongTupleHashFunction;
import org.apache.commons.codec.digest.MurmurHash3;
import org.greenrobot.essentials.hash.Murmur3A;
import org.greenrobot.essentials.hash.Murmur3F;
import org.junit.jupiter.api.Test;

class CrossCheckTest {

  private static final int MAX_LENGTH = 2000;
  private static final int NUM_ITERATIONS = 5;

  private static byte[] hashCryptoMurmurHash3_X64_128(int seed, byte[] data) {
    Constructor<Algorithm.MurmurHash3_X64_128> constructor;
    try {
      constructor = Algorithm.MurmurHash3_X64_128.class.getDeclaredConstructor(int.class);
    } catch (NoSuchMethodException e) {
      throw new RuntimeException(e);
    }
    constructor.setAccessible(true);

    byte[] hash;
    try {
      hash = constructor.newInstance(seed).hash(data);
    } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
      throw new RuntimeException(e);
    }
    return new byte[] {
      hash[7], hash[6], hash[5], hash[4], hash[3], hash[2], hash[1], hash[0], hash[15], hash[14],
      hash[13], hash[12], hash[11], hash[10], hash[9], hash[8]
    };
  }

  @Test
  void testFarmHashNaGuava() {
    SplittableRandom random = new SplittableRandom(0x722e4f85b4254fe8L);
    for (int len = 0; len < MAX_LENGTH; ++len) {
      byte[] b = new byte[len];
      for (int i = 0; i < NUM_ITERATIONS; ++i) {
        random.nextBytes(b);
        assertThat(com.google.common.hash.Hashing.farmHashFingerprint64().hashBytes(b).asLong())
            .isEqualTo(Hashing.farmHashNa().hashBytesToLong(b));
      }
    }
  }

  @Test
  void testFarmHashNaZeroAllocationHashing() {
    SplittableRandom random = new SplittableRandom(0xe2f6f169c7596086L);
    for (int len = 0; len < MAX_LENGTH; ++len) {
      byte[] b = new byte[len];
      for (int i = 0; i < NUM_ITERATIONS; ++i) {
        long seed0 = random.nextLong();
        long seed1 = random.nextLong();
        random.nextBytes(b);
        assertThat(LongHashFunction.farmNa().hashBytes(b))
            .isEqualTo(Hashing.farmHashNa().hashBytesToLong(b));
        assertThat(LongHashFunction.farmNa(seed0).hashBytes(b))
            .isEqualTo(Hashing.farmHashNa(seed0).hashBytesToLong(b));
        assertThat(LongHashFunction.farmNa(seed0, seed1).hashBytes(b))
            .isEqualTo(Hashing.farmHashNa(seed0, seed1).hashBytesToLong(b));
      }
    }
  }

  @Test
  void testFarmHashUoZeroAllocationHashing() {
    SplittableRandom random = new SplittableRandom(0xcd5e18290d533575L);
    for (int len = 0; len < MAX_LENGTH; ++len) {
      byte[] b = new byte[len];
      for (int i = 0; i < NUM_ITERATIONS; ++i) {
        long seed0 = random.nextLong();
        long seed1 = random.nextLong();
        random.nextBytes(b);
        assertThat(LongHashFunction.farmUo().hashBytes(b))
            .isEqualTo(Hashing.farmHashUo().hashBytesToLong(b));
        assertThat(LongHashFunction.farmUo(seed0).hashBytes(b))
            .isEqualTo(Hashing.farmHashUo(seed0).hashBytesToLong(b));
        assertThat(LongHashFunction.farmUo(seed0, seed1).hashBytes(b))
            .isEqualTo(Hashing.farmHashUo(seed0, seed1).hashBytesToLong(b));
      }
    }
  }

  @Test
  void testMurmur3_32Guava() {
    SplittableRandom random = new SplittableRandom(0x73cdedef7e62b59eL);
    for (int len = 0; len < MAX_LENGTH; ++len) {
      byte[] b = new byte[len];
      for (int i = 0; i < NUM_ITERATIONS; ++i) {
        int seed = random.nextInt();
        random.nextBytes(b);
        assertThat(com.google.common.hash.Hashing.murmur3_32_fixed().hashBytes(b).asInt())
            .isEqualTo(Hashing.murmur3_32().hashBytesToInt(b));
        assertThat(com.google.common.hash.Hashing.murmur3_32_fixed(seed).hashBytes(b).asInt())
            .isEqualTo(Hashing.murmur3_32(seed).hashBytesToInt(b));
      }
    }
  }

  @Test
  void testMurmur3_32ApacheCommonsCodec() {
    SplittableRandom random = new SplittableRandom(0x120f2310868c1197L);
    for (int len = 0; len < MAX_LENGTH; ++len) {
      byte[] b = new byte[len];
      for (int i = 0; i < NUM_ITERATIONS; ++i) {
        int seed = random.nextInt();
        random.nextBytes(b);
        assertThat(MurmurHash3.hash32x86(b, 0, b.length, 0))
            .isEqualTo(Hashing.murmur3_32().hashBytesToInt(b));
        assertThat(MurmurHash3.hash32x86(b, 0, b.length, seed))
            .isEqualTo(Hashing.murmur3_32(seed).hashBytesToInt(b));
      }
    }
  }

  @Test
  void testMurmur3_32GreenrobotEssentials() {
    SplittableRandom random = new SplittableRandom(0xa2912c4a88f4683aL);
    for (int len = 0; len < MAX_LENGTH; ++len) {
      byte[] b = new byte[len];
      for (int i = 0; i < NUM_ITERATIONS; ++i) {
        int seed = random.nextInt();
        random.nextBytes(b);

        Murmur3A murmur = new Murmur3A();
        murmur.update(b);
        assertThat((int) murmur.getValue()).isEqualTo(Hashing.murmur3_32().hashBytesToInt(b));

        Murmur3A murmurWithSeed = new Murmur3A(seed);
        murmurWithSeed.update(b);
        assertThat((int) murmurWithSeed.getValue())
            .isEqualTo(Hashing.murmur3_32(seed).hashBytesToInt(b));
      }
    }
  }

  @Test
  void testMurmur3_128ApacheCommonsCodec() {
    SplittableRandom random = new SplittableRandom(0x5f3c9ea4d9a8d55aL);
    for (int len = 0; len < MAX_LENGTH; ++len) {
      byte[] b = new byte[len];
      for (int i = 0; i < NUM_ITERATIONS; ++i) {
        int seed = random.nextInt();
        random.nextBytes(b);
        assertThat(tupleToByteArray(MurmurHash3.hash128x64(b, 0, b.length, 0)))
            .isEqualTo(Hashing.murmur3_128().hashBytesTo128Bits(b).toByteArray());
        assertThat(tupleToByteArray(MurmurHash3.hash128x64(b, 0, b.length, seed)))
            .isEqualTo(Hashing.murmur3_128(seed).hashBytesTo128Bits(b).toByteArray());
      }
    }
  }

  @Test
  void testMurmur3_128Crypto() {
    SplittableRandom random = new SplittableRandom(0x6a9c62b7c18051d5L);
    for (int len = 0; len < MAX_LENGTH; ++len) {
      byte[] b = new byte[len];
      for (int i = 0; i < NUM_ITERATIONS; ++i) {
        int seed = random.nextInt();
        random.nextBytes(b);
        assertThat(hashCryptoMurmurHash3_X64_128(0, b))
            .isEqualTo(Hashing.murmur3_128().hashBytesTo128Bits(b).toByteArray());
        assertThat(hashCryptoMurmurHash3_X64_128(seed, b))
            .isEqualTo(Hashing.murmur3_128(seed).hashBytesTo128Bits(b).toByteArray());
      }
    }
  }

  @Test
  void testMurmur3_128Guava() {
    SplittableRandom random = new SplittableRandom(0x8ad838d0cd7694afL);
    for (int len = 0; len < MAX_LENGTH; ++len) {
      byte[] b = new byte[len];
      for (int i = 0; i < NUM_ITERATIONS; ++i) {
        int seed = random.nextInt();
        random.nextBytes(b);
        assertThat(com.google.common.hash.Hashing.murmur3_128().hashBytes(b).asBytes())
            .isEqualTo(Hashing.murmur3_128().hashBytesTo128Bits(b).toByteArray());
        assertThat(
                com.google.common.hash.Hashing.murmur3_128()
                    .hashObject(b, (d, f) -> f.putBytes(d))
                    .asBytes())
            .isEqualTo(Hashing.murmur3_128().hashBytesTo128Bits(b).toByteArray());

        boolean isAffectedBySeedBug = seed >= 0; // see https://github.com/google/guava/issues/3493

        byte[] hash = Hashing.murmur3_128(seed).hashBytesTo128Bits(b).toByteArray();

        assertThat(
                Arrays.equals(
                    hash, com.google.common.hash.Hashing.murmur3_128(seed).hashBytes(b).asBytes()))
            .isEqualTo(isAffectedBySeedBug);
        assertThat(
                Arrays.equals(
                    hash,
                    com.google.common.hash.Hashing.murmur3_128(seed)
                        .hashObject(b, (d, f) -> f.putBytes(d))
                        .asBytes()))
            .isEqualTo(isAffectedBySeedBug);
      }
    }
  }

  @Test
  void testMurmur3_128ZeroAllocationHashing() {
    SplittableRandom random = new SplittableRandom(0xe095048b1ce9b64eL);
    for (int len = 0; len < MAX_LENGTH; ++len) {
      byte[] b = new byte[len];
      for (int i = 0; i < NUM_ITERATIONS; ++i) {
        int seed = random.nextInt();
        random.nextBytes(b);
        assertThat(tupleToByteArray(LongTupleHashFunction.murmur_3().hashBytes(b)))
            .isEqualTo(Hashing.murmur3_128().hashBytesTo128Bits(b).toByteArray());

        boolean isAffectedBySeedBug =
            seed >= 0; // see https://github.com/OpenHFT/Zero-Allocation-Hashing/issues/68

        byte[] hash = Hashing.murmur3_128(seed).hashBytesTo128Bits(b).toByteArray();
        assertThat(
                Arrays.equals(
                    hash, tupleToByteArray(LongTupleHashFunction.murmur_3(seed).hashBytes(b))))
            .isEqualTo(isAffectedBySeedBug);
      }
    }
  }

  @Test
  void testMurmur3_128GreenrobotEssentials() {
    SplittableRandom random = new SplittableRandom(0x7113276fae6d89ddL);
    for (int len = 0; len < MAX_LENGTH; ++len) {
      byte[] b = new byte[len];
      for (int i = 0; i < NUM_ITERATIONS; ++i) {
        int seed = random.nextInt();
        random.nextBytes(b);

        Murmur3F murmur = new Murmur3F();
        murmur.update(b);
        assertThat(murmur.getValueBytesLittleEndian())
            .isEqualTo(Hashing.murmur3_128().hashBytesTo128Bits(b).toByteArray());

        Murmur3F murmurWithSeed = new Murmur3F(seed);
        murmurWithSeed.update(b);
        assertThat(murmurWithSeed.getValueBytesLittleEndian())
            .isEqualTo(Hashing.murmur3_128(seed).hashBytesTo128Bits(b).toByteArray());
      }
    }
  }

  @Test
  void testMurmur3_128ComSanguptaMurmur3() {
    SplittableRandom random = new SplittableRandom(0x60679c14de1c0954L);
    for (int len = 0; len < MAX_LENGTH; ++len) {
      byte[] b = new byte[len];
      for (int i = 0; i < NUM_ITERATIONS; ++i) {
        int seed = random.nextInt();
        random.nextBytes(b);

        // copy data as workaround of https://github.com/sangupta/murmur/issues/7
        byte[] copy1 = Arrays.copyOf(b, b.length);
        byte[] copy2 = Arrays.copyOf(b, b.length);

        assertThat(tupleToByteArray(com.sangupta.murmur.Murmur3.hash_x64_128(copy1, b.length, 0)))
            .isEqualTo(Hashing.murmur3_128().hashBytesTo128Bits(b).toByteArray());
        assertThat(
                tupleToByteArray(
                    com.sangupta.murmur.Murmur3.hash_x64_128(copy2, b.length, seed & 0xFFFFFFFFL)))
            .isEqualTo(Hashing.murmur3_128(seed).hashBytesTo128Bits(b).toByteArray());
      }
    }
  }

  @Test
  void testXXH3ZeroAllocationHashing() {
    SplittableRandom random = new SplittableRandom(0x97c4eb4e39a52615L);
    for (int len = 0; len < MAX_LENGTH; ++len) {
      byte[] b = new byte[len];
      for (int i = 0; i < NUM_ITERATIONS; ++i) {
        long seed = random.nextLong();
        random.nextBytes(b);
        assertThat(LongHashFunction.xx3().hashBytes(b))
            .isEqualTo(Hashing.xxh3_64().hashBytesToLong(b));
        assertThat(LongHashFunction.xx3(seed).hashBytes(b))
            .isEqualTo(Hashing.xxh3_64(seed).hashBytesToLong(b));
      }
    }
  }

  @Test
  void testXXH3Crypto() {
    SplittableRandom random = new SplittableRandom(0xd51892c0663e06e1L);
    for (int len = 0; len < MAX_LENGTH; ++len) {
      byte[] b = new byte[len];
      for (int i = 0; i < NUM_ITERATIONS; ++i) {
        long seed = random.nextLong();
        random.nextBytes(b);
        assertThat(Long.reverseBytes(byteArrayToLong(new Algorithm.XXH3_64.Seeded(0L).hash(b))))
            .isEqualTo(Hashing.xxh3_64().hashBytesToLong(b));
        assertThat(Long.reverseBytes(byteArrayToLong(new Algorithm.XXH3_64.Seeded(seed).hash(b))))
            .isEqualTo(Hashing.xxh3_64(seed).hashBytesToLong(b));
      }
    }
  }
}
