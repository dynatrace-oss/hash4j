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
import static com.dynatrace.hash4j.testutils.TestUtils.compareWithMaxRelativeError;
import static java.lang.Math.exp;
import static java.lang.Math.pow;
import static org.assertj.core.api.Assertions.*;
import static org.assertj.core.data.Percentage.withPercentage;
import static org.hipparchus.special.Gamma.gamma;

import com.dynatrace.hash4j.distinctcount.UltraLogLog.AbstractSmallRangeCorrectedGraEstimator;
import java.util.Arrays;
import java.util.List;
import java.util.SplittableRandom;
import java.util.stream.IntStream;
import org.assertj.core.data.Offset;
import org.hipparchus.optim.MaxEval;
import org.hipparchus.optim.nonlinear.scalar.GoalType;
import org.hipparchus.optim.univariate.*;
import org.junit.jupiter.api.Test;

class UltraLogLogTest extends DistinctCounterTest<UltraLogLog, UltraLogLog.Estimator> {

  private static final double VARIANCE_FACTOR_GRA =
      calculateVarianceFactorGra(AbstractSmallRangeCorrectedGraEstimator.TAU);

  // see https://www.wolframalpha.com/input?i=ln%282%29%2Fzeta%282%2C+5%2F4%29
  private static final double VARIANCE_FACTOR_ML =
      0.5789111356311075471022417636427943448393849646878849257018607764;

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
            .mapToDouble(this::calculateTheoreticalRelativeStandardErrorGRA)
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
  void testXi() {
    assertThat(AbstractSmallRangeCorrectedGraEstimator.xi(2, 0.)).isZero();
    assertThat(AbstractSmallRangeCorrectedGraEstimator.xi(2, 0.5))
        .isCloseTo(1.2814941480755806, withPercentage(1e-8));
    assertThat(AbstractSmallRangeCorrectedGraEstimator.xi(2, 1.))
        .isEqualTo(Double.POSITIVE_INFINITY);
    assertThat(AbstractSmallRangeCorrectedGraEstimator.xi(2, Double.NEGATIVE_INFINITY)).isZero();
    assertThat(AbstractSmallRangeCorrectedGraEstimator.xi(2, Double.MIN_VALUE))
        .isCloseTo(0., Offset.offset(1e-200));
    assertThat(AbstractSmallRangeCorrectedGraEstimator.xi(2, Double.MAX_VALUE))
        .isEqualTo(Double.POSITIVE_INFINITY);
    assertThat(AbstractSmallRangeCorrectedGraEstimator.xi(2, -Double.MIN_VALUE)).isZero();
    assertThat(AbstractSmallRangeCorrectedGraEstimator.xi(2, Double.NaN)).isNaN();
    assertThat(AbstractSmallRangeCorrectedGraEstimator.xi(Double.NaN, 0.5)).isNaN();
    assertThat(AbstractSmallRangeCorrectedGraEstimator.xi(Double.NaN, Double.NaN)).isNaN();
    assertThat(AbstractSmallRangeCorrectedGraEstimator.xi(2, Math.nextDown(1.)))
        .isCloseTo(1.2994724158464226E16, withPercentage(1e-8));
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
    int gamma = ((beta + alpha) << 1) + ((h0 + h2) << 2);
    return SmallRangeCorrected4GraEstimator.calculateZ(m, alpha, beta, gamma);
  }

