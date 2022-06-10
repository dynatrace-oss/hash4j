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
package com.dynatrace.hash4j.distinctcount;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.zip.Deflater;
import org.assertj.core.data.Offset;
import org.assertj.core.data.Percentage;
import org.junit.jupiter.api.Test;

public class UltraLogLogTest {

  @Test
  void testEmpty() {
    UltraLogLog sketch = UltraLogLog.create(10);
    assertThat(sketch.getDistinctCountEstimate()).isEqualTo(0.);
  }

  @Test
  void testDistinctCountEstimation() {
    int minP = 6;
    int maxP = 14;
    SplittableRandom random = new SplittableRandom(0xd77b9e4ea99553e0L);
    long[] distinctCounts = TestUtils.getDistinctCountValues(0, 100000, 0.2);

    for (int p = minP; p <= maxP; ++p) {
      testDistinctCountEstimation(p, random.nextLong(), distinctCounts);
    }
  }

  @Test
  void testRelativeStandardErrorAgainstConstants() {
    assertThat(UltraLogLog.calculateTheoreticalRelativeStandardError(3))
        .isCloseTo(0.2815233995369789, Percentage.withPercentage(1e-8));
    assertThat(UltraLogLog.calculateTheoreticalRelativeStandardError(4))
        .isCloseTo(0.19906710487528756, Percentage.withPercentage(1e-8));
    assertThat(UltraLogLog.calculateTheoreticalRelativeStandardError(5))
        .isCloseTo(0.14076169976848946, Percentage.withPercentage(1e-8));
    assertThat(UltraLogLog.calculateTheoreticalRelativeStandardError(6))
        .isCloseTo(0.09953355243764378, Percentage.withPercentage(1e-8));
    assertThat(UltraLogLog.calculateTheoreticalRelativeStandardError(7))
        .isCloseTo(0.07038084988424473, Percentage.withPercentage(1e-8));
    assertThat(UltraLogLog.calculateTheoreticalRelativeStandardError(8))
        .isCloseTo(0.04976677621882189, Percentage.withPercentage(1e-8));
    assertThat(UltraLogLog.calculateTheoreticalRelativeStandardError(9))
        .isCloseTo(0.035190424942122364, Percentage.withPercentage(1e-8));
    assertThat(UltraLogLog.calculateTheoreticalRelativeStandardError(10))
        .isCloseTo(0.024883388109410945, Percentage.withPercentage(1e-8));
    assertThat(UltraLogLog.calculateTheoreticalRelativeStandardError(11))
        .isCloseTo(0.017595212471061182, Percentage.withPercentage(1e-8));
    assertThat(UltraLogLog.calculateTheoreticalRelativeStandardError(12))
        .isCloseTo(0.012441694054705472, Percentage.withPercentage(1e-8));
    assertThat(UltraLogLog.calculateTheoreticalRelativeStandardError(13))
        .isCloseTo(0.008797606235530591, Percentage.withPercentage(1e-8));
    assertThat(UltraLogLog.calculateTheoreticalRelativeStandardError(14))
        .isCloseTo(0.006220847027352736, Percentage.withPercentage(1e-8));
    assertThat(UltraLogLog.calculateTheoreticalRelativeStandardError(15))
        .isCloseTo(0.0043988031177652955, Percentage.withPercentage(1e-8));
    assertThat(UltraLogLog.calculateTheoreticalRelativeStandardError(16))
        .isCloseTo(0.003110423513676368, Percentage.withPercentage(1e-8));
    assertThat(UltraLogLog.calculateTheoreticalRelativeStandardError(17))
        .isCloseTo(0.0021994015588826478, Percentage.withPercentage(1e-8));
    assertThat(UltraLogLog.calculateTheoreticalRelativeStandardError(18))
        .isCloseTo(0.001555211756838184, Percentage.withPercentage(1e-8));
    assertThat(UltraLogLog.calculateTheoreticalRelativeStandardError(19))
        .isCloseTo(0.0010997007794413239, Percentage.withPercentage(1e-8));
    assertThat(UltraLogLog.calculateTheoreticalRelativeStandardError(20))
        .isCloseTo(7.77605878419092E-4, Percentage.withPercentage(1e-8));
    assertThat(UltraLogLog.calculateTheoreticalRelativeStandardError(21))
        .isCloseTo(5.498503897206619E-4, Percentage.withPercentage(1e-8));
    assertThat(UltraLogLog.calculateTheoreticalRelativeStandardError(22))
        .isCloseTo(3.88802939209546E-4, Percentage.withPercentage(1e-8));
    assertThat(UltraLogLog.calculateTheoreticalRelativeStandardError(23))
        .isCloseTo(2.7492519486033097E-4, Percentage.withPercentage(1e-8));
    assertThat(UltraLogLog.calculateTheoreticalRelativeStandardError(24))
        .isCloseTo(1.94401469604773E-4, Percentage.withPercentage(1e-8));
    assertThat(UltraLogLog.calculateTheoreticalRelativeStandardError(25))
        .isCloseTo(1.3746259743016549E-4, Percentage.withPercentage(1e-8));
    assertThat(UltraLogLog.calculateTheoreticalRelativeStandardError(26))
        .isCloseTo(9.72007348023865E-5, Percentage.withPercentage(1e-8));
  }

