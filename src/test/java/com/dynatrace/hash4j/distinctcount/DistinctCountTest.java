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

abstract class DistinctCountTest<T> {

  protected static final int MIN_P = 3;
  protected static final int MAX_P = 26;

  protected static final double RELATIVE_ERROR = 1e-10;

  protected abstract T create(int p);

  protected abstract double getDistinctCountEstimate(T sketch);

  protected abstract T merge(T sketch1, T sketch2);

  protected abstract T add(T sketch, long hashValue);

  protected abstract T add(T sketch, T otherSketch);

  protected abstract T downsize(T sketch, int p);

  protected abstract T copy(T sketch);

  protected abstract T reset(T sketch);

  protected abstract byte[] getState(T sketch);

  protected abstract double calculateTheoreticalRelativeStandardError(int p);

  protected abstract long getCompatibilityFingerPrint();

  protected abstract double calculateVarianceFactor();

  protected abstract int getStateLength(int p);

  protected abstract T wrap(byte[] state);

  protected abstract double getApproximateStorageFactor();

  protected abstract int getP(T sketch);

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
            add(sketch, pseudoRandomGenerator.nextLong());
          }
          hashStream.putByteArray(getState(sketch));
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
    assertThat(getDistinctCountEstimate(sketchMerged)).isZero();
  }

  @Test
  void testReset() {
    T sketch = create(12);
    SplittableRandom random = new SplittableRandom(0L);
    for (int i = 0; i < 10000; ++i) {
      add(sketch, random.nextLong());
    }
    assertThat(getDistinctCountEstimate(sketch)).isGreaterThan(1000);
    reset(sketch);
    assertThat(getDistinctCountEstimate(sketch)).isZero();
    assertThat(getState(sketch)).containsOnly(0);
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
      add(sketch1a, hashValue);
      add(sketch1b, hashValue);
      add(sketchTotal, hashValue);
    }
    for (long i = 0; i < distinctCount2; ++i) {
      long hashValue = random.nextLong();
      add(sketch2a, hashValue);
      add(sketch2b, hashValue);
      add(sketchTotal, hashValue);
    }
    assertThat(getState(merge(sketch1a, sketch2a))).isEqualTo(getState(sketchTotal));
    if (p1 < p2) {
      add(sketch1a, sketch2a);
      assertThatIllegalArgumentException().isThrownBy(() -> add(sketch2b, sketch1b));
      assertThat(getState(sketch1a)).isEqualTo(getState(sketchTotal));
    } else if (p1 > p2) {
      add(sketch2a, sketch1a);
      assertThatIllegalArgumentException().isThrownBy(() -> add(sketch1b, sketch2b));
      assertThat(getState(sketch2a)).isEqualTo(getState(sketchTotal));
    } else {
      add(sketch1a, sketch2a);
      add(sketch2b, sketch1b);
      assertThat(getState(sketch1a)).isEqualTo(getState(sketchTotal));
      assertThat(getState(sketch2b)).isEqualTo(getState(sketchTotal));
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
        add(sketch, hash);
      }
      sumSize += compress(getState(sketch)).length;
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
        assertThatNoException().isThrownBy(() -> downsize(sketch, pFinal));
      } else {
        assertThatIllegalArgumentException().isThrownBy(() -> downsize(sketch, pFinal));
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
      add(sketchOrig, hashValue);
      add(sketchDownsized, hashValue);
    }
    assertThat(getState(downsize(sketchOrig, pDownsized))).isEqualTo(getState(sketchDownsized));
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

  @Test
  void testRelativeStandardErrorAgainstFormula() {
    for (int p = MIN_P; p <= MAX_P; ++p) {
      int numRegisters = 1 << p;
      assertThat(calculateTheoreticalRelativeStandardError(p))
          .usingComparator(compareWithMaxRelativeError(RELATIVE_ERROR))
          .isEqualTo(Math.sqrt(calculateVarianceFactor() / numRegisters));
    }
  }

  private void testDistinctCountEstimation(int p, long seed, long[] distinctCounts) {

    double relativeStandardError = calculateTheoreticalRelativeStandardError(p);

    double[] estimationErrorsMoment1 = new double[distinctCounts.length];
    double[] estimationErrorsMoment2 = new double[distinctCounts.length];
    int numIterations = 1000;
    SplittableRandom random = new SplittableRandom(seed);
    for (int i = 0; i < numIterations; ++i) {
      T sketch = create(p);
      long trueDistinctCount = 0;
      int distinctCountIndex = 0;
      while (distinctCountIndex < distinctCounts.length) {
        if (trueDistinctCount == distinctCounts[distinctCountIndex]) {
          double distinctCountEstimationErrror =
              getDistinctCountEstimate(sketch) - trueDistinctCount;
          estimationErrorsMoment1[distinctCountIndex] += distinctCountEstimationErrror;
          estimationErrorsMoment2[distinctCountIndex] +=
              distinctCountEstimationErrror * distinctCountEstimationErrror;
          distinctCountIndex += 1;
        }
        add(sketch, random.nextLong());
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

      if (trueDistinctCount > 0) {
        // verify bias to be significantly smaller than standard error
        assertThat(relativeBias).isLessThan(relativeStandardError * 0.2);
      }
      if (trueDistinctCount > 0) {
        // test if observed root mean square error is not much greater than relative standard error
        assertThat(relativeRootMeanSquareError).isLessThan(relativeStandardError * 1.3);
      }
      if (trueDistinctCount > 10 * (1L << p)) {
        // test asymptotic behavior (distinct count is much greater than number of registers (state
        // size) given by (1 << p)
        // observed root mean square error should be approximately equal to the standard error
        assertThat(relativeRootMeanSquareError)
            .isCloseTo(relativeStandardError, withPercentage(30));
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
      assertThat(getDistinctCountEstimate(sketch)).isZero();
      T mergedSketch = merge(sketch, sketch);
      assertThat(getState(mergedSketch)).isEqualTo(new byte[getStateLength(p)]);
      assertThat(getDistinctCountEstimate(mergedSketch)).isZero();
    }
  }

  @Test
  void testWrapZeros() {
    for (int p = MIN_P; p <= MAX_P; p += 1) {
      assertThat(getDistinctCountEstimate(wrap(new byte[getStateLength(p)]))).isZero();
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
      assertThatNoException().isThrownBy(() -> getDistinctCountEstimate(sketch1));
      assertThatNoException().isThrownBy(() -> getDistinctCountEstimate(sketch2));
      assertThatNoException().isThrownBy(() -> copy(sketch1));
      assertThatNoException().isThrownBy(() -> copy(sketch2));
      assertThatNoException().isThrownBy(() -> downsize(sketch1, newP1));
      assertThatNoException().isThrownBy(() -> downsize(sketch2, newP2));
      assertThatNoException().isThrownBy(() -> merge(sketch1, sketch2));
      if (getP(sketch1) <= getP(sketch2)) {
        assertThatNoException().isThrownBy(() -> add(sketch1, sketch2));
      } else {
        assertThatNoException().isThrownBy(() -> add(sketch2, sketch1));
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
          add(sketch1, hashValue);
          if (insertedHashes.add(hashValue)) {
            add(sketch2, hashValue);
          }
        }
        assertThat(getState(sketch1)).isEqualTo(getState(sketch2));
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
        assertThatNoException().isThrownBy(() -> getDistinctCountEstimate(wrap(b)));
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