  @Test
  void testXiIterations() {

    double x = pow(2., AbstractSmallRangeCorrectedGraEstimator.TAU);

    assertThat(xiIterations(x, 0.)).isZero();
    assertThat(xiIterations(x, Double.POSITIVE_INFINITY)).isZero();
    assertThat(xiIterations(x, 1.)).isZero();
    for (int p = 1; p <= 53; ++p) {
      assertThat(xiIterations(x, Math.pow(1. - Math.pow(2., -p), 5)))
          .isLessThanOrEqualTo(
              p + 4); // p + 3 should be enough, +1 more to encounter potential platform
      // dependencies
    }
    for (int p = 54; p <= 60; ++p) {
      assertThat(xiIterations(x, Math.pow(1. - Math.pow(2., -p), 5))).isLessThanOrEqualTo(1);
    }
    assertThat(xiIterations(x, Math.nextUp(0.))).isEqualTo(1);
    for (int p = MIN_P; p <= MAX_P; ++p) {
      int maxNumIterations =
          p + 4; // p + 3 should be enough, +1 more to encounter potential platform dependencies
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

  private static double calculateVarianceFactorGra(double tau) {
    double gamma2tauP1 = gamma(2 * tau + 1);
    double gammaTauP1 = gamma(tau + 1);
    double sum =
        0.5
            + exp(-tau * Math.log(4)) / Math.expm1(tau * Math.log(2))
            + exp(-tau * Math.log(3.92))
            + exp(-tau * Math.log(5.76));

    sum *= Math.log(2) * gamma2tauP1 * tau / (gammaTauP1 * gammaTauP1);
    sum -= 1;
    sum /= (tau * tau);
    return sum;
  }

  @Test
  void testOptimalTau() {
    UnivariateObjectiveFunction f =
        new UnivariateObjectiveFunction(UltraLogLogTest::calculateVarianceFactorGra);
    final MaxEval maxEval = new MaxEval(Integer.MAX_VALUE);
    UnivariateOptimizer optimizer = new BrentOptimizer(2 * Math.ulp(1d), Double.MIN_VALUE);
    SearchInterval searchInterval = new SearchInterval(0, 5, 1);
    UnivariatePointValuePair result =
        optimizer.optimize(GoalType.MINIMIZE, f, searchInterval, maxEval);

    double optimalTau = result.getPoint();

    assertThat(AbstractSmallRangeCorrectedGraEstimator.TAU)
        .usingComparator(compareWithMaxRelativeError(RELATIVE_ERROR))
        .isEqualTo(optimalTau);
  }

  @Test
  void testRegisterContributions() {
    double[] expectedContributions = new double[252 - 4 * MIN_P];

    final double kappa1 = Math.pow(2., AbstractSmallRangeCorrectedGraEstimator.TAU);
    final double kappa2 =
        1.
            / (Math.pow(8, AbstractSmallRangeCorrectedGraEstimator.TAU)
                - Math.pow(4, AbstractSmallRangeCorrectedGraEstimator.TAU));
    final double kappa3 = kappa2 + 1. / kappa1;

    for (int k = 0; k < 252 - 4 * MIN_P; ++k) {
      int k0 = k & 1;
      int k1 = (k & 2) >>> 1;
      int k765432 = k >>> 2;
      expectedContributions[k] =
          (1 - k0 + (1 - k1) * kappa3 + k1 * kappa2)
              * pow(2., -AbstractSmallRangeCorrectedGraEstimator.TAU * (1 + k765432));
    }

    assertThat(AbstractSmallRangeCorrectedGraEstimator.REGISTER_CONTRIBUTIONS)
        .usingElementComparator(compareWithMaxRelativeError(RELATIVE_ERROR))
        .isEqualTo(expectedContributions);
  }

  private static double calculateEstimationFactor(int p) {
    int m = 1 << p;
    double biasCorrectionFactor =
        1.
            / (1.
                + VARIANCE_FACTOR_GRA
                    * (1. + AbstractSmallRangeCorrectedGraEstimator.TAU)
                    / (2. * m));
    return biasCorrectionFactor
        * ((4. * m) / 5.)
        * Math.pow(
            m * gamma(AbstractSmallRangeCorrectedGraEstimator.TAU) / Math.log(2),
            1. / AbstractSmallRangeCorrectedGraEstimator.TAU);
  }

  @Test
  void testEstimationFactors() {
    double[] expectedEstimationFactors = new double[MAX_P - MIN_P + 1];
    for (int p = MIN_P; p <= MAX_P; ++p) {
      expectedEstimationFactors[p - MIN_P] = calculateEstimationFactor(p);
    }
    assertThat(AbstractSmallRangeCorrectedGraEstimator.ESTIMATION_FACTORS)
        .usingElementComparator(compareWithMaxRelativeError(RELATIVE_ERROR))
        .isEqualTo(expectedEstimationFactors);
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
  protected double calculateTheoreticalRelativeStandardErrorGRA(int p) {
    return Math.sqrt(VARIANCE_FACTOR_GRA / (1 << p));
  }

  protected double calculateTheoreticalRelativeStandardErrorDefault(int p) {
    return calculateTheoreticalRelativeStandardErrorGRA(p);
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
    return 2.3121675172273321507391374981099550309731319926228541209892337122;
  }

  @Override
  protected List<UltraLogLog.Estimator> getEstimators() {
    return Arrays.asList(
        MAXIMUM_LIKELIHOOD_ESTIMATOR,
        SMALL_RANGE_CORRECTED_1_GRA_ESTIMATOR,
        SMALL_RANGE_CORRECTED_4_GRA_ESTIMATOR);
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
              SMALL_RANGE_CORRECTED_1_GRA_ESTIMATOR,
              SMALL_RANGE_CORRECTED_4_GRA_ESTIMATOR,
              MAXIMUM_LIKELIHOOD_ESTIMATOR),
          Arrays.asList(
              this::calculateTheoreticalRelativeStandardErrorGRA,
              this::calculateTheoreticalRelativeStandardErrorGRA,
              this::calculateTheoreticalRelativeStandardErrorML),
          new double[] {0.2, 0.2, 0.3},
          new double[] {1.3, 1.3, 1.2},
          new double[] {0.3, 0.3, 0.2},
          0.1,
          1.2,
          0.15,
          SMALL_RANGE_CORRECTED_4_GRA_ESTIMATOR);
    }
  }