  @Test
  void testRelativeStandardErrorAgainstFormula() {
    for (int p = 3; p <= 26; ++p) {
      assertThat(UltraLogLog.calculateTheoreticalRelativeStandardError(p))
          .isCloseTo(calculateRelativeStandardError(p), Percentage.withPercentage(1e-8));
    }
  }

  private static double calculateRelativeStandardError(int p) {
    double s = 0;
    double z = 5. / 4.;
    for (int t = 1; t <= 2; ++t) {
      s += 2. * Math.log(2) * Math.pow(z, 2) * Math.pow(2., -t) / Math.pow(z + Math.pow(2., -t), 2);
    }
    s += 2 * Math.log(2.) * Math.pow(2., -2);
    s += Math.log(2);
    s -= 1;
    return Math.sqrt(s * Math.pow(2., -p));
  }

  private void testDistinctCountEstimation(int p, long seed, long[] distinctCounts) {

    double relativeStandardError = UltraLogLog.calculateTheoreticalRelativeStandardError(p);

    double[] estimationErrorsMoment1 = new double[distinctCounts.length];
    double[] estimationErrorsMoment2 = new double[distinctCounts.length];
    int numIterations = 1000;
    SplittableRandom random = new SplittableRandom(seed);
    for (int i = 0; i < numIterations; ++i) {
      UltraLogLog sketch = UltraLogLog.create(p);
      long trueDistinctCount = 0;
      int distinctCountIndex = 0;
      while (distinctCountIndex < distinctCounts.length) {
        if (trueDistinctCount == distinctCounts[distinctCountIndex]) {
          double distinctCountEstimationErrror =
              sketch.getDistinctCountEstimate() - trueDistinctCount;
          estimationErrorsMoment1[distinctCountIndex] += distinctCountEstimationErrror;
          estimationErrorsMoment2[distinctCountIndex] +=
              distinctCountEstimationErrror * distinctCountEstimationErrror;
          distinctCountIndex += 1;
        }
        sketch.add(random.nextLong());
        trueDistinctCount += 1;
      }
    }

    for (int distinctCountIndex = 0;
        distinctCountIndex < distinctCounts.length;
        ++distinctCountIndex) {
      long trueDistinctCount = distinctCounts[distinctCountIndex];
      double relativeBias =
          estimationErrorsMoment1[distinctCountIndex]
              / (trueDistinctCount * (double) numIterations);
      double relativeRootMeanSquareError =
          Math.sqrt(estimationErrorsMoment2[distinctCountIndex] / numIterations)
              / trueDistinctCount;
      //      System.out.println("p = " + p +
      //              "; true distinct count = "
      //                      + trueDistinctCount
      //                      + "; relative bias = "
      //                      + relativeBias
      //                      + "; relative root mean square error = "
      //                      + relativeRootMeanSquareError);

      if (trueDistinctCount > 0) {
        // verify bias to be significantly smaller than standard error
        assertThat(relativeBias).isLessThan(relativeStandardError * 0.2);
      }
      if (trueDistinctCount > 0) {
        // test if observed root mean square error is not much greater than relative standard error
        assertThat(relativeRootMeanSquareError).isLessThan(relativeStandardError * 1.15);
      }
      if (trueDistinctCount > 10 * (1L << p)) {
        // test asymptotic behavior (distinct count is much greater than number of registers (state
        // size) given by (1 << p)
        // observed root mean square error should be approximately equal to the standard error
        assertThat(relativeRootMeanSquareError)
            .isCloseTo(relativeStandardError, Percentage.withPercentage(15));
      }
    }
  }

