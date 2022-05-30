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

import static com.dynatrace.hash4j.hashing.TestUtils.byteArrayToLong;
import static com.dynatrace.hash4j.hashing.TestUtils.tupleToByteArray;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.appmattus.crypto.Algorithm;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.List;
import net.openhft.hashing.LongHashFunction;
import net.openhft.hashing.LongTupleHashFunction;
import org.apache.commons.codec.digest.MurmurHash3;
import org.greenrobot.essentials.hash.Murmur3A;
import org.greenrobot.essentials.hash.Murmur3F;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

public class CrossCheckTest {

  private static List<FarmHashNaReferenceData.ReferenceRecord> getFarmHashNaReferenceData() {
    return FarmHashNaReferenceData.get();
  }

  private static List<FarmHashUoReferenceData.ReferenceRecord> getFarmHashUoReferenceData() {
    return FarmHashUoReferenceData.get();
  }

  private static List<Murmur3_32ReferenceData.ReferenceRecord> getMurmur3_32ReferenceData() {
    return Murmur3_32ReferenceData.get();
  }

  private static List<Murmur3_128ReferenceData.ReferenceRecord> getMurmur3_128ReferenceData() {
    return Murmur3_128ReferenceData.get();
  }

  private static List<XXH3ReferenceData.ReferenceRecord> getXXH3ReferenceData() {
    return XXH3ReferenceData.get();
  }

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

  @ParameterizedTest
  @MethodSource("getFarmHashNaReferenceData")
  void testFarmHashNaGuava(FarmHashNaReferenceData.ReferenceRecord r) {
    assertEquals(
        r.getHash0(),
        com.google.common.hash.Hashing.farmHashFingerprint64().hashBytes(r.getData()).asLong());
  }

  @ParameterizedTest
  @MethodSource("getFarmHashNaReferenceData")
  void testFarmHashNaZeroAllocationHashing(FarmHashNaReferenceData.ReferenceRecord r) {
    assertEquals(r.getHash0(), LongHashFunction.farmNa().hashBytes(r.getData()));
    assertEquals(r.getHash1(), LongHashFunction.farmNa(r.getSeed0()).hashBytes(r.getData()));
    assertEquals(
        r.getHash2(), LongHashFunction.farmNa(r.getSeed0(), r.getSeed1()).hashBytes(r.getData()));
  }

  @ParameterizedTest
  @MethodSource("getFarmHashUoReferenceData")
  void testFarmHashUoZeroAllocationHashing(FarmHashUoReferenceData.ReferenceRecord r) {
    assertEquals(r.getHash0(), LongHashFunction.farmUo().hashBytes(r.getData()));
    assertEquals(r.getHash1(), LongHashFunction.farmUo(r.getSeed0()).hashBytes(r.getData()));
    assertEquals(
        r.getHash2(), LongHashFunction.farmUo(r.getSeed0(), r.getSeed1()).hashBytes(r.getData()));
  }

  @ParameterizedTest
  @MethodSource("getMurmur3_32ReferenceData")
  void testMurmur3_32Guava(Murmur3_32ReferenceData.ReferenceRecord r) {
    assertEquals(
        r.getHash0(),
        com.google.common.hash.Hashing.murmur3_32_fixed().hashBytes(r.getData()).asInt());
    assertEquals(
        r.getHash1(),
        com.google.common.hash.Hashing.murmur3_32_fixed(r.getSeed())
            .hashBytes(r.getData())
            .asInt());
  }

  @ParameterizedTest
  @MethodSource("getMurmur3_32ReferenceData")
  void testMurmur3_32ApacheCommonsCodec(Murmur3_32ReferenceData.ReferenceRecord r) {
    assertEquals(r.getHash0(), MurmurHash3.hash32x86(r.getData(), 0, r.getData().length, 0));
    assertEquals(
        r.getHash1(), MurmurHash3.hash32x86(r.getData(), 0, r.getData().length, r.getSeed()));
  }

  @ParameterizedTest
  @MethodSource("getMurmur3_32ReferenceData")
  void testMurmur3_32GreenrobotEssentials(Murmur3_32ReferenceData.ReferenceRecord r) {
    Murmur3A murmur = new Murmur3A();
    murmur.update(r.getData());
    assertEquals(r.getHash0(), (int) murmur.getValue());

    Murmur3A murmurWithSeed = new Murmur3A(r.getSeed());
    murmurWithSeed.update(r.getData());
    assertEquals(r.getHash1(), (int) murmurWithSeed.getValue());
  }

