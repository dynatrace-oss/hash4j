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

import static com.dynatrace.hash4j.distinctcount.UltraLogLog.*;
import static com.dynatrace.hash4j.distinctcount.UltraLogLog.OptimalFGRAEstimator.ETA_0;
import static com.dynatrace.hash4j.testutils.TestUtils.compareWithMaxRelativeError;
import static java.lang.Math.*;
import static java.lang.Math.sqrt;
import static org.assertj.core.api.Assertions.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.SplittableRandom;
import java.util.stream.IntStream;
import org.hipparchus.optim.MaxEval;
import org.hipparchus.optim.nonlinear.scalar.GoalType;
import org.hipparchus.optim.univariate.*;
import org.hipparchus.special.Gamma;
import org.junit.jupiter.api.Test;

class UltraLogLogTest extends DistinctCounterTest<UltraLogLog, UltraLogLog.Estimator> {

  // see https://www.wolframalpha.com/input?i=ln%282%29%2Fzeta%282%2C+5%2F4%29
  private static final double VARIANCE_FACTOR_ML =
      0.5789111356311075471022417636427943448393849646878849257018607764;

  @Test
  void testRelativeStandardErrorOfOptimalFGRAEstimatorAgainstConstants() {
    double[] expected = {
      0.276562187784647,
      0.19555899840231122,
      0.1382810938923235,
      0.09777949920115561,
      0.06914054694616174,
      0.048889749600577806,
      0.03457027347308087,
      0.024444874800288903,
      0.017285136736540436,
      0.012222437400144451,
      0.008642568368270218,
      0.006111218700072226,
      0.004321284184135109,
      0.003055609350036113,
      0.0021606420920675545,
      0.0015278046750180564,
      0.0010803210460337772,
      7.639023375090282E-4,
      5.401605230168886E-4,
      3.819511687545141E-4,
      2.700802615084443E-4,
      1.9097558437725705E-4,
      1.3504013075422215E-4,
      9.548779218862853E-5
    };
    double[] actual =
        IntStream.range(MIN_P, MAX_P + 1)
            .mapToDouble(OptimalFGRAEstimator::calculateTheoreticalRelativeStandardError)
            .toArray();
    assertThat(actual)
        .usingElementComparator(compareWithMaxRelativeError(RELATIVE_ERROR))
        .isEqualTo(expected);
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
  void testUpdateValues() {
    for (int p = MIN_P; p <= MAX_P; ++p) {
      for (int i = p; i <= 64; ++i) {
        long hash = 0xFFFFFFFFFFFFFFFFL >>> 1 >>> (i - 1);
        int nlz = Long.numberOfLeadingZeros(~((~hash) << p));
        assertThat(i - p + 1).isEqualTo(nlz + 1);
        assertThat(((UltraLogLog.create(p).add(hash).getState()[0] & 0xFF) >>> 2) + 2 - p)
            .isEqualTo(nlz + 1);
      }
    }
  }

  @Override
  protected UltraLogLog create(int p) {
    return UltraLogLog.create(p);
  }

  @Override
  protected UltraLogLog merge(UltraLogLog sketch1, UltraLogLog sketch2) {
    return UltraLogLog.merge(sketch1, sketch2);
  }

  /**
   * Returns the theoretical asymptotic (for large p and as the distinct count goes to infinity)
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
  protected double calculateTheoreticalRelativeStandardErrorDefault(int p) {
    return OptimalFGRAEstimator.calculateTheoreticalRelativeStandardError(p);
  }

  @Override
  protected double calculateTheoreticalRelativeStandardErrorML(int p) {
    return Math.sqrt(VARIANCE_FACTOR_ML / (1 << p));
  }

  @Override
  protected double calculateTheoreticalRelativeStandardErrorMartingale(int p) {
    return Math.sqrt(Math.log(2.) * 5. / (8. * (1L << p)));
  }

  @Override
  protected long getCompatibilityFingerPrint() {
    return 0xfc124320345bd3b9L;
  }

  @Override
  protected int getStateLength(int p) {
    return 1 << p;
  }

  @Override
  protected UltraLogLog wrap(byte[] state) {
    return UltraLogLog.wrap(state);
  }

  @Override
  protected double getCompressedStorageFactorLowerBound() {
    // = (4/5 + int_0^1 z^(-3/4) (1-z)*ln(1-z)/ln(z) dz) / (ln(2) * zeta(2,5/4))
    // where zeta denotes the Hurvitz zeta function,
    // see https://en.wikipedia.org/wiki/Hurwitz_zeta_function
    //
    // for a numerical evaluation see
    // https://www.wolframalpha.com/input?i=%284%2F5+%2B+int_0%5E1+z%5E%28-3%2F4%29+%281-z%29*ln%281-z%29%2Fln%28z%29+dz%29+%2F+%28ln%282%29+*+zeta%282%2C5%2F4%29%29
    return 2.312167517227332;
  }

  @Override
  protected int getNumberOfExtraBits() {
    return 2;
  }

  @Override
  protected int computeToken(long hashValue) {
    return UltraLogLog.computeToken(hashValue);
  }

  @Override
  protected List<UltraLogLog.Estimator> getEstimators() {
    return Arrays.asList(MAXIMUM_LIKELIHOOD_ESTIMATOR, OPTIMAL_FGRA_ESTIMATOR);
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
          Arrays.asList(MAXIMUM_LIKELIHOOD_ESTIMATOR, OPTIMAL_FGRA_ESTIMATOR),
          Arrays.asList(
              this::calculateTheoreticalRelativeStandardErrorML,
              OptimalFGRAEstimator::calculateTheoreticalRelativeStandardError),
          new double[] {0.2, 0.2},
          new double[] {1.2, 1.2},
          new double[] {0.2, 0.2},
          0.1,
          1.2,
          0.15,
          OPTIMAL_FGRA_ESTIMATOR);
    }
  }

  @Test
  void testDistinctCountEqualOneOptimalFGRAEstimator() {
    testErrorOfDistinctCountEqualOne(
        new int[] {
          3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26
        },
        OPTIMAL_FGRA_ESTIMATOR,
        OptimalFGRAEstimator::calculateTheoreticalRelativeStandardError,
        new double[] {
          0.0569, 0.0492, 0.0381, 0.0281, 0.0203, 0.0145, 0.0102, 0.0072, 0.005, 0.0034, 0.0022,
          0.0012, 4.0E-4, 5.0E-4, 0.0014, 0.0024, 0.0037, 0.0054, 0.0077, 0.011, 0.0157, 0.0222,
          0.0314, 0.0445
        },
        new double[] {
          0.1997, 0.1278, 0.0852, 0.0583, 0.0404, 0.0283, 0.0198, 0.0139, 0.0098, 0.0068, 0.0047,
          0.0032, 0.0022, 0.0016, 0.0017, 0.0025, 0.0037, 0.0054, 0.0077, 0.011, 0.0157, 0.0222,
          0.0314, 0.0445
        });
  }

  @Test
  void testDistinctCountEqualTwoOptimalFGRAEstimator() {
    testErrorOfDistinctCountEqualTwo(
        new int[] {3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18},
        OPTIMAL_FGRA_ESTIMATOR,
        OptimalFGRAEstimator::calculateTheoreticalRelativeStandardError,
        new double[] {
          0.0506, 0.0456, 0.0366, 0.0275, 0.0201, 0.0144, 0.0102, 0.0072, 0.005, 0.0034, 0.0021,
          0.0012, 4.0E-4, 5.0E-4, 0.0014, 0.0024
        },
        new double[] {
          0.6399, 0.5914, 0.5722, 0.5647, 0.5617, 0.5603, 0.5597, 0.5594, 0.5593, 0.5592, 0.5592,
          0.5592, 0.5592, 0.5591, 0.5591, 0.5592
        });
  }

  @Test
  void testDistinctCountEqualThreeOptimalFGRAEstimator() {
    testErrorOfDistinctCountEqualThree(
        new int[] {3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14},
        OPTIMAL_FGRA_ESTIMATOR,
        OptimalFGRAEstimator::calculateTheoreticalRelativeStandardError,
        new double[] {
          0.0505, 0.0434, 0.0354, 0.0271, 0.02, 0.0144, 0.0104, 0.0075, 0.0054, 0.0039, 0.003,
          0.0023
        },
        new double[] {
          0.7334, 0.6861, 0.6622, 0.6526, 0.6487, 0.6471, 0.6463, 0.646, 0.6458, 0.6457, 0.6457,
          0.6456
        });
  }

  @Test
  void testDistinctCountEqualOneMLEstimator() {
    testErrorOfDistinctCountEqualOne(
        new int[] {
          3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26
        },
        MAXIMUM_LIKELIHOOD_ESTIMATOR,
        this::calculateTheoreticalRelativeStandardErrorML,
        new double[] {
          0.0959, 0.0723, 0.0528, 0.0379, 0.027, 0.0192, 0.0136, 0.0096, 0.0068, 0.0048, 0.0034,
          0.0025, 0.0017, 0.0013, 9.0E-4, 7.0E-4, 5.0E-4, 4.0E-4, 3.0E-4, 2.0E-4, 2.0E-4, 1.0E-4,
          1.0E-4, 1.0E-4
        },
        new double[] {
          0.1344, 0.0969, 0.0693, 0.0493, 0.035, 0.0248, 0.0176, 0.0124, 0.0088, 0.0062, 0.0044,
          0.0031, 0.0022, 0.0016, 0.0011, 8.0E-4, 6.0E-4, 4.0E-4, 3.0E-4, 2.0E-4, 2.0E-4, 1.0E-4,
          1.0E-4, 1.0E-4
        });
  }

  @Test
  void testDistinctCountEqualTwoMLEstimator() {
    testErrorOfDistinctCountEqualTwo(
        new int[] {3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18},
        MAXIMUM_LIKELIHOOD_ESTIMATOR,
        this::calculateTheoreticalRelativeStandardErrorML,
        new double[] {
          0.093, 0.0713, 0.0524, 0.0378, 0.027, 0.0192, 0.0136, 0.0096, 0.0068, 0.0048, 0.0034,
          0.0025, 0.0017, 0.0013, 9.0E-4, 7.0E-4
        },
        new double[] {
          0.4925, 0.4806, 0.4749, 0.4721, 0.4708, 0.4701, 0.4697, 0.4695, 0.4695, 0.4694, 0.4694,
          0.4694, 0.4694, 0.4694, 0.4694, 0.4694
        });
  }

  @Test
  void testDistinctCountEqualThreeMLEstimator() {
    testErrorOfDistinctCountEqualThree(
        new int[] {3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14},
        MAXIMUM_LIKELIHOOD_ESTIMATOR,
        this::calculateTheoreticalRelativeStandardErrorML,
        new double[] {
          0.0901, 0.0704, 0.0521, 0.0377, 0.0269, 0.0192, 0.0136, 0.0096, 0.0068, 0.0048, 0.0034,
          0.0025
        },
        new double[] {
          0.5685, 0.5542, 0.5479, 0.5449, 0.5434, 0.5427, 0.5423, 0.5422, 0.5421, 0.542, 0.542,
          0.542
        });
  }

  @Test
  void testStateChangeProbabilityForAlmostFullSketch251() {
    for (int p = MIN_P; p <= 16; ++p) {
      UltraLogLog sketch = create(p);
      for (int k = 0; k < (1 << p); ++k) {
        sketch.add(createUpdateValue(p, k, 63 - p));
        sketch.add(createUpdateValue(p, k, 62 - p));
        sketch.add(createUpdateValue(p, k, 61 - p));
      }
      assertThat(sketch.getState()).containsOnly((byte) 251);
      assertThat(sketch.getStateChangeProbability()).isEqualTo(Math.pow(0.5, 64 - p));
    }
  }

  @Test
  void testStateChangeProbabilityForAlmostFullSketch252() {
    for (int p = MIN_P; p <= 16; ++p) {
      UltraLogLog sketch = create(p);
      for (int k = 0; k < (1 << p); ++k) {
        sketch.add(createUpdateValue(p, k, 64 - p));
      }
      assertThat(sketch.getState()).containsOnly((byte) 252);
      assertThat(sketch.getStateChangeProbability())
          .isEqualTo(Math.pow(0.5, 63 - p) + Math.pow(0.5, 64 - p));
    }
  }

  @Test
  void testStateChangeProbabilityForAlmostFullSketch253() {
    for (int p = MIN_P; p <= 16; ++p) {
      UltraLogLog sketch = create(p);
      for (int k = 0; k < (1 << p); ++k) {
        sketch.add(createUpdateValue(p, k, 64 - p));
        sketch.add(createUpdateValue(p, k, 62 - p));
      }
      assertThat(sketch.getState()).containsOnly((byte) 253);
      assertThat(sketch.getStateChangeProbability()).isEqualTo(Math.pow(0.5, 64 - p));
    }
  }

  @Test
  void testStateChangeProbabilityForAlmostFullSketch254() {
    for (int p = MIN_P; p <= 16; ++p) {
      UltraLogLog sketch = create(p);
      for (int k = 0; k < (1 << p); ++k) {
        sketch.add(createUpdateValue(p, k, 64 - p));
        sketch.add(createUpdateValue(p, k, 63 - p));
      }
      assertThat(sketch.getState()).containsOnly((byte) 254);
      assertThat(sketch.getStateChangeProbability()).isEqualTo(Math.pow(0.5, 63 - p));
    }
  }

  @Test
  void testStateChangeProbabilityForAlmostFullSketch255() {
    for (int p = MIN_P; p <= 16; ++p) {
      UltraLogLog sketch = create(p);
      for (int k = 0; k < (1 << p); ++k) {
        sketch.add(createUpdateValue(p, k, 64 - p));
        sketch.add(createUpdateValue(p, k, 63 - p));
        sketch.add(createUpdateValue(p, k, 62 - p));
      }
      assertThat(sketch.getState()).containsOnly((byte) 255);
      assertThat(sketch.getStateChangeProbability()).isZero();
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
        Arrays.asList(MAXIMUM_LIKELIHOOD_ESTIMATOR, OPTIMAL_FGRA_ESTIMATOR),
        Arrays.asList(
            this::calculateTheoreticalRelativeStandardErrorML,
            OptimalFGRAEstimator::calculateTheoreticalRelativeStandardError),
        new double[] {0.06, 0.06},
        new double[] {0.15, 0.15});
  }

  @Test
  void testDistinctCountEstimationFromFullSketch() {
    for (int p = MIN_P; p <= MAX_P; ++p) {
      for (UltraLogLog.Estimator estimator :
          Arrays.asList(MAXIMUM_LIKELIHOOD_ESTIMATOR, OPTIMAL_FGRA_ESTIMATOR)) {
        UltraLogLog sketch = createFullSketch(p);
        assertThat(sketch.getDistinctCountEstimate(estimator)).isInfinite();
      }
    }
  }

  private static double omega0(double tau) {
    return pow(7, -tau) - pow(8, -tau);
  }

  private static double omega1(double tau) {
    return pow(3, -tau) - pow(4, -tau) - pow(7, -tau) + pow(8, -tau);
  }

  private static double omega2(double tau) {
    return pow(5, -tau) - pow(6, -tau) - pow(7, -tau) + pow(8, -tau);
  }

  private static double omega3(double tau) {
    return 1
        - pow(2, -tau)
        - pow(3, -tau)
        + pow(4, -tau)
        - pow(5, -tau)
        + pow(6, -tau)
        + pow(7, -tau)
        - pow(8, -tau);
  }

  private static double calculateEtaPreFactor(double tau) {
    return (Math.log(2) / Gamma.gamma(tau))
        / (pow(omega0(tau), 2) / omega0(2 * tau)
            + pow(omega1(tau), 2) / omega1(2 * tau)
            + pow(omega2(tau), 2) / omega2(2 * tau)
            + pow(omega3(tau), 2) / omega3(2 * tau));
  }

  private static double calculateEta0(double tau) {
    return calculateEtaPreFactor(tau) * (omega0(tau) / omega0(2 * tau));
  }

  private static double calculateEta1(double tau) {
    return calculateEtaPreFactor(tau) * (omega1(tau) / omega1(2 * tau));
  }

  private static double calculateEta2(double tau) {
    return calculateEtaPreFactor(tau) * (omega2(tau) / omega2(2 * tau));
  }

  private static double calculateEta3(double tau) {
    return calculateEtaPreFactor(tau) * (omega3(tau) / omega3(2 * tau));
  }

  private static double calculateVarianceFactor(double tau) {
    return (calculateEtaPreFactor(tau) * (Gamma.gamma(2 * tau) / Gamma.gamma(tau)) - 1)
        / pow(tau, 2);
  }

  private double[] determineZTestValuesForPhi(int p) {
    int m = 1 << p;
    List<Double> list = new ArrayList<>();

    for (int h0 = 0; h0 <= 2; ++h0) {
      for (int h1 = 0; h1 <= 2; ++h1) {
        for (int h2 = 0; h2 <= 2; ++h2) {
          int h3 = m - h0 - h1 - h2;
          list.add(OptimalFGRAEstimator.largeRangeEstimate(h0, h1, h2, h3, m));
        }
      }
    }

    for (int h0 = 0; h0 <= 2; ++h0) {
      for (int h1 = 0; h1 <= 2; ++h1) {
        for (int h2 = 0; h2 <= 2; ++h2) {
          for (int h3 = 0; h3 <= 2; ++h3) {
            list.add(OptimalFGRAEstimator.largeRangeEstimate(h0, h1, h2, h3, m));
          }
        }
      }
    }

    return list.stream().mapToDouble(Double::doubleValue).toArray();
  }

  private double[] determineZTestValuesForSigma(int p) {
    int m = 1 << p;
    List<Double> list = new ArrayList<>();

    for (int h1 = 0; h1 <= 2; ++h1) {
      for (int h2 = 0; h2 <= 2; ++h2) {
        for (int h3 = 0; h3 <= 2; ++h3) {
          int h0 = m - h1 - h2 - h3;
          list.add(OptimalFGRAEstimator.smallRangeEstimate(h0, h1, h2, h3, m));
        }
      }
    }

    for (int h0 = 0; h0 <= 2; ++h0) {
      for (int h1 = 0; h1 <= 2; ++h1) {
        for (int h2 = 0; h2 <= 2; ++h2) {
          for (int h3 = 0; h3 <= 2; ++h3) {
            list.add(OptimalFGRAEstimator.smallRangeEstimate(h0, h1, h2, h3, m));
          }
        }
      }
    }

    return list.stream().mapToDouble(Double::doubleValue).toArray();
  }

  private static int optimalFGRAEstimatorPhiNumTermsUntilConvergence(double z) {
    int numTerms = 0;
    double expectedResult = OptimalFGRAEstimator.phi(z, z * z);
    if (z <= 0.) {
      assertThat(0.).isEqualTo(expectedResult);
      return numTerms;
    }
    if (z >= 1.) {
      assertThat(
              ETA_0 / (OptimalFGRAEstimator.POW_2_TAU * (2. * OptimalFGRAEstimator.POW_2_TAU - 1)))
          .isEqualTo(expectedResult);
      return numTerms;
    }
    double previousPowZ = z * z;
    double powZ = z;
    double nextPowZ = sqrt(powZ);
    double p =
        (OptimalFGRAEstimator.ETA_X
                * (OptimalFGRAEstimator.POW_4_MINUS_TAU
                    / (2 - OptimalFGRAEstimator.POW_2_MINUS_TAU)))
            / (1. + nextPowZ);
    double ps = OptimalFGRAEstimator.psiPrime(powZ, previousPowZ);
    double s = nextPowZ * (ps + ps) * p;
    while (true) {
      previousPowZ = powZ;
      powZ = nextPowZ;
      double oldS = s;
      nextPowZ = sqrt(powZ);
      double nextPs = OptimalFGRAEstimator.psiPrime(powZ, previousPowZ);
      p *= OptimalFGRAEstimator.POW_2_MINUS_TAU / (1. + nextPowZ);
      s += nextPowZ * ((nextPs + nextPs) - (powZ + nextPowZ) * ps) * p;
      numTerms += 1;
      if (!(s > oldS)) {
        assertThat(s).isEqualTo(expectedResult);
        return numTerms;
      }
      ps = nextPs;
    }
  }

  @Test
  void testOptimalFGRAEstimatorPhiConvergence() {
    assertThat(optimalFGRAEstimatorPhiNumTermsUntilConvergence(0.)).isZero();
    assertThat(optimalFGRAEstimatorPhiNumTermsUntilConvergence(1.)).isZero();
    assertThat(optimalFGRAEstimatorPhiNumTermsUntilConvergence(Math.nextDown(1.))).isEqualTo(2);
    assertThat(optimalFGRAEstimatorPhiNumTermsUntilConvergence(Double.MIN_VALUE)).isEqualTo(30);

    for (int p = MIN_P; p <= MAX_P; ++p) {
      double[] zValues = determineZTestValuesForPhi(p);
      for (double z : zValues) {
        assertThat(optimalFGRAEstimatorPhiNumTermsUntilConvergence(sqrt(z)))
            .isLessThanOrEqualTo(22);
      }
    }
  }

  private static int optimalFGRAEstimatorSigmaNumTermsUntilConvergence(double z) {
    int numTerms = 0;
    double expectedResult = OptimalFGRAEstimator.sigma(z);

    if (z <= 0.) {
      assertThat(OptimalFGRAEstimator.ETA_3).isEqualTo(expectedResult);
      return numTerms;
    }
    if (z >= 1.) {
      assertThat(Double.POSITIVE_INFINITY).isEqualTo(expectedResult);
      return numTerms;
    }

    double powZ = z;
    double s = 0;
    double powTau = OptimalFGRAEstimator.ETA_X;
    while (true) {
      double oldS = s;
      double nextPowZ = powZ * powZ;
      s +=
          powTau * (powZ - nextPowZ) * OptimalFGRAEstimator.psiPrime(nextPowZ, nextPowZ * nextPowZ);
      numTerms += 1;
      if (!(s > oldS)) {
        assertThat(s / z).isEqualTo(expectedResult);
        return numTerms;
      }
      powZ = nextPowZ;
      powTau *= OptimalFGRAEstimator.POW_2_TAU;
    }
  }

  @Test
  void testOptimalFGRAEstimatorSigmaConvergence() {

    assertThat(optimalFGRAEstimatorSigmaNumTermsUntilConvergence(0.)).isZero();
    assertThat(optimalFGRAEstimatorSigmaNumTermsUntilConvergence(1.)).isZero();
    assertThat(optimalFGRAEstimatorSigmaNumTermsUntilConvergence(Math.nextDown(1.))).isEqualTo(60);
    assertThat(optimalFGRAEstimatorSigmaNumTermsUntilConvergence(Double.MIN_VALUE)).isEqualTo(2);

    for (int p = MIN_P; p <= MAX_P; ++p) {
      double[] zValues = determineZTestValuesForSigma(p);
      for (double z : zValues) {
        assertThat(optimalFGRAEstimatorSigmaNumTermsUntilConvergence(z)).isLessThanOrEqualTo(7 + p);
      }
    }
  }

  @Test
  void testOptimalFGRAEstimatorEta0() {
    assertThat(ETA_0)
        .usingComparator(compareWithMaxRelativeError(RELATIVE_ERROR))
        .isEqualTo(calculateEta0(OptimalFGRAEstimator.TAU));
  }

  @Test
  void testOptimalFGRAEstimatorEta1() {
    assertThat(OptimalFGRAEstimator.ETA_1)
        .usingComparator(compareWithMaxRelativeError(RELATIVE_ERROR))
        .isEqualTo(calculateEta1(OptimalFGRAEstimator.TAU));
  }

  @Test
  void testOptimalFGRAEstimatorEta2() {
    assertThat(OptimalFGRAEstimator.ETA_2)
        .usingComparator(compareWithMaxRelativeError(RELATIVE_ERROR))
        .isEqualTo(calculateEta2(OptimalFGRAEstimator.TAU));
  }

  @Test
  void testOptimalFGRAEstimatorEta3() {
    assertThat(OptimalFGRAEstimator.ETA_3)
        .usingComparator(compareWithMaxRelativeError(RELATIVE_ERROR))
        .isEqualTo(calculateEta3(OptimalFGRAEstimator.TAU));
  }

  @Test
  void testOptimalFGRAEstimatorVarianceFactor() {
    assertThat(OptimalFGRAEstimator.V)
        .usingComparator(compareWithMaxRelativeError(RELATIVE_ERROR))
        .isEqualTo(calculateVarianceFactor(OptimalFGRAEstimator.TAU));
  }

  @Test
  void testOptimalFGRAEstimatorTau() {
    UnivariateObjectiveFunction f =
        new UnivariateObjectiveFunction(UltraLogLogTest::calculateVarianceFactor);
    final MaxEval maxEval = new MaxEval(Integer.MAX_VALUE);
    UnivariateOptimizer optimizer = new BrentOptimizer(2. * Math.ulp(1d), Double.MIN_VALUE);
    SearchInterval searchInterval = new SearchInterval(0, 5, 1);
    UnivariatePointValuePair result =
        optimizer.optimize(GoalType.MINIMIZE, f, searchInterval, maxEval);

    double optimalTau = result.getPoint();

    assertThat(OptimalFGRAEstimator.TAU)
        .usingComparator(compareWithMaxRelativeError(RELATIVE_ERROR))
        .isEqualTo(optimalTau);
  }

  private static double calculateOptimalFGRARegisterContribution(int r) {
    int q10 = r & 3;
    int q765432 = (r + 12) >>> 2;
    final double f;
    if (q10 == 0) {
      f = OptimalFGRAEstimator.ETA_0;
    } else if (q10 == 1) {
      f = OptimalFGRAEstimator.ETA_1;
    } else if (q10 == 2) {
      f = OptimalFGRAEstimator.ETA_2;
    } else {
      f = OptimalFGRAEstimator.ETA_3;
    }
    return f * pow(2., -OptimalFGRAEstimator.TAU * q765432);
  }

  @Test
  void testOptimalFGRAEstimatorRegisterContributions() {
    assertThat(OptimalFGRAEstimator.REGISTER_CONTRIBUTIONS)
        .isEqualTo(
            IntStream.range(0, 236)
                .mapToDouble(r -> calculateOptimalFGRARegisterContribution(r))
                .toArray());
  }

  private static double calculateEstimationFactor(int p) {
    int m = 1 << p;
    double biasCorrectionFactor =
        1. / (1. + OptimalFGRAEstimator.V * (1. + OptimalFGRAEstimator.TAU) / (2. * m));
    return biasCorrectionFactor * m * Math.pow(m, 1. / OptimalFGRAEstimator.TAU);
  }

  @Test
  void testOptimalFGRAEstimatorEstimationFactors() {
    assertThat(OptimalFGRAEstimator.ESTIMATION_FACTORS)
        .isEqualTo(
            IntStream.range(MIN_P, MAX_P + 1)
                .mapToDouble(p -> calculateEstimationFactor(p))
                .toArray());
  }
}