  @Test
  void testPrefixConversion() {
    assertEquals((byte) 8, UltraLogLog.hashPrefixToRegister(0x4L));
    assertEquals((byte) 9, UltraLogLog.hashPrefixToRegister(0x5L));
    assertEquals((byte) 10, UltraLogLog.hashPrefixToRegister(0x6L));
    assertEquals((byte) 11, UltraLogLog.hashPrefixToRegister(0x7L));
    assertEquals((byte) 12, UltraLogLog.hashPrefixToRegister(0x8L));
    assertEquals((byte) 12, UltraLogLog.hashPrefixToRegister(0x9L));
    assertEquals((byte) 13, UltraLogLog.hashPrefixToRegister(0xAL));
    assertEquals((byte) 13, UltraLogLog.hashPrefixToRegister(0xBL));
    assertEquals((byte) 14, UltraLogLog.hashPrefixToRegister(12));
    assertEquals((byte) 44, UltraLogLog.hashPrefixToRegister(1L << (12 - 1)));
    assertEquals((byte) 48, UltraLogLog.hashPrefixToRegister(1L << 12));
    assertEquals((byte) 50, UltraLogLog.hashPrefixToRegister((1L << (12 - 1)) | (1L << (12))));
    assertEquals((byte) 52, UltraLogLog.hashPrefixToRegister(1L << (12 + 1)));
    assertEquals((byte) 252, UltraLogLog.hashPrefixToRegister(0x8000000000000000L));
    assertEquals((byte) 255, UltraLogLog.hashPrefixToRegister(0xFFFFFFFFFFFFFFFFL));

    assertEquals(0, UltraLogLog.registerToHashPrefix((byte) 0));
    assertEquals(0, UltraLogLog.registerToHashPrefix((byte) 4));
    assertEquals(5, UltraLogLog.registerToHashPrefix((byte) 9));
    assertEquals(6, UltraLogLog.registerToHashPrefix((byte) 10));
    assertEquals(7, UltraLogLog.registerToHashPrefix((byte) 11));
    assertEquals(8, UltraLogLog.registerToHashPrefix((byte) 12));
    assertEquals(10, UltraLogLog.registerToHashPrefix((byte) 13));
    assertEquals(12, UltraLogLog.registerToHashPrefix((byte) 14));
    assertEquals(1L << (12 - 1), UltraLogLog.registerToHashPrefix((byte) 44));
    assertEquals((1L << (12 - 1)) + (1L << (12 - 3)), UltraLogLog.registerToHashPrefix((byte) 45));
    assertEquals((1L << (12 - 1)) + (1L << (12 - 2)), UltraLogLog.registerToHashPrefix((byte) 46));
    assertEquals(
        (1L << (12 - 1)) + (1L << (12 - 2)) + (1L << (12 - 3)),
        UltraLogLog.registerToHashPrefix((byte) 47));
    assertEquals(0xE000000000000000L, UltraLogLog.registerToHashPrefix((byte) 255));
    for (int i = 8; i < 256; i += 1) {
      byte b = (byte) i;
      assertEquals(b, UltraLogLog.hashPrefixToRegister(UltraLogLog.registerToHashPrefix(b)));
    }
  }

  @Test
  void testCalculateRegisterContribution() {

    for (int i = 8; i < 256; i += 1) {
      double actual = UltraLogLog.calculateRegisterContribution((byte) i);
      double expected = Double.NaN;
      switch (i & 3) {
        case 0:
          expected = (1. / 4 + 1. / 8 + 1. / 16) * Math.pow(0.5, (i - 8) / 4);
          break;
        case 1:
          expected = (0. / 4 + 1. / 8 + 1. / 16) * Math.pow(0.5, (i - 8) / 4);
          break;
        case 2:
          expected = (1. / 4 + 0. / 8 + 1. / 16) * Math.pow(0.5, (i - 8) / 4);
          break;
        case 3:
          expected = (0. / 4 + 0. / 8 + 1. / 16) * Math.pow(0.5, (i - 8) / 4);
          break;
        default:
          fail();
      }
      assertEquals(expected, actual);
    }
  }