  @ParameterizedTest
  @MethodSource("getMurmur3_128ReferenceData")
  void testMurmur3_128ApacheCommonsCodec(Murmur3_128ReferenceData.ReferenceRecord r) {
    assertArrayEquals(
        r.getHash0(),
        tupleToByteArray(MurmurHash3.hash128x64(r.getData(), 0, r.getData().length, 0)));
    assertArrayEquals(
        r.getHash1(),
        tupleToByteArray(MurmurHash3.hash128x64(r.getData(), 0, r.getData().length, r.getSeed())));
  }

  @ParameterizedTest
  @MethodSource("getMurmur3_128ReferenceData")
  void testMurmur3_128Crypto(Murmur3_128ReferenceData.ReferenceRecord r) {
    assertArrayEquals(r.getHash0(), hashCryptoMurmurHash3_X64_128(0, r.getData()));
    assertArrayEquals(r.getHash1(), hashCryptoMurmurHash3_X64_128(r.getSeed(), r.getData()));
  }

  @ParameterizedTest
  @MethodSource("getMurmur3_128ReferenceData")
  void testMurmur3_128Guava(Murmur3_128ReferenceData.ReferenceRecord r) {
    assertArrayEquals(
        r.getHash0(),
        com.google.common.hash.Hashing.murmur3_128().hashBytes(r.getData()).asBytes());
    assertArrayEquals(
        r.getHash0(),
        com.google.common.hash.Hashing.murmur3_128()
            .hashObject(r.getData(), (b, f) -> f.putBytes(b))
            .asBytes());

    boolean isAffectedBySeedBug =
        r.getSeed() >= 0; // see https://github.com/google/guava/issues/3493
    assertEquals(
        isAffectedBySeedBug,
        Arrays.equals(
            r.getHash1(),
            com.google.common.hash.Hashing.murmur3_128(r.getSeed())
                .hashBytes(r.getData())
                .asBytes()));
    assertEquals(
        isAffectedBySeedBug,
        Arrays.equals(
            r.getHash1(),
            com.google.common.hash.Hashing.murmur3_128(r.getSeed())
                .hashObject(r.getData(), (b, f) -> f.putBytes(b))
                .asBytes()));
  }

  @ParameterizedTest
  @MethodSource("getMurmur3_128ReferenceData")
  void testMurmur3_128ZeroAllocationHashing(Murmur3_128ReferenceData.ReferenceRecord r) {
    assertArrayEquals(
        r.getHash0(), tupleToByteArray(LongTupleHashFunction.murmur_3().hashBytes(r.getData())));
    boolean isAffectedBySeedBug =
        r.getSeed() >= 0; // see https://github.com/OpenHFT/Zero-Allocation-Hashing/issues/68
    assertEquals(
        isAffectedBySeedBug,
        Arrays.equals(
            r.getHash1(),
            tupleToByteArray(LongTupleHashFunction.murmur_3(r.getSeed()).hashBytes(r.getData()))));
  }

  @ParameterizedTest
  @MethodSource("getMurmur3_128ReferenceData")
  void testMurmur3_128GreenrobotEssentials(Murmur3_128ReferenceData.ReferenceRecord r) {
    Murmur3F murmur = new Murmur3F();
    murmur.update(r.getData());
    assertArrayEquals(r.getHash0(), murmur.getValueBytesLittleEndian());

    Murmur3F murmurWithSeed = new Murmur3F(r.getSeed());
    murmurWithSeed.update(r.getData());
    assertArrayEquals(r.getHash1(), murmurWithSeed.getValueBytesLittleEndian());
  }

  @ParameterizedTest
  @MethodSource("getXXH3ReferenceData")
  void testXXH3ZeroAllocationHashing(XXH3ReferenceData.ReferenceRecord r) {
    assertEquals(r.getHash0(), LongHashFunction.xx3().hashBytes(r.getData()));
    assertEquals(r.getHash1(), LongHashFunction.xx3(r.getSeed()).hashBytes(r.getData()));
  }

  @ParameterizedTest
  @MethodSource("getXXH3ReferenceData")
  void testXXH3Crypto(XXH3ReferenceData.ReferenceRecord r) {
    if (r.getData().length != 1
        && r.getData().length != 2
        && r.getData().length != 3) { // crypto has a bug affecting input lengths 1, 2, and 3
      assertEquals(
          r.getHash0(),
          Long.reverseBytes(byteArrayToLong(new Algorithm.XXH3_64.Seeded(0L).hash(r.getData()))));
      assertEquals(
          r.getHash1(),
          Long.reverseBytes(
              byteArrayToLong(new Algorithm.XXH3_64.Seeded(r.getSeed()).hash(r.getData()))));
    }
  }
}
