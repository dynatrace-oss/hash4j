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

import static com.dynatrace.hash4j.testutils.TestUtils.compareWithMaxRelativeError;
import static java.lang.Math.pow;
import static java.util.stream.Collectors.joining;
import static org.assertj.core.api.Assertions.*;
import static org.assertj.core.data.Percentage.withPercentage;

import com.dynatrace.hash4j.hashing.HashStream;
import com.dynatrace.hash4j.hashing.Hashing;
import com.dynatrace.hash4j.random.PseudoRandomGenerator;
import com.dynatrace.hash4j.random.PseudoRandomGeneratorProvider;
import com.google.common.collect.Sets;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.SplittableRandom;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.zip.Deflater;
import org.assertj.core.data.Offset;
import org.junit.jupiter.api.Test;

public class HyperLogLogTest {
  private static final int MIN_P = 2;
  private static final int MAX_P = 26;

  @Test
  void testEmpty() {
    HyperLogLog sketch = HyperLogLog.create(10);
    assertThat(sketch.getDistinctCountEstimate()).isZero();
  }

  @Test
  void testDistinctCountEstimation() {
    int maxP = 14;
    SplittableRandom random = new SplittableRandom(0x9511e2cda44e123dL);
    long[] distinctCounts = TestUtils.getDistinctCountValues(0, 100000, 0.2);

    for (int p = MIN_P; p <= maxP; ++p) {
      testDistinctCountEstimation(p, random.nextLong(), distinctCounts);
    }
  }

  @Test
  void testRelativeStandardErrorAgainstConstants() {
    double[] expected = {
      0.5194808807068446,
      0.36732845344456977,
      0.2597404403534223,
      0.18366422672228488,
      0.12987022017671115,
      0.09183211336114244,
      0.06493511008835558,
      0.04591605668057122,
      0.03246755504417779,
      0.02295802834028561,
      0.016233777522088894,
      0.011479014170142805,
      0.008116888761044447,
      0.005739507085071403,
      0.004058444380522224,
      0.0028697535425357013,
      0.002029222190261112,
      0.0014348767712678507,
      0.001014611095130556,
      7.174383856339253E-4,
      5.07305547565278E-4,
      3.5871919281696267E-4,
      2.53652773782639E-4,
      1.7935959640848133E-4,
      1.268263868913195E-4
    };
    double[] actual =
        IntStream.range(MIN_P, MAX_P + 1)
            .mapToDouble(HyperLogLog::calculateTheoreticalRelativeStandardError)
            .toArray();
    assertThat(actual)
        .usingElementComparator(compareWithMaxRelativeError(RELATIVE_ERROR))
        .isEqualTo(expected);
  }

  @Test
  void testRelativeStandardErrorAgainstFormula() {
    for (int p = MIN_P; p <= MAX_P; ++p) {
      assertThat(HyperLogLog.calculateTheoreticalRelativeStandardError(p))
          .usingComparator(compareWithMaxRelativeError(RELATIVE_ERROR))
          .isEqualTo(calculateRelativeStandardError(p));
    }
  }

  private static double calculateRelativeStandardError(int p) {
    int numberOfRegisters = 1 << p;
    return Math.sqrt(calculateVarianceFactor() / numberOfRegisters);
  }