  @Test
  void testDistinctCountEqualOneGRA1Estimator() {
    testErrorOfDistinctCountEqualOne(
        new int[] {
          3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26
        },
        SMALL_RANGE_CORRECTED_1_GRA_ESTIMATOR,
        this::calculateTheoreticalRelativeStandardErrorGRA,
        new double[] {
          9.0E-4, 0.0068, 0.0071, 0.0058, 0.0044, 0.0031, 0.0022, 0.0014, 9.0E-4, 4.0E-4, 2.0E-4,
          7.0E-4, 0.0012, 0.0019, 0.0028, 0.004, 0.0058, 0.0082, 0.0116, 0.0164, 0.0232, 0.0327,
          0.0463, 0.0654
        },
        new double[] {
          0.0816, 0.0375, 0.0176, 0.009, 0.0053, 0.0034, 0.0022, 0.0015, 9.0E-4, 4.0E-4, 2.0E-4,
          7.0E-4, 0.0012, 0.0019, 0.0028, 0.004, 0.0058, 0.0082, 0.0116, 0.0164, 0.0232, 0.0327,
          0.0463, 0.0654
        });
  }

  @Test
  void testDistinctCountEqualTwoGRA1Estimator() {
    testErrorOfDistinctCountEqualTwo(
        new int[] {3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18},
        SMALL_RANGE_CORRECTED_1_GRA_ESTIMATOR,
        this::calculateTheoreticalRelativeStandardErrorGRA,
        new double[] {
          0.0032, 0.0045, 0.006, 0.0053, 0.0042, 0.0031, 0.0022, 0.0014, 9.0E-4, 4.0E-4, 2.0E-4,
          7.0E-4, 0.0012, 0.0019, 0.0028, 0.004
        },
        new double[] {
          0.6958, 0.665, 0.6528, 0.6476, 0.6453, 0.6441, 0.6435, 0.6433, 0.6431, 0.643, 0.643,
          0.643, 0.643, 0.643, 0.643, 0.643
        });
  }

  @Test
  void testDistinctCountEqualThreeGRA1Estimator() {
    testErrorOfDistinctCountEqualThree(
        new int[] {3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14},
        SMALL_RANGE_CORRECTED_1_GRA_ESTIMATOR,
        this::calculateTheoreticalRelativeStandardErrorGRA,
        new double[] {
          0.0035, 0.0036, 0.0053, 0.0051, 0.0042, 0.0032, 0.0025, 0.0019, 0.0015, 0.0013, 0.0012,
          0.0013
        },
        new double[] {
          0.8003, 0.771, 0.756, 0.7492, 0.7458, 0.7441, 0.7433, 0.7428, 0.7426, 0.7425, 0.7425,
          0.7424
        });
  }

  @Test
  void testDistinctCountEqualOneGRA4Estimator() {
    testErrorOfDistinctCountEqualOne(
        new int[] {
          3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26
        },
        SMALL_RANGE_CORRECTED_4_GRA_ESTIMATOR,
        this::calculateTheoreticalRelativeStandardErrorGRA,
        new double[] {
          0.0495, 0.0441, 0.0344, 0.0255, 0.0184, 0.0131, 0.0093, 0.0065, 0.0044, 0.0029, 0.0017,
          7.0E-4, 3.0E-4, 0.0013, 0.0023, 0.0037, 0.0055, 0.008, 0.0115, 0.0163, 0.0231, 0.0327,
          0.0463, 0.0654
        },
        new double[] {
          0.2046, 0.1294, 0.0853, 0.0578, 0.0399, 0.0277, 0.0194, 0.0136, 0.0095, 0.0066, 0.0045,
          0.0031, 0.0022, 0.002, 0.0026, 0.0038, 0.0056, 0.008, 0.0115, 0.0163, 0.0231, 0.0327,
          0.0463, 0.0654
        });
  }

