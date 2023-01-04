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

import static com.dynatrace.hash4j.testutils.TestUtils.compareWithMaxRelativeError;
import static org.assertj.core.api.Assertions.*;
import static org.assertj.core.data.Percentage.withPercentage;

import com.dynatrace.hash4j.distinctcount.HyperLogLog.Estimator;
import com.dynatrace.hash4j.random.PseudoRandomGenerator;
import com.dynatrace.hash4j.random.PseudoRandomGeneratorProvider;
import java.util.Arrays;
import java.util.SplittableRandom;
import java.util.stream.IntStream;
import org.assertj.core.data.Offset;
import org.junit.jupiter.api.Test;

class HyperLogLogTest extends DistinctCounterTest<HyperLogLog, Estimator> {

  private static final double VARIANCE_FACTOR_RAW = 3. * Math.log(2) - 1.;

  private static final double VARIANCE_FACTOR_MARTINGALE = Math.log(2);

  // see https://www.wolframalpha.com/input?i=ln%282%29%2Fzeta%282%2C+2%29
  private static final double VARIANCE_FACTOR_ML =
      1.0747566552769260937701392307869745509357687653622079625754435936;

  @Test
  void testRelativeStandardErrorAgainstConstants() {
    double[] expected = {
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
            .mapToDouble(this::calculateTheoreticalRelativeStandardErrorRaw)
            .toArray();
    assertThat(actual)
        .usingElementComparator(compareWithMaxRelativeError(RELATIVE_ERROR))
        .isEqualTo(expected);
  }

  @Override
  protected HyperLogLog create(int p) {
    return HyperLogLog.create(p);
  }

  @Override
  protected HyperLogLog merge(HyperLogLog sketch1, HyperLogLog sketch2) {
    return HyperLogLog.merge(sketch1, sketch2);
  }

  /**
   * Visible for testing.
   *
   * <p>Returns the theoretical asymptotic (for large p and as the distinct count goes to infinity)
   * relative standard error of the distinct count estimate for a given precision parameter.
   *
   * <p>For small cardinalities (up to the order of {@code 2^p} where {@code p} is the precision
   * parameter, the relative error is usually less than this theoretical error.
   *
   * <p>The empirical root-mean square error might be slightly greater than this theoretical error,
   * especially for small precision parameters.
   *
   * @param p the precision parameter
   * @return the relative standard error
   */
  protected double calculateTheoreticalRelativeStandardErrorRaw(int p) {
    return Math.sqrt(VARIANCE_FACTOR_RAW / (1 << p));
  }

  protected double calculateTheoreticalRelativeStandardErrorDefault(int p) {
    return calculateTheoreticalRelativeStandardErrorRaw(p);
  }

  @Override
  protected double calculateTheoreticalRelativeStandardErrorML(int p) {
    return Math.sqrt(VARIANCE_FACTOR_ML / (1 << p));
  }

  @Override
  protected double calculateTheoreticalRelativeStandardErrorMartingale(int p) {
    return Math.sqrt(VARIANCE_FACTOR_MARTINGALE / (1L << p));
  }

  @Override
  protected long getCompatibilityFingerPrint() {
    return 0xac8fdde22c15315eL;
  }

  @Override
  protected int getStateLength(int p) {
    return ((1 << p) * 6) / 8;
  }

  @Override
  protected HyperLogLog wrap(byte[] state) {
    return HyperLogLog.wrap(state);
  }

  @Override
  protected double getCompressedStorageFactorLowerBound() {
    return 3.043659973416202; // TODO compute this constant directly from formula
  }

  @Override
  protected Estimator[] getEstimators() {
    return Estimator.values();
  }

  @Test
  void testSigma() {
    assertThat(Estimator.sigma(0.)).isZero();
    assertThat(Estimator.sigma(0.5)).isCloseTo(0.3907470740377903, withPercentage(1e-8));
    assertThat(Estimator.sigma(1.)).isEqualTo(Double.POSITIVE_INFINITY);
    assertThat(Estimator.sigma(Double.NEGATIVE_INFINITY)).isZero();
    assertThat(Estimator.sigma(Double.MIN_VALUE)).isCloseTo(0., Offset.offset(1e-200));
    assertThat(Estimator.sigma(Double.MAX_VALUE)).isEqualTo(Double.POSITIVE_INFINITY);
    assertThat(Estimator.sigma(-Double.MIN_VALUE)).isZero();
    assertThat(Estimator.sigma(Double.NaN)).isNaN();
    assertThat(Estimator.sigma(Math.nextDown(1.)))
        .isCloseTo(6.497362079232113E15, withPercentage(1e-8));
  }

  @Test
  void testTau() {
    assertThat(Estimator.tau(0.)).isZero();
    assertThat(Estimator.tau(0.5)).isCloseTo(0.14992949586408805, withPercentage(1e-8));
    assertThat(Estimator.tau(1.)).isEqualTo(0.);
    assertThat(Estimator.tau(Double.NEGATIVE_INFINITY)).isZero();
    assertThat(Estimator.tau(Double.MIN_VALUE))
        .isNotNegative()
        .isCloseTo(9.689758351310876E-4, within(1e-6));
    assertThat(Estimator.tau(Double.MAX_VALUE)).isNotNegative().isCloseTo(0.0, within(1e-6));
    assertThat(Estimator.tau(-Double.MIN_VALUE)).isZero();
    assertThat(Estimator.tau(Double.NaN)).isNaN();
    assertThat(Estimator.tau(Math.nextDown(1.)))
        .isCloseTo(3.700743415417188E-17, withPercentage(1e-8));
  }

  @Test
  void testDistinctCountEqualOneSmallRangeCorrectedRawEstimator() {
    testErrorOfDistinctCountEqualOne(
        new int[] {
          3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26
        },
        Estimator.SMALL_RANGE_CORRECTED_RAW_ESTIMATOR,
        this::calculateTheoreticalRelativeStandardErrorRaw,
        new double[] {
          0.1619, 0.1271, 0.0946, 0.0687, 0.0493, 0.0351, 0.025, 0.0178, 0.0128, 0.0092, 0.0068,
          0.0052, 0.0043, 0.0038, 0.0038, 0.0043, 0.0053, 0.0069, 0.0094, 0.013, 0.0181, 0.0255,
          0.0359, 0.0507
        },
        new double[] {
          0.1623, 0.1271, 0.0947, 0.0687, 0.0493, 0.0351, 0.025, 0.0178, 0.0128, 0.0092, 0.0068,
          0.0052, 0.0043, 0.0038, 0.0038, 0.0043, 0.0053, 0.0069, 0.0094, 0.013, 0.0181, 0.0255,
          0.0359, 0.0507
        });
  }

  @Test
  void testDistinctCountEqualTwoSmallRangeCorrectedRawEstimator() {
    testErrorOfDistinctCountEqualTwo(
        new int[] {3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18},
        Estimator.SMALL_RANGE_CORRECTED_RAW_ESTIMATOR,
        this::calculateTheoreticalRelativeStandardErrorRaw,
        new double[] {
          0.1511, 0.1233, 0.0933, 0.0682, 0.0491, 0.0351, 0.025, 0.0178, 0.0128, 0.0092, 0.0068,
          0.0052, 0.0043, 0.0038, 0.0038, 0.0043
        },
        new double[] {
          0.5158, 0.5019, 0.4945, 0.4904, 0.4883, 0.4872, 0.4867, 0.4864, 0.4863, 0.4862, 0.4862,
          0.4861, 0.4861, 0.4861, 0.4861, 0.4861
        });
  }

  @Test
  void testDistinctCountEqualThreeSmallRangeCorrectedRawEstimator() {
    testErrorOfDistinctCountEqualThree(
        new int[] {3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14},
        Estimator.SMALL_RANGE_CORRECTED_RAW_ESTIMATOR,
        this::calculateTheoreticalRelativeStandardErrorRaw,
        new double[] {
          0.1412, 0.1197, 0.092, 0.0677, 0.0489, 0.0349, 0.0248, 0.0176, 0.0124, 0.0087, 0.0061,
          0.0043
        },
        new double[] {
          0.6039, 0.5812, 0.5713, 0.5664, 0.5639, 0.5626, 0.562, 0.5616, 0.5615, 0.5614, 0.5614,
          0.5613
        });
  }

  @Test
  void testDistinctCountEqualOneCorrectedRawEstimator() {
    testErrorOfDistinctCountEqualOne(
        new int[] {
          3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26
        },
        Estimator.CORRECTED_RAW_ESTIMATOR,
        this::calculateTheoreticalRelativeStandardErrorRaw,
        new double[] {
          0.1619, 0.1271, 0.0946, 0.0687, 0.0493, 0.0351, 0.025, 0.0178, 0.0128, 0.0092, 0.0068,
          0.0052, 0.0043, 0.0038, 0.0038, 0.0043, 0.0053, 0.0069, 0.0094, 0.013, 0.0181, 0.0255,
          0.0359, 0.0507
        },
        new double[] {
          0.1623, 0.1271, 0.0947, 0.0687, 0.0493, 0.0351, 0.025, 0.0178, 0.0128, 0.0092, 0.0068,
          0.0052, 0.0043, 0.0038, 0.0038, 0.0043, 0.0053, 0.0069, 0.0094, 0.013, 0.0181, 0.0255,
          0.0359, 0.0507
        });
  }

  @Test
  void testDistinctCountEqualTwoCorrectedRawEstimator() {
    testErrorOfDistinctCountEqualTwo(
        new int[] {3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18},
        Estimator.CORRECTED_RAW_ESTIMATOR,
        this::calculateTheoreticalRelativeStandardErrorRaw,
        new double[] {
          0.1511, 0.1233, 0.0933, 0.0682, 0.0491, 0.0351, 0.025, 0.0178, 0.0128, 0.0092, 0.0068,
          0.0052, 0.0043, 0.0038, 0.0038, 0.0043
        },
        new double[] {
          0.5158, 0.5019, 0.4945, 0.4904, 0.4883, 0.4872, 0.4867, 0.4864, 0.4863, 0.4862, 0.4862,
          0.4861, 0.4861, 0.4861, 0.4861, 0.4861
        });
  }

  @Test
  void testDistinctCountEqualThreeCorrectedRawEstimator() {
    testErrorOfDistinctCountEqualThree(
        new int[] {3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14},
        Estimator.CORRECTED_RAW_ESTIMATOR,
        this::calculateTheoreticalRelativeStandardErrorRaw,
        new double[] {
          0.1412, 0.1197, 0.092, 0.0677, 0.0489, 0.0349, 0.0248, 0.0176, 0.0124, 0.0087, 0.0061,
          0.0043
        },
        new double[] {
          0.6039, 0.5812, 0.5713, 0.5664, 0.5639, 0.5626, 0.562, 0.5616, 0.5615, 0.5614, 0.5614,
          0.5613
        });
  }

  @Test
  void testDistinctCountEqualOneMaximumLikelihoodEstimator() {
    testErrorOfDistinctCountEqualOne(
        new int[] {
          3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26
        },
        Estimator.MAXIMUM_LIKELIHOOD_ESTIMATOR,
        this::calculateTheoreticalRelativeStandardErrorML,
        new double[] {
          0.1881, 0.1272, 0.088, 0.0616, 0.0433, 0.0306, 0.0216, 0.0153, 0.0108, 0.0077, 0.0054,
          0.0039, 0.0027, 0.002, 0.0014, 0.001, 7.0E-4, 5.0E-4, 4.0E-4, 3.0E-4, 2.0E-4, 2.0E-4,
          1.0E-4, 1.0E-4
        },
        new double[] {
          0.2159, 0.145, 0.1001, 0.0699, 0.0492, 0.0347, 0.0245, 0.0173, 0.0123, 0.0087, 0.0062,
          0.0044, 0.0031, 0.0022, 0.0016, 0.0011, 8.0E-4, 6.0E-4, 4.0E-4, 3.0E-4, 2.0E-4, 2.0E-4,
          1.0E-4, 1.0E-4
        });
  }

  @Test
  void testDistinctCountEqualTwoMaximumLikelihoodEstimator() {

    testErrorOfDistinctCountEqualTwo(
        new int[] {3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18},
        Estimator.MAXIMUM_LIKELIHOOD_ESTIMATOR,
        this::calculateTheoreticalRelativeStandardErrorML,
        new double[] {
          0.1983, 0.1303, 0.089, 0.0619, 0.0435, 0.0306, 0.0216, 0.0153, 0.0108, 0.0077, 0.0054,
          0.0039, 0.0027, 0.002, 0.0014, 0.001
        },
        new double[] {
          0.6064, 0.5415, 0.5132, 0.4999, 0.4935, 0.4903, 0.4887, 0.488, 0.4876, 0.4874, 0.4873,
          0.4872, 0.4872, 0.4872, 0.4872, 0.4872
        });
  }

  @Test
  void testDistinctCountEqualThreeMaximumLikelihoodEstimator() {
    testErrorOfDistinctCountEqualThree(
        new int[] {3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14},
        Estimator.MAXIMUM_LIKELIHOOD_ESTIMATOR,
        this::calculateTheoreticalRelativeStandardErrorML,
        new double[] {
          0.2087, 0.1334, 0.0901, 0.0623, 0.0436, 0.0307, 0.0216, 0.0153, 0.0108, 0.0077, 0.0054,
          0.0039
        },
        new double[] {
          0.7067, 0.6252, 0.5919, 0.5768, 0.5696, 0.566, 0.5643, 0.5634, 0.563, 0.5627, 0.5626,
          0.5626
        });
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
    for (int p = MIN_P; p <= MAX_P; ++p) {
      int maxNumIterations =
          p + 7; // p + 6 should be enough, +1 to encounter potential platform dependencies
      int m = 1 << p;
      assertThat(sigmaIterations(1. / m)).isLessThanOrEqualTo(maxNumIterations);
      assertThat(sigmaIterations((m - 1.) / m)).isLessThanOrEqualTo(maxNumIterations);
    }
  }

  private static final double RELATIVE_ERROR = 1e-10;

  private double calculateEstimationFactor(int p) {
    double m = 1 << p;
    return m * m / (2. * Math.log(2) * (1. + VARIANCE_FACTOR_RAW / m));
  }

  @Test
  void testEstimationFactors() {
    double[] expectedEstimationFactors = new double[MAX_P - MIN_P + 1];
    for (int p = MIN_P; p <= MAX_P; ++p) {
      expectedEstimationFactors[p - MIN_P] = calculateEstimationFactor(p);
    }
    assertThat(Estimator.getEstimationFactors())
        .usingElementComparator(compareWithMaxRelativeError(RELATIVE_ERROR))
        .isEqualTo(expectedEstimationFactors);
  }

  @Test
  void testCalculateP() {
    for (int p = MIN_P; p <= MAX_P; ++p) {
      int stateLength = ((1 << p) * 6) / 8;
      assertThat(HyperLogLog.calculateP(stateLength)).isEqualTo(p);
    }
  }

  @Test
  void testCreateFromUltraLogLog() {
    PseudoRandomGenerator pseudoRandomGenerator =
        PseudoRandomGeneratorProvider.splitMix64_V1().create();
    long[] cardinalities = {1, 10, 100, 1000, 10000, 100000};
    int numCycles = 10;
    for (int p = MIN_P; p <= MAX_P; ++p) {
      for (long cardinality : cardinalities) {
        for (int i = 0; i < numCycles; ++i) {
          HyperLogLog hllSketch = HyperLogLog.create(p);
          UltraLogLog ullSketch = UltraLogLog.create(p);
          for (long c = 0; c < cardinality; ++c) {
            long hash = pseudoRandomGenerator.nextLong();
            hllSketch.add(hash);
            ullSketch.add(hash);
          }
          assertThat(HyperLogLog.create(ullSketch).getState()).isEqualTo(hllSketch.getState());
        }
      }
    }
  }

  @Test
  void testDistinctCountEstimation() {
    int maxP = 14;
    long[] distinctCounts = TestUtils.getDistinctCountValues(0, 100000, 0.2);
    SplittableRandom random = new SplittableRandom(0xd77b9e4ea99553e0L);
    for (int p = MIN_P; p <= maxP; ++p) {
      testDistinctCountEstimation(
          p,
          random.nextLong(),
          distinctCounts,
          Arrays.asList(
              Estimator.SMALL_RANGE_CORRECTED_RAW_ESTIMATOR,
              Estimator.CORRECTED_RAW_ESTIMATOR,
              Estimator.MAXIMUM_LIKELIHOOD_ESTIMATOR),
          Arrays.asList(
              this::calculateTheoreticalRelativeStandardErrorRaw,
              this::calculateTheoreticalRelativeStandardErrorRaw,
              this::calculateTheoreticalRelativeStandardErrorML),
          new double[] {0.2, 0.2, 0.5},
          new double[] {1.3, 1.3, 1.5},
          new double[] {0.3, 0.3, 0.5},
          0.1,
          1.2,
          0.15,
          Estimator.SMALL_RANGE_CORRECTED_RAW_ESTIMATOR);
    }
  }

  @Test
  void testLargeDistinctCountEstimation() {
    long[] distinctCountSteps = {1L << 16};
    SplittableRandom random = new SplittableRandom(0xd77b9e4ea99553e0L);
    testLargeDistinctCountEstimation(
        10,
        random.nextLong(),
        48,
        distinctCountSteps,
        Arrays.asList(Estimator.MAXIMUM_LIKELIHOOD_ESTIMATOR, Estimator.CORRECTED_RAW_ESTIMATOR),
        Arrays.asList(
            this::calculateTheoreticalRelativeStandardErrorML,
            this::calculateTheoreticalRelativeStandardErrorRaw),
        new double[] {0.06, 0.06},
        new double[] {0.15, 0.15});
  }

  @Test
  void testStateChangeProbabilityForAlmostFullSketch() {
    for (int p = MIN_P; p <= MAX_P; ++p) {
      HyperLogLog sketch = create(p);
      for (long k = 0; k < (1 << p); ++k) {
        sketch.add((k << -p) | 1);
      }
      assertThat(sketch.getStateChangeProbability()).isEqualTo(Math.pow(0.5, 64 - p));
    }
  }
}
