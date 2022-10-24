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

import static com.dynatrace.hash4j.distinctcount.UltraLogLog.TAU;
import static com.dynatrace.hash4j.testutils.TestUtils.compareWithMaxRelativeError;
import static java.lang.Math.exp;
import static java.lang.Math.pow;
import static org.assertj.core.api.Assertions.*;
import static org.assertj.core.data.Percentage.withPercentage;
import static org.hipparchus.special.Gamma.gamma;

import com.dynatrace.hash4j.hashing.HashStream;
import com.dynatrace.hash4j.hashing.Hashing;
import com.dynatrace.hash4j.random.PseudoRandomGenerator;
import com.dynatrace.hash4j.random.PseudoRandomGeneratorProvider;
import com.google.common.collect.Sets;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.zip.Deflater;
import org.assertj.core.data.Offset;
import org.hipparchus.optim.MaxEval;
import org.hipparchus.optim.nonlinear.scalar.GoalType;
import org.hipparchus.optim.univariate.*;
import org.junit.jupiter.api.Test;

class UltraLogLogTest {

  private static final int MIN_P = 3;
  private static final int MAX_P = 26;

  @Test
  void testEmpty() {
    UltraLogLog sketch = UltraLogLog.create(10);
    assertThat(sketch.getDistinctCountEstimate()).isZero();
  }

  @Test
  void testDistinctCountEstimation() {
    int maxP = 14;
    SplittableRandom random = new SplittableRandom(0xd77b9e4ea99553e0L);
    long[] distinctCounts = TestUtils.getDistinctCountValues(0, 100000, 0.2);

    for (int p = MIN_P; p <= maxP; ++p) {
      testDistinctCountEstimation(p, random.nextLong(), distinctCounts);
    }
  }

  @Test
  void testRelativeStandardErrorAgainstConstants() {
    double[] expected = {
      0.27771155104636824,
      0.19637172095872105,
      0.13885577552318412,
      0.09818586047936052,
      0.06942788776159206,
      0.04909293023968026,
      0.03471394388079603,
      0.02454646511984013,
      0.017356971940398015,
      0.012273232559920065,
      0.008678485970199008,
      0.006136616279960033,
      0.004339242985099504,
      0.0030683081399800164,
      0.002169621492549752,
      0.0015341540699900082,
      0.001084810746274876,
      7.670770349950041E-4,
      5.42405373137438E-4,
      3.8353851749750204E-4,
      2.71202686568719E-4,
      1.9176925874875102E-4,
      1.356013432843595E-4,
      9.588462937437551E-5
    };
    double[] actual =
        IntStream.range(MIN_P, MAX_P + 1)
            .mapToDouble(UltraLogLog::calculateTheoreticalRelativeStandardError)
            .toArray();
    assertThat(actual)
        .usingElementComparator(compareWithMaxRelativeError(RELATIVE_ERROR))
        .isEqualTo(expected);
  }

  @Test
  void testRelativeStandardErrorAgainstFormula() {
    for (int p = MIN_P; p <= MAX_P; ++p) {
      assertThat(UltraLogLog.calculateTheoreticalRelativeStandardError(p))
          .usingComparator(compareWithMaxRelativeError(RELATIVE_ERROR))
          .isEqualTo(calculateRelativeStandardError(p));
    }
  }

