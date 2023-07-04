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

import static com.dynatrace.hash4j.distinctcount.DistinctCounter.reconstructHash;
import static com.dynatrace.hash4j.util.Preconditions.checkArgument;
import static java.lang.Math.pow;
import static org.assertj.core.api.Assertions.*;
import static org.assertj.core.data.Percentage.withPercentage;

import com.dynatrace.hash4j.hashing.HashStream64;
import com.dynatrace.hash4j.hashing.Hashing;
import com.dynatrace.hash4j.random.PseudoRandomGenerator;
import com.dynatrace.hash4j.random.PseudoRandomGeneratorProvider;
import com.google.common.collect.Sets;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.DoubleUnaryOperator;
import java.util.function.IntToDoubleFunction;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.zip.Deflater;
import org.assertj.core.data.Offset;
import org.junit.jupiter.api.Test;

abstract class DistinctCounterTest<
    T extends DistinctCounter<T, R>, R extends DistinctCounter.Estimator<T>> {

  protected static final int MIN_P = 3;
  protected static final int MAX_P = 26;

  protected static final double RELATIVE_ERROR = 1e-12;

  protected abstract T create(int p);

  protected abstract T merge(T sketch1, T sketch2);

  protected abstract double calculateTheoreticalRelativeStandardErrorML(int p);

  protected abstract double calculateTheoreticalRelativeStandardErrorMartingale(int p);

  protected abstract long getCompatibilityFingerPrint();

  protected abstract int getStateLength(int p);

  protected abstract T wrap(byte[] state);

  protected abstract double getCompressedStorageFactorLowerBound();

  protected abstract List<? extends R> getEstimators();

  protected abstract int getNumberOfExtraBits();

  protected abstract int computeToken(long hashValue);

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

  private static int compressedLength(byte[] data, byte[] work) {
    Deflater deflater = new Deflater(Deflater.BEST_COMPRESSION);
    deflater.setInput(data);
    deflater.finish();
    int numBytes = deflater.deflate(work);
    assertThat(numBytes).isLessThan(work.length);
    assertThat(deflater.finished()).isTrue();
    return numBytes;
  }

  @Test
  void testCompressedStorageFactors() {
    int numCycles = 100;
    long trueDistinctCount = 100000;
    int p = 12;
    long sumSize = 0;

    int bitsPerRegister = (getStateLength(p) * Byte.SIZE) / (1 << p);

    SplittableRandom random = new SplittableRandom(0L);
    T sketch = create(p);
    byte[] work = new byte[(1 << p) * 10];
    for (int i = 0; i < numCycles; ++i) {
      sketch.reset();
      for (long k = 0; k < trueDistinctCount; ++k) {
        long hash = random.nextLong();
        sketch.add(hash);
      }
      sumSize += compressedLength(sketch.getState(), work);
    }

    double expectedVariance = pow(calculateTheoreticalRelativeStandardErrorML(p), 2);

    double storageFactor = bitsPerRegister * sumSize * expectedVariance / numCycles;

    assertThat(storageFactor).isCloseTo(getCompressedStorageFactorLowerBound(), withPercentage(18));
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

  protected void testDistinctCountEstimation(
      int p,
      long seed,
      long[] distinctCounts,
      List<? extends R> estimators,
      List<IntToDoubleFunction> pToTheoreticalRelativeStandardErrorFunctions,
      double[] relativeBiasThresholds,
      double[] relativeRmseThresholds,
      double[] asymptoticThresholds,
      double relativeBiasThresholdMartingale,
      double relativeRmseThresholdMartingale,
      double asymptoticThresholdMartingale,
      R defaultEstimator) {

    int numIterations = 1000;
    SplittableRandom random = new SplittableRandom(seed);

    assertThat(estimators.size())
        .isEqualTo(relativeBiasThresholds.length)
        .isEqualTo(relativeRmseThresholds.length)
        .isEqualTo(asymptoticThresholds.length)
        .isEqualTo(pToTheoreticalRelativeStandardErrorFunctions.size());
    int numEstimators = estimators.size();

    double[] theoreticalRelativeStandardErrors = new double[numEstimators + 1];
    double[][] estimationErrorsMoment1 = new double[numEstimators + 1][];
    double[][] estimationErrorsMoment2 = new double[numEstimators + 1][];
    for (int i = 0; i < numEstimators + 1; ++i) {
      estimationErrorsMoment1[i] = new double[distinctCounts.length];
      estimationErrorsMoment2[i] = new double[distinctCounts.length];
    }

    for (int i = 0; i < numEstimators; ++i) {
      theoreticalRelativeStandardErrors[i] =
          pToTheoreticalRelativeStandardErrorFunctions.get(i).applyAsDouble(p);
    }
    theoreticalRelativeStandardErrors[numEstimators] =
        calculateTheoreticalRelativeStandardErrorMartingale(p);

    for (int i = 0; i < numIterations; ++i) {
      T sketch = create(p);
      T sketchMartingale = create(p);
      MartingaleEstimator martingaleEstimator = new MartingaleEstimator();
      long trueDistinctCount = 0;
      int distinctCountIndex = 0;
      while (distinctCountIndex < distinctCounts.length) {
        if (trueDistinctCount == distinctCounts[distinctCountIndex]) {
          for (int estimatorIdx = 0; estimatorIdx < numEstimators; ++estimatorIdx) {
            R estimator = estimators.get(estimatorIdx);
            double estimate = sketch.getDistinctCountEstimate(estimator);
            if (defaultEstimator.equals(estimator)) {
              assertThat(sketch.getDistinctCountEstimate(defaultEstimator)).isEqualTo(estimate);
            }
            double distinctCountEstimationError = estimate - trueDistinctCount;
            estimationErrorsMoment1[estimatorIdx][distinctCountIndex] +=
                distinctCountEstimationError;
            estimationErrorsMoment2[estimatorIdx][distinctCountIndex] +=
                distinctCountEstimationError * distinctCountEstimationError;
          }

          double distinctCountEstimationErrorMartingale =
              martingaleEstimator.getDistinctCountEstimate() - trueDistinctCount;
          estimationErrorsMoment1[numEstimators][distinctCountIndex] +=
              distinctCountEstimationErrorMartingale;
          estimationErrorsMoment2[numEstimators][distinctCountIndex] +=
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

      for (int estimatorIdx = 0; estimatorIdx < numEstimators; ++estimatorIdx) {
        double relativeBias =
            estimationErrorsMoment1[estimatorIdx][distinctCountIndex]
                / (trueDistinctCount
                    * (double) numIterations
                    * theoreticalRelativeStandardErrors[estimatorIdx]);
        double relativeRmse =
            Math.sqrt(estimationErrorsMoment2[estimatorIdx][distinctCountIndex] / numIterations)
                / (trueDistinctCount * theoreticalRelativeStandardErrors[estimatorIdx]);

        double relativeBiasThreshold = relativeBiasThresholds[estimatorIdx];
        double relativeRmseThreshold = relativeRmseThresholds[estimatorIdx];
        double asymptoticThreshold = asymptoticThresholds[estimatorIdx];

        if (trueDistinctCount > 0) {
          // verify bias to be significantly smaller than standard error
          assertThat(Math.abs(relativeBias)).isLessThan(relativeBiasThreshold);
        }
        if (trueDistinctCount > 0) {
          // test if observed root mean square error is not much greater than relative standard
          // error
          assertThat(relativeRmse).isLessThan(relativeRmseThreshold);
        }
        if (trueDistinctCount > 10 * (1L << p)) {
          // test asymptotic behavior (distinct count is much greater than number of registers
          // (state size) given by (1 << p)
          // observed root mean square error should be approximately equal to the standard error
          assertThat(relativeRmse).isCloseTo(1., within(asymptoticThreshold));
        }
      }

      {
        double relativeBiasMartingale =
            estimationErrorsMoment1[numEstimators][distinctCountIndex]
                / (trueDistinctCount
                    * (double) numIterations
                    * theoreticalRelativeStandardErrors[numEstimators]);
        double relativeRmseMartingale =
            Math.sqrt(estimationErrorsMoment2[numEstimators][distinctCountIndex] / numIterations)
                / (trueDistinctCount * theoreticalRelativeStandardErrors[numEstimators]);

        if (trueDistinctCount > 0) {
          // verify bias to be significantly smaller than standard error
          // System.out.println(trueDistinctCount + " " + p);
          assertThat(Math.abs(relativeBiasMartingale)).isLessThan(relativeBiasThresholdMartingale);
        }
        if (trueDistinctCount > 0) {
          // test if observed root mean square error is not much greater than relative standard
          // error
          assertThat(relativeRmseMartingale).isLessThan(relativeRmseThresholdMartingale);
        }
        if (trueDistinctCount > 10 * (1L << p)) {
          // test asymptotic behavior (distinct count is much greater than number of registers
          // (state size) given by (1 << p)
          // observed root mean square error should be approximately equal to the standard error
          assertThat(relativeRmseMartingale).isCloseTo(1., within(asymptoticThresholdMartingale));
        }
      }
    }
  }

  protected void testLargeDistinctCountEstimation(
      int p,
      long seed,
      int distinctCountStepExponent,
      long[] distinctCountSteps,
      List<? extends R> estimators,
      List<IntToDoubleFunction> pToTheoreticalRelativeStandardErrorFunctions,
      double[] relativeBiasThresholds,
      double[] relativeRmseThresholds) {

    int numIterations = 10000;
    SplittableRandom random = new SplittableRandom(seed);

    assertThat(estimators.size())
        .isEqualTo(relativeRmseThresholds.length)
        .isEqualTo(pToTheoreticalRelativeStandardErrorFunctions.size());
    int numEstimators = estimators.size();

    double[] theoreticalRelativeStandardErrors = new double[numEstimators + 1];
    double[][] estimationErrorsMoment1 = new double[numEstimators][];
    double[][] estimationErrorsMoment2 = new double[numEstimators][];
    for (int i = 0; i < numEstimators; ++i) {
      estimationErrorsMoment1[i] = new double[distinctCountSteps.length];
      estimationErrorsMoment2[i] = new double[distinctCountSteps.length];
    }

    for (int i = 0; i < numEstimators; ++i) {
      theoreticalRelativeStandardErrors[i] =
          pToTheoreticalRelativeStandardErrorFunctions.get(i).applyAsDouble(p);
    }
    theoreticalRelativeStandardErrors[numEstimators] =
        calculateTheoreticalRelativeStandardErrorMartingale(p);

    final long mask =
        ~(((1L << distinctCountStepExponent) - 1) << (-p - distinctCountStepExponent));

    for (int i = 0; i < numIterations; ++i) {
      T sketch = create(p);
      long trueDistinctCountSteps = 0;
      int distinctCountStepIndex = 0;
      while (distinctCountStepIndex < distinctCountSteps.length) {
        if (trueDistinctCountSteps == distinctCountSteps[distinctCountStepIndex]) {
          for (int estimatorIdx = 0; estimatorIdx < numEstimators; ++estimatorIdx) {
            R estimator = estimators.get(estimatorIdx);
            double estimate = sketch.getDistinctCountEstimate(estimator);
            double trueDistinctCount =
                trueDistinctCountSteps * Math.pow(2., distinctCountStepExponent);
            double distinctCountEstimationError = estimate - trueDistinctCount;
            estimationErrorsMoment1[estimatorIdx][distinctCountStepIndex] +=
                distinctCountEstimationError;
            estimationErrorsMoment2[estimatorIdx][distinctCountStepIndex] +=
                distinctCountEstimationError * distinctCountEstimationError;
          }

          distinctCountStepIndex += 1;
        }
        long hash = random.nextLong() & mask;
        sketch.add(hash);
        trueDistinctCountSteps += 1;
      }
    }

    for (int distinctCountStepIndex = 0;
        distinctCountStepIndex < distinctCountSteps.length;
        ++distinctCountStepIndex) {
      double trueDistinctCount =
          distinctCountSteps[distinctCountStepIndex] * Math.pow(2., distinctCountStepExponent);

      for (int estimatorIdx = 0; estimatorIdx < numEstimators; ++estimatorIdx) {
        double relativeBias =
            Math.abs(estimationErrorsMoment1[estimatorIdx][distinctCountStepIndex] / numIterations)
                / (trueDistinctCount * theoreticalRelativeStandardErrors[estimatorIdx]);
        double relativeRmse =
            Math.sqrt(estimationErrorsMoment2[estimatorIdx][distinctCountStepIndex] / numIterations)
                / (trueDistinctCount * theoreticalRelativeStandardErrors[estimatorIdx]);

        assertThat(relativeBias).isCloseTo(0., within(relativeBiasThresholds[estimatorIdx]));
        assertThat(relativeRmse).isCloseTo(1., within(relativeRmseThresholds[estimatorIdx]));
      }
    }
  }

  @Test
  void testEmpty() {
    for (int p = MIN_P; p <= MAX_P; ++p) {
      T sketch = create(p);
      assertThat(sketch.getDistinctCountEstimate()).isZero();
      for (R estimator : getEstimators()) {
        assertThat(estimator.estimate(sketch)).isZero();
      }
      T mergedSketch = merge(sketch, sketch);
      assertThat(mergedSketch.getState()).isEqualTo(new byte[getStateLength(p)]);
      assertThat(mergedSketch.getDistinctCountEstimate()).isZero();
      for (R estimator : getEstimators()) {
        assertThat(estimator.estimate(mergedSketch)).isZero();
      }
      assertThat(sketch.getStateChangeProbability()).isOne();
    }
  }

  @Test
  void testWrapZeros() {
    for (int p = MIN_P; p <= MAX_P; p += 1) {
      T sketch = wrap(new byte[getStateLength(p)]);
      assertThat(sketch.getDistinctCountEstimate()).isZero();
      for (R estimator : getEstimators()) {
        assertThat(estimator.estimate(sketch)).isZero();
      }
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
      for (DistinctCounter.Estimator<T> estimator : getEstimators()) {
        assertThatNoException().isThrownBy(() -> estimator.estimate(sketch1));
        assertThatNoException().isThrownBy(() -> estimator.estimate(sketch2));
      }
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
        for (R estimator : getEstimators()) {
          assertThatNoException().isThrownBy(() -> estimator.estimate(wrap(b)));
        }
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

  protected void testErrorOfDistinctCountEqualOne(
      int[] pValues,
      R estimator,
      IntToDoubleFunction pToTheoreticalRelativeStandardError,
      double[] relativeBiasLimit,
      double[] relativeRmseLimit) {

    testErrorOfDistinctCount(
        pValues,
        estimator,
        pToTheoreticalRelativeStandardError,
        relativeBiasLimit,
        relativeRmseLimit,
        this::calculateErrorOfDistinctCountEqualOne);
  }

  protected void testErrorOfDistinctCountEqualTwo(
      int[] pValues,
      R estimator,
      IntToDoubleFunction pToTheoreticalRelativeStandardError,
      double[] relativeBiasLimit,
      double[] relativeRmseLimit) {

    testErrorOfDistinctCount(
        pValues,
        estimator,
        pToTheoreticalRelativeStandardError,
        relativeBiasLimit,
        relativeRmseLimit,
        this::calculateErrorOfDistinctCountEqualTwo);
  }

  protected void testErrorOfDistinctCountEqualThree(
      int[] pValues,
      R estimator,
      IntToDoubleFunction pToTheoreticalRelativeStandardError,
      double[] relativeBiasLimit,
      double[] relativeRmseLimit) {

    testErrorOfDistinctCount(
        pValues,
        estimator,
        pToTheoreticalRelativeStandardError,
        relativeBiasLimit,
        relativeRmseLimit,
        this::calculateErrorOfDistinctCountEqualThree);
  }

  private void testErrorOfDistinctCount(
      int[] pValues,
      R estimator,
      IntToDoubleFunction pToTheoreticalRelativeStandardError,
      double[] relativeBiasLimit,
      double[] relativeRmseLimit,
      BiFunction<Integer, R, double[]> errorCalculator) {

    double[] relativeBiasValues = new double[pValues.length];
    double[] relativeRmseValues = new double[pValues.length];
    for (int i = 0; i < pValues.length; ++i) {
      double[] r = errorCalculator.apply(pValues[i], estimator);
      double bias = r[0];
      double rmse = r[1];
      double theoreticalRelativeError =
          pToTheoreticalRelativeStandardError.applyAsDouble(pValues[i]);
      relativeBiasValues[i] = bias / theoreticalRelativeError;
      relativeRmseValues[i] = rmse / theoreticalRelativeError;
    }

    DoubleUnaryOperator limitCalculator =
        x -> {
          BigDecimal bd = BigDecimal.valueOf(Math.abs(x) * 1.01);
          return bd.setScale(4, RoundingMode.UP).doubleValue();
        };
    double[] proposedBiasLimits = Arrays.stream(relativeBiasValues).map(limitCalculator).toArray();
    double[] proposedRmseLimits = Arrays.stream(relativeRmseValues).map(limitCalculator).toArray();

    String description =
        "proposed bias limits: "
            + Arrays.toString(proposedBiasLimits)
            + '\n'
            + "proposed rmse limits: "
            + Arrays.toString(proposedRmseLimits);

    assertThat(relativeBiasLimit).describedAs(description).hasSize(relativeBiasValues.length);
    for (int i = 0; i < pValues.length; ++i) {
      assertThat(relativeBiasValues[i])
          .describedAs(description)
          .isLessThanOrEqualTo(relativeBiasLimit[i]);
    }

    assertThat(relativeRmseLimit).describedAs(description).hasSize(relativeRmseValues.length);
    for (int i = 0; i < pValues.length; ++i) {
      assertThat(relativeRmseValues[i])
          .describedAs(description)
          .isLessThanOrEqualTo(relativeRmseLimit[i]);
    }
  }

  private double[] calculateErrorOfDistinctCountEqualOne(int p, R estimator) {
    T sketch = create(p);
    double sumProbability = 0;
    double averageEstimate = 0;
    double averageRmse = 0;
    double trueDistinctCount = 1;
    for (int nlz = 0; nlz < 64 - p; ++nlz) {
      long hash1 = createUpdateValue(p, 0, nlz);
      sketch.getState()[0] = 0;
      sketch.add(hash1);
      double probability = pow(0.5, nlz + 1);
      sumProbability += probability;
      double estimate = sketch.getDistinctCountEstimate(estimator);
      averageEstimate += probability * estimate;
      double error = estimate - trueDistinctCount;
      averageRmse += probability * (error * error);
    }

    sketch.getState()[0] = 0;
    double relativeBias = (averageEstimate - trueDistinctCount) / (trueDistinctCount);
    double relativeRmse = Math.sqrt(averageRmse) / (trueDistinctCount);

    assertThat(sumProbability).isCloseTo(1., Offset.offset(1e-6));
    assertThat(sketch.getState()).containsOnly((byte) 0);
    return new double[] {Math.abs(relativeBias), relativeRmse};
  }

  private double[] calculateErrorOfDistinctCountEqualTwo(int p, R estimator) {

    double[] pow_0_5 = IntStream.range(0, 129 - p).mapToDouble(i -> Math.pow(0.5, i)).toArray();

    long m = 1L << p;
    T sketch = create(p);
    double sumProbabiltiy = 0;
    double averageEstimate = 0;
    double averageRmse = 0;
    double trueDistinctCount = 2;
    for (int nlz1 = 0; nlz1 < 64 - p; ++nlz1) {
      for (int nlz2 = nlz1; nlz2 < 64 - p; ++nlz2) {
        {
          Arrays.fill(sketch.getState(), 0, 2, (byte) 0);
          sketch.add(createUpdateValue(p, 0, nlz1));
          sketch.add(createUpdateValue(p, 0, nlz2));
          double probability = pow_0_5[nlz1 + nlz2 + 2 + p];
          if (nlz1 != nlz2) probability *= 2;
          sumProbabiltiy += probability;
          double estimate = sketch.getDistinctCountEstimate(estimator);
          averageEstimate += probability * estimate;
          double error = estimate - trueDistinctCount;
          averageRmse += probability * (error * error);
        }
        {
          Arrays.fill(sketch.getState(), 0, 2, (byte) 0);
          sketch.add(createUpdateValue(p, 0, nlz1));
          sketch.add(createUpdateValue(p, 1, nlz2));
          double probability = (m - 1) * pow_0_5[nlz1 + nlz2 + 2 + p];
          if (nlz1 != nlz2) probability *= 2;
          sumProbabiltiy += probability;
          double estimate = sketch.getDistinctCountEstimate(estimator);
          averageEstimate += probability * estimate;
          double error = estimate - trueDistinctCount;
          averageRmse += probability * (error * error);
        }
      }
    }

    Arrays.fill(sketch.getState(), 0, 2, (byte) 0);
    double relativeBias = Math.abs(averageEstimate - trueDistinctCount) / trueDistinctCount;
    double relativeRmse = Math.sqrt(averageRmse) / trueDistinctCount;

    assertThat(sumProbabiltiy).isCloseTo(1., Offset.offset(1e-6));
    assertThat(sketch.getState()).containsOnly((byte) 0);

    return new double[] {relativeBias, relativeRmse};
  }

  private double[] calculateErrorOfDistinctCountEqualThree(int p, R estimator) {
    double[] pow_0_5 = IntStream.range(0, 193 - p).mapToDouble(i -> Math.pow(0.5, i)).toArray();
    long m = 1L << p;
    T sketch = create(p);
    double sumProbabiltiy = 0;
    double averageEstimate = 0;
    double averageRmse = 0;
    double trueDistinctCount = 3;
    for (int nlz1 = 0; nlz1 < 64 - p; ++nlz1) {
      for (int nlz2 = nlz1; nlz2 < 64 - p; ++nlz2) {
        for (int nlz3 = 0; nlz3 < 64 - p; ++nlz3) {
          {
            Arrays.fill(sketch.getState(), 0, 3, (byte) 0);
            sketch.add(createUpdateValue(p, 0, nlz1));
            sketch.add(createUpdateValue(p, 0, nlz2));
            sketch.add(createUpdateValue(p, 0, nlz3));
            double probability = pow_0_5[nlz1 + nlz2 + nlz3 + 3 + p + p];
            if (nlz1 != nlz2) probability *= 2;
            sumProbabiltiy += probability;
            double estimate = sketch.getDistinctCountEstimate(estimator);
            averageEstimate += probability * estimate;
            double error = estimate - trueDistinctCount;
            averageRmse += probability * (error * error);
          }
          {
            Arrays.fill(sketch.getState(), 0, 3, (byte) 0);
            sketch.add(createUpdateValue(p, 0, nlz1));
            sketch.add(createUpdateValue(p, 0, nlz2));
            sketch.add(createUpdateValue(p, 1, nlz3));
            double probability = (3 * (m - 1)) * pow_0_5[nlz1 + nlz2 + nlz3 + 3 + p + p];
            if (nlz1 != nlz2) probability *= 2;
            sumProbabiltiy += probability;
            double estimate = sketch.getDistinctCountEstimate(estimator);
            averageEstimate += probability * estimate;
            double error = estimate - trueDistinctCount;
            averageRmse += probability * (error * error);
          }
          {
            Arrays.fill(sketch.getState(), 0, 3, (byte) 0);
            sketch.add(createUpdateValue(p, 0, nlz1));
            sketch.add(createUpdateValue(p, 1, nlz2));
            sketch.add(createUpdateValue(p, 2, nlz3));
            double probability = ((m - 1) * (m - 2)) * pow_0_5[nlz1 + nlz2 + nlz3 + 3 + p + p];
            if (nlz1 != nlz2) probability *= 2;
            sumProbabiltiy += probability;
            double estimate = sketch.getDistinctCountEstimate(estimator);
            averageEstimate += probability * estimate;
            double error = estimate - trueDistinctCount;
            averageRmse += probability * (error * error);
          }
        }
      }
    }

    Arrays.fill(sketch.getState(), 0, 3, (byte) 0);
    double relativeBias = (averageEstimate - trueDistinctCount) / trueDistinctCount;
    double relativeRmse = Math.sqrt(averageRmse) / trueDistinctCount;

    assertThat(sumProbabiltiy).isCloseTo(1., Offset.offset(1e-6));
    assertThat(sketch.getState()).containsOnly((byte) 0);

    return new double[] {relativeBias, relativeRmse};
  }

  static long createUpdateValue(int p, int registerIndex, int nlz) {
    return createUpdateValue(p, registerIndex, nlz, 0L);
  }

  static long createUpdateValue(int p, int registerIndex, int nlz, long random) {
    checkArgument(nlz >= 0);
    checkArgument(nlz <= 64 - p);
    return ((long) registerIndex << -p) | ((random | 0x8000000000000000L) >>> p >>> nlz);
  }

  @Test
  void testStateChangeProbabilityForEmptySketch() {
    for (int p = MIN_P; p <= MAX_P; ++p) {
      T sketch = create(p);
      assertThat(sketch.getStateChangeProbability()).isOne();
    }
  }

  protected T createFullSketch(int p) {
    T sketch = create(p);
    for (int k = 0; k < (1 << p); ++k) {
      for (int nlz = 64 - p - getNumberOfExtraBits(); nlz <= 64 - p; ++nlz) {
        sketch.add(createUpdateValue(p, k, nlz));
      }
    }
    return sketch;
  }

  protected T createAlmostFullSketch(int p) {
    T sketch = create(p);
    for (int nlz = 63 - p - getNumberOfExtraBits(); nlz <= 63 - p; ++nlz) {
      sketch.add(createUpdateValue(p, 0, nlz));
    }
    for (int k = 1; k < (1 << p); ++k) {
      for (int nlz = 64 - p - getNumberOfExtraBits(); nlz <= 64 - p; ++nlz) {
        sketch.add(createUpdateValue(p, k, nlz));
      }
    }
    return sketch;
  }

  @Test
  void testStateChangeProbabilityForFullSketch() {
    for (int p = MIN_P; p <= MAX_P; ++p) {
      assertThat(createFullSketch(p).getStateChangeProbability()).isZero();
    }
  }

  @Test
  void testStateChangeProbabilityForAlmostFullSketch() {
    for (int p = MIN_P; p <= 12; ++p) {
      T sketch = create(p);
      for (int off = 0; off + getNumberOfExtraBits() < 64 - p; ++off) {
        for (int nlz = off; nlz <= off + getNumberOfExtraBits(); ++nlz) {
          for (int k = 0; k < (1 << p); ++k) {
            sketch.add(createUpdateValue(p, k, nlz));
          }
        }
        assertThat(sketch.getStateChangeProbability())
            .isEqualTo(Math.pow(0.5, off + getNumberOfExtraBits() + 1));
      }
    }
  }

  @Test
  void testDistinctCountEstimationFromAlmostFullSketchIsFinite() {
    for (int p = MIN_P; p <= MAX_P; ++p) {
      T sketch = createAlmostFullSketch(p);
      assertThat(sketch.getDistinctCountEstimate()).isFinite();
      for (R estimator : getEstimators()) {
        assertThat(sketch.getDistinctCountEstimate(estimator)).isFinite();
      }
    }
  }

  @Test
  void testStateChangeProbabilityForHalfFullSketch() {
    for (int p = MIN_P; p <= MAX_P; ++p) {
      T sketch = create(p);
      for (int k = 0; k < (1 << p); ++k) {
        sketch.add(createUpdateValue(p, k, 0));
      }
      assertThat(sketch.getStateChangeProbability()).isEqualTo(0.5);
    }
  }

  @Test
  void testToken() {
    SplittableRandom random = new SplittableRandom(0xe7213ba5106acddfL);
    for (int p = MIN_P; p <= 10; ++p) {
      T sketchToken = create(p);
      T sketchHash = create(p);
      T sketchTokenMartingale = create(p);
      T sketchHashMartingale = create(p);
      MartingaleEstimator tokenMartingaleEstimator = new MartingaleEstimator();
      MartingaleEstimator hashMartingaleEstimator = new MartingaleEstimator();
      for (int nlz = 0; nlz <= 64 - p; ++nlz) {
        sketchToken.reset();
        sketchHash.reset();
        sketchTokenMartingale.reset();
        sketchHashMartingale.reset();
        tokenMartingaleEstimator.reset();
        hashMartingaleEstimator.reset();
        for (int k = 0; k < (1 << p); ++k) {

          long hash = createUpdateValue(p, k, nlz, random.nextLong());
          int token = computeToken(hash);

          sketchToken.addToken(token);
          sketchHash.add(hash);
          sketchTokenMartingale.addToken(token, tokenMartingaleEstimator);
          sketchHashMartingale.add(hash, hashMartingaleEstimator);

          assertThat(computeToken(reconstructHash(token))).isEqualTo(token);
        }
        assertThat(sketchToken.getState())
            .isEqualTo(sketchHash.getState())
            .isEqualTo(sketchTokenMartingale.getState())
            .isEqualTo(sketchHashMartingale.getState());
        assertThat(tokenMartingaleEstimator.getDistinctCountEstimate())
            .isEqualTo(hashMartingaleEstimator.getDistinctCountEstimate());
        assertThat(tokenMartingaleEstimator.getStateChangeProbability())
            .isEqualTo(hashMartingaleEstimator.getStateChangeProbability());
      }
    }
  }
}
