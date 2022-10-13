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

import static com.dynatrace.hash4j.testutils.TestUtils.byteArrayToLong;
import static com.dynatrace.hash4j.testutils.TestUtils.tupleToByteArray;
import static org.assertj.core.api.Assertions.assertThat;

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

class CrossCheckTest {

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
    assertThat(
            com.google.common.hash.Hashing.farmHashFingerprint64().hashBytes(r.getData()).asLong())
        .isEqualTo(r.getHash0());
  }

  @ParameterizedTest
  @MethodSource("getFarmHashNaReferenceData")
  void testFarmHashNaZeroAllocationHashing(FarmHashNaReferenceData.ReferenceRecord r) {
    assertThat(LongHashFunction.farmNa().hashBytes(r.getData())).isEqualTo(r.getHash0());
    assertThat(LongHashFunction.farmNa(r.getSeed0()).hashBytes(r.getData()))
        .isEqualTo(r.getHash1());
    assertThat(LongHashFunction.farmNa(r.getSeed0(), r.getSeed1()).hashBytes(r.getData()))
        .isEqualTo(r.getHash2());
  }

  @ParameterizedTest
  @MethodSource("getFarmHashUoReferenceData")
  void testFarmHashUoZeroAllocationHashing(FarmHashUoReferenceData.ReferenceRecord r) {
    assertThat(LongHashFunction.farmUo().hashBytes(r.getData())).isEqualTo(r.getHash0());
    assertThat(LongHashFunction.farmUo(r.getSeed0()).hashBytes(r.getData()))
        .isEqualTo(r.getHash1());
    assertThat(LongHashFunction.farmUo(r.getSeed0(), r.getSeed1()).hashBytes(r.getData()))
        .isEqualTo(r.getHash2());
  }

  @ParameterizedTest
  @MethodSource("getMurmur3_32ReferenceData")
  void testMurmur3_32Guava(Murmur3_32ReferenceData.ReferenceRecord r) {
    assertThat(com.google.common.hash.Hashing.murmur3_32_fixed().hashBytes(r.getData()).asInt())
        .isEqualTo(r.getHash0());
    assertThat(
            com.google.common.hash.Hashing.murmur3_32_fixed(r.getSeed())
                .hashBytes(r.getData())
                .asInt())
        .isEqualTo(r.getHash1());
  }

  @ParameterizedTest
  @MethodSource("getMurmur3_32ReferenceData")
  void testMurmur3_32ApacheCommonsCodec(Murmur3_32ReferenceData.ReferenceRecord r) {
    assertThat(MurmurHash3.hash32x86(r.getData(), 0, r.getData().length, 0))
        .isEqualTo(r.getHash0());
    assertThat(MurmurHash3.hash32x86(r.getData(), 0, r.getData().length, r.getSeed()))
        .isEqualTo(r.getHash1());
  }

  @ParameterizedTest
  @MethodSource("getMurmur3_32ReferenceData")
  void testMurmur3_32GreenrobotEssentials(Murmur3_32ReferenceData.ReferenceRecord r) {
    Murmur3A murmur = new Murmur3A();
    murmur.update(r.getData());
    assertThat((int) murmur.getValue()).isEqualTo(r.getHash0());

    Murmur3A murmurWithSeed = new Murmur3A(r.getSeed());
    murmurWithSeed.update(r.getData());
    assertThat((int) murmurWithSeed.getValue()).isEqualTo(r.getHash1());
  }

  @ParameterizedTest
  @MethodSource("getMurmur3_128ReferenceData")
  void testMurmur3_128ApacheCommonsCodec(Murmur3_128ReferenceData.ReferenceRecord r) {
    assertThat(tupleToByteArray(MurmurHash3.hash128x64(r.getData(), 0, r.getData().length, 0)))
        .isEqualTo(r.getHash0());
    assertThat(
            tupleToByteArray(
                MurmurHash3.hash128x64(r.getData(), 0, r.getData().length, r.getSeed())))
        .isEqualTo(r.getHash1());
  }

  @ParameterizedTest
  @MethodSource("getMurmur3_128ReferenceData")
  void testMurmur3_128Crypto(Murmur3_128ReferenceData.ReferenceRecord r) {
    assertThat(hashCryptoMurmurHash3_X64_128(0, r.getData())).isEqualTo(r.getHash0());
    assertThat(hashCryptoMurmurHash3_X64_128(r.getSeed(), r.getData())).isEqualTo(r.getHash1());
  }

  @ParameterizedTest
  @MethodSource("getMurmur3_128ReferenceData")
  void testMurmur3_128Guava(Murmur3_128ReferenceData.ReferenceRecord r) {
    assertThat(com.google.common.hash.Hashing.murmur3_128().hashBytes(r.getData()).asBytes())
        .isEqualTo(r.getHash0());
    assertThat(
            com.google.common.hash.Hashing.murmur3_128()
                .hashObject(r.getData(), (b, f) -> f.putBytes(b))
                .asBytes())
        .isEqualTo(r.getHash0());

    boolean isAffectedBySeedBug =
        r.getSeed() >= 0; // see https://github.com/google/guava/issues/3493
    assertThat(
            Arrays.equals(
                r.getHash1(),
                com.google.common.hash.Hashing.murmur3_128(r.getSeed())
                    .hashBytes(r.getData())
                    .asBytes()))
        .isEqualTo(isAffectedBySeedBug);
    assertThat(
            Arrays.equals(
                r.getHash1(),
                com.google.common.hash.Hashing.murmur3_128(r.getSeed())
                    .hashObject(r.getData(), (b, f) -> f.putBytes(b))
                    .asBytes()))
        .isEqualTo(isAffectedBySeedBug);
  }

  @ParameterizedTest
  @MethodSource("getMurmur3_128ReferenceData")
  void testMurmur3_128ZeroAllocationHashing(Murmur3_128ReferenceData.ReferenceRecord r) {
    assertThat(tupleToByteArray(LongTupleHashFunction.murmur_3().hashBytes(r.getData())))
        .isEqualTo(r.getHash0());
    boolean isAffectedBySeedBug =
        r.getSeed() >= 0; // see https://github.com/OpenHFT/Zero-Allocation-Hashing/issues/68
    assertThat(
            Arrays.equals(
                r.getHash1(),
                tupleToByteArray(
                    LongTupleHashFunction.murmur_3(r.getSeed()).hashBytes(r.getData()))))
        .isEqualTo(isAffectedBySeedBug);
  }

  @ParameterizedTest
  @MethodSource("getMurmur3_128ReferenceData")
  void testMurmur3_128GreenrobotEssentials(Murmur3_128ReferenceData.ReferenceRecord r) {
    Murmur3F murmur = new Murmur3F();
    murmur.update(r.getData());
    assertThat(murmur.getValueBytesLittleEndian()).isEqualTo(r.getHash0());

    Murmur3F murmurWithSeed = new Murmur3F(r.getSeed());
    murmurWithSeed.update(r.getData());
    assertThat(murmurWithSeed.getValueBytesLittleEndian()).isEqualTo(r.getHash1());
  }

  @ParameterizedTest
  @MethodSource("getXXH3ReferenceData")
  void testXXH3ZeroAllocationHashing(XXH3ReferenceData.ReferenceRecord r) {
    assertThat(LongHashFunction.xx3().hashBytes(r.getData())).isEqualTo(r.getHash0());
    assertThat(LongHashFunction.xx3(r.getSeed()).hashBytes(r.getData())).isEqualTo(r.getHash1());
  }

  @ParameterizedTest
  @MethodSource("getXXH3ReferenceData")
  void testXXH3Crypto(XXH3ReferenceData.ReferenceRecord r) {
    assertThat(
            Long.reverseBytes(byteArrayToLong(new Algorithm.XXH3_64.Seeded(0L).hash(r.getData()))))
        .isEqualTo(r.getHash0());
    assertThat(
            Long.reverseBytes(
                byteArrayToLong(new Algorithm.XXH3_64.Seeded(r.getSeed()).hash(r.getData()))))
        .isEqualTo(r.getHash1());
  }
}