  @Test
  void testAddAndMerge() {
    SplittableRandom random = new SplittableRandom(0x11a73f21bb8ad8f6L);
    int[] pVals = {3, 4, 5, 6, 7, 8, 9, 10};
    long[] distinctCounts = {
      0, 1, 2, 4, 8, 16, 32, 64, 128, 256, 512, 1024, 2048, 4096, 8192, 16384
    };
    for (int p1 : pVals) {
      for (int p2 : pVals) {
        for (long distinctCount1 : distinctCounts) {
          for (long distinctCount2 : distinctCounts) {
            testAddAndMerge(p1, distinctCount1, p2, distinctCount2, random.nextLong());
          }
        }
      }
    }
  }

  @Test
  void testDownsize() {
    SplittableRandom random = new SplittableRandom(0x237846c7b27df6b4L);
    int[] pVals = {3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16};
    long[] distinctCounts = {
      0, 1, 2, 4, 8, 16, 32, 64, 128, 256, 512, 1024, 2048, 4096, 8192, 16384
    };
    for (int pOriginalIdx = 0; pOriginalIdx < pVals.length; ++pOriginalIdx) {
      for (int pDownsizedIdx = 0; pDownsizedIdx <= pOriginalIdx; ++pDownsizedIdx) {
        for (long distinctCount : distinctCounts) {
          testDownsize(pVals[pOriginalIdx], pVals[pDownsizedIdx], distinctCount, random.nextLong());
        }
      }
    }
  }

  private static void testAddAndMerge(
      int p1, long distinctCount1, int p2, long distinctCount2, long seed) {
    UltraLogLog sketch1a = UltraLogLog.create(p1);
    UltraLogLog sketch2a = UltraLogLog.create(p2);
    UltraLogLog sketch1b = UltraLogLog.create(p1);
    UltraLogLog sketch2b = UltraLogLog.create(p2);
    UltraLogLog sketchTotal = UltraLogLog.create(Math.min(p1, p2));
    SplittableRandom random = new SplittableRandom(seed);
    for (long i = 0; i < distinctCount1; ++i) {
      long hashValue = random.nextLong();
      sketch1a.add(hashValue);
      sketch1b.add(hashValue);
      sketchTotal.add(hashValue);
    }
    for (long i = 0; i < distinctCount2; ++i) {
      long hashValue = random.nextLong();
      sketch2a.add(hashValue);
      sketch2b.add(hashValue);
      sketchTotal.add(hashValue);
    }
    assertThat(UltraLogLog.merge(sketch1a, sketch2a).getState()).isEqualTo(sketchTotal.getState());
    if (p1 < p2) {
      sketch1a.add(sketch2a);
      assertThatThrownBy(() -> sketch2b.add(sketch1b)).isInstanceOf(IllegalArgumentException.class);
      assertThat(sketch1a.getState()).isEqualTo(sketchTotal.getState());
    } else if (p1 > p2) {
      sketch2a.add(sketch1a);
      assertThatThrownBy(() -> sketch1b.add(sketch2b)).isInstanceOf(IllegalArgumentException.class);
      assertThat(sketch2a.getState()).isEqualTo(sketchTotal.getState());
    } else {
      sketch1a.add(sketch2a);
      sketch2b.add(sketch1b);
      assertThat(sketch1a.getState()).isEqualTo(sketchTotal.getState());
      assertThat(sketch2b.getState()).isEqualTo(sketchTotal.getState());
    }
  }

  private static void testDownsize(int pOriginal, int pDownsized, long distinctCount, long seed) {
    SplittableRandom random = new SplittableRandom(seed);
    UltraLogLog sketchOrig = UltraLogLog.create(pOriginal);
    UltraLogLog sketchDownsized = UltraLogLog.create(pDownsized);

    for (long i = 0; i < distinctCount; ++i) {
      long hashValue = random.nextLong();
      sketchOrig.add(hashValue);
      sketchDownsized.add(hashValue);
    }
    assertThat(sketchOrig.downsize(pDownsized).getState()).isEqualTo(sketchDownsized.getState());
  }

  @Test
  void testIsUnsignedPowerOfTwo() {
    for (int exponent = 0; exponent < 32; exponent++) {
      assertTrue(UltraLogLog.isUnsignedPowerOfTwo(1 << exponent));
    }
    assertTrue(UltraLogLog.isUnsignedPowerOfTwo(0));
    for (int i = -1000; i < 0; ++i) {
      assertFalse(UltraLogLog.isUnsignedPowerOfTwo(i));
    }
    assertTrue(UltraLogLog.isUnsignedPowerOfTwo(Integer.MIN_VALUE));
    assertFalse(UltraLogLog.isUnsignedPowerOfTwo(Integer.MAX_VALUE));
  }