  @Test
  void testDistinctCountEqualTwoGRA4Estimator() {
    testErrorOfDistinctCountEqualTwo(
        new int[] {3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18},
        SMALL_RANGE_CORRECTED_4_GRA_ESTIMATOR,
        this::calculateTheoreticalRelativeStandardErrorGRA,
        new double[] {
          0.0425, 0.0403, 0.0329, 0.0249, 0.0182, 0.0131, 0.0092, 0.0065, 0.0044, 0.0029, 0.0017,
          7.0E-4, 3.0E-4, 0.0013, 0.0023, 0.0037
        },
        new double[] {
          0.6445, 0.5914, 0.5706, 0.5627, 0.5595, 0.5581, 0.5574, 0.5571, 0.557, 0.5569, 0.5569,
          0.5568, 0.5568, 0.5568, 0.5568, 0.5568
        });
  }

  @Test
  void testDistinctCountEqualThreeGRA4Estimator() {
    testErrorOfDistinctCountEqualThree(
        new int[] {3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14},
        SMALL_RANGE_CORRECTED_4_GRA_ESTIMATOR,
        this::calculateTheoreticalRelativeStandardErrorGRA,
        new double[] {
          0.0418, 0.038, 0.0317, 0.0245, 0.0182, 0.0132, 0.0095, 0.0069, 0.0051, 0.0038, 0.003,
          0.0025
        },
        new double[] {
          0.7409, 0.6868, 0.6607, 0.6503, 0.6462, 0.6444, 0.6437, 0.6433, 0.6431, 0.643, 0.643,
          0.643
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
      for (long k = 0; k < (1 << p); ++k) {
        sketch.add((k << -p) | 1);
        sketch.add((k << -p) | 2);
        sketch.add((k << -p) | 4);
      }
      assertThat(sketch.getState()).containsOnly((byte) 251);
      assertThat(sketch.getStateChangeProbability()).isEqualTo(Math.pow(0.5, 64 - p));
    }
  }

  @Test
  void testStateChangeProbabilityForAlmostFullSketch252() {
    for (int p = MIN_P; p <= 16; ++p) {
      UltraLogLog sketch = create(p);
      for (long k = 0; k < (1 << p); ++k) {
        sketch.add((k << -p) | 0);
      }
      assertThat(sketch.getState()).containsOnly((byte) 252);
      assertThat(sketch.getStateChangeProbability())
          .isEqualTo(Math.pow(0.5, 62 - p) + Math.pow(0.5, 63 - p));
    }
  }

  @Test
  void testStateChangeProbabilityForAlmostFullSketch253() {
    for (int p = MIN_P; p <= 16; ++p) {
      UltraLogLog sketch = create(p);
      for (long k = 0; k < (1 << p); ++k) {
        sketch.add((k << -p) | 0);
        sketch.add((k << -p) | 2);
      }
      assertThat(sketch.getState()).containsOnly((byte) 253);
      assertThat(sketch.getStateChangeProbability()).isEqualTo(Math.pow(0.5, 63 - p));
    }
  }

  @Test
  void testStateChangeProbabilityForAlmostFullSketch254() {
    for (int p = MIN_P; p <= 16; ++p) {
      UltraLogLog sketch = create(p);
      for (long k = 0; k < (1 << p); ++k) {
        sketch.add((k << -p) | 0);
        sketch.add((k << -p) | 1);
      }
      assertThat(sketch.getState()).containsOnly((byte) 254);
      assertThat(sketch.getStateChangeProbability()).isEqualTo(Math.pow(0.5, 62 - p));
    }
  }

  @Test
  void testStateChangeProbabilityForAlmostFullSketch255() {
    for (int p = MIN_P; p <= 16; ++p) {
      UltraLogLog sketch = create(p);
      for (long k = 0; k < (1 << p); ++k) {
        sketch.add((k << -p) | 0);
        sketch.add((k << -p) | 1);
        sketch.add((k << -p) | 2);
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
        Arrays.asList(MAXIMUM_LIKELIHOOD_ESTIMATOR),
        Arrays.asList(this::calculateTheoreticalRelativeStandardErrorML),
        new double[] {0.06},
        new double[] {0.15});
  }
}