  private void testDistinctCountEstimation(int p, long seed, long[] distinctCounts) {

    double relativeStandardError = HyperLogLog.calculateTheoreticalRelativeStandardError(p);

    double[] estimationErrorsMoment1 = new double[distinctCounts.length];
    double[] estimationErrorsMoment2 = new double[distinctCounts.length];
    int numIterations = 1000;
    SplittableRandom random = new SplittableRandom(seed);
    for (int i = 0; i < numIterations; ++i) {
      HyperLogLog sketch = HyperLogLog.create(p);
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
        assertThat(relativeBias).isLessThan(relativeStandardError * 0.25);
      }
      if (trueDistinctCount > 0) {
        // test if observed root mean square error is not much greater than relative standard error
        assertThat(relativeRootMeanSquareError).isLessThan(relativeStandardError * 1.9);
      }
      if (trueDistinctCount > 10 * (1L << p)) {
        // test asymptotic behavior (distinct count is much greater than number of registers (state
        // size) given by (1 << p)
        // observed root mean square error should be approximately equal to the standard error
        assertThat(relativeRootMeanSquareError)
            .isCloseTo(relativeStandardError, withPercentage(90));
      }
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
    HyperLogLog sketch1a = HyperLogLog.create(p1);
    HyperLogLog sketch2a = HyperLogLog.create(p2);
    HyperLogLog sketch1b = HyperLogLog.create(p1);
    HyperLogLog sketch2b = HyperLogLog.create(p2);
    HyperLogLog sketchTotal = HyperLogLog.create(Math.min(p1, p2));
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
    assertThat(HyperLogLog.merge(sketch1a, sketch2a).getState()).isEqualTo(sketchTotal.getState());
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
    HyperLogLog sketchOrig = HyperLogLog.create(pOriginal);
    HyperLogLog sketchDownsized = HyperLogLog.create(pDownsized);

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
      assertThat(HyperLogLog.isUnsignedPowerOfTwo(1 << exponent)).isTrue();
    }
    assertThat(HyperLogLog.isUnsignedPowerOfTwo(0)).isTrue();
    for (int i = -1000; i < 0; ++i) {
      assertThat(HyperLogLog.isUnsignedPowerOfTwo(i)).isFalse();
    }
    assertThat(HyperLogLog.isUnsignedPowerOfTwo(Integer.MIN_VALUE)).isTrue();
    assertThat(HyperLogLog.isUnsignedPowerOfTwo(Integer.MAX_VALUE)).isFalse();
  }

  @Test
  void testSigma() {
    assertThat(HyperLogLog.sigma(0.)).isZero();
    assertThat(HyperLogLog.sigma(0.5)).isCloseTo(0.8907470740377903, withPercentage(1e-8));
    assertThat(HyperLogLog.sigma(1.)).isEqualTo(Double.POSITIVE_INFINITY);
    assertThat(HyperLogLog.sigma(Double.NEGATIVE_INFINITY)).isZero();
    assertThat(HyperLogLog.sigma(Double.MIN_VALUE)).isCloseTo(0., Offset.offset(1e-200));
    assertThat(HyperLogLog.sigma(Double.MAX_VALUE)).isEqualTo(Double.POSITIVE_INFINITY);
    assertThat(HyperLogLog.sigma(-Double.MIN_VALUE)).isZero();
    assertThat(HyperLogLog.sigma(Double.NaN)).isNaN();
    assertThat(HyperLogLog.sigma(Math.nextDown(1.)))
        .isCloseTo(6.497362079232113E15, withPercentage(1e-8));
  }

  @Test
  void testWrapZeros() {
    for (int len = ((1 << MIN_P) * 6) / 8; len <= ((1 << MAX_P) * 6) / 8; len *= 2) {
      assertThat(HyperLogLog.wrap(new byte[len]).getDistinctCountEstimate()).isZero();
    }
  }

  @Test
  void testRandomStates() {
    int numCycles = 1000000;
    int minP = MIN_P;
    int maxP = 8;
    SplittableRandom random = new SplittableRandom(0x822fa1dcf86f953eL);
    for (int i = 0; i < numCycles; ++i) {
      byte[] state1 = new byte[(6 << random.nextInt(minP, maxP + 1)) / 8];
      byte[] state2 = new byte[(6 << random.nextInt(minP, maxP + 1)) / 8];
      random.nextBytes(state1);
      random.nextBytes(state2);
      HyperLogLog sketch1 = HyperLogLog.wrap(state1);
      HyperLogLog sketch2 = HyperLogLog.wrap(state2);
      int newP1 = random.nextInt(minP, maxP + 1);
      int newP2 = random.nextInt(minP, maxP + 1);
      assertThatNoException().isThrownBy(sketch1::getDistinctCountEstimate);
      assertThatNoException().isThrownBy(sketch2::getDistinctCountEstimate);
      assertThatNoException().isThrownBy(sketch1::copy);
      assertThatNoException().isThrownBy(sketch2::copy);
      assertThatNoException().isThrownBy(() -> sketch1.downsize(newP1));
      assertThatNoException().isThrownBy(() -> sketch2.downsize(newP2));
      assertThatNoException().isThrownBy(() -> HyperLogLog.merge(sketch1, sketch2));
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
        assertThatNoException().isThrownBy(() -> HyperLogLog.create(pFinal));
      } else {
        assertThatThrownBy(() -> HyperLogLog.create(pFinal))
            .isInstanceOf(IllegalArgumentException.class);
      }
    }
  }

  @Test
  void testDownsizeIllegalArguments() {
    HyperLogLog sketch = HyperLogLog.create(8);
    for (int p = MIN_P - 100; p < MAX_P + 100; ++p) {
      int pFinal = p;
      if (p >= MIN_P && p <= MAX_P) {
        assertThatNoException().isThrownBy(() -> sketch.downsize(pFinal));
      } else {
        assertThatThrownBy(() -> sketch.downsize(pFinal))
            .isInstanceOf(IllegalArgumentException.class);
      }
    }
  }

  @Test
  void testWrapIllegalArguments() {
    Set<Integer> validLengths =
        IntStream.range(MIN_P, MAX_P + 1)
            .map(p -> (1 << p) * 6 / 8)
            .boxed()
            .collect(Collectors.toSet());
    Set<Integer> testLengths =
        IntStream.range(MIN_P, MAX_P + 1)
            .map(p -> (1 << p) * 6 / 8)
            .flatMap(p -> IntStream.of(p - 3, p - 2, p - 1, p, p + 1, p + 2, p + 3))
            .boxed()
            .collect(Collectors.toCollection(HashSet::new));

    for (int len : validLengths) {
      assertThatNoException().isThrownBy(() -> HyperLogLog.wrap(new byte[len]));
    }

    for (int len : Sets.difference(testLengths, validLengths)) {
      assertThatThrownBy(() -> HyperLogLog.wrap(new byte[len]))
          .isInstanceOf(IllegalArgumentException.class);
    }

    assertThatThrownBy(() -> HyperLogLog.wrap(null)).isInstanceOf(NullPointerException.class);
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
    long sumSizeHyperLogLog = 0;
    int bitsPerRegister = 6;

    SplittableRandom random = new SplittableRandom(0L);
    for (int i = 0; i < numCycles; ++i) {
      HyperLogLog HyperLogLogSketch = HyperLogLog.create(p);

      for (long k = 0; k < trueDistinctCount; ++k) {
        long hash = random.nextLong();
        HyperLogLogSketch.add(hash);
      }
      sumSizeHyperLogLog += compress(HyperLogLogSketch.getState()).length;
    }

    double expectedVarianceHyperLogLog =
        pow(HyperLogLog.calculateTheoreticalRelativeStandardError(p), 2);

    double storageFactorHyperLogLog =
        bitsPerRegister * sumSizeHyperLogLog * expectedVarianceHyperLogLog / numCycles;

    assertThat(storageFactorHyperLogLog).isCloseTo(3.3741034021645766, withPercentage(1));
  }

  @Test
  void testReset() {
    HyperLogLog sketch = HyperLogLog.create(12);
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
    HyperLogLog sketch = HyperLogLog.create(p);
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
        HyperLogLog.calculateTheoreticalRelativeStandardError(p);
    assertThat(sumProbabiltiy).isCloseTo(1., Offset.offset(1e-6));
    assertThat(relativeBias).isLessThan(theoreticalRelativeStandardError * 0.07);
    assertThat(relativeRMSE).isLessThan(theoreticalRelativeStandardError * 0.7);
  }

  private static void testErrorOfDistinctCountEqualTwo(int p) {
    double m = 1 << p;
    HyperLogLog sketch = HyperLogLog.create(p);
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
        HyperLogLog.calculateTheoreticalRelativeStandardError(p);
    assertThat(sumProbabiltiy).isCloseTo(1., Offset.offset(1e-6));
    assertThat(relativeBias).isLessThan(theoreticalRelativeStandardError * 0.07);
    assertThat(relativeRMSE).isLessThan(theoreticalRelativeStandardError * 0.7);
  }

  private static void testErrorOfDistinctCountEqualThree(int p) {
    double m = 1 << p;
    HyperLogLog sketch = HyperLogLog.create(p);
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
        HyperLogLog.calculateTheoreticalRelativeStandardError(p);
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

  private static strictfp int sigmaIterations(double x) {
    int counter = 0;
    if (x <= 0.) return 0;
    if (x >= 1.) return 0;
    double z = 1;
    double sum = x;
    double oldSum;
    do {
      counter += 1;
      x *= x;
      oldSum = sum;
      sum += x * z;
      z += z;
    } while (oldSum < sum);
    return counter;
  }

  @Test
  void testSigmaIterations() {
    assertThat(sigmaIterations(0.)).isZero();
    assertThat(sigmaIterations(Double.POSITIVE_INFINITY)).isZero();
    assertThat(sigmaIterations(1.)).isZero();
    assertThat(sigmaIterations(Math.nextDown(1.))).isEqualTo(59);
    assertThat(sigmaIterations(Math.nextUp(0.))).isEqualTo(1);
    int maxNumIterations =
        33; // 32 should be enough, but let's take 33 to encounter potential platform dependencies
    for (int p = MIN_P; p <= MAX_P; ++p) {
      int m = 1 << p;
      assertThat(sigmaIterations(1. / m)).isLessThanOrEqualTo(maxNumIterations);
      assertThat(sigmaIterations((m - 1.) / m)).isLessThanOrEqualTo(maxNumIterations);
    }
  }

  private static final double LOG_2 = Math.log(2);

  private static double calculateVarianceFactor() {
    return 3. * LOG_2 - 1.;
  }

  private static final double RELATIVE_ERROR = 1e-10;

  private static double calculateRegisterContribution(int k) {
    return StrictMath.pow(0.5, k);
  }

  @Test
  void testVarianceFactor() {
    assertThat(HyperLogLog.VARIANCE_FACTOR)
        .usingComparator(compareWithMaxRelativeError(RELATIVE_ERROR))
        .isEqualTo(calculateVarianceFactor());
  }

  @Test
  void testRegisterContributions() {
    double[] expectedContributions = new double[64];
    for (int i = 0; i < 64; ++i) {
      expectedContributions[i] = calculateRegisterContribution(i);
    }
    assertThat(HyperLogLog.getRegisterContributions())
        .withRepresentation(
            o -> {
              if (o instanceof double[]) {
                return DoubleStream.of((double[]) o)
                    .mapToObj(Double::toHexString)
                    .collect(joining(",", "[", "]"));
              } else {
                return Objects.toString(o);
              }
            })
        .isEqualTo(expectedContributions);
  }

  private static double calculateEstimationFactor(int p) {
    double m = 1 << p;
    return m * m / (2. * LOG_2 * (1. + calculateVarianceFactor() / m));
  }

  @Test
  void testEstimationFactors() {
    double[] expectedEstimationFactors = new double[MAX_P + 1];
    for (int p = MIN_P; p <= MAX_P; ++p) {
      expectedEstimationFactors[p] = calculateEstimationFactor(p);
    }
    assertThat(HyperLogLog.getEstimationFactors())
        .usingElementComparator(compareWithMaxRelativeError(RELATIVE_ERROR))
        .isEqualTo(expectedEstimationFactors);
  }

  @Test
  void testWrappingOfPotentiallyInvalidByteArrays() {
    for (int p = MIN_P; p <= MAX_P; ++p) {
      byte[] b = new byte[(6 << p) / 8];
      int c = 0;
      while (c < 256) {
        for (int k = 0; k < b.length; ++k) {
          b[k] = (byte) c;
          c += 1;
        }
        assertThatNoException().isThrownBy(() -> HyperLogLog.wrap(b).getDistinctCountEstimate());
      }
    }
  }

  @Test
  void testEmptyMerge() {
    HyperLogLog HyperLogLog1 = HyperLogLog.create(12);
    HyperLogLog HyperLogLog2 = HyperLogLog.create(12);
    HyperLogLog HyperLogLogMerged = HyperLogLog.merge(HyperLogLog1, HyperLogLog2);
    assertThat(HyperLogLogMerged.getDistinctCountEstimate()).isZero();
  }

  @Test
  void testCalculateP() {
    for (int p = MIN_P; p <= MAX_P; ++p) {
      int stateLength = ((1 << p) * 6) / 8;
      assertThat(HyperLogLog.calculateP(stateLength)).isEqualTo(p);
    }
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
          HyperLogLog sketch = HyperLogLog.create(p);
          for (long c = 0; c < cardinality; ++c) {
            sketch.add(pseudoRandomGenerator.nextLong());
          }
          hashStream.putByteArray(sketch.getState());
        }
      }
    }
    assertThat(hashStream.getAsLong()).isEqualTo(0xbdaf1768adb7bd8bL);
  }
}