  @Test
  void testXi() {
    assertThat(UltraLogLog.xi(0.)).isEqualTo(0.);
    assertThat(UltraLogLog.xi(0.5)).isCloseTo(1.2814941480755806, Percentage.withPercentage(1e-8));
    assertThat(UltraLogLog.xi(1.)).isEqualTo(Double.POSITIVE_INFINITY);
    assertThat(UltraLogLog.xi(Double.NEGATIVE_INFINITY)).isEqualTo(0);
    assertThat(UltraLogLog.xi(Double.MIN_VALUE)).isCloseTo(0., Offset.offset(1e-200));
    assertThat(UltraLogLog.xi(Double.MAX_VALUE)).isEqualTo(Double.POSITIVE_INFINITY);
    assertThat(UltraLogLog.xi(-Double.MIN_VALUE)).isEqualTo(0.);
    assertThat(UltraLogLog.xi(Double.NaN)).isNaN();
    assertThat(UltraLogLog.xi(Math.nextDown(1.)))
        .isCloseTo(1.2994724158464226E16, Percentage.withPercentage(1e-8));
  }

  @Test
  void testWrapZeros() {
    for (int len = 8; len <= 1 << 26; len *= 2) {
      assertThat(UltraLogLog.wrap(new byte[len]).getDistinctCountEstimate()).isEqualTo(0.);
    }
  }

  @Test
  void testRandomStates() {
    int numCycles = 1000000;
    int minP = 3;
    int maxP = 8;
    SplittableRandom random = new SplittableRandom(0x822fa1dcf86f953eL);
    for (int i = 0; i < numCycles; ++i) {
      byte[] state1 = new byte[1 << random.nextInt(minP, maxP + 1)];
      byte[] state2 = new byte[1 << random.nextInt(minP, maxP + 1)];
      random.nextBytes(state1);
      random.nextBytes(state2);
      UltraLogLog sketch1 = UltraLogLog.wrap(state1);
      UltraLogLog sketch2 = UltraLogLog.wrap(state2);
      int newP1 = random.nextInt(minP, maxP + 1);
      int newP2 = random.nextInt(minP, maxP + 1);
      assertDoesNotThrow(() -> sketch1.getDistinctCountEstimate());
      assertDoesNotThrow(() -> sketch2.getDistinctCountEstimate());
      assertDoesNotThrow(() -> sketch1.copy());
      assertDoesNotThrow(() -> sketch2.copy());
      assertDoesNotThrow(() -> sketch1.downsize(newP1));
      assertDoesNotThrow(() -> sketch2.downsize(newP2));
      assertDoesNotThrow(() -> UltraLogLog.merge(sketch1, sketch2));
      if (sketch1.getP() <= sketch2.getP()) {
        assertDoesNotThrow(() -> sketch1.add(sketch2));
      } else {
        assertDoesNotThrow(() -> sketch2.add(sketch1));
      }
    }
  }

