/*
 * Copyright 2022-2023 Dynatrace LLC
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

import static java.lang.Math.pow;
import static org.assertj.core.api.Assertions.*;
import static org.assertj.core.data.Percentage.withPercentage;

import com.dynatrace.hash4j.hashing.HashStream64;
import com.dynatrace.hash4j.hashing.Hashing;
import com.dynatrace.hash4j.random.PseudoRandomGenerator;
import com.dynatrace.hash4j.random.PseudoRandomGeneratorProvider;
import com.google.common.collect.Sets;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.SplittableRandom;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.zip.Deflater;
import org.junit.jupiter.api.Test;

abstract class DistinctCounterTest<T extends DistinctCounter<T>> {

  protected static final int MIN_P = 3;
  protected static final int MAX_P = 26;

  protected static final double RELATIVE_ERROR = 1e-10;

  protected abstract T create(int p);

  protected abstract T merge(T sketch1, T sketch2);

  protected abstract double calculateTheoreticalRelativeStandardError(int p);

  protected abstract double calculateTheoreticalRelativeStandardErrorMartingale(int p);

  protected abstract long getCompatibilityFingerPrint();

  protected abstract int getStateLength(int p);

  protected abstract T wrap(byte[] state);

  protected abstract double getApproximateStorageFactor();

  @Test
  void testStateCompatibility() {
    PseudoRandomGenerator pseudoRandomGenerator =
        PseudoRandomGeneratorProvider.splitMix64_V1().create();
    HashStream64 hashStream = Hashing.komihash4_3().hashStream();
    long[] cardinalities = {1, 10, 100, 1000, 10000, 100000};
    int numCycles = 10;
    for (int p = MIN_P; p <= MAX_P; ++p) {
      for (long cardinality : cardinalities) {
        for (int i = 0; i < numCycles; ++i) {
          T sketch = create(p);
          for (long c = 0; c < cardinality; ++c) {
            sketch.add(pseudoRandomGenerator.nextLong());
          }
          hashStream.putByteArray(sketch.getState());
        }
      }
    }
    assertThat(hashStream.getAsLong()).isEqualTo(getCompatibilityFingerPrint());
  }

  @Test
  void testEmptyMerge() {
    T sketch1 = create(12);
    T sketch2 = create(12);
    T sketchMerged = merge(sketch1, sketch2);
    assertThat(sketchMerged.getDistinctCountEstimate()).isZero();
  }

  @Test
  void testReset() {
    T sketch = create(12);
    SplittableRandom random = new SplittableRandom(0L);
    for (int i = 0; i < 10000; ++i) {
      sketch.add(random.nextLong());
    }
    assertThat(sketch.getDistinctCountEstimate()).isGreaterThan(1000);
    sketch.reset();
    assertThat(sketch.getDistinctCountEstimate()).isZero();
    assertThat(sketch.getState()).containsOnly(0);
  }

  private void testAddAndMerge(
      int p1, long distinctCount1, int p2, long distinctCount2, long seed) {
    T sketch1a = create(p1);
    T sketch2a = create(p2);
    T sketch1b = create(p1);
    T sketch2b = create(p2);
    T sketchTotal = create(Math.min(p1, p2));
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
    assertThat(merge(sketch1a, sketch2a).getState()).isEqualTo(sketchTotal.getState());
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
    long sumSize = 0;

    int bitsPerRegister = (getStateLength(p) * Byte.SIZE) / (1 << p);

    SplittableRandom random = new SplittableRandom(0L);
    for (int i = 0; i < numCycles; ++i) {
      T sketch = create(p);

      for (long k = 0; k < trueDistinctCount; ++k) {
        long hash = random.nextLong();
        sketch.add(hash);
      }
      sumSize += compress(sketch.getState()).length;
    }

    double expectedVariance = pow(calculateTheoreticalRelativeStandardError(p), 2);

    double storageFactor = bitsPerRegister * sumSize * expectedVariance / numCycles;

    assertThat(storageFactor).isCloseTo(getApproximateStorageFactor(), withPercentage(1));
  }

  @Test
  void testDownsizeIllegalArguments() {
    T sketch = create(8);
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
  void testCreateIllegalArguments() {
    for (int p = MIN_P - 100; p < MAX_P + 100; ++p) {
      int pFinal = p;
      if (p >= MIN_P && p <= MAX_P) {
        assertThatNoException().isThrownBy(() -> create(pFinal));
      } else {
        assertThatIllegalArgumentException().isThrownBy(() -> create(pFinal));
      }
    }
  }

  private void testDownsize(int pOriginal, int pDownsized, long distinctCount, long seed) {
    SplittableRandom random = new SplittableRandom(seed);
    T sketchOrig = create(pOriginal);
    T sketchDownsized = create(pDownsized);

    for (long i = 0; i < distinctCount; ++i) {
      long hashValue = random.nextLong();
      sketchOrig.add(hashValue);
      sketchDownsized.add(hashValue);
    }
    assertThat(sketchOrig.downsize(pDownsized).getState()).isEqualTo(sketchDownsized.getState());
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

  private void testDistinctCountEstimation(int p, long seed, long[] distinctCounts) {

    double relativeStandardError = calculateTheoreticalRelativeStandardError(p);
    double relativeStandardErrorMartingale = calculateTheoreticalRelativeStandardErrorMartingale(p);

    double[] estimationErrorsMoment1 = new double[distinctCounts.length];
    double[] estimationErrorsMoment2 = new double[distinctCounts.length];
    double[] estimationErrorsMoment1Martingale = new double[distinctCounts.length];
    double[] estimationErrorsMoment2Martingale = new double[distinctCounts.length];
    int numIterations = 1000;
    SplittableRandom random = new SplittableRandom(seed);
    for (int i = 0; i < numIterations; ++i) {
      T sketch = create(p);
      T sketchMartingale = create(p);
      MartingaleEstimator martingaleEstimator = new MartingaleEstimator();
      long trueDistinctCount = 0;
      int distinctCountIndex = 0;
      while (distinctCountIndex < distinctCounts.length) {
        if (trueDistinctCount == distinctCounts[distinctCountIndex]) {

          double distinctCountEstimationError =
              sketch.getDistinctCountEstimate() - trueDistinctCount;
          estimationErrorsMoment1[distinctCountIndex] += distinctCountEstimationError;
          estimationErrorsMoment2[distinctCountIndex] +=
              distinctCountEstimationError * distinctCountEstimationError;

          double distinctCountEstimationErrorMartingale =
              martingaleEstimator.getDistinctCountEstimate() - trueDistinctCount;
          estimationErrorsMoment1Martingale[distinctCountIndex] +=
              distinctCountEstimationErrorMartingale;
          estimationErrorsMoment2Martingale[distinctCountIndex] +=
              distinctCountEstimationErrorMartingale * distinctCountEstimationErrorMartingale;

          distinctCountIndex += 1;

          assertThat(sketch.getState()).isEqualTo(sketchMartingale.getState());
          assertThat(sketch.getStateChangeProbability())
              .isEqualTo(martingaleEstimator.getStateChangeProbability());
        }
        long hash = random.nextLong();
        sketch.add(hash);
        sketchMartingale.add(hash, martingaleEstimator);
        trueDistinctCount += 1;
      }
    }

    for (int distinctCountIndex = 0;
        distinctCountIndex < distinctCounts.length;
        ++distinctCountIndex) {
      long trueDistinctCount = distinctCounts[distinctCountIndex];

      {
        double relativeBias =
            estimationErrorsMoment1[distinctCountIndex]
                / (trueDistinctCount * (double) numIterations);
        double relativeRootMeanSquareError =
            Math.sqrt(estimationErrorsMoment2[distinctCountIndex] / numIterations)
                / trueDistinctCount;

        if (trueDistinctCount > 0) {
          // verify bias to be significantly smaller than standard error
          assertThat(relativeBias).isLessThan(relativeStandardError * 0.2);
        }
        if (trueDistinctCount > 0) {
          // test if observed root mean square error is not much greater than relative standard
          // error
          assertThat(relativeRootMeanSquareError).isLessThan(relativeStandardError * 1.3);
        }
        if (trueDistinctCount > 10 * (1L << p)) {
          // test asymptotic behavior (distinct count is much greater than number of registers
          // (state
          // size) given by (1 << p)
          // observed root mean square error should be approximately equal to the standard error
          assertThat(relativeRootMeanSquareError)
              .isCloseTo(relativeStandardError, withPercentage(30));
        }
      }

      {
        double relativeBiasMartingale =
            estimationErrorsMoment1Martingale[distinctCountIndex]
                / (trueDistinctCount * (double) numIterations);
        double relativeRootMeanSquareErrorMartingale =
            Math.sqrt(estimationErrorsMoment2Martingale[distinctCountIndex] / numIterations)
                / trueDistinctCount;

        if (trueDistinctCount > 0) {
          // verify bias to be significantly smaller than standard error
          // System.out.println(trueDistinctCount + " " + p);
          assertThat(relativeBiasMartingale).isLessThan(relativeStandardErrorMartingale * 0.1);
        }
        if (trueDistinctCount > 0) {
          // test if observed root mean square error is not much greater than relative standard
          // error
          assertThat(relativeRootMeanSquareErrorMartingale)
              .isLessThan(relativeStandardErrorMartingale * 1.2);
        }
        if (trueDistinctCount > 10 * (1L << p)) {
          // test asymptotic behavior (distinct count is much greater than number of registers
          // (state size) given by (1 << p)
          // observed root mean square error should be approximately equal to the standard error
          assertThat(relativeRootMeanSquareErrorMartingale)
              .isCloseTo(relativeStandardErrorMartingale, withPercentage(15));
        }
      }
    }
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
  void testEmpty() {
    for (int p = MIN_P; p <= MAX_P; ++p) {
      T sketch = create(p);
      assertThat(sketch.getDistinctCountEstimate()).isZero();
      T mergedSketch = merge(sketch, sketch);
      assertThat(mergedSketch.getState()).isEqualTo(new byte[getStateLength(p)]);
      assertThat(mergedSketch.getDistinctCountEstimate()).isZero();
      assertThat(sketch.getStateChangeProbability()).isOne();
    }
  }

  @Test
  void testWrapZeros() {
    for (int p = MIN_P; p <= MAX_P; p += 1) {
      T sketch = wrap(new byte[getStateLength(p)]);
      assertThat(sketch.getDistinctCountEstimate()).isZero();
    }
  }

  @Test
  void testRandomStates() {
    int numCycles = 1000000;
    int minP = MIN_P;
    int maxP = 8;
    SplittableRandom random = new SplittableRandom(0x822fa1dcf86f953eL);
    for (int i = 0; i < numCycles; ++i) {
      int p1 = random.nextInt(minP, maxP + 1);
      int p2 = random.nextInt(minP, maxP + 1);
      byte[] state1 = new byte[getStateLength(p1)];
      byte[] state2 = new byte[getStateLength(p2)];
      random.nextBytes(state1);
      random.nextBytes(state2);
      T sketch1 = wrap(state1);
      T sketch2 = wrap(state2);
      int newP1 = random.nextInt(minP, maxP + 1);
      int newP2 = random.nextInt(minP, maxP + 1);
      assertThatNoException().isThrownBy(sketch1::getDistinctCountEstimate);
      assertThatNoException().isThrownBy(sketch2::getDistinctCountEstimate);
      assertThatNoException().isThrownBy(sketch1::copy);
      assertThatNoException().isThrownBy(sketch2::copy);
      assertThatNoException().isThrownBy(() -> sketch1.downsize(newP1));
      assertThatNoException().isThrownBy(() -> sketch2.downsize(newP2));
      assertThatNoException().isThrownBy(() -> merge(sketch1, sketch2));
      if (sketch1.getP() <= sketch2.getP()) {
        assertThatNoException().isThrownBy(() -> sketch1.add(sketch2));
      } else {
        assertThatNoException().isThrownBy(() -> sketch2.add(sketch1));
      }
    }
  }

  @Test
  void testDeduplication() {
    SplittableRandom random = new SplittableRandom(0x1dd4dbffe1c9f639L);
    int hashPoolSize = 100000;

    long[] hashValues = random.longs(hashPoolSize).toArray();
    for (int p = MIN_P; p <= 16; ++p) {
      T sketch1 = create(p);
      T sketch2 = create(p);
      Set<Long> insertedHashes = new HashSet<>();
      for (int i = 1; i <= hashPoolSize; ++i) {
        for (int k = 0; k < 3; ++k) {
          long hashValue = hashValues[random.nextInt(i)];
          sketch1.add(hashValue);
          if (insertedHashes.add(hashValue)) {
            sketch2.add(hashValue);
          }
        }
        assertThat(sketch1.getState()).isEqualTo(sketch2.getState());
      }
    }
  }

  @Test
  void testWrappingOfPotentiallyInvalidByteArrays() {
    for (int p = MIN_P; p <= MAX_P; ++p) {
      byte[] b = new byte[getStateLength(p)];
      int c = 0;
      while (c < 256) {
        for (int k = 0; k < b.length; ++k) {
          b[k] = (byte) c;
          c += 1;
        }
        assertThatNoException().isThrownBy(() -> wrap(b).getDistinctCountEstimate());
      }
    }
  }

  @Test
  void testWrapIllegalArguments() {
    Set<Integer> validLengths =
        IntStream.range(MIN_P, MAX_P + 1)
            .map(this::getStateLength)
            .boxed()
            .collect(Collectors.toSet());
    Set<Integer> testLengths =
        IntStream.range(MIN_P, MAX_P + 1)
            .map(this::getStateLength)
            .flatMap(p -> IntStream.of(p - 3, p - 2, p - 1, p, p + 1, p + 2, p + 3))
            .boxed()
            .collect(Collectors.toCollection(HashSet::new));

    for (int len : validLengths) {
      assertThatNoException().isThrownBy(() -> wrap(new byte[len]));
    }

    for (int len : Sets.difference(testLengths, validLengths)) {
      assertThatIllegalArgumentException().isThrownBy(() -> wrap(new byte[len]));
    }

    assertThatNullPointerException().isThrownBy(() -> wrap(null));
  }
}