  private static double calculateRelativeStandardError(int p) {
    int numberOfRegisters = 1 << p;
    return Math.sqrt(calculateVarianceFactor(TAU) / numberOfRegisters);
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
            .isCloseTo(relativeStandardError, withPercentage(15));
      }
    }
  }

  @Test
  void testPrefixConversion() {
    assertThat(UltraLogLog.hashPrefixToRegister(0x4L)).isEqualTo((byte) 8);
    assertThat(UltraLogLog.hashPrefixToRegister(0x5L)).isEqualTo((byte) 9);
    assertThat(UltraLogLog.hashPrefixToRegister(0x6L)).isEqualTo((byte) 10);
    assertThat(UltraLogLog.hashPrefixToRegister(0x7L)).isEqualTo((byte) 11);
    assertThat(UltraLogLog.hashPrefixToRegister(0x8L)).isEqualTo((byte) 12);
    assertThat(UltraLogLog.hashPrefixToRegister(0x9L)).isEqualTo((byte) 12);
    assertThat(UltraLogLog.hashPrefixToRegister(0xAL)).isEqualTo((byte) 13);
    assertThat(UltraLogLog.hashPrefixToRegister(0xBL)).isEqualTo((byte) 13);
    assertThat(UltraLogLog.hashPrefixToRegister(12)).isEqualTo((byte) 14);
    assertThat(UltraLogLog.hashPrefixToRegister(1L << (12 - 1))).isEqualTo((byte) 44);
    assertThat(UltraLogLog.hashPrefixToRegister(1L << 12)).isEqualTo((byte) 48);
    assertThat(UltraLogLog.hashPrefixToRegister((1L << (12 - 1)) | (1L << (12))))
        .isEqualTo((byte) 50);
    assertThat(UltraLogLog.hashPrefixToRegister(1L << (12 + 1))).isEqualTo((byte) 52);
    assertThat(UltraLogLog.hashPrefixToRegister(0x8000000000000000L)).isEqualTo((byte) 252);
    assertThat(UltraLogLog.hashPrefixToRegister(0xFFFFFFFFFFFFFFFFL)).isEqualTo((byte) 255);

    assertThat(UltraLogLog.registerToHashPrefix((byte) 0)).isZero();
    assertThat(UltraLogLog.registerToHashPrefix((byte) 4)).isZero();
    assertThat(UltraLogLog.registerToHashPrefix((byte) 8)).isEqualTo(4);
    assertThat(UltraLogLog.registerToHashPrefix((byte) 9)).isEqualTo(5);
    assertThat(UltraLogLog.registerToHashPrefix((byte) 10)).isEqualTo(6);
    assertThat(UltraLogLog.registerToHashPrefix((byte) 11)).isEqualTo(7);
    assertThat(UltraLogLog.registerToHashPrefix((byte) 12)).isEqualTo(8);
    assertThat(UltraLogLog.registerToHashPrefix((byte) 13)).isEqualTo(10);
    assertThat(UltraLogLog.registerToHashPrefix((byte) 14)).isEqualTo(12);
    assertThat(UltraLogLog.registerToHashPrefix((byte) 44)).isEqualTo(1L << (12 - 1));
    assertThat(UltraLogLog.registerToHashPrefix((byte) 45))
        .isEqualTo((1L << (12 - 1)) + (1L << (12 - 3)));
    assertThat(UltraLogLog.registerToHashPrefix((byte) 46))
        .isEqualTo((1L << (12 - 1)) + (1L << (12 - 2)));
    assertThat(UltraLogLog.registerToHashPrefix((byte) 47))
        .isEqualTo((1L << (12 - 1)) + (1L << (12 - 2)) + (1L << (12 - 3)));
    assertThat(UltraLogLog.registerToHashPrefix((byte) 255)).isEqualTo(0xE000000000000000L);

    int smallestRegisterValue = (MIN_P << 2) - 4;
    for (int i = smallestRegisterValue; i < 256; i += 1) {
      byte b = (byte) i;
      assertThat(UltraLogLog.hashPrefixToRegister(UltraLogLog.registerToHashPrefix(b)))
          .isEqualTo(b);
    }
  }

  @Test
  void testAddAndMerge() {
    SplittableRandom random = new SplittableRandom(0x11a73f21bb8ad8f6L);
    int[] pVals = IntStream.range(MIN_P, 10).toArray();
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
    int[] pVals = IntStream.range(MIN_P, 16).toArray();
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
      assertThatIllegalArgumentException().isThrownBy(() -> sketch2b.add(sketch1b));
      assertThat(sketch1a.getState()).isEqualTo(sketchTotal.getState());
    } else if (p1 > p2) {
      sketch2a.add(sketch1a);
      assertThatIllegalArgumentException().isThrownBy(() -> sketch1b.add(sketch2b));
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
  void testXi() {
    assertThat(UltraLogLog.xi(2, 0.)).isZero();
    assertThat(UltraLogLog.xi(2, 0.5)).isCloseTo(1.2814941480755806, withPercentage(1e-8));
    assertThat(UltraLogLog.xi(2, 1.)).isEqualTo(Double.POSITIVE_INFINITY);
    assertThat(UltraLogLog.xi(2, Double.NEGATIVE_INFINITY)).isZero();
    assertThat(UltraLogLog.xi(2, Double.MIN_VALUE)).isCloseTo(0., Offset.offset(1e-200));
    assertThat(UltraLogLog.xi(2, Double.MAX_VALUE)).isEqualTo(Double.POSITIVE_INFINITY);
    assertThat(UltraLogLog.xi(2, -Double.MIN_VALUE)).isZero();
    assertThat(UltraLogLog.xi(2, Double.NaN)).isNaN();
    assertThat(UltraLogLog.xi(Double.NaN, 0.5)).isNaN();
    assertThat(UltraLogLog.xi(Double.NaN, Double.NaN)).isNaN();
    assertThat(UltraLogLog.xi(2, Math.nextDown(1.)))
        .isCloseTo(1.2994724158464226E16, withPercentage(1e-8));
  }

  @Test
  void testWrapZeros() {
    for (int len = 1 << MIN_P; len <= 1 << MAX_P; len *= 2) {
      assertThat(UltraLogLog.wrap(new byte[len]).getDistinctCountEstimate()).isZero();
    }
  }

  @Test
  void testRandomStates() {
    int numCycles = 1000000;
    int minP = MIN_P;
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
      assertThatNoException().isThrownBy(sketch1::getDistinctCountEstimate);
      assertThatNoException().isThrownBy(sketch2::getDistinctCountEstimate);
      assertThatNoException().isThrownBy(sketch1::copy);
      assertThatNoException().isThrownBy(sketch2::copy);
      assertThatNoException().isThrownBy(() -> sketch1.downsize(newP1));
      assertThatNoException().isThrownBy(() -> sketch2.downsize(newP2));
      assertThatNoException().isThrownBy(() -> UltraLogLog.merge(sketch1, sketch2));
      if (sketch1.getP() <= sketch2.getP()) {
        assertThatNoException().isThrownBy(() -> sketch1.add(sketch2));
      } else {
        assertThatNoException().isThrownBy(() -> sketch2.add(sketch1));
      }
    }
  }

  @Test
  void testCreateIllegalArguments() {
    for (int p = MIN_P - 100; p < MAX_P + 100; ++p) {
      int pFinal = p;
      if (p >= MIN_P && p <= MAX_P) {
        assertThatNoException().isThrownBy(() -> UltraLogLog.create(pFinal));
      } else {
        assertThatIllegalArgumentException().isThrownBy(() -> UltraLogLog.create(pFinal));
      }
    }
  }

  @Test
  void testDownsizeIllegalArguments() {
    UltraLogLog sketch = UltraLogLog.create(8);
    for (int p = MIN_P - 100; p < MAX_P + 100; ++p) {
      int pFinal = p;
      if (p >= MIN_P && p <= MAX_P) {
        assertThatNoException().isThrownBy(() -> sketch.downsize(pFinal));
      } else {
        assertThatIllegalArgumentException().isThrownBy(() -> sketch.downsize(pFinal));
      }
    }
  }

  @Test
  void testWrapIllegalArguments() {
    Set<Integer> validLengths =
        IntStream.range(MIN_P, MAX_P + 1).map(p -> 1 << p).boxed().collect(Collectors.toSet());
    Set<Integer> testLengths =
        IntStream.range(MIN_P, MAX_P + 1)
            .map(p -> 1 << p)
            .flatMap(p -> IntStream.of(p - 3, p - 2, p - 1, p, p + 1, p + 2, p + 3))
            .boxed()
            .collect(Collectors.toCollection(HashSet::new));

    for (int len : validLengths) {
      assertThatNoException().isThrownBy(() -> UltraLogLog.wrap(new byte[len]));
    }

    for (int len : Sets.difference(testLengths, validLengths)) {
      assertThatIllegalArgumentException().isThrownBy(() -> UltraLogLog.wrap(new byte[len]));
    }

    assertThatNullPointerException().isThrownBy(() -> UltraLogLog.wrap(null));
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
    int bitsPerRegister = Byte.SIZE;

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
        pow(UltraLogLog.calculateTheoreticalRelativeStandardError(p), 2);

    double storageFactorUltraLogLog =
        bitsPerRegister * sumSizeUltraLogLog * expectedVarianceUltraLogLog / numCycles;

    assertThat(storageFactorUltraLogLog).isCloseTo(2.8895962872532435, withPercentage(1));
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
    assertThat(sketch.getDistinctCountEstimate()).isZero();
    assertThat(sketch.getState()).containsOnly(0);
  }

  private static void testErrorOfDistinctCountEqualOne(int p) {
    UltraLogLog sketch = UltraLogLog.create(p);
    double sumProbabiltiy = 0;
    double averageEstimate = 0;
    double averageRMSE = 0;
    double trueDistinctCount = 1;
    for (int nlz = 0; nlz < 64 - p; ++nlz) {
      long hash1 = 0xFFFFFFFFFFFFFFFFL >>> p >>> nlz;
      sketch.reset().add(hash1);
      double probability = pow(0.5, nlz + 1);
      sumProbabiltiy += probability;
      double estimate = sketch.getDistinctCountEstimate();
      averageEstimate += probability * estimate;
      averageRMSE += probability * pow(estimate - trueDistinctCount, 2);
    }

    double relativeBias = (averageEstimate - trueDistinctCount) / trueDistinctCount;
    double relativeRMSE = Math.sqrt(averageRMSE) / trueDistinctCount;
    double theoreticalRelativeStandardError =
        UltraLogLog.calculateTheoreticalRelativeStandardError(p);
    assertThat(sumProbabiltiy).isCloseTo(1., Offset.offset(1e-6));
    assertThat(relativeBias).isLessThan(theoreticalRelativeStandardError * 0.07);
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
          double probability = 1. / m * pow(0.5, nlz1 + nlz2 + 2);
          if (nlz1 != nlz2) probability *= 2;
          sumProbabiltiy += probability;
          double estimate = sketch.getDistinctCountEstimate();
          averageEstimate += probability * estimate;
          averageRMSE += probability * pow(estimate - trueDistinctCount, 2);
        }
        {
          sketch.reset().add(hash1 | regMask1).add(hash2 | regMask2);
          double probability = (m - 1) / m * pow(0.5, nlz1 + nlz2 + 2);
          if (nlz1 != nlz2) probability *= 2;
          sumProbabiltiy += probability;
          double estimate = sketch.getDistinctCountEstimate();
          averageEstimate += probability * estimate;
          averageRMSE += probability * pow(estimate - trueDistinctCount, 2);
        }
      }
    }

    double relativeBias = (averageEstimate - trueDistinctCount) / trueDistinctCount;
    double relativeRMSE = Math.sqrt(averageRMSE) / trueDistinctCount;
    double theoreticalRelativeStandardError =
        UltraLogLog.calculateTheoreticalRelativeStandardError(p);
    assertThat(sumProbabiltiy).isCloseTo(1., Offset.offset(1e-6));
    assertThat(relativeBias).isLessThan(theoreticalRelativeStandardError * 0.07);
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
            double probability = 1. / (m * m) * pow(0.5, nlz1 + nlz2 + nlz3 + 3);
            if (nlz1 != nlz2) probability *= 2;
            sumProbabiltiy += probability;
            double estimate = sketch.getDistinctCountEstimate();
            averageEstimate += probability * estimate;
            averageRMSE += probability * pow(estimate - trueDistinctCount, 2);
          }
          {
            sketch.reset().add(hash1 | regMask1).add(hash2 | regMask1).add(hash3 | regMask2);
            double probability = 3. * (m - 1) / (m * m) * pow(0.5, nlz1 + nlz2 + nlz3 + 3);
            if (nlz1 != nlz2) probability *= 2;
            sumProbabiltiy += probability;
            double estimate = sketch.getDistinctCountEstimate();
            averageEstimate += probability * estimate;
            averageRMSE += probability * pow(estimate - trueDistinctCount, 2);
          }
          {
            sketch.reset().add(hash1 | regMask1).add(hash2 | regMask2).add(hash3 | regMask3);
            double probability = (m - 1) * (m - 2) / (m * m) * pow(0.5, nlz1 + nlz2 + nlz3 + 3);
            if (nlz1 != nlz2) probability *= 2;
            sumProbabiltiy += probability;
            double estimate = sketch.getDistinctCountEstimate();
            averageEstimate += probability * estimate;
            averageRMSE += probability * pow(estimate - trueDistinctCount, 2);
          }
        }
      }
    }

    double relativeBias = (averageEstimate - trueDistinctCount) / trueDistinctCount;
    double relativeRMSE = Math.sqrt(averageRMSE) / trueDistinctCount;
    double theoreticalRelativeStandardError =
        UltraLogLog.calculateTheoreticalRelativeStandardError(p);
    assertThat(sumProbabiltiy).isCloseTo(1., Offset.offset(1e-6));
    assertThat(relativeBias).isLessThan(theoreticalRelativeStandardError * 0.07);
    assertThat(relativeRMSE).isLessThan(theoreticalRelativeStandardError * 0.75);
  }

  @Test
  void testVarianceOfDistinctCountEqualOne() {
    for (int p = MIN_P; p <= MAX_P; ++p) {
      testErrorOfDistinctCountEqualOne(p);
    }
  }

  @Test
  void testVarianceOfDistinctCountEqualTwo() {
    for (int p = MIN_P; p <= 20; ++p) {
      testErrorOfDistinctCountEqualTwo(p);
    }
  }

  @Test
  void testVarianceOfDistinctCountEqualThree() {
    for (int p = MIN_P; p <= 14; ++p) {
      testErrorOfDistinctCountEqualThree(p);
    }
  }

  private static strictfp int xiIterations(double x, double y) {
    int counter = 0;
    if (y <= 0.) return 0;
    if (y >= 1.) return 0;
    double z = x;
    double sum = y;
    double oldSum;
    do {
      counter += 1;
      y *= y;
      oldSum = sum;
      sum += y * z;
      z *= x;
    } while (oldSum < sum);
    return counter;
  }

  private static double calculateZ(int m, int h0, int h1, int h2, int h3) {
    int alpha = h0 + h1;
    int beta = alpha + h2 + h3;
    int gamma = beta + alpha + ((h0 + h2) << 1);
    return UltraLogLog.calculateZ(m, alpha, beta, gamma);
  }

  @Test
  void testXiIterations() {

    double x = pow(2., TAU);

    assertThat(xiIterations(x, 0.)).isZero();
    assertThat(xiIterations(x, Double.POSITIVE_INFINITY)).isZero();
    assertThat(xiIterations(x, 1.)).isZero();
    assertThat(xiIterations(x, Math.nextDown(1.))).isEqualTo(59);
    assertThat(xiIterations(x, Math.nextUp(0.))).isEqualTo(1);
    int maxNumIterations =
        30; // 29 should be enough, but let's take 30 to encounter potential platform dependencies
    for (int p = MIN_P; p <= MAX_P; ++p) {
      int m = 1 << p;
      {
        // case h0 = m - 1, h1 = 1, h2 = 0, h3 = 0
        double z = calculateZ(m, m - 1, 1, 0, 0);
        assertThat(xiIterations(x, pow(z, 5))).isLessThanOrEqualTo(maxNumIterations);
      }
      {
        // case h0 = m - 1, h1 = 0, h2 = 1, h3 = 0
        double z = calculateZ(m, m - 1, 0, 1, 0);
        assertThat(xiIterations(x, pow(z, 5))).isLessThanOrEqualTo(maxNumIterations);
      }
      {
        // case h0 = m - 1, h1 = 0, h2 = 0, h3 = 1
        double z = calculateZ(m, m - 1, 0, 0, 1);
        assertThat(xiIterations(x, pow(z, 5))).isLessThanOrEqualTo(maxNumIterations);
      }
      {
        // case h0 = m - 1, h1 = 0, h2 = 0, h3 = 0
        double z = calculateZ(m, m - 1, 0, 0, 0);
        assertThat(xiIterations(x, pow(z, 5))).isLessThanOrEqualTo(maxNumIterations);
      }
      // further cases
      for (int h1 = 0; h1 <= 2; ++h1) {
        for (int h2 = 0; h2 <= 2; ++h2) {
          for (int h3 = 0; h3 <= 2; ++h3) {
            double z = calculateZ(m, m - h1 - h2 - h3, h1, h2, h3);
            assertThat(xiIterations(x, pow(z, 5))).isLessThanOrEqualTo(maxNumIterations);
          }
        }
      }
    }
  }

  private static final double LOG_2 = Math.log(2);
  private static final double LOG_4 = Math.log(4);
  private static final double LOG_3_92 = Math.log(3.92);
  private static final double LOG_5_76 = Math.log(5.76);
  private static final double LOG_1_25 = Math.log(1.25);

  private static final double RELATIVE_ERROR = 1e-10;

  private static double calculateVarianceFactor(double tau) {
    double gamma2tauP1 = gamma(2 * tau + 1);
    double gammaTauP1 = gamma(tau + 1);
    double sum =
        0.5
            + exp(-tau * LOG_4) / Math.expm1(tau * LOG_2)
            + exp(-tau * LOG_3_92)
            + exp(-tau * LOG_5_76);

    sum *= LOG_2 * gamma2tauP1 * tau / (gammaTauP1 * gammaTauP1);
    sum -= 1;
    sum /= (tau * tau);
    return sum;
  }

  @Test
  void testOptimalTau() {
    UnivariateObjectiveFunction f =
        new UnivariateObjectiveFunction(UltraLogLogTest::calculateVarianceFactor);
    final MaxEval maxEval = new MaxEval(Integer.MAX_VALUE);
    UnivariateOptimizer optimizer = new BrentOptimizer(2 * Math.ulp(1d), Double.MIN_VALUE);
    SearchInterval searchInterval = new SearchInterval(0, 5, 1);
    UnivariatePointValuePair result =
        optimizer.optimize(GoalType.MINIMIZE, f, searchInterval, maxEval);

    double optimalTau = result.getPoint();

    assertThat(TAU)
        .usingComparator(compareWithMaxRelativeError(RELATIVE_ERROR))
        .isEqualTo(optimalTau);
  }

  private static double calculateRegisterContribution(byte x) {
    int i = x & 0xFF;
    switch (i & 3) {
      case 0:
        return (1. + pow(2., -TAU) + 1. / (pow(2., 2 * TAU) * (pow(2., TAU) - 1.)))
            * pow(2., -TAU * (double) (i / 4));
      case 1:
        return (0. + pow(2., -TAU) + 1. / (pow(2., 2 * TAU) * (pow(2., TAU) - 1.)))
            * pow(2., -TAU * (double) (i / 4));
      case 2:
        return (1. + 1. / (pow(2., 2 * TAU) * (pow(2., TAU) - 1.)))
            * pow(2., -TAU * (double) (i / 4));
      default:
        return (0. + 1. / (pow(2., 2 * TAU) * (pow(2., TAU) - 1.)))
            * pow(2., -TAU * (double) (i / 4));
    }
  }

  @Test
  void testVarianceFactor() {
    assertThat(UltraLogLog.VARIANCE_FACTOR)
        .usingComparator(compareWithMaxRelativeError(RELATIVE_ERROR))
        .isEqualTo(calculateVarianceFactor(TAU));
  }

  @Test
  void testRegisterContributions() {
    double[] expectedContributions = new double[252];
    for (int i = 0; i < 252; ++i) {
      expectedContributions[i] = calculateRegisterContribution((byte) (i + 4));
    }
    assertThat(UltraLogLog.getRegisterContributions())
        .usingElementComparator(compareWithMaxRelativeError(RELATIVE_ERROR))
        .isEqualTo(expectedContributions);
  }

  private static double calculateEstimationFactor(int p) {
    int m = 1 << p;
    double biasCorrectionFactor = 1. / (1. + calculateVarianceFactor(TAU) * (1. + TAU) / (2. * m));
    return m * pow(m * gamma(TAU) / (LOG_2 * exp(LOG_1_25 * TAU)), 1. / TAU) * biasCorrectionFactor;
  }

  @Test
  void testEstimationFactors() {
    double[] expectedEstimationFactors = new double[MAX_P + 1];
    for (int p = MIN_P; p <= MAX_P; ++p) {
      expectedEstimationFactors[p] = calculateEstimationFactor(p);
    }
    assertThat(UltraLogLog.getEstimationFactors())
        .usingElementComparator(compareWithMaxRelativeError(RELATIVE_ERROR))
        .isEqualTo(expectedEstimationFactors);
  }

  @Test
  void testWrappingOfPotentiallyInvalidByteArrays() {
    for (int p = MIN_P; p <= MAX_P; ++p) {
      byte[] b = new byte[1 << p];
      int c = 0;
      while (c < 256) {
        for (int k = 0; k < b.length; ++k) {
          b[k] = (byte) c;
          c += 1;
        }
        assertThatNoException().isThrownBy(() -> UltraLogLog.wrap(b).getDistinctCountEstimate());
      }
    }
  }

  @Test
  void testEmptyMerge() {
    UltraLogLog ultraLogLog1 = UltraLogLog.create(12);
    UltraLogLog ultraLogLog2 = UltraLogLog.create(12);
    UltraLogLog ultraLogLogMerged = UltraLogLog.merge(ultraLogLog1, ultraLogLog2);
    assertThat(ultraLogLogMerged.getDistinctCountEstimate()).isZero();
  }

  @Test
  void testSmallestRegisterValues() {
    for (int p = MIN_P; p <= MAX_P; ++p) {
      long hashPrefix1 = 1L << (p - 1);
      long hashPrefix2 = 2L << (p - 1);
      long hashPrefix3 = 3L << (p - 1);
      long hashPrefix4 = 4L << (p - 1);
      long hashPrefix5 = 5L << (p - 1);
      long hashPrefix6 = 6L << (p - 1);
      long hashPrefix7 = 7L << (p - 1);

      byte register1 = UltraLogLog.hashPrefixToRegister(hashPrefix1);
      byte register2 = UltraLogLog.hashPrefixToRegister(hashPrefix2);
      byte register3 = UltraLogLog.hashPrefixToRegister(hashPrefix3);
      byte register4 = UltraLogLog.hashPrefixToRegister(hashPrefix4);
      byte register5 = UltraLogLog.hashPrefixToRegister(hashPrefix5);
      byte register6 = UltraLogLog.hashPrefixToRegister(hashPrefix6);
      byte register7 = UltraLogLog.hashPrefixToRegister(hashPrefix7);

      assertThat(register1).isEqualTo((byte) ((p << 2) - 4));
      assertThat(register2).isEqualTo((byte) (p << 2));
      assertThat(register3).isEqualTo((byte) ((p << 2) + 2));
      assertThat(register4).isEqualTo((byte) ((p << 2) + 4));
      assertThat(register5).isEqualTo((byte) ((p << 2) + 5));
      assertThat(register6).isEqualTo((byte) ((p << 2) + 6));
      assertThat(register7).isEqualTo((byte) ((p << 2) + 7));
    }

    long hashPrefixLargest = 0xFFFFFFFFFFFFFFFFL;
    byte registerLargest = UltraLogLog.hashPrefixToRegister(hashPrefixLargest);
    assertThat(registerLargest).isEqualTo((byte) 255);
  }

  @Test
  void testStateCompatibility() {
    PseudoRandomGenerator pseudoRandomGenerator =
        PseudoRandomGeneratorProvider.splitMix64_V1().create();
    HashStream hashStream = Hashing.komihash4_3().hashStream();
    long[] cardinalities = {1, 10, 100, 1000, 10000, 100000};
    int numCycles = 10;
    for (int p = MIN_P; p <= MAX_P; ++p) {
      for (long cardinality : cardinalities) {
        for (int i = 0; i < numCycles; ++i) {
          UltraLogLog sketch = UltraLogLog.create(p);
          for (long c = 0; c < cardinality; ++c) {
            sketch.add(pseudoRandomGenerator.nextLong());
          }
          hashStream.putByteArray(sketch.getState());
        }
      }
    }
    assertThat(hashStream.getAsLong()).isEqualTo(0xfc124320345bd3b9L);
  }
}