  @Test
  void testCreateIllegalArguments() {
    assertThatThrownBy(() -> UltraLogLog.create(-1)).isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> UltraLogLog.create(0)).isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> UltraLogLog.create(1)).isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> UltraLogLog.create(2)).isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> UltraLogLog.create(27)).isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> UltraLogLog.create(28)).isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void testDownsizeIllegalArguments() {
    UltraLogLog sketch = UltraLogLog.create(8);
    assertThatThrownBy(() -> sketch.downsize(-1)).isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> sketch.downsize(0)).isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> sketch.downsize(1)).isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> sketch.downsize(2)).isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> sketch.downsize(27)).isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> sketch.downsize(28)).isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void testWrapIllegalArguments() {
    assertThatThrownBy(() -> UltraLogLog.wrap(null)).isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> UltraLogLog.wrap(new byte[0]))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> UltraLogLog.wrap(new byte[1]))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> UltraLogLog.wrap(new byte[2]))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> UltraLogLog.wrap(new byte[3]))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> UltraLogLog.wrap(new byte[4]))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> UltraLogLog.wrap(new byte[5]))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> UltraLogLog.wrap(new byte[6]))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> UltraLogLog.wrap(new byte[7]))
        .isInstanceOf(IllegalArgumentException.class);
    assertDoesNotThrow(() -> UltraLogLog.wrap(new byte[8]));
    assertThatThrownBy(() -> UltraLogLog.wrap(new byte[9]))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> UltraLogLog.wrap(new byte[10]))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> UltraLogLog.wrap(new byte[11]))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> UltraLogLog.wrap(new byte[12]))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> UltraLogLog.wrap(new byte[13]))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> UltraLogLog.wrap(new byte[14]))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> UltraLogLog.wrap(new byte[15]))
        .isInstanceOf(IllegalArgumentException.class);
    assertDoesNotThrow(() -> UltraLogLog.wrap(new byte[16]));
    assertThatThrownBy(() -> UltraLogLog.wrap(new byte[17]))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> UltraLogLog.wrap(new byte[(1 << 26) - 1]))
        .isInstanceOf(IllegalArgumentException.class);
    assertDoesNotThrow(() -> UltraLogLog.wrap(new byte[(1 << 26)]));
    assertThatThrownBy(() -> UltraLogLog.wrap(new byte[(1 << 26) + 1]))
        .isInstanceOf(IllegalArgumentException.class);
  }

  private static byte[] compress(byte[] data) throws IOException {
    try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
      Deflater deflater = new Deflater();
      deflater.setInput(data);
      deflater.finish();
      byte[] buffer = new byte[1024];
      while (!deflater.finished()) {
        outputStream.write(buffer, 0, deflater.deflate(buffer));
      }
      return outputStream.toByteArray();
    }
  }

  @Test
  void testCompressedStorageFactors() throws IOException {
    int numCycles = 100;
    long trueDistinctCount = 100000;
    int p = 12;
    long sumSizeUltraLogLog = 0;

    SplittableRandom random = new SplittableRandom(0L);
    for (int i = 0; i < numCycles; ++i) {
      UltraLogLog ultraLogLogSketch = UltraLogLog.create(p);

      for (long k = 0; k < trueDistinctCount; ++k) {
        long hash = random.nextLong();
        ultraLogLogSketch.add(hash);
      }
      sumSizeUltraLogLog += compress(ultraLogLogSketch.getState()).length;
    }

    double expectedVarianceUltraLogLog =
        Math.pow(UltraLogLog.calculateTheoreticalRelativeStandardError(p), 2);

    double storageFactorUltraLogLog =
        8. * sumSizeUltraLogLog * expectedVarianceUltraLogLog / numCycles;

    assertThat(storageFactorUltraLogLog)
        .isCloseTo(2.9694654659811044, Percentage.withPercentage(1));
  }

  @Test
  void testReset() {
    UltraLogLog sketch = UltraLogLog.create(12);
    SplittableRandom random = new SplittableRandom(0L);
    for (int i = 0; i < 10000; ++i) {
      sketch.add(random.nextLong());
    }
    assertThat(sketch.getDistinctCountEstimate()).isGreaterThan(1000);
    sketch.reset();
    assertThat(sketch.getDistinctCountEstimate()).isEqualTo(0.);
    assertThat(sketch.getState()).containsOnly(0);
  }

  private static void testErrorOfDistinctCountEqualOne(int p) {
    double m = 1 << p;
    UltraLogLog sketch = UltraLogLog.create(p);
    double sumProbabiltiy = 0;
    double averageEstimate = 0;
    double averageRMSE = 0;
    double trueDistinctCount = 1;
    for (int nlz = 0; nlz < 64 - p; ++nlz) {
      long hash1 = 0xFFFFFFFFFFFFFFFFL >>> p >>> nlz;
      sketch.reset().add(hash1);
      double probability = Math.pow(0.5, nlz + 1);
      sumProbabiltiy += probability;
      double estimate = sketch.getDistinctCountEstimate();
      averageEstimate += probability * estimate;
      averageRMSE += probability * Math.pow(estimate - trueDistinctCount, 2);
    }

    double relativeBias = (averageEstimate - trueDistinctCount) / trueDistinctCount;
    double relativeRMSE = Math.sqrt(averageRMSE) / trueDistinctCount;
    double theoreticalRelativeStandardError =
        UltraLogLog.calculateTheoreticalRelativeStandardError(p);
    assertThat(sumProbabiltiy).isCloseTo(1., Offset.offset(1e-6));
    assertThat(relativeBias).isLessThan(theoreticalRelativeStandardError * 0.3);
    assertThat(relativeRMSE).isLessThan(theoreticalRelativeStandardError * 0.7);
  }

  private static void testErrorOfDistinctCountEqualTwo(int p) {
    double m = 1 << p;
    UltraLogLog sketch = UltraLogLog.create(p);
    double sumProbabiltiy = 0;
    double averageEstimate = 0;
    double averageRMSE = 0;
    double trueDistinctCount = 2;
    long regMask1 = 0x0000000000000000L;
    long regMask2 = 0x8000000000000000L;
    for (int nlz1 = 0; nlz1 < 64 - p; ++nlz1) {
      for (int nlz2 = nlz1; nlz2 < 64 - p; ++nlz2) {
        long hash1 = 0xFFFFFFFFFFFFFFFFL >>> p >>> nlz1;
        long hash2 = 0xFFFFFFFFFFFFFFFFL >>> p >>> nlz2;
        {
          sketch.reset().add(hash1 | regMask1).add(hash2 | regMask1);
          double probability = 1. / m * Math.pow(0.5, nlz1 + nlz2 + 2);
          if (nlz1 != nlz2) probability *= 2;
          sumProbabiltiy += probability;
          double estimate = sketch.getDistinctCountEstimate();
          averageEstimate += probability * estimate;
          averageRMSE += probability * Math.pow(estimate - trueDistinctCount, 2);
        }
        {
          sketch.reset().add(hash1 | regMask1).add(hash2 | regMask2);
          double probability = (m - 1) / m * Math.pow(0.5, nlz1 + nlz2 + 2);
          if (nlz1 != nlz2) probability *= 2;
          sumProbabiltiy += probability;
          double estimate = sketch.getDistinctCountEstimate();
          averageEstimate += probability * estimate;
          averageRMSE += probability * Math.pow(estimate - trueDistinctCount, 2);
        }
      }
    }

    double relativeBias = (averageEstimate - trueDistinctCount) / trueDistinctCount;
    double relativeRMSE = Math.sqrt(averageRMSE) / trueDistinctCount;
    double theoreticalRelativeStandardError =
        UltraLogLog.calculateTheoreticalRelativeStandardError(p);
    assertThat(sumProbabiltiy).isCloseTo(1., Offset.offset(1e-6));
    assertThat(relativeBias).isLessThan(theoreticalRelativeStandardError * 0.3);
    assertThat(relativeRMSE).isLessThan(theoreticalRelativeStandardError * 0.7);
  }

  private static void testErrorOfDistinctCountEqualThree(int p) {
    double m = 1 << p;
    UltraLogLog sketch = UltraLogLog.create(p);
    double sumProbabiltiy = 0;
    double averageEstimate = 0;
    double averageRMSE = 0;
    double trueDistinctCount = 3;
    long regMask1 = 0x0000000000000000L;
    long regMask2 = 0x8000000000000000L;
    long regMask3 = 0x4000000000000000L;
    for (int nlz1 = 0; nlz1 < 64 - p; ++nlz1) {
      for (int nlz2 = nlz1; nlz2 < 64 - p; ++nlz2) {
        for (int nlz3 = 0; nlz3 < 64 - p; ++nlz3) {
          long hash1 = 0xFFFFFFFFFFFFFFFFL >>> p >>> nlz1;
          long hash2 = 0xFFFFFFFFFFFFFFFFL >>> p >>> nlz2;
          long hash3 = 0xFFFFFFFFFFFFFFFFL >>> p >>> nlz3;
          {
            sketch.reset().add(hash1 | regMask1).add(hash2 | regMask1).add(hash3 | regMask1);
            double probability = 1. / (m * m) * Math.pow(0.5, nlz1 + nlz2 + nlz3 + 3);
            if (nlz1 != nlz2) probability *= 2;
            sumProbabiltiy += probability;
            double estimate = sketch.getDistinctCountEstimate();
            averageEstimate += probability * estimate;
            averageRMSE += probability * Math.pow(estimate - trueDistinctCount, 2);
          }
          {
            sketch.reset().add(hash1 | regMask1).add(hash2 | regMask1).add(hash3 | regMask2);
            double probability = 3. * (m - 1) / (m * m) * Math.pow(0.5, nlz1 + nlz2 + nlz3 + 3);
            if (nlz1 != nlz2) probability *= 2;
            sumProbabiltiy += probability;
            double estimate = sketch.getDistinctCountEstimate();
            averageEstimate += probability * estimate;
            averageRMSE += probability * Math.pow(estimate - trueDistinctCount, 2);
          }
          {
            sketch.reset().add(hash1 | regMask1).add(hash2 | regMask2).add(hash3 | regMask3);
            double probability =
                (m - 1) * (m - 2) / (m * m) * Math.pow(0.5, nlz1 + nlz2 + nlz3 + 3);
            if (nlz1 != nlz2) probability *= 2;
            sumProbabiltiy += probability;
            double estimate = sketch.getDistinctCountEstimate();
            averageEstimate += probability * estimate;
            averageRMSE += probability * Math.pow(estimate - trueDistinctCount, 2);
          }
        }
      }
    }

    double relativeBias = (averageEstimate - trueDistinctCount) / trueDistinctCount;
    double relativeRMSE = Math.sqrt(averageRMSE) / trueDistinctCount;
    double theoreticalRelativeStandardError =
        UltraLogLog.calculateTheoreticalRelativeStandardError(p);
    assertThat(sumProbabiltiy).isCloseTo(1., Offset.offset(1e-6));
    assertThat(relativeBias).isLessThan(theoreticalRelativeStandardError * 0.3);
    assertThat(relativeRMSE).isLessThan(theoreticalRelativeStandardError * 0.8);
  }

  @Test
  void testVarianceOfDistinctCountEqualOne() {
    testErrorOfDistinctCountEqualOne(3);
    testErrorOfDistinctCountEqualOne(4);
    testErrorOfDistinctCountEqualOne(5);
    testErrorOfDistinctCountEqualOne(6);
    testErrorOfDistinctCountEqualOne(7);
    testErrorOfDistinctCountEqualOne(8);
    testErrorOfDistinctCountEqualOne(9);
    testErrorOfDistinctCountEqualOne(10);
    testErrorOfDistinctCountEqualOne(11);
    testErrorOfDistinctCountEqualOne(12);
    testErrorOfDistinctCountEqualOne(13);
    testErrorOfDistinctCountEqualOne(14);
    testErrorOfDistinctCountEqualOne(15);
    testErrorOfDistinctCountEqualOne(16);
    testErrorOfDistinctCountEqualOne(17);
    testErrorOfDistinctCountEqualOne(18);
    testErrorOfDistinctCountEqualOne(19);
    testErrorOfDistinctCountEqualOne(20);
    testErrorOfDistinctCountEqualOne(21);
    testErrorOfDistinctCountEqualOne(22);
    testErrorOfDistinctCountEqualOne(23);
    testErrorOfDistinctCountEqualOne(24);
    testErrorOfDistinctCountEqualOne(25);
    testErrorOfDistinctCountEqualOne(26);
  }

  @Test
  void testVarianceOfDistinctCountEqualTwo() {
    testErrorOfDistinctCountEqualTwo(3);
    testErrorOfDistinctCountEqualTwo(4);
    testErrorOfDistinctCountEqualTwo(5);
    testErrorOfDistinctCountEqualTwo(6);
    testErrorOfDistinctCountEqualTwo(7);
    testErrorOfDistinctCountEqualTwo(8);
    testErrorOfDistinctCountEqualTwo(9);
    testErrorOfDistinctCountEqualTwo(10);
    testErrorOfDistinctCountEqualTwo(11);
    testErrorOfDistinctCountEqualTwo(12);
    testErrorOfDistinctCountEqualTwo(13);
    testErrorOfDistinctCountEqualTwo(14);
    testErrorOfDistinctCountEqualTwo(15);
    testErrorOfDistinctCountEqualTwo(16);
    testErrorOfDistinctCountEqualTwo(17);
    testErrorOfDistinctCountEqualTwo(18);
    testErrorOfDistinctCountEqualTwo(19);
    testErrorOfDistinctCountEqualTwo(20);
  }

  @Test
  void testVarianceOfDistinctCountEqualThree() {
    testErrorOfDistinctCountEqualThree(3);
    testErrorOfDistinctCountEqualThree(4);
    testErrorOfDistinctCountEqualThree(5);
    testErrorOfDistinctCountEqualThree(6);
    testErrorOfDistinctCountEqualThree(7);
    testErrorOfDistinctCountEqualThree(8);
    testErrorOfDistinctCountEqualThree(9);
    testErrorOfDistinctCountEqualThree(10);
    testErrorOfDistinctCountEqualThree(11);
    testErrorOfDistinctCountEqualThree(12);
    testErrorOfDistinctCountEqualThree(13);
    testErrorOfDistinctCountEqualThree(14);
  }
}
